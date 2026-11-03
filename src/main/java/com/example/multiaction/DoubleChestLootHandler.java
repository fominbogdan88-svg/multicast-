package com.example.multiaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class DoubleChestLootHandler {

    private static final String DISPLAY_TAG = "MultiActionChestDisplay";
    private static final String EXPIRES_TAG = "MultiActionChestDisplayExpires";

    private DoubleChestLootHandler() {
    }

    /**
     * Вызывается из ChestLootHandler при нажатии на двойной сундук.
     */
    public static void handle(
            ServerLevel level,
            ServerPlayer player,
            BlockPos firstPos,
            BlockState firstState,
            ChestBlockEntity firstChest
    ) {
        Direction connectedDirection = ChestBlock.getConnectedDirection(firstState);
        BlockPos secondPos = firstPos.relative(connectedDirection);

        // Не загружаем соседний чанк специально для обработки сундука.
        if (!level.hasChunkAt(secondPos)) {
            return;
        }

        BlockState secondState = level.getBlockState(secondPos);
        BlockEntity secondEntity = level.getBlockEntity(secondPos);

        if (!(secondEntity instanceof ChestBlockEntity secondChest)
                || secondState.getBlock() != firstState.getBlock()
                || !secondState.hasProperty(ChestBlock.TYPE)
                || secondState.getValue(ChestBlock.TYPE)
                        == firstState.getValue(ChestBlock.TYPE)
                || ChestBlock.getConnectedDirection(secondState)
                        != connectedDirection.getOpposite()) {
            return;
        }

        boolean firstPending = ChestLootHandler.hasPendingLoot(firstChest);
        boolean secondPending = ChestLootHandler.hasPendingLoot(secondChest);

        // Обе половины уже сгенерированы — повторно не умножаем.
        if (!firstPending && !secondPending) {
            return;
        }

        int multiplier = level.random.nextInt(9);

        if (firstPending) {
            ChestLootHandler.multiplyChest(
                    level,
                    player,
                    firstChest,
                    firstPos,
                    multiplier
            );
        }

        if (secondPending) {
            ChestLootHandler.multiplyChest(
                    level,
                    player,
                    secondChest,
                    secondPos,
                    multiplier
            );
        }

        showResult(level, firstPos, secondPos, multiplier);
    }

    private static void showResult(
            ServerLevel level,
            BlockPos first,
            BlockPos second,
            int multiplier
    ) {
        double x = (first.getX() + second.getX() + 1.0D) / 2.0D;
        double y = Math.max(first.getY(), second.getY()) + 1.3D;
        double z = (first.getZ() + second.getZ() + 1.0D) / 2.0D;

        ArmorStand display = new ArmorStand(level, x, y, z);

        /*
         * Marker убирает хитбокс стойки, чтобы она не мешала игрокам.
         * Свойства задаются через NBT, поскольку прямые методы закрыты.
         */
        CompoundTag data = new CompoundTag();
        display.saveWithoutId(data);
        data.putBoolean("Marker", true);
        data.putBoolean("Small", true);
        data.putBoolean("NoBasePlate", true);
        display.load(data);

        display.setInvisible(true);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.noPhysics = true;

        display.setCustomName(
                multiplier == 0
                        ? Component.literal("§c✘ Сундук: x0")
                        : Component.literal("§6✦ Сундук: §ex" + multiplier)
        );

        display.setCustomNameVisible(true);
        display.getPersistentData().putBoolean(DISPLAY_TAG, true);
        display.getPersistentData().putLong(
                EXPIRES_TAG,
                level.getGameTime() + 40L
        );

        level.addFreshEntity(display);

        level.playSound(
                null,
                first,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS,
                0.7F,
                multiplier == 0 ? 0.7F : 1.3F
        );
    }

    @SubscribeEvent
    public static void onDisplayTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ArmorStand display)) {
            return;
        }

        if (!(display.level() instanceof ServerLevel level)) {
            return;
        }

        if (!display.getPersistentData().getBoolean(DISPLAY_TAG)) {
            return;
        }

        if (level.getGameTime()
                >= display.getPersistentData().getLong(EXPIRES_TAG)) {
            display.discard();
        }
    }
}
