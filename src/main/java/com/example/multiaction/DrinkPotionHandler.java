package com.example.multiaction;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class DrinkPotionHandler {

    private DrinkPotionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionUseTick(LivingEntityUseItemEvent.Tick event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack potion = event.getItem();

        if (!potion.is(Items.POTION)) {
            return;
        }

        /*
         * Перехватываем последний тик питья, до обычного применения.
         * До этого момента Minecraft показывает обычную анимацию питья.
         */
        if (event.getDuration() > 1) {
            return;
        }

        InteractionHand hand = player.getUsedItemHand();
        boolean luckPotion = PotionUtils.getPotion(potion) == Potions.LUCK;
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(potion);

        int multiplier = MulticastManager.roll(player);

        // Останавливаем обычное завершение, чтобы не применить эффекты дважды.
        event.setCanceled(true);

        // Учитываем использование предмета.
        player.awardStat(Stats.ITEM_USED.get(Items.POTION));

        if (!player.getAbilities().instabuild) {
            potion.shrink(1);

            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);

            if (potion.isEmpty()) {
                player.setItemInHand(hand, bottle);
            } else if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }

        if (multiplier == 0) {
            MultiplierDisplay.show(player, 0);

            player.displayClientMessage(
                    Component.literal("§c✘ Действие зелья сорвалось!"),
                    true
            );

            return;
        }

        for (MobEffectInstance original : effects) {
            MobEffectInstance scaled =
                    PotionEffectScaling.scale(original, multiplier);

            if (scaled.getEffect().isInstantenous()) {
                // Мгновенное исцеление, урон и другие мгновенные эффекты.
                scaled.getEffect().applyInstantenousEffect(
                        player,
                        player,
                        player,
                        scaled.getAmplifier(),
                        1.0D
                );
            } else {
                player.addEffect(scaled);
            }
        }

        if (luckPotion) {
            // Рулетка выбирает новый базовый результат независимо от
            // множителя, с которым было выпито зелье.
            int rouletteResult = MulticastManager.rollForPotion(player);

            MulticastManager.applyPotionMultiplier(player, rouletteResult);
            ModNetwork.sendRoulette(player, rouletteResult);
        }

        if (multiplier > 1) {
            MultiplierDisplay.show(player, multiplier);

            player.displayClientMessage(
                    Component.literal("§d✦ Мультизелье: x" + multiplier),
                    true
            );
        }
    }
}
