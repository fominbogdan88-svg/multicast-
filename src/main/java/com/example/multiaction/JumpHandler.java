package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class JumpHandler {

    // Редкий шанс: 1 из 1000 прыжков.
    private static final int CHANCE = 1000;

    private JumpHandler() {
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getRandom().nextInt(CHANCE) != 0) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier <= 1) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();

        /*
         * Дополнительный вертикальный импульс.
         * Горизонтальная скорость сохраняется.
         */
        double extraJump = (multiplier - 1) * 0.08D;

        player.setDeltaMovement(
                motion.x,
                motion.y + extraJump,
                motion.z
        );

        // Синхронизация изменения скорости с клиентом.
        player.hurtMarked = true;

        MultiplierDisplay.show(player, multiplier);

        player.displayClientMessage(
                Component.literal("§b✦ Мультипрыжок: x" + multiplier),
                true
        );
    }
}
