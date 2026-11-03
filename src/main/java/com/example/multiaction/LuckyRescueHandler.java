package com.example.multiaction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class LuckyRescueHandler {

    // Обычный шанс спасения: 1 из 10 000.
    private static final int RESCUE_CHANCE = 10_000;

    // Игроки, ожидающие проверку гарантированного спасения.
    private static final Set<UUID> TEST_PLAYERS = new HashSet<>();

    private LuckyRescueHandler() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("multiactiontest")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("rescue")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource()
                                            .getPlayerOrException();

                                    UUID id = player.getUUID();

                                    if (TEST_PLAYERS.remove(id)) {
                                        player.sendSystemMessage(
                                                Component.literal(
                                                        "§eПроверка спасения отменена."
                                                )
                                        );
                                    } else {
                                        TEST_PLAYERS.add(id);

                                        player.sendSystemMessage(
                                                Component.literal(
                                                        "§aПроверка включена: "
                                                                + "следующее смертельное "
                                                                + "повреждение без тотема "
                                                                + "должно тебя спасти."
                                                )
                                        );
                                    }

                                    return 1;
                                })
                        )
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // И обычное спасение, и проверка требуют отсутствия тотема.
        if (player.getInventory().contains(
                new ItemStack(Items.TOTEM_OF_UNDYING)
        )) {
            return;
        }

        boolean test = TEST_PLAYERS.remove(player.getUUID());

        if (!test && player.getRandom().nextInt(RESCUE_CHANCE) != 0) {
            return;
        }

        event.setCanceled(true);

        player.setHealth(1.0F);
        player.removeAllEffects();

        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                900,
                1
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION,
                100,
                1
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                800,
                0
        ));

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        player.displayClientMessage(
                Component.literal(
                        test
                                ? "§6✦ Проверка: спасение без тотема сработало!"
                                : "§6✦ Невероятная удача! Ты спасся без тотема!"
                ),
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
