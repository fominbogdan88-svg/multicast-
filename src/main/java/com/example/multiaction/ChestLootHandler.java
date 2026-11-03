package com.example.multiaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class ChestLootHandler {

    private static final int ANIMATION_TICKS = 40;
    private static final int RESULT_TICKS = 40;

    private static final List<ChestAnimation> ANIMATIONS = new ArrayList<>();

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

        // Равные шансы на результаты от x0 до x8.
        int multiplier = level.random.nextInt(9);

        // Добыча определяется сразу, до показа анимации.
        multiplyChest(level, player, chest, pos, multiplier);

        ANIMATIONS.add(new ChestAnimation(
                level,
                pos.immutable(),
                multiplier,
                level.getGameTime()
        ));
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

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        Iterator<ChestAnimation> iterator = ANIMATIONS.iterator();

        while (iterator.hasNext()) {
            ChestAnimation animation = iterator.next();

            if (animation.level != level) {
                continue;
            }

            long age = level.getGameTime() - animation.startedAt;

            // Если чанк выгрузился, прекращаем анимацию.
            if (!level.hasChunkAt(animation.pos)) {
                animation.removeDisplay();
                iterator.remove();
                continue;
            }

            if (age < ANIMATION_TICKS) {
                // Частицы отправляем раз в два тика, чтобы уменьшить нагрузку.
                if (age % 2 == 0) {
                    showWheel(level, animation.pos, age);
                }
                continue;
            }

            if (!animation.resultShown) {
                animation.resultShown = true;
                animation.display = showResult(
                        level,
                        animation.pos,
                        animation.multiplier
                );
            }

            if (age >= ANIMATION_TICKS + RESULT_TICKS) {
                animation.removeDisplay();
                iterator.remove();
            }
        }
    }

    private static void showWheel(
            ServerLevel level,
            BlockPos pos,
            long age
    ) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 1.15D;
        double centerZ = pos.getZ() + 0.5D;

        /*
         * Две дуги вращаются вокруг сундука. Угол меняется каждый тик,
         * поэтому колесо выглядит движущимся.
         */
        double rotation = age * 0.22D;

        for (int i = 0; i < 12; i++) {
            double angle = rotation + i * Math.PI / 12.0D;

            double x = Math.cos(angle) * 0.85D;
            double z = Math.sin(angle) * 0.85D;

            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    centerX + x,
                    centerY,
                    centerZ + z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );

            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    centerX - x,
                    centerY,
                    centerZ - z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private static ArmorStand showResult(
            ServerLevel level,
            BlockPos pos,
            int multiplier
    ) {
        level.playSound(
                null,
                pos,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS,
                0.7F,
                multiplier == 0 ? 0.7F : 1.3F
        );

        ArmorStand display = new ArmorStand(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 1.2D,
                pos.getZ() + 0.5D
        );

        // Marker убирает хитбокс, чтобы стойка не мешала игрокам.
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

        return level.addFreshEntity(display) ? display : null;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        for (ChestAnimation animation : ANIMATIONS) {
            animation.removeDisplay();
        }

        ANIMATIONS.clear();
    }

    private static final class ChestAnimation {

        private final ServerLevel level;
        private final BlockPos pos;
        private final int multiplier;
        private final long startedAt;

        private boolean resultShown;
        private ArmorStand display;

        private ChestAnimation(
                ServerLevel level,
                BlockPos pos,
                int multiplier,
                long startedAt
        ) {
            this.level = level;
            this.pos = pos;
            this.multiplier = multiplier;
            this.startedAt = startedAt;
        }

        private void removeDisplay() {
            if (display != null) {
                display.discard();
                display = null;
            }
        }
    }
}
