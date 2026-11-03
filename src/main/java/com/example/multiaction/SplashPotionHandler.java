package com.example.multiaction;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class SplashPotionHandler {

    private SplashPotionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionImpact(ProjectileImpactEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getProjectile() instanceof ThrownPotion potion)) {
            return;
        }

        if (!(potion.level() instanceof ServerLevel level)) {
            return;
        }

        // Обрабатываем только зелья, брошенные игроком.
        if (!(potion.getOwner() instanceof ServerPlayer thrower)) {
            return;
        }

        ItemStack stack = potion.getItem();

        // Оседающие зелья подключим отдельно.
        if (!stack.is(Items.SPLASH_POTION)) {
            return;
        }

        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);

        /*
         * Бутылки с водой и зелья без эффектов оставляем Minecraft:
         * у воды есть отдельные взаимодействия с огнём и мобами.
         */
        if (effects.isEmpty()) {
            return;
        }

        boolean luckPotion = PotionUtils.getPotion(stack) == Potions.LUCK;
        int multiplier = MulticastManager.roll(thrower);

        HitResult hit = event.getRayTraceResult();
        Entity directHit = hit instanceof EntityHitResult entityHit
                ? entityHit.getEntity()
                : null;

        /*
         * Отменяем обычное столкновение, чтобы эффекты не применились
         * второй раз, и удаляем брошенное зелье сами.
         */
        event.setCanceled(true);

        BlockPos impactPos = potion.blockPosition();
        int color = PotionUtils.getColor(stack);

        if (multiplier == 0) {
            potion.discard();
            level.levelEvent(2002, impactPos, color);

            MultiplierDisplay.show(thrower, 0);
            thrower.displayClientMessage(
                    Component.literal("§c✘ Взрывное зелье: срыв!"),
                    true
            );
            return;
        }

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                potion.getBoundingBox().inflate(4.0D, 2.0D, 4.0D)
        );

        for (LivingEntity target : targets) {
            if (!target.isAffectedByPotions()) {
                continue;
            }

            double distanceSquared = potion.distanceToSqr(target);

            if (distanceSquared >= 16.0D && target != directHit) {
                continue;
            }

            // При прямом попадании — полная сила, иначе зависит от расстояния.
            double intensity = target == directHit
                    ? 1.0D
                    : 1.0D - Math.sqrt(distanceSquared) / 4.0D;

            boolean receivedEffect = false;

            for (MobEffectInstance original : effects) {
                MobEffectInstance scaled =
                        PotionEffectScaling.scale(original, multiplier);

                if (scaled.getEffect().isInstantenous()) {
                    scaled.getEffect().applyInstantenousEffect(
                            potion,
                            thrower,
                            target,
                            scaled.getAmplifier(),
                            intensity
                    );
                    receivedEffect = true;
                    continue;
                }

                int duration = scaled.getDuration();

                if (duration >= 0) {
                    duration = (int) (duration * intensity + 0.5D);

                    // Как и у обычного взрывного зелья, слишком короткий
                    // эффект не накладывается.
                    if (duration <= 20) {
                        continue;
                    }
                }

                MobEffectInstance applied = new MobEffectInstance(
                        scaled.getEffect(),
                        duration,
                        scaled.getAmplifier(),
                        scaled.isAmbient(),
                        scaled.isVisible(),
                        scaled.showIcon()
                );

                if (target.addEffect(applied, thrower)) {
                    receivedEffect = true;
                }
            }

            /*
             * Рулетку получают только игроки, на которых подействовало
             * именно зелье удачи.
             */
            if (luckPotion && receivedEffect
                    && target instanceof ServerPlayer affectedPlayer) {
                int rouletteResult =
                        MulticastManager.rollForPotion(affectedPlayer);

                MulticastManager.applyPotionMultiplier(
                        affectedPlayer,
                        rouletteResult
                );
                ModNetwork.sendRoulette(affectedPlayer, rouletteResult);
            }
        }

        potion.discard();
        level.levelEvent(2002, impactPos, color);

        if (multiplier > 1) {
            MultiplierDisplay.show(thrower, multiplier);
            thrower.displayClientMessage(
                    Component.literal(
                            "§d✦ Взрывное мультизелье: x" + multiplier
                    ),
                    true
            );
        }
    }
}
