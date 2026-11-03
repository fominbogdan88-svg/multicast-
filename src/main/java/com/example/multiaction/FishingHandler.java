package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class FishingHandler {

    private FishingHandler() {
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        List<ItemStack> drops = event.getDrops();

        // Если улова нет, не выполняем бросок.
        if (drops.isEmpty()) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            drops.clear();

            MultiplierDisplay.show(player, 0);
            showResult(player, 0);
            return;
        }

        if (multiplier == 1) {
            return;
        }

        List<ItemStack> multipliedDrops = new ArrayList<>();

        for (ItemStack original : drops) {
            if (original.isEmpty()) {
                continue;
            }

            long remaining = (long) original.getCount() * multiplier;
            int maxStackSize = original.getMaxStackSize();

            // Разделяем результат на допустимые стаки.
            while (remaining > 0) {
                int count = (int) Math.min(remaining, maxStackSize);

                ItemStack copy = original.copy();
                copy.setCount(count);

                multipliedDrops.add(copy);
                remaining -= count;
            }
        }

        drops.clear();
        drops.addAll(multipliedDrops);

        MultiplierDisplay.show(player, multiplier);
        showResult(player, multiplier);
    }

    private static void showResult(
            ServerPlayer player,
            int multiplier
    ) {
        String message = multiplier == 0
                ? "§c✘ Рыбалка: улов сорвался!"
                : "§e✦ Мультиулов: x" + multiplier;

        player.displayClientMessage(Component.literal(message), true);

        player.playNotifySound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.25F,
                multiplier == 0 ? 0.8F : 1.4F
        );
    }
}
