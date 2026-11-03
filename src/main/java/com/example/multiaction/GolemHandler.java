package com.example.multiaction;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class GolemHandler {

    /*
     * Запоминаем последнее вырезание тыквы в каждом мире,
     * чтобы связать появление голема с действием игрока.
     */
    private static final Map<ServerLevel, PumpkinUse> PENDING =
            new WeakHashMap<>();

    private static final ThreadLocal<Boolean> CREATING_COPIES =
            ThreadLocal.withInitial(() -> false);

    private GolemHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPumpkinCarved(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!event.getItemStack().is(Items.SHEARS)) {
            return;
        }

        BlockPos pos = event.getPos();

        if (!level.getBlockState(pos).is(Blocks.PUMPKIN)) {
            return;
        }

        PENDING.put(
                level,
                new PumpkinUse(player, pos.immutable(), level.getGameTime())
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGolemJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled()
                || event.loadedFromDisk()
                || CREATING_COPIES.get()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity original = event.getEntity();

        if (!(original instanceof IronGolem)
                && !(original instanceof SnowGolem)) {
            return;
        }

        PumpkinUse use = PENDING.get(level);

        if (use == null || use.tick() != level.getGameTime()) {
            return;
        }

        // Голем должен появиться рядом с вырезанной тыквой.
        if (original.distanceToSqr(
                use.pos().getX() + 0.5D,
                use.pos().getY() + 0.5D,
                use.pos().getZ() + 0.5D
        ) > 16.0D) {
            return;
        }

        PENDING.remove(level);

        ServerPlayer player = use.player();
        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            // Обычного голема оставляем: конструкция уже расходуется игрой.
            MultiplierDisplay.show(player, 0);

            player.displayClientMessage(
                    Component.literal(
                            "§c✘ Создание дополнительных големов сорвалось!"
                    ),
                    true
            );

            return;
        }

        if (multiplier == 1) {
            return;
        }

        int created = 0;
        CREATING_COPIES.set(true);

        try {
            for (int i = 1; i < multiplier; i++) {
                Entity copy = original.getType().create(level);

                if (copy == null) {
                    continue;
                }

                double x = original.getX()
                        + (player.getRandom().nextDouble() - 0.5D) * 2.0D;
                double z = original.getZ()
                        + (player.getRandom().nextDouble() - 0.5D) * 2.0D;

                copy.moveTo(
                        x,
                        original.getY(),
                        z,
                        original.getYRot(),
                        original.getXRot()
                );

                if (level.addFreshEntity(copy)) {
                    created++;
                }
            }
        } finally {
            CREATING_COPIES.remove();
        }

        if (created > 0) {
            int total = created + 1;

            MultiplierDisplay.show(player, total);

            player.displayClientMessage(
                    Component.literal(
                            "§6✦ Мультиголемы: §e" + total
                                    + " §7(дополнительно: " + created + ")"
                    ),
                    true
            );
        }
    }

    private record PumpkinUse(ServerPlayer player, BlockPos pos, long tick) {
    }
}
