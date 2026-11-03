package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class ShearingHandler {

    private ShearingHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack shears = event.getItemStack();

        if (!shears.is(Items.SHEARS)) {
            return;
        }

        Entity target = event.getTarget();

        if (!(target instanceof IForgeShearable shearable)) {
            return;
        }

        if (!shearable.isShearable(
                shears,
                player.level(),
                target.blockPosition()
        )) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        // Отменяем обычную обработку: ниже выполняем стрижку сами.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (multiplier == 0) {
            player.displayClientMessage(
                    Component.literal("§c✘ Стрижка сорвалась!"),
                    true
            );
            return;
        }

        // Вызываем стрижку ровно один раз, чтобы состояние моба изменилось.
        List<ItemStack> drops = shearable.onSheared(
                player,
                shears,
                player.level(),
                target.blockPosition(),
                0
        );

        for (ItemStack original : drops) {
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

                ItemEntity item = new ItemEntity(
                        player.level(),
                        target.getX(),
                        target.getY() + target.getBbHeight(),
                        target.getZ(),
                        copy
                );

                item.setDefaultPickUpDelay();
                player.level().addFreshEntity(item);

                remaining -= count;
            }
        }

        shears.hurtAndBreak(
                1,
                player,
                p -> p.broadcastBreakEvent(event.getHand())
        );

        player.level().playSound(
                null,
                target.blockPosition(),
                SoundEvents.SHEEP_SHEAR,
                SoundSource.PLAYERS,
                0.7F,
                1.0F
        );

        player.swing(event.getHand(), true);

        if (multiplier > 1) {
            player.displayClientMessage(
                    Component.literal("§e✦ Мультистрижка: x" + multiplier),
                    true
            );
        }
    }
}
