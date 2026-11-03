package com.example.multiaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class LingeringPotionHandler {

    /*
     * Временно запоминаем попадание зелья удачи. Minecraft создаёт
     * облако после обработки столкновения.
     */
    private static final Map<UUID, PendingCloud> PENDING = new HashMap<>();

    /*
     * Для каждого отмеченного облака запоминаем игроков,
     * которые уже получили рулетку.
     */
    private static final Map<UUID, Set<UUID>> CLOUD_PLAYERS =
            new HashMap<>();

    private LingeringPotionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionImpact(ProjectileImpactEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getProjectile() instanceof ThrownPotion potion)) {
            return;
        }

        if (!(potion.level() instanceof ServerLevel level)) {
            return;
        }

        if (!(potion.getOwner() instanceof ServerPlayer thrower)) {
            return;
        }

        ItemStack originalStack = potion.getItem();

        if (!originalStack.is(Items.LINGERING_POTION)) {
            return;
        }

        List<MobEffectInstance> effects =
                PotionUtils.getMobEffects(originalStack);

        if (effects.isEmpty()) {
            return;
        }

        boolean luckPotion =
                PotionUtils.getPotion(originalStack) == Potions.LUCK;

        int multiplier = MulticastManager.roll(thrower);

        if (multiplier == 0) {
            event.setCanceled(true);

            level.levelEvent(
                    2002,
                    potion.blockPosition(),
                    PotionUtils.getColor(originalStack)
            );

            potion.discard();

            MultiplierDisplay.show(thrower, 0);
            thrower.displayClientMessage(
                    Component.literal("§c✘ Оседающее зелье: срыв!"),
                    true
            );
            return;
        }

        if (luckPotion) {
            PENDING.put(
                    thrower.getUUID(),
                    new PendingCloud(
                            level,
                            level.getGameTime(),
                            potion.getX(),
                            potion.getY(),
                            potion.getZ()
                    )
            );
        }

        if (multiplier == 1) {
            return;
        }

        int originalColor = PotionUtils.getColor(originalStack);
        ItemStack scaledStack = originalStack.copy();

        CompoundTag tag = scaledStack.getOrCreateTag();
        ListTag scaledEffects = new ListTag();

        for (MobEffectInstance effect : effects) {
            MobEffectInstance scaled =
                    PotionEffectScaling.scale(effect, multiplier);

            scaledEffects.add(scaled.save(new CompoundTag()));
        }

        /*
         * Переносим эффекты в пользовательский список, чтобы базовые
         * эффекты зелья не добавились второй раз.
         */
        tag.putString("Potion", "minecraft:empty");
        tag.put("CustomPotionEffects", scaledEffects);
        tag.putInt("CustomPotionColor", originalColor);

        potion.setItem(scaledStack);

        MultiplierDisplay.show(thrower, multiplier);
        thrower.displayClientMessage(
                Component.literal(
                        "§d✦ Оседающее мультизелье: x" + multiplier
                ),
                true
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCloudJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || event.loadedFromDisk()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getEntity() instanceof AreaEffectCloud cloud)) {
            return;
        }

        if (!(cloud.getOwner() instanceof ServerPlayer owner)) {
            return;
        }

        PendingCloud pending = PENDING.get(owner.getUUID());

        if (pending == null
                || pending.level() != level
                || pending.tick() != level.getGameTime()) {
            return;
        }

        double dx = cloud.getX() - pending.x();
        double dy = cloud.getY() - pending.y();
        double dz = cloud.getZ() - pending.z();

        // Облако должно появиться рядом с местом попадания зелья.
        if (dx * dx + dy * dy + dz * dz > 4.0D) {
            return;
        }

        PENDING.remove(owner.getUUID());
        CLOUD_PLAYERS.put(cloud.getUUID(), new HashSet<>());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        // Удаляем незавершённые записи после тика попадания.
        PENDING.entrySet().removeIf(entry ->
                entry.getValue().level() == level
                        && entry.getValue().tick() < level.getGameTime()
        );

        Iterator<Map.Entry<UUID, Set<UUID>>> iterator =
                CLOUD_PLAYERS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Set<UUID>> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());

            /*
             * Запись может относиться к другому измерению.
             * Её очистим при остановке сервера или когда обработаем
             * облако в соответствующем измерении.
             */
            if (entity == null) {
                continue;
            }

            if (!(entity instanceof AreaEffectCloud cloud)
                    || cloud.isRemoved()) {
                iterator.remove();
                continue;
            }

            /*
             * У обычного облака оседающего зелья есть короткая задержка
             * перед действием. Проверяем игроков раз в пять тиков.
             */
            if (cloud.tickCount < 10 || cloud.tickCount % 5 != 0) {
                continue;
            }

            float radius = cloud.getRadius();

            if (radius <= 0.0F) {
                continue;
            }

            AABB area = cloud.getBoundingBox();
            Set<UUID> alreadyAffected = entry.getValue();

            for (ServerPlayer player : level.getEntitiesOfClass(
                    ServerPlayer.class,
                    area
            )) {
                if (!player.isAlive()
                        || player.isSpectator()
                        || alreadyAffected.contains(player.getUUID())) {
                    continue;
                }

                double dx = player.getX() - cloud.getX();
                double dz = player.getZ() - cloud.getZ();

                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }

                alreadyAffected.add(player.getUUID());

                int rouletteResult =
                        MulticastManager.rollForPotion(player);

                MulticastManager.applyPotionMultiplier(
                        player,
                        rouletteResult
                );

                ModNetwork.sendRoulette(player, rouletteResult);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        CLOUD_PLAYERS.clear();
    }

    private record PendingCloud(
            ServerLevel level,
            long tick,
            double x,
            double y,
            double z
    ) {
    }
}
