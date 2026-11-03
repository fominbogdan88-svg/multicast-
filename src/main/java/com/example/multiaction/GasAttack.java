package com.example.multiaction;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GasAttack {

    // Минимальная задержка между нажатиями — одна секунда.
    private static final long COOLDOWN_TICKS = 20L;

    // Частое использование считается в течение 20 секунд.
    private static final long WINDOW_TICKS = 400L;
    private static final int PENALTY_THRESHOLD = 5;

    private static final Map<UUID, Usage> USAGE = new HashMap<>();

    private GasAttack() {
    }

    public static void trigger(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = player.server.overworld().getGameTime();

        Usage previous = USAGE.get(player.getUUID());

        if (previous != null && now - previous.lastUse() < COOLDOWN_TICKS) {
            return;
        }

        int count;
        long windowStart;

        if (previous == null || now - previous.windowStart() >= WINDOW_TICKS) {
            count = 1;
            windowStart = now;
        } else {
            count = previous.count() + 1;
            windowStart = previous.windowStart();
        }

        USAGE.put(
                player.getUUID(),
                new Usage(windowStart, now, count)
        );

        int multiplier = MulticastManager.roll(player);

        // x1 не показывается, x0 показывает «Срыв!».
        MultiplierDisplay.show(player, multiplier);

        if (multiplier == 0) {
            player.displayClientMessage(
                    Component.literal("§c✘ Газовая атака сорвалась!"),
                    true
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.SLIME_SQUISH,
                    SoundSource.PLAYERS,
                    0.5F,
                    0.5F
            );
        } else {
            int particles = Math.min(120, 15 + multiplier * 6);

            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.getX(),
                    player.getY() + 0.3D,
                    player.getZ(),
                    particles,
                    0.5D + multiplier * 0.04D,
                    0.25D,
                    0.5D + multiplier * 0.04D,
                    0.03D
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.SLIME_SQUISH,
                    SoundSource.PLAYERS,
                    1.0F,
                    0.5F
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.PUFFER_FISH_BLOW_OUT,
                    SoundSource.PLAYERS,
                    0.7F,
                    0.6F
            );

            player.displayClientMessage(
                    Component.literal("§a💨 Газовая атака: x" + multiplier),
                    true
            );

            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(
                            "§a💨 Игрок §e"
                                    + player.getName().getString()
                                    + " §aпёрнул! §7(x"
                                    + multiplier + ")"
                    ),
                    false
            );
        }

        if (count >= PENALTY_THRESHOLD) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION,
                    100,
                    0
            ));

            player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    60,
                    0
            ));

            player.displayClientMessage(
                    Component.literal("§cКажется, ты перестарался..."),
                    true
            );
        }
    }

    public static void clearAll() {
        USAGE.clear();
    }

    private record Usage(long windowStart, long lastUse, int count) {
    }
}
