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
public final class CraftingHandler {

    private CraftingHandler() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack crafted = event.getCrafting();

        if (crafted.isEmpty()) {
            return;
        }

        int roll = player.getRandom().nextInt(1000);
        int multiplier;

        if (roll == 0) {
            multiplier = 3;
        } else if (roll <= 10) {
            multiplier = 2;
        } else {
            return;
        }

        /*
         * Оригинальный результат уже выдаёт Minecraft.
         * Поэтому добавляем только дополнительные копии:
         *
         * x2 → одна дополнительная копия;
         * x3 → две дополнительные копии.
         */
        int extraCopies = multiplier - 1;

        for (int i = 0; i < extraCopies; i++) {
            ItemStack extra = crafted.copy();

            // После add() в extra остаётся то, что не поместилось.
            player.getInventory().add(extra);

            if (!extra.isEmpty()) {
                player.drop(extra, false);
            }
        }

        player.displayClientMessage(
                Component.literal("§6✦ Редкий мультикрафт: x" + multiplier),
                true
        );

        player.playNotifySound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.25F,
                1.4F
        );
    }
}
