package com.example.multiaction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class JumpHandler {

    // Обычный шанс: 1 из 1000 прыжков.
    private static final int CHANCE = 1000;

    // Игроки, у которых следующий прыжок проверит механику.
    private static final Set<UUID> TEST_PLAYERS = new HashSet<>();

    private JumpHandler() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("multiactiontest")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("jump")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource()
                                            .getPlayerOrException();

                                    UUID id = player.getUUID();

                                    if (TEST_PLAYERS.remove(id)) {
                                        player.sendSystemMessage(
                                                Component.literal(
                                                        "§eПроверка мультипрыжка отменена."
                                                )
                                        );
                                    } else {
                                        TEST_PLAYERS.add(id);

                                        player.sendSystemMessage(
                                                Component.literal(
                                                        "§aСледующий прыжок проверит "
                                                                + "мультипрыжок. "
                                                                + "Нужен множитель выше x1."
                                                )
                                        );
                                    }

                                    return 1;
                                })
                        )
        );
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean test = TEST_PLAYERS.remove(player.getUUID());

        if (!test && player.getRandom().nextInt(CHANCE) != 0) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier <= 1) {
            if (test) {
                player.displayClientMessage(
                        Component.literal(
                                "§eПроверка: множитель x" + multiplier
                                        + " — прыжок обычный."
                        ),
                        true
                );
            }

            return;
        }

        Vec3 motion = player.getDeltaMovement();

        // Горизонтальная скорость сохраняется.
        double extraJump = (multiplier - 1) * 0.08D;

        player.setDeltaMovement(
                motion.x,
                motion.y + extraJump,
                motion.z
        );

        player.hurtMarked = true;

        MultiplierDisplay.show(player, multiplier);

        player.displayClientMessage(
                Component.literal("§b✦ Мультипрыжок: x" + multiplier),
                true
        );
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TEST_PLAYERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TEST_PLAYERS.clear();
    }
}
