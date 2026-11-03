package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class PoisonousPotatoHandler {

    private PoisonousPotatoHandler() {
    }

    @SubscribeEvent
    public static void onItemFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack used = event.getItem();

        if (!used.is(Items.POISONOUS_POTATO)) {
            return;
        }

        int before = MulticastManager.getPotatoBonus(player);
        int bonus = MulticastManager.addPotatoBonus(player);

        String message = before >= 16
                ? "§eБонус картошки уже максимальный: §6+16"
                : "§a✦ Постоянный бонус к множителю: §6+" + bonus;

        player.displayClientMessage(Component.literal(message), true);
    }
}
