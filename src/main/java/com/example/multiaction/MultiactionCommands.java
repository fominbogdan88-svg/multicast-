package com.example.multiaction;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class MultiactionCommands {

    private MultiactionCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("multiaction")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.argument(
                                        "multiplier",
                                        IntegerArgumentType.integer(-1, 16)
                                )
                                .executes(context -> setMultiplier(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(
                                                context,
                                                "multiplier"
                                        )
                                ))
                        )

                        .then(Commands.literal("luckpotion")
                                .executes(context ->
                                        giveLuckPotion(context.getSource())
                                )
                        )

                        .then(Commands.literal("equal")
                                .executes(context ->
                                        toggleEqual(context.getSource())
                                )
                        )

                        .then(Commands.literal("balance")
                                .executes(context -> setMode(
                                        context.getSource(),
                                        MulticastManager.ProbabilityMode.BALANCED,
                                        "сбалансированный"
                                ))
                        )

                        .then(Commands.literal("normal")
                                .executes(context -> setMode(
                                        context.getSource(),
                                        MulticastManager.ProbabilityMode.NORMAL,
                                        "обычный"
                                ))
                        )
        );
    }

    private static int setMultiplier(
            CommandSourceStack source,
            int multiplier
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        MulticastManager.setForcedMultiplier(player, multiplier);

        if (multiplier == -1) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§aПринудительная фиксация множителя снята."
                    ),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal(
                            "§aМножитель зафиксирован: §ex" + multiplier
                    ),
                    false
            );
        }

        return 1;
    }

    private static int giveLuckPotion(
            CommandSourceStack source
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        ItemStack potion = PotionUtils.setPotion(
                new ItemStack(Items.POTION),
                Potions.LUCK
        );

        if (!player.getInventory().add(potion)) {
            player.drop(potion, false);
        }

        source.sendSuccess(
                () -> Component.literal("§aВыдано зелье удачи."),
                false
        );

        return 1;
    }

    private static int toggleEqual(
            CommandSourceStack source
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        boolean enabled = MulticastManager.toggleEqual(player);

        source.sendSuccess(
                () -> Component.literal(
                        enabled
                                ? "§aРавные шансы включены."
                                : "§aРавные шансы выключены. Включён обычный режим."
                ),
                false
        );

        return 1;
    }

    private static int setMode(
            CommandSourceStack source,
            MulticastManager.ProbabilityMode mode,
            String modeName
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        MulticastManager.setMode(player, mode);

        source.sendSuccess(
                () -> Component.literal(
                        "§aРежим шансов: §e" + modeName
                ),
                false
        );

        return 1;
    }
}
