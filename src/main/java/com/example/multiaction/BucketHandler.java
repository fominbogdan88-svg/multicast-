package com.example.multiaction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "multiaction")
public class BucketHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack heldStack = event.getItemStack();

        if (!heldStack.is(Items.BUCKET)) {
            return;
        }

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(5.0D));

        BlockHitResult hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos target = hit.getBlockPos();
        BlockState targetState = level.getBlockState(target);
        FluidState targetFluid = targetState.getFluidState();

        // Не удаляем блоки с водой внутри, например ступеньки.
        if (!(targetState.getBlock() instanceof LiquidBlock)
                || !targetFluid.isSource()) {
            return;
        }

        boolean water = targetFluid.is(FluidTags.WATER);
        boolean lava = targetFluid.is(FluidTags.LAVA);

        if (!water && !lava) {
            return;
        }

        // Дополнительный сбор происходит только при x16.
        int multiplier = MulticastManager.roll(player);

        if (multiplier != 16) {
            return;
        }

        Item filledBucket = water ? Items.WATER_BUCKET : Items.LAVA_BUCKET;
        int collected = 0;

        /*
         * Область размером 4×4×4 вокруг выбранного блока.
         * Собираются только источники того же вида жидкости.
         */
        for (int x = -1; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -1; z <= 2; z++) {
                    BlockPos pos = target.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    FluidState fluid = state.getFluidState();

                    if (!(state.getBlock() instanceof LiquidBlock)
                            || !fluid.isSource()) {
                        continue;
                    }

                    if (water && !fluid.is(FluidTags.WATER)) {
                        continue;
                    }

                    if (lava && !fluid.is(FluidTags.LAVA)) {
                        continue;
                    }

                    if (level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3)) {
                        collected++;
                    }
                }
            }
        }

        if (collected == 0) {
            return;
        }

        InteractionHand hand = event.getHand();

        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }

        for (int i = 0; i < collected; i++) {
            ItemStack bucket = new ItemStack(filledBucket);

            if (!player.getInventory().add(bucket)) {
                player.drop(bucket, false);
            }
        }

        level.playSound(
                null,
                target,
                water ? SoundEvents.BUCKET_FILL : SoundEvents.BUCKET_FILL_LAVA,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        MultiplierDisplay.show(player, 16);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
