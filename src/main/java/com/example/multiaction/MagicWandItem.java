package com.example.multiaction;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MagicWandItem extends Item {

    private static final int XP_COST = 15;
    private static final String MODE_TAG = "MultiActionWandMode";

    private static final String[] MODE_NAMES = {
            "Молния",
            "Гнев земли",
            "Дыхание дракона",
            "Регенерация"
    };

    public MagicWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        CompoundTag tag = stack.getOrCreateTag();
        int mode = Math.floorMod(tag.getInt(MODE_TAG), MODE_NAMES.length);

        if (player.isShiftKeyDown()) {
            mode = (mode + 1) % MODE_NAMES.length;
            tag.putInt(MODE_TAG, mode);

            player.displayClientMessage(
                    Component.literal("§d✦ Режим: " + MODE_NAMES[mode]),
                    true
            );

            return InteractionResultHolder.consume(stack);
        }

        if (getAvailableExperience(player) < XP_COST) {
            player.displayClientMessage(
                    Component.literal("§cДля заклинания нужно 15 очков опыта."),
                    true
            );

            return InteractionResultHolder.fail(stack);
        }

        // Снимаем очки опыта, а не целые уровни.
        player.giveExperiencePoints(-XP_COST);

        int multiplier = MulticastManager.roll(serverPlayer);

        if (multiplier == 0) {
            player.displayClientMessage(
                    Component.literal("§c✘ Заклинание сорвалось!"),
                    true
            );

            return InteractionResultHolder.consume(stack);
        }

        HitResult hit = player.pick(24.0D, 0.0F, false);
        Vec3 target = hit.getLocation();

        switch (mode) {
            case 0 -> castLightning(serverLevel, serverPlayer, target, multiplier);
            case 1 -> castFangs(serverLevel, serverPlayer, target, multiplier);
            case 2 -> castDragonBreath(serverLevel, serverPlayer, target, multiplier);
            case 3 -> castRegeneration(serverLevel, serverPlayer, multiplier);
            default -> {
                return InteractionResultHolder.fail(stack);
            }
        }

        player.displayClientMessage(
                Component.literal(
                        "§d✦ " + MODE_NAMES[mode] + ": x" + multiplier
                ),
                true
        );

        player.playNotifySound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.2F,
                1.3F
        );

        return InteractionResultHolder.consume(stack);
    }

    private static void castLightning(
            ServerLevel level,
            ServerPlayer player,
            Vec3 target,
            int multiplier
    ) {
        int accurateCount = (multiplier + 1) / 2;

        for (int i = 0; i < multiplier; i++) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);

            if (lightning == null) {
                continue;
            }

            double x = target.x;
            double z = target.z;

            if (i >= accurateCount) {
                x += (player.getRandom().nextDouble() - 0.5D) * 5.0D;
                z += (player.getRandom().nextDouble() - 0.5D) * 5.0D;
            }

            lightning.moveTo(x, target.y, z);
            lightning.setCause(player);
            level.addFreshEntity(lightning);
        }
    }

    private static void castFangs(
            ServerLevel level,
            ServerPlayer player,
            Vec3 target,
            int multiplier
    ) {
        for (int i = 0; i < multiplier; i++) {
            double angle = 2.0D * Math.PI * i / multiplier;
            double radius = multiplier == 1 ? 0.0D : 1.5D;

            double x = target.x + Math.cos(angle) * radius;
            double z = target.z + Math.sin(angle) * radius;

            EvokerFangs fangs = new EvokerFangs(
                    level,
                    x,
                    target.y,
                    z,
                    (float) angle,
                    i,
                    player
            );

            level.addFreshEntity(fangs);
        }

        AABB area = new AABB(target, target).inflate(3.0D);

        List<ServerPlayer> targets = level.getEntitiesOfClass(
                ServerPlayer.class,
                area,
                other -> other != player && other.isAlive()
        );

        for (ServerPlayer other : targets) {
            other.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    60,
                    0
            ));

            other.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    60,
                    4
            ));
        }
    }

    private static void castDragonBreath(
            ServerLevel level,
            ServerPlayer player,
            Vec3 target,
            int multiplier
    ) {
        double radius = Math.min(6.0D, 2.0D + multiplier * 0.25D);
        AABB area = new AABB(target, target).inflate(radius);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player && entity.isAlive()
        );

        int duration = Math.min(400, 40 + multiplier * 20);
        int amplifier = Math.min(2, multiplier / 6);

        for (LivingEntity entity : targets) {
            entity.addEffect(new MobEffectInstance(
                    MobEffects.WITHER,
                    duration,
                    amplifier
            ));
        }

        level.sendParticles(
                ParticleTypes.DRAGON_BREATH,
                target.x,
                target.y + 0.5D,
                target.z,
                50,
                radius / 2.0D,
                0.5D,
                radius / 2.0D,
                0.02D
        );
    }

    private static void castRegeneration(
            ServerLevel level,
            ServerPlayer player,
            int multiplier
    ) {
        int amplifier = Math.min(4, (multiplier - 1) / 4);
        int duration = Math.min(400, 60 + multiplier * 15);

        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                duration,
                amplifier
        ));

        level.sendParticles(
                ParticleTypes.HEART,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                10,
                0.4D,
                0.5D,
                0.4D,
                0.02D
        );
    }

    /**
     * Считает доступные очки опыта, включая прогресс текущего уровня.
     */
    private static int getAvailableExperience(Player player) {
        int level = player.experienceLevel;
        int base;

        if (level <= 16) {
            base = level * level + 6 * level;
        } else if (level <= 31) {
            base = (int) (
                    2.5D * level * level
                            - 40.5D * level
                            + 360.0D
            );
        } else {
            base = (int) (
                    4.5D * level * level
                            - 162.5D * level
                            + 2220.0D
            );
        }

        int progress = Math.round(
                player.experienceProgress
                        * player.getXpNeededForNextLevel()
        );

        return base + progress;
    }
}
