package com.example.multiaction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class LuckPotionHandler {

    private LuckPotionHandler() {
    }

    @SubscribeEvent
    public static void onPotionFinished(
            LivingEntityUseItemEvent.Finish event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack used = event.getItem();

        if (!used.is(Items.POTION)
                || PotionUtils.getPotion(used) != Potions.LUCK) {
            return;
        }

        // Результат выбирается на сервере, а не на клиенте.
        int multiplier = MulticastManager.rollForPotion(player);

        // Фиксируем его на 4 минуты 11 секунд.
        MulticastManager.applyPotionMultiplier(player, multiplier);

        // Убираем стандартный эффект удачи.
        player.removeEffect(MobEffects.LUCK);

        // Отправляем результат клиенту для показа интерфейса.
        ModNetwork.sendRoulette(player, multiplier);
    }
}
