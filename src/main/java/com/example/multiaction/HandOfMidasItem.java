package com.example.multiaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.particles.DustParticleOptions;
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

import org.joml.Vector3f;

public class HandOfMidasItem extends Item {

    private static final double SEARCH_RADIUS = 6.0D;

    private static final DustParticleOptions GOLD_PARTICLE =
            new DustParticleOptions(
                    new Vector3f(1.0F, 0.75F, 0.05F),
                    1.2F
            );

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

        // Игроки и другие сущности, не являющиеся Mob, не обмениваются.
        if (!(target instanceof Mob targetMob)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§cРука Мидаса действует только на разрешённых мобов."
            ));
            return InteractionResult.CONSUME;
        }

        if (isForbidden(targetMob)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§cЭтого моба нельзя обменять на опыт."
            ));
            return InteractionResult.CONSUME;
        }

        int needed = requiredLevel(targetMob);

        if (serverPlayer.experienceLevel < needed) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§cДля обмена этого моба нужен уровень опыта: §e"
                            + needed
                            + " §7(категория: "
                            + categoryName(category(targetMob))
                            + ")"
            ));
            return InteractionResult.CONSUME;
        }

        int multiplier = MulticastManager.roll(serverPlayer);

        if (multiplier == 0) {
            MultiplierDisplay.show(serverPlayer, 0);

            serverPlayer.displayClientMessage(
                    Component.literal("§c✘ Рука Мидаса: срыв!"),
                    true
            );

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

        // Уровень проверяем до выдачи опыта за этот обмен.
        int levelBeforeCast = serverPlayer.experienceLevel;

        List<Mob> targets = findTargets(
                level,
                targetMob,
                levelBeforeCast,
                multiplier
        );

        int totalXp = 0;
        int exchanged = 0;

        for (Mob mob : targets) {
            if (!mob.isAlive() || mob.isRemoved()) {
                continue;
            }

            totalXp += experienceFor(mob);
            exchanged++;

            spawnExchangeEffect(level, mob);

            // Удаляем без обычной смерти, предметов и сфер опыта.
            mob.discard();
        }

        if (exchanged > 0) {
            MultiplierDisplay.show(serverPlayer, exchanged);

            serverPlayer.displayClientMessage(
                    Component.literal(
                            "§6✦ Рука Мидаса: обменено §e" + exchanged
                    ),
                    true
            );

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
                            + "§6, базовый опыт: §e"
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

    /*
     * Редкость задана вручную для отдельных ванильных мобов.
     * Остальные считаются обычными, но здоровье может повысить категорию.
     */
    private static int rarityCategory(Mob mob) {
        EntityType<?> type = mob.getType();

        // Категория 3: очень редкие или опасные.
        if (type == EntityType.ELDER_GUARDIAN
                || type == EntityType.EVOKER
                || type == EntityType.RAVAGER
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.SHULKER) {
            return 3;
        }

        // Категория 2: редкие.
        if (type == EntityType.ENDERMAN
                || type == EntityType.BLAZE
                || type == EntityType.GHAST
                || type == EntityType.GUARDIAN
                || type == EntityType.VINDICATOR
                || type == EntityType.PIGLIN
                || type == EntityType.HOGLIN) {
            return 2;
        }

        // Категория 1: необычные.
        if (type == EntityType.WITCH
                || type == EntityType.PILLAGER
                || type == EntityType.DROWNED
                || type == EntityType.STRAY
                || type == EntityType.HUSK
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.MAGMA_CUBE) {
            return 1;
        }

        return 0;
    }

    private static int healthCategory(Mob mob) {
        float health = mob.getMaxHealth();

        if (health <= 20.0F) {
            return 0;
        }

        if (health <= 40.0F) {
            return 1;
        }

        if (health <= 80.0F) {
            return 2;
        }

        return 3;
    }

    private static int category(Mob mob) {
        return Math.max(rarityCategory(mob), healthCategory(mob));
    }

    private static int requiredLevel(Mob mob) {
        return category(mob) * 10;
    }

    private static String categoryName(int category) {
        return switch (category) {
            case 1 -> "необычный";
            case 2 -> "редкий";
            case 3 -> "очень редкий";
            default -> "обычный";
        };
    }

    /*
     * Базовый опыт: от 2 до 20 очков за моба.
     * Учитываются категория и максимальное здоровье.
     */
    private static int experienceFor(Mob mob) {
        int categoryBonus = category(mob) * 3;
        int healthBonus = Math.min(
                9,
                Math.max(0, (int) (mob.getMaxHealth() / 10.0F))
        );

        return 2 + categoryBonus + healthBonus;
    }

    private static void spawnExchangeEffect(ServerLevel level, Mob mob) {
        double x = mob.getX();
        double y = mob.getY() + mob.getBbHeight() / 2.0D;
        double z = mob.getZ();

        double width = Math.max(0.25D, mob.getBbWidth() / 2.0D);
        double height = Math.max(0.25D, mob.getBbHeight() / 2.0D);

        level.sendParticles(
                GOLD_PARTICLE,
                x,
                y,
                z,
                24,
                width,
                height,
                width,
                0.02D
        );
    }
}
