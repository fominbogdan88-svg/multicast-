package com.example.multiaction;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.PiglinBarterEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class PiglinBarterHandler {

    private PiglinBarterHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBarter(PiglinBarterEvent event) {
        if (event.isCanceled()) {
            return;
        }

        Piglin piglin = event.getEntity();

        if (!(piglin.level() instanceof ServerLevel level)) {
            return;
        }

        List<ItemStack> outcome = event.getOutcome();

        if (outcome.isEmpty()) {
            return;
        }

        // Отдельный случайный результат пиглина: от x0 до x16.
        int multiplier = piglin.getRandom().nextInt(17);

        if (multiplier == 1) {
            return;
        }

        List<ItemStack> result = new ArrayList<>();

        if (multiplier > 0) {
            for (ItemStack original : outcome) {
                if (original.isEmpty()) {
                    continue;
                }

                long remaining = (long) original.getCount() * multiplier;

                while (remaining > 0) {
                    int count = (int) Math.min(
                            remaining,
                            original.getMaxStackSize()
                    );

                    ItemStack copy = original.copy();
                    copy.setCount(count);
                    result.add(copy);
                    remaining -= count;
                }
            }
        }

        // При x0 список остаётся пустым.
        outcome.clear();
        outcome.addAll(result);

        MultiplierDisplay.show(piglin, multiplier);

        Component message = multiplier == 0
                ? Component.literal("§c✘ Обмен с пиглином: срыв!")
                : Component.literal("§6✦ Обмен с пиглином: x" + multiplier);

        // Короткое сообщение над хотбаром для игроков поблизости.
        AABB area = piglin.getBoundingBox().inflate(8.0D);

        for (ServerPlayer nearby : level.getEntitiesOfClass(
                ServerPlayer.class,
                area
        )) {
            nearby.displayClientMessage(message, true);
        }
    }
}
