package com.example.multiaction;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class ChestLootHandler {

    private ChestLootHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChestInteract(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        BlockPos pos = event.getPos();

        if (!level.mayInteract(player, pos)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            return;
        }

        /*
         * Проверяем сохранённые данные сундука. LootTable присутствует
         * до генерации добычи и удаляется при её генерации.
         * Число 8 означает строковый NBT-тег.
         */
        if (!chest.saveWithoutMetadata().contains("LootTable", 8)) {
            return;
        }

        // Создаём обычную добычу сундука.
        chest.unpackLootTable(player);

        // Равные шансы от x0 до x8.
        int multiplier = level.random.nextInt(9);

        List<ItemStack> originalItems = new ArrayList<>();

        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);

            if (!stack.isEmpty()) {
                originalItems.add(stack.copy());
            }

            chest.setItem(slot, ItemStack.EMPTY);
        }

        List<ItemStack> overflow = new ArrayList<>();
        int nextSlot = 0;

        for (ItemStack original : originalItems) {
            long remaining = (long) original.getCount() * multiplier;

            while (remaining > 0) {
                int count = (int) Math.min(
                        remaining,
                        original.getMaxStackSize()
                );

                ItemStack copy = original.copy();
                copy.setCount(count);

                if (nextSlot < chest.getContainerSize()) {
                    chest.setItem(nextSlot, copy);
                    nextSlot++;
                } else {
                    overflow.add(copy);
                }

                remaining -= count;
            }
        }

        chest.setChanged();

        // Предметы, которые не поместились, появляются рядом.
        for (ItemStack stack : overflow) {
            ItemEntity item = new ItemEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.1D,
                    pos.getZ() + 0.5D,
                    stack
            );

            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }

        showResult(level, pos, multiplier);
    }

    private static void showResult(
            ServerLevel level,
            BlockPos pos,
            int multiplier
    ) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.7D;
        double centerZ = pos.getZ() + 0.5D;

        int points = 32;

        for (int i = 0; i < points; i++) {
            double angle = 2.0D * Math.PI * i / points;
            double x = centerX + Math.cos(angle);
            double z = centerZ + Math.sin(angle);

            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    x,
                    centerY,
                    z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }

        level.sendParticles(
                ParticleTypes.ENCHANT,
                centerX,
                centerY + 0.3D,
                centerZ,
                30,
                0.5D,
                0.3D,
                0.5D,
                0.1D
        );

        level.playSound(
                null,
                pos,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS,
                0.7F,
                multiplier == 0 ? 0.7F : 1.3F
        );

        Component message = multiplier == 0
                ? Component.literal("§c✘ Сундук: добыча сорвалась! x0")
                : Component.literal("§6✦ Сундук удачи: §ex" + multiplier);

        AABB area = new AABB(pos).inflate(16.0D);

        for (ServerPlayer nearby : level.getEntitiesOfClass(
                ServerPlayer.class,
                area
        )) {
            nearby.displayClientMessage(message, true);
        }
    }
}
