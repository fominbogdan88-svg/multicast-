package com.example.multiaction;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HandOfMidasItem extends Item {

    private static final double SEARCH_RADIUS = 6.0D;

    public HandOfMidasItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        if (!(target instanceof Mob targetMob)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§cРука Мидаса действует только на мобов."
            ));
            return InteractionResult.CONSUME;
        }

        if (isForbidden(targetMob)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§cЭтого моба нельзя обменять на опыт."
            ));
            return InteractionResult.CONSUME;
        }

        int requiredLevel = requiredLevel(targetMob);

        if (serverPlayer.experienceLevel < requiredLevel) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§cДля обмена этого моба нужен уровень опыта: §e"
                            + requiredLevel
            ));
            return InteractionResult.CONSUME;
        }

        int multiplier = MulticastManager.roll(serverPlayer);

        showMultiplier(serverPlayer, multiplier);

        if (multiplier == 0) {
            MultiplierDisplay.show(serverPlayer, 0);

            level.playSound(
                    null,
                    serverPlayer.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS,
                    0.35F,
                    1.4F
            );

            return InteractionResult.CONSUME;
        }

        /*
         * Уровень проверяем до получения опыта от обмена.
         * Это не даёт открыть более сильных мобов посреди одного каста.
         */
        int playerLevelBeforeCast = serverPlayer.experienceLevel;

        List<Mob> targets = findTargets(
                level,
                targetMob,
                playerLevelBeforeCast,
                multiplier
        );

        int totalXp = 0;
        int exchanged = 0;

        for (Mob mob : targets) {
            if (!mob.isAlive() || mob.isRemoved()) {
                continue;
            }

            int xp = experienceFor(mob);
            totalXp += xp;
            exchanged++;

            spawnExchangeEffect(level, mob);

            /*
             * discard() удаляет моба без обычной смерти:
             * предметы и стандартные сферы опыта не выпадают.
             */
            mob.discard();
        }

        if (exchanged > 0) {
            // x1 метод отображения пропустит.
            MultiplierDisplay.show(serverPlayer, exchanged);

            serverPlayer.giveExperiencePoints(totalXp);

            level.playSound(
                    null,
                    serverPlayer.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    0.6F,
                    1.1F
            );

            serverPlayer.sendSystemMessage(Component.literal(
                    "§6Рука Мидаса: §eобменено мобов: "
                            + exchanged
                            + "§6, получено опыта: §e"
                            + totalXp
            ));
        }

        return InteractionResult.CONSUME;
    }

    private static List<Mob> findTargets(
            ServerLevel level,
            Mob primary,
            int playerLevel,
            int limit
    ) {
        List<Mob> result = new ArrayList<>();

        // Основная цель всегда первая.
        result.add(primary);

        if (limit <= 1) {
            return result;
        }

        AABB area = primary.getBoundingBox().inflate(SEARCH_RADIUS);

        List<Mob> nearby = level.getEntitiesOfClass(
                Mob.class,
                area,
                mob -> mob != primary
                        && mob.isAlive()
                        && !mob.isRemoved()
                        && !isForbidden(mob)
                        && playerLevel >= requiredLevel(mob)
        );

        nearby.sort(Comparator.comparingDouble(primary::distanceToSqr));

        for (Mob mob : nearby) {
            if (result.size() >= limit) {
                break;
            }

            result.add(mob);
        }

        return result;
    }

    private static boolean isForbidden(Mob mob) {
        EntityType<?> type = mob.getType();

        return type == EntityType.WARDEN
                || type == EntityType.PIGLIN_BRUTE
                || type == EntityType.ENDER_DRAGON
                || type == EntityType.WITHER;
    }

    /**
     * Простое разделение по максимальному здоровью.
     *
     * Приблизительное правило:
     * - до 20 HP: уровень 0;
     * - до 40 HP: уровень 10;
     * - до 80 HP: уровень 20;
     * - выше 80 HP: уровень 30.
     */
    private static int requiredLevel(Mob mob) {
        float health = mob.getMaxHealth();

        if (health <= 20.0F) {
            return 0;
        }

        if (health <= 40.0F) {
            return 10;
        }

        if (health <= 80.0F) {
            return 20;
        }

        return 30;
    }

    /**
     * Очки опыта за одного моба, а не уровни игрока.
     */
    private static int experienceFor(Mob mob) {
        float health = mob.getMaxHealth();

        if (health <= 20.0F) {
            return 2;
        }

        if (health <= 40.0F) {
            return 4;
        }

        if (health <= 80.0F) {
            return 6;
        }

        return 10;
    }

    private static void spawnExchangeEffect(ServerLevel level, Mob mob) {
        double x = mob.getX();
        double y = mob.getY() + mob.getBbHeight() / 2.0D;
        double z = mob.getZ();

        double width = Math.max(0.25D, mob.getBbWidth() / 2.0D);
        double height = Math.max(0.25D, mob.getBbHeight() / 2.0D);

        level.sendParticles(
                ParticleTypes.END_ROD,
                x,
                y,
                z,
                12,
                width,
                height,
                width,
                0.03D
        );

        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                x,
                y,
                z,
                8,
                width,
                height,
                width,
                0.05D
        );
    }

    private static void showMultiplier(ServerPlayer player, int multiplier) {
        String message;

        if (multiplier == 0) {
            message = "§c✘ Рука Мидаса: срыв!";
        } else {
            message = "§6✦ Рука Мидаса: x" + multiplier;
        }

        // true = над панелью быстрого доступа, не в чате.
        player.displayClientMessage(Component.literal(message), true);
    }
}
