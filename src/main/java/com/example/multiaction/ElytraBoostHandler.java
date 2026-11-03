package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class ElytraBoostHandler {

    // Ограничение дополнительного импульса за одно использование.
    private static final double MAX_EXTRA_BOOST = 1.2D;

    private ElytraBoostHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.isFallFlying()) {
            return;
        }

        if (!event.getItemStack().is(Items.FIREWORK_ROCKET)) {
            return;
        }

        /*
         * Если в основной руке есть ракета, обрабатываем именно её.
         * Это помогает избежать двух бросков при использовании двух рук.
         */
        if (event.getHand() == InteractionHand.OFF_HAND
                && player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier <= 1) {
            if (multiplier == 0) {
                player.displayClientMessage(
                        Component.literal("§7Дополнительное ускорение сорвалось"),
                        true
                );
            }
            return;
        }

        double boost = Math.min(
                MAX_EXTRA_BOOST,
                (multiplier - 1) * 0.08D
        );

        Vec3 direction = player.getLookAngle();
        player.setDeltaMovement(
                player.getDeltaMovement().add(direction.scale(boost))
        );

        // Помечаем изменение скорости для синхронизации с клиентом.
        player.hurtMarked = true;

        player.displayClientMessage(
                Component.literal("§b✦ Мультиускорение: x" + multiplier),
                true
        );

        player.playNotifySound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.2F,
                1.4F
        );
    }
}
