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

        // Даже если выпало x16, для наковальни максимум x2.
        int multiplier = Math.min(MulticastManager.roll(player), 2);

        // При x0 и x1 остаётся только обычный результат.
        if (multiplier <= 1) {
            return;
        }

        // Оригинал выдаёт наковальня.
        ItemStack extra = result.copy();

        player.getInventory().add(extra);

        // Если копия не поместилась, она выпадет рядом.
        if (!extra.isEmpty()) {
            player.drop(extra, false);
        }

        // Надпись видна другим игрокам над головой.
        MultiplierDisplay.show(player, 2);

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
