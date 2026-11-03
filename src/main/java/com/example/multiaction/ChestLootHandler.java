package com.example.multiaction;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
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

        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(state.getBlock() instanceof ChestBlock)
                || !(blockEntity instanceof ChestBlockEntity chest)) {
            return;
        }

        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            DoubleChestLootHandler.handle(level, player, pos, state, chest);
            return;
        }

        if (!hasPendingLoot(chest)) {
            return;
        }

        int multiplier = level.random.nextInt(9);

        multiplyChest(level, player, chest, pos, multiplier);
        showSingleResult(level, pos, multiplier);
    }

    /**
     * Таблица добычи присутствует в NBT до генерации содержимого.
     */
    static boolean hasPendingLoot(ChestBlockEntity chest) {
        return chest.saveWithoutMetadata()
                .contains("LootTable", Tag.TAG_STRING);
    }

    /**
     * Генерирует и умножает содержимое одной половины сундука.
     * Не вызывается для уже сгенерированных половин.
     */
    static void multiplyChest(
            ServerLevel level,
            ServerPlayer player,
            ChestBlockEntity chest,
            BlockPos dropPos,
            int multiplier
    ) {
        chest.unpackLootTable(player);

        List<ItemStack> originalItems = new ArrayList<>();

        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);

            if (!stack.isEmpty()) {
                originalItems.add(stack.copy());
            }

            chest.setItem(slot, ItemStack.EMPTY);
        }

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
                    ItemEntity item = new ItemEntity(
                            level,
                            dropPos.getX() + 0.5D,
                            dropPos.getY() + 1.1D,
                            dropPos.getZ() + 0.5D,
                            copy
                    );

                    item.setDefaultPickUpDelay();
                    level.addFreshEntity(item);
                }

                remaining -= count;
            }
        }

        chest.setChanged();
    }

    private static void showSingleResult(
            ServerLevel level,
            BlockPos pos,
            int multiplier
    ) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.7D;
        double centerZ = pos.getZ() + 0.5D;

        for (int i = 0; i < 32; i++) {
            double angle = 2.0D * Math.PI * i / 32.0D;

            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    centerX + Math.cos(angle),
                    centerY,
                    centerZ + Math.sin(angle),
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
