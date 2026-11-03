package com.example.multiaction;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class MultiactionInfoCommand {

    private MultiactionInfoCommand() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("multiactioninfo")
                        .executes(context -> {
                            ServerPlayer player = context.getSource()
                                    .getPlayerOrException();

                            int bonus = MulticastManager.getPotatoBonus(player);
                            Integer forced =
                                    MulticastManager.getForcedMultiplier(player);

                            MulticastManager.ProbabilityMode mode =
                                    MulticastManager.getMode(player);

                            String forcedText = forced == null
                                    ? "не установлен"
                                    : "x" + forced;

                            String modeText = switch (mode) {
                                case NORMAL -> "обычный";
                                case BALANCED -> "сбалансированный";
                                case EQUAL -> "равные шансы";
                            };

                            player.sendSystemMessage(Component.literal(
                                    "§6✦ Настройки MultiAction"
                            ));

                            player.sendSystemMessage(Component.literal(
                                    "§eБонус картошки: §a+" + bonus
                            ));

                            player.sendSystemMessage(Component.literal(
                                    "§eФиксация команды: §a" + forcedText
                            ));

                            player.sendSystemMessage(Component.literal(
                                    "§eРежим шансов: §a" + modeText
                            ));

                            if (forced != null) {
                                int total = Math.min(16, forced + bonus);

                                player.sendSystemMessage(Component.literal(
                                        "§eС фиксацией и бонусом: §aдо x" + total
                                ));
                            }

                            return 1;
                        })
        );
    }
}
