package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class SmeltingHandler {

    private SmeltingHandler() {
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack result = event.getSmelting();

        if (result.isEmpty()) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier <= 1) {
            return;
        }

        // Оригинальный результат уже выдаётся обычной механикой.
        long remaining = (long) result.getCount() * (multiplier - 1);
        int maxStackSize = result.getMaxStackSize();

        while (remaining > 0) {
            int count = (int) Math.min(remaining, maxStackSize);

            ItemStack extra = result.copy();
            extra.setCount(count);

            player.getInventory().add(extra);

            // Если инвентарь заполнен, остаток выпадет рядом.
            if (!extra.isEmpty()) {
                player.drop(extra, false);
            }

            remaining -= count;
        }

        player.displayClientMessage(
                Component.literal("§6✦ Мультиплавка: x" + multiplier),
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
