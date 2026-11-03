package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class FishingHandler {

    private FishingHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        boolean hasCatch = false;

        for (ItemStack stack : event.getDrops()) {
            if (!stack.isEmpty()) {
                hasCatch = true;
                break;
            }
        }

        // При отсутствии улова не выбираем множитель.
        if (!hasCatch) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            // Отменяем событие вместо изменения списка улова.
            event.setCanceled(true);

            MultiplierDisplay.show(player, 0);

            player.displayClientMessage(
                    Component.literal("§c✘ Рыбалка: улов сорвался!"),
                    true
            );

            return;
        }

        if (multiplier == 1) {
            return;
        }

        int spawnedStacks = 0;

        for (ItemStack original : event.getDrops()) {
            if (original.isEmpty()) {
                continue;
            }

            // Один исходный улов создаст сама игра.
            long remaining = (long) original.getCount() * (multiplier - 1);
            int maxStackSize = original.getMaxStackSize();

            while (remaining > 0) {
                int count = (int) Math.min(remaining, maxStackSize);

                // copy() сохраняет данные предмета, например зачарования.
                ItemStack copy = original.copy();
                copy.setCount(count);

                ItemEntity extra = new ItemEntity(
                        level,
                        player.getX(),
                        player.getY() + 0.5D,
                        player.getZ(),
                        copy
                );

                extra.setDefaultPickUpDelay();

                if (level.addFreshEntity(extra)) {
                    spawnedStacks++;
                }

                remaining -= count;
            }
        }

        if (spawnedStacks > 0) {
            MultiplierDisplay.show(player, multiplier);

            player.displayClientMessage(
                    Component.literal("§e✦ Мультиулов: x" + multiplier),
                    true
            );
        }
    }
}
