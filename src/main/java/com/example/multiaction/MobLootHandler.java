package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class MobLootHandler {

    private MobLootHandler() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        // Владелец снаряда также может быть указан как источник урона.
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Игроков эта механика не затрагивает.
        if (event.getEntity() instanceof ServerPlayer) {
            return;
        }

        if (event.getDrops().isEmpty()) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            event.getDrops().clear();
            showResult(player, 0);
            return;
        }

        if (multiplier == 1) {
            return;
        }

        List<ItemEntity> extraDrops = new ArrayList<>();

        for (ItemEntity originalEntity : event.getDrops()) {
            ItemStack original = originalEntity.getItem();

            if (original.isEmpty()) {
                continue;
            }

            // Оригинал уже присутствует в списке дропа.
            long remaining = (long) original.getCount() * (multiplier - 1);
            int maxStackSize = original.getMaxStackSize();

            while (remaining > 0) {
                int count = (int) Math.min(remaining, maxStackSize);

                ItemStack copy = original.copy();
                copy.setCount(count);

                ItemEntity extra = new ItemEntity(
                        event.getEntity().level(),
                        originalEntity.getX(),
                        originalEntity.getY(),
                        originalEntity.getZ(),
                        copy
                );

                extra.setDefaultPickUpDelay();
                extraDrops.add(extra);

                remaining -= count;
            }
        }

        // Добавляем после обхода, чтобы не изменять список во время перебора.
        event.getDrops().addAll(extraDrops);
        showResult(player, multiplier);
    }

    private static void showResult(ServerPlayer player, int multiplier) {
        String message = multiplier == 0
                ? "§c✘ Добыча с моба сорвалась!"
                : "§6✦ Добыча с моба: x" + multiplier;

        player.displayClientMessage(Component.literal(message), true);

        player.playNotifySound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.2F,
                multiplier == 0 ? 0.8F : 1.4F
        );
    }
}
