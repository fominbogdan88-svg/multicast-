package com.example.multiaction;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class BoneMealHandler {

    private BoneMealHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBoneMeal(BonemealEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            event.setCanceled(true);
            MultiplierDisplay.show(player, 0);

            player.displayClientMessage(
                    Component.literal("§c✘ Костная мука: срыв!"),
                    true
            );
            return;
        }

        if (multiplier == 1) {
            return;
        }

        int successfulExtraUses = 0;

        for (int i = 1; i < multiplier; i++) {
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof BonemealableBlock growable)) {
                break;
            }

            if (!growable.isValidBonemealTarget(
                    level,
                    pos,
                    state,
                    false
            )) {
                break;
            }

            // У некоторых растений успешное применение зависит от шанса.
            if (growable.isBonemealSuccess(
                    level,
                    level.random,
                    pos,
                    state
            )) {
                growable.performBonemeal(
                        level,
                        level.random,
                        pos,
                        state
                );

                successfulExtraUses++;
            }
        }

        MultiplierDisplay.show(player, multiplier);

        player.displayClientMessage(
                Component.literal(
                        "§a✦ Костная мука: x" + multiplier
                                + " §7(дополнительных срабатываний: "
                                + successfulExtraUses + ")"
                ),
                true
        );

        // Обычное применение костной муки обработает Minecraft.
    }
}
