package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class DamageHandler {

    private DamageHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Урон рассчитываем только на сервере.
        if (event.getEntity().level().isClientSide) {
            return;
        }

        Entity attacker = event.getSource().getEntity();

        if (attacker instanceof ServerPlayer player) {
            applyPlayerDamage(event, player);
        } else if (attacker instanceof Mob mob) {
            applyMobDamage(event, mob);
        }
    }

    private static void applyPlayerDamage(
            LivingHurtEvent event,
            ServerPlayer player
    ) {
        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            event.setCanceled(true);
            showPlayerResult(player, multiplier);
            return;
        }

        if (multiplier > 1) {
            event.setAmount(event.getAmount() * multiplier);
            showPlayerResult(player, multiplier);
        }
    }

    private static void applyMobDamage(
            LivingHurtEvent event,
            Mob mob
    ) {
        int roll = mob.getRandom().nextInt(1000);
        int multiplier;

        if (roll == 0) {
            multiplier = 3;
        } else if (roll <= 10) {
            multiplier = 2;
        } else {
            return;
        }

        event.setAmount(event.getAmount() * multiplier);

        // Уведомляем только игрока, которого ударил моб.
        if (event.getEntity() instanceof Player target) {
            target.displayClientMessage(
                    Component.literal(
                            "§c⚔ Моб нанёс мультиурон: x" + multiplier
                    ),
                    true
            );
        }
    }

    private static void showPlayerResult(
            ServerPlayer player,
            int multiplier
    ) {
        String message = multiplier == 0
                ? "§c✘ Удар сорвался!"
                : "§e⚔ Мультиурон: x" + multiplier;

        player.displayClientMessage(Component.literal(message), true);

        player.playNotifySound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.2F,
                multiplier == 0 ? 0.8F : 1.4F
        );
    }
}
