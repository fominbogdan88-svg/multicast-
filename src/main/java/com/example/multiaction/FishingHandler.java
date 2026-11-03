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

        int multiplier = MulticastManager.roll(player);

        showResult(player, multiplier);

        List<ItemStack> drops = event.getDrops();

        if (multiplier == 0) {
            drops.clear();
            return;
        }

        if (multiplier == 1 || drops.isEmpty()) {
            return;
        }

        /*
         * Сначала создаём отдельный список. Нельзя добавлять предметы
         * в drops прямо во время перебора этого же списка.
         */
        List<ItemStack> multipliedDrops = new ArrayList<>();

        for (ItemStack original : drops) {
            if (original.isEmpty()) {
                continue;
            }

            long remaining = (long) original.getCount() * multiplier;
            int maxStackSize = original.getMaxStackSize();

            /*
             * Разбиваем результат на допустимые стаки.
             * Например, 16 неповреждённых удочек — это 16 отдельных
             * стаков, а не один стак с недопустимым количеством.
             */
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
    }

    private static void showResult(ServerPlayer player, int multiplier) {
        String message;

        if (multiplier == 0) {
            message = "§c✘ Рыбалка: улов сорвался!";
        } else if (multiplier == 1) {
            message = "§7🎣 Рыбалка: x1";
        } else if (multiplier >= 10) {
            message = "§6★ Мультиулов: x" + multiplier + "!";
        } else {
            message = "§e✦ Мультиулов: x" + multiplier;
        }

        // Сообщение над панелью быстрого доступа, а не в чате.
        player.displayClientMessage(Component.literal(message), true);

        // Тихий короткий звук только для самого игрока.
        player.playNotifySound(
                multiplier == 0
                        ? SoundEvents.UI_BUTTON_CLICK
                        : SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.25F,
                multiplier == 0 ? 0.7F : 1.4F
        );
    }
}
