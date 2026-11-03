package com.example.multiaction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public final class MulticastManager {

    public static final long POTION_DURATION_TICKS = 251L * 20L;

    private static final int[] NORMAL_WEIGHTS = {
            30, 600, 180, 85, 45, 25, 15, 10, 7,
            5, 4, 3, 2, 2, 1, 1, 1
    };

    private static final int[] BALANCED_WEIGHTS = {
            20, 800, 120, 40, 15, 8, 5, 3, 2,
            2, 1, 1, 1, 1, 1, 1, 1, 1
    };

    private MulticastManager() {
    }

    public enum ProbabilityMode {
        NORMAL,
        BALANCED,
        EQUAL
    }

    public static int roll(ServerPlayer player) {
        PlayerSettingsData data = getData(player);
        CompoundTag settings = data.getPlayerSettings(player);

        int base;

        if (settings.contains("Forced")) {
            int forced = settings.getInt("Forced");

            if (forced >= 0 && forced <= 16) {
                return addBonus(forced, settings);
            }
        }

        if (settings.contains("PotionMultiplier")
                && settings.contains("PotionExpires")) {
            long now = player.server.overworld().getGameTime();
            int potionMultiplier = settings.getInt("PotionMultiplier");

            if (now < settings.getLong("PotionExpires")
                    && potionMultiplier >= 0
                    && potionMultiplier <= 16) {
                return addBonus(potionMultiplier, settings);
            }

            settings.remove("PotionMultiplier");
            settings.remove("PotionExpires");
            data.setPlayerSettings(player, settings);
        }

        base = rollRandom(player);
        return addBonus(base, settings);
    }

    private static int addBonus(int base, CompoundTag settings) {
        int bonus = Math.max(0, Math.min(16, settings.getInt("PotatoBonus")));
        return Math.min(16, base + bonus);
    }

    /*
     * Рулетка выбирает базовый множитель.
     * Бонус прибавляется при последующих действиях через roll().
     */
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
        PlayerSettingsData data = getData(player);
        CompoundTag settings = data.getPlayerSettings(player);

        if (multiplier == -1) {
            settings.remove("Forced");
        } else {
            validate(multiplier);
            settings.putInt("Forced", multiplier);
        }

        data.setPlayerSettings(player, settings);
    }

    public static Integer getForcedMultiplier(ServerPlayer player) {
        CompoundTag settings = getData(player).getPlayerSettings(player);

        if (!settings.contains("Forced")) {
            return null;
        }

        int forced = settings.getInt("Forced");
        return forced >= 0 && forced <= 16 ? forced : null;
    }

    public static void applyPotionMultiplier(
            ServerPlayer player,
            int multiplier
    ) {
        validate(multiplier);

        PlayerSettingsData data = getData(player);
        CompoundTag settings = data.getPlayerSettings(player);
        long now = player.server.overworld().getGameTime();

        settings.putInt("PotionMultiplier", multiplier);
        settings.putLong("PotionExpires", now + POTION_DURATION_TICKS);

        data.setPlayerSettings(player, settings);
    }

    public static void clearPotionMultiplier(ServerPlayer player) {
        PlayerSettingsData data = getData(player);
        CompoundTag settings = data.getPlayerSettings(player);

        settings.remove("PotionMultiplier");
        settings.remove("PotionExpires");

        data.setPlayerSettings(player, settings);
    }

    public static ProbabilityMode getMode(ServerPlayer player) {
        CompoundTag settings = getData(player).getPlayerSettings(player);
        String savedMode = settings.getString("Mode");

        try {
            return ProbabilityMode.valueOf(savedMode);
        } catch (IllegalArgumentException exception) {
            return ProbabilityMode.NORMAL;
        }
    }

    public static void setMode(
            ServerPlayer player,
            ProbabilityMode mode
    ) {
        PlayerSettingsData data = getData(player);
        CompoundTag settings = data.getPlayerSettings(player);

        if (mode == ProbabilityMode.NORMAL) {
            settings.remove("Mode");
        } else {
            settings.putString("Mode", mode.name());
        }

        data.setPlayerSettings(player, settings);
    }

    public static boolean toggleEqual(ServerPlayer player) {
        if (getMode(player) == ProbabilityMode.EQUAL) {
            setMode(player, ProbabilityMode.NORMAL);
            return false;
        }

        setMode(player, ProbabilityMode.EQUAL);
        return true;
    }

    public static int getPotatoBonus(ServerPlayer player) {
        CompoundTag settings = getData(player).getPlayerSettings(player);
        return Math.max(0, Math.min(16, settings.getInt("PotatoBonus")));
    }

    /**
     * Добавляет один постоянный бонус и возвращает его новое значение.
     */
    public static int addPotatoBonus(ServerPlayer player) {
        PlayerSettingsData data = getData(player);
        CompoundTag settings = data.getPlayerSettings(player);

        int current = Math.max(
                0,
                Math.min(16, settings.getInt("PotatoBonus"))
        );

        int updated = Math.min(16, current + 1);

        settings.putInt("PotatoBonus", updated);
        data.setPlayerSettings(player, settings);

        return updated;
    }

    // Оставлено для совместимости с MultiActionMod.
    public static void clearAll() {
    }

    private static void validate(int multiplier) {
        if (multiplier < 0 || multiplier > 16) {
            throw new IllegalArgumentException(
                    "Multiplier must be between 0 and 16: " + multiplier
            );
        }
    }

    private static PlayerSettingsData getData(ServerPlayer player) {
        return player.server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PlayerSettingsData::load,
                        PlayerSettingsData::new,
                        "multiaction_players"
                );
    }

    public static final class PlayerSettingsData extends SavedData {

        private CompoundTag players = new CompoundTag();

        public PlayerSettingsData() {
        }

        public static PlayerSettingsData load(CompoundTag tag) {
            PlayerSettingsData data = new PlayerSettingsData();
            data.players = tag.getCompound("Players").copy();
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.put("Players", players.copy());
            return tag;
        }

        private CompoundTag getPlayerSettings(ServerPlayer player) {
            return players.getCompound(player.getUUID().toString()).copy();
        }

        private void setPlayerSettings(
                ServerPlayer player,
                CompoundTag settings
        ) {
            String id = player.getUUID().toString();

            if (settings.isEmpty()) {
                players.remove(id);
            } else {
                players.put(id, settings.copy());
            }

            setDirty();
        }
    }
}
