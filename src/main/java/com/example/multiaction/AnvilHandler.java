package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class AnvilHandler {

    private AnvilHandler() {
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack result = event.getOutput();

        if (result.isEmpty()) {
            return;
        }

        // Ковка ограничена x2, даже если выпал множитель выше.
        int multiplier = Math.min(MulticastManager.roll(player), 2);

        // При x0 и x1 остаётся только обычный результат наковальни.
        if (multiplier <= 1) {
            return;
        }

        ItemStack extra = result.copy();

        // Если копия не поместилась в инвентарь, она выпадет рядом.
        player.getInventory().add(extra);

        if (!extra.isEmpty()) {
            player.drop(extra, false);
        }

        player.displayClientMessage(
                Component.literal("§6✦ Мультиковка: x2"),
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
