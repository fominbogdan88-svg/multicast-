package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class FoodHandler {

    private FoodHandler() {
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack food = event.getItem();

        if (food.isEmpty() || !food.isEdible()) {
            return;
        }

        FoodProperties properties = food.getItem()
                .getFoodProperties(food, player);

        if (properties == null) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier <= 1) {
            return;
        }

        /*
         * Обычное питание даёт сам Minecraft.
         * Добавляем только эффект дополнительных порций.
         */
        for (int i = 1; i < multiplier; i++) {
            player.getFoodData().eat(
                    properties.getNutrition(),
                    properties.getSaturationModifier()
            );
        }

        MultiplierDisplay.show(player, multiplier);

        player.displayClientMessage(
                Component.literal("§6✦ Мультипитание: x" + multiplier),
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
