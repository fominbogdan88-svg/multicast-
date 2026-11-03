package com.example.multiaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class BlockBreakingHandler {

    /*
     * Защита от повторного запуска мультиломания для каждого
     * дополнительного блока.
     *
     * Обработчик работает на серверном игровом потоке.
     */
    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();

    private BlockBreakingHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (event.isCanceled()) {
            return;
        }

        UUID playerId = player.getUUID();

        // Дополнительные блоки не должны запускать новый поиск.
        if (ACTIVE_PLAYERS.contains(playerId)) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            event.setCanceled(true);

            player.displayClientMessage(
                    Component.literal("§c✘ Добыча блока сорвалась!"),
                    true
            );

            return;
        }

        if (multiplier == 1) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos origin = event.getPos().immutable();
        Block targetBlock = event.getState().getBlock();

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(origin);
        visited.add(origin);

        int broken = 0;
        int extraLimit = multiplier - 1;

        // Запоминаем, был ли в руке предмет в начале действия.
        boolean startedWithTool = !player.getMainHandItem().isEmpty();

        ACTIVE_PLAYERS.add(playerId);

        try {
            while (!queue.isEmpty() && broken < extraLimit) {
                BlockPos current = queue.remove();

                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction).immutable();

                    if (!visited.add(neighbor)) {
                        continue;
                    }

                    // Не загружаем новые чанки ради поиска блоков.
                    if (!level.hasChunkAt(neighbor)) {
                        continue;
                    }

                    if (!level.getBlockState(neighbor).is(targetBlock)) {
                        continue;
                    }

                    // Если инструмент сломался, не продолжаем им добычу.
                    if (startedWithTool && player.getMainHandItem().isEmpty()) {
                        queue.clear();
                        break;
                    }

                    /*
                     * Используем серверное ломание игрока, а не просто
                     * удаляем блок: так сохраняются обычные проверки,
                     * прочность инструмента и события Forge.
                     */
                    boolean success = player.gameMode.destroyBlock(neighbor);

                    if (success) {
                        broken++;
                        queue.add(neighbor);
                    }

                    if (broken >= extraLimit) {
                        break;
                    }
                }
            }
        } finally {
            ACTIVE_PLAYERS.remove(playerId);
        }

        if (broken > 0) {
            player.displayClientMessage(
                    Component.literal(
                            "§6⛏ Мультиломание: до x" + multiplier
                                    + " §7(дополнительно: " + broken + ")"
                    ),
                    true
            );
        }

        // Исходный блок после выхода из события ломает сам Minecraft.
    }
}
