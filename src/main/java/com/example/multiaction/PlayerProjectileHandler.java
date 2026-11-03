package com.example.multiaction;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class PlayerProjectileHandler {

    private static final ThreadLocal<Boolean> CREATING_COPIES =
            ThreadLocal.withInitial(() -> false);

    private PlayerProjectileHandler() {
    }

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (event.isCanceled()
                || event.loadedFromDisk()
                || CREATING_COPIES.get()) {
            return;
        }

        if (!(event.getEntity() instanceof Projectile original)) {
            return;
        }

        boolean arrow = original instanceof AbstractArrow;
        boolean rocket = original instanceof FireworkRocketEntity;

        // Поплавки и другие снаряды здесь не копируются.
        if (!arrow && !rocket) {
            return;
        }

        if (!(original.getOwner() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag originalData = new CompoundTag();
        original.saveWithoutId(originalData);

        // Пропускаем обычные ракеты и ракеты для элитр.
        if (rocket && !originalData.getBoolean("ShotAtAngle")) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            event.setCanceled(true);
            MultiplierDisplay.show(player, 0);

            player.displayClientMessage(
                    Component.literal("§c✘ Выстрел сорвался!"),
                    true
            );

            return;
        }

        if (multiplier == 1) {
            return;
        }

        int createdCount = 0;
        Vec3 originalMotion = original.getDeltaMovement();

        CREATING_COPIES.set(true);

        try {
            for (int i = 1; i < multiplier; i++) {
                Entity created = original.getType().create(level);

                if (!(created instanceof Projectile copy)) {
                    continue;
                }

                CompoundTag copyData = originalData.copy();

                // Каждая копия должна получить уникальный UUID.
                copyData.remove("UUID");
                copy.load(copyData);
                copy.setUUID(UUID.randomUUID());

                copy.setOwner(player);
                copy.setPos(
                        original.getX(),
                        original.getY(),
                        original.getZ()
                );

                /*
                 * Чередуем небольшой поворот влево и вправо.
                 * Копии не летят строго по одной линии.
                 */
                int step = (i + 1) / 2;
                float angle = (float) Math.toRadians(
                        Math.min(step * 3.0D, 24.0D)
                                * (i % 2 == 0 ? -1.0D : 1.0D)
                );

                copy.setDeltaMovement(originalMotion.yRot(angle));

                // Дополнительные стрелы и трезубцы нельзя подобрать
                // в режиме выживания.
                if (copy instanceof AbstractArrow arrowCopy) {
                    arrowCopy.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }

                if (level.addFreshEntity(copy)) {
                    createdCount++;
                }
            }
        } finally {
            CREATING_COPIES.remove();
        }

        if (createdCount > 0) {
            int total = createdCount + 1;

            MultiplierDisplay.show(player, total);

            player.displayClientMessage(
                    Component.literal("§e✦ Мультиснаряды: " + total),
                    true
            );
        }
    }
}
