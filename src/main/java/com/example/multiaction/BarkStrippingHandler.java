package com.example.multiaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class BarkStrippingHandler {

    private BarkStrippingHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack tool = event.getItemStack();

        // Проверяем способность снимать кору, а не название предмета.
        if (!tool.canPerformAction(ToolActions.AXE_STRIP)) {
            return;
        }

        BlockPos origin = event.getPos();
        BlockState originalState = player.level().getBlockState(origin);

        UseOnContext originalContext = new UseOnContext(
                player,
                event.getHand(),
                event.getHitVec()
        );

        BlockState strippedState = originalState.getToolModifiedState(
                originalContext,
                ToolActions.AXE_STRIP,
                true
        );

        // Не тратим бросок на блоки, с которых нельзя снять кору.
        if (strippedState == null) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        // x1 метод сам пропустит; x0 покажет «Срыв!».
        MultiplierDisplay.show(player, multiplier);

        player.displayClientMessage(
                Component.literal(
                        multiplier == 0
                                ? "§c✘ Снятие коры: срыв!"
                                : "§6🪓 Снятие коры: x" + multiplier
                ),
                true
        );

        if (multiplier == 0) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (multiplier == 1) {
            // Обычное снятие коры выполнит сам Minecraft.
            return;
        }

        stripNeighbors(player, event, originalState, multiplier - 1);

        // Исходный блок также обработает Minecraft обычным образом.
    }

    private static void stripNeighbors(
            ServerPlayer player,
            PlayerInteractEvent.RightClickBlock event,
            BlockState originalState,
            int extraLimit
    ) {
        BlockPos origin = event.getPos().immutable();

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(origin);
        visited.add(origin);

        int stripped = 0;

        while (!queue.isEmpty() && stripped < extraLimit) {
            BlockPos current = queue.remove();

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction).immutable();

                if (!visited.add(neighbor)) {
                    continue;
                }

                BlockState state = player.level().getBlockState(neighbor);

                // Обрабатываем только тот же вид блока, что и исходное бревно.
                if (!state.is(originalState.getBlock())) {
                    continue;
                }

                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(neighbor),
                        direction,
                        neighbor,
                        false
                );

                UseOnContext context = new UseOnContext(
                        player,
                        event.getHand(),
                        hit
                );

                BlockState replacement = state.getToolModifiedState(
                        context,
                        ToolActions.AXE_STRIP,
                        false
                );

                if (replacement == null) {
                    continue;
                }

                if (player.level().setBlock(neighbor, replacement, 3)) {
                    stripped++;
                    queue.add(neighbor);
                }

                if (stripped >= extraLimit) {
                    break;
                }
            }
        }
    }
}
