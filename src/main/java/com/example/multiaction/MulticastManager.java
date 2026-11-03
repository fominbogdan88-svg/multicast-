package com.example.multiaction;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MulticastManager {

    public static final long POTION_DURATION_TICKS = 251L * 20L;

    private static final int[] NORMAL_WEIGHTS = {
            30, 600, 180, 85, 45, 25, 15, 10, 7,
            5, 4, 3, 2, 2, 1, 1, 1
    };

    private static final int[] BALANCED_WEIGHTS = {
            20, 800, 120, 40, 15, 8, 5, 3, 2,
            2, 1, 1, 1, 1, 1, 1, 1
    };

    private static final Map<UUID, Integer> FORCED = new HashMap<>();
    private static final Map<UUID, TimedMultiplier> POTIONS = new HashMap<>();
    private static final Map<UUID, ProbabilityMode> MODES = new HashMap<>();

    private MulticastManager() {
    }

    public enum ProbabilityMode {
        NORMAL,
        BALANCED,
        EQUAL
    }

    public static int roll(ServerPlayer player) {
        UUID id = player.getUUID();

        Integer forced = FORCED.get(id);
        if (forced != null) {
            return forced;
        }

        TimedMultiplier potion = POTIONS.get(id);
        if (potion != null) {
            long now = player.server.overworld().getGameTime();

            if (now < potion.expiresAt()) {
                return potion.multiplier();
            }

            POTIONS.remove(id);
        }

        return rollRandom(player);
    }

    public static int rollForPotion(ServerPlayer player) {
        return rollRandom(player);
    }

    private static int rollRandom(ServerPlayer player) {
        ProbabilityMode mode = getMode(player);

        if (mode == ProbabilityMode.EQUAL) {
            return player.getRandom().nextInt(17);
        }

        int[] weights = mode == ProbabilityMode.BALANCED
                ? BALANCED_WEIGHTS
                : NORMAL_WEIGHTS;

        int total = 0;
        for (int weight : weights) {
            total += weight;
        }

        int value = player.getRandom().nextInt(total);

        for (int multiplier = 0; multiplier < weights.length; multiplier++) {
            value -= weights[multiplier];
            if (value < 0) {
                return multiplier;
            }
        }

        return 1;
    }

    public static void setForcedMultiplier(
            ServerPlayer player,
            int multiplier
    ) {
        if (multiplier == -1) {
            FORCED.remove(player.getUUID());
            return;
        }

        validate(multiplier);
        FORCED.put(player.getUUID(), multiplier);
    }

    public static Integer getForcedMultiplier(ServerPlayer player) {
        return FORCED.get(player.getUUID());
    }

    public static void applyPotionMultiplier(
            ServerPlayer player,
            int multiplier
    ) {
        validate(multiplier);

        long now = player.server.overworld().getGameTime();

        POTIONS.put(
                player.getUUID(),
                new TimedMultiplier(
                        multiplier,
                        now + POTION_DURATION_TICKS
                )
        );
    }

    public static void clearPotionMultiplier(ServerPlayer player) {
        POTIONS.remove(player.getUUID());
    }

    public static ProbabilityMode getMode(ServerPlayer player) {
        return MODES.getOrDefault(
                player.getUUID(),
                ProbabilityMode.NORMAL
        );
    }

    public static void setMode(
            ServerPlayer player,
            ProbabilityMode mode
    ) {
        if (mode == ProbabilityMode.NORMAL) {
            MODES.remove(player.getUUID());
        } else {
            MODES.put(player.getUUID(), mode);
        }
    }

    public static boolean toggleEqual(ServerPlayer player) {
        if (getMode(player) == ProbabilityMode.EQUAL) {
            setMode(player, ProbabilityMode.NORMAL);
            return false;
        }

        setMode(player, ProbabilityMode.EQUAL);
        return true;
    }

    public static void clearAll() {
        FORCED.clear();
        POTIONS.clear();
        MODES.clear();
    }

    private static void validate(int multiplier) {
        if (multiplier < 0 || multiplier > 16) {
            throw new IllegalArgumentException(
                    "Multiplier must be between 0 and 16: " + multiplier
            );
        }
    }

    private record TimedMultiplier(int multiplier, long expiresAt) {
    }
}
