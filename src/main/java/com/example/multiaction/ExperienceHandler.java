package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class ExperienceHandler {

    private ExperienceHandler() {
    }

    @SubscribeEvent
    public static void onExperienceChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int originalXp = event.getAmount();

        // Не изменяем нулевой или отрицательный опыт.
        if (originalXp <= 0) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            event.setAmount(0);

            MultiplierDisplay.show(player, 0);

            player.displayClientMessage(
                    Component.literal("§c✘ Получение опыта сорвалось!"),
                    true
            );

            return;
        }

        if (multiplier == 1) {
            return;
        }

        // Используем long, чтобы избежать переполнения при умножении.
        long multipliedXp = (long) originalXp * multiplier;

        event.setAmount(
                (int) Math.min(multipliedXp, Integer.MAX_VALUE)
        );

        MultiplierDisplay.show(player, multiplier);

        player.displayClientMessage(
                Component.literal("§a✦ Мультиопыт: x" + multiplier),
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
