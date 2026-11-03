package com.example.multiaction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class MobProjectileHandler {

    private static final String PROCESSED_TAG =
            "MultiActionMobProjectileProcessed";

    private MobProjectileHandler() {
    }

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getEntity() instanceof Projectile original)) {
            return;
        }

        // Поплавки удочек никогда не копируем.
        if (original instanceof FishingHook) {
            return;
        }

        // Не обрабатываем снаряд повторно.
        if (original.getPersistentData().getBoolean(PROCESSED_TAG)) {
            return;
        }

        // Механика предназначена только для снарядов мобов.
        if (!(original.getOwner() instanceof Mob shooter)) {
            return;
        }

        // Метка сохраняется вместе с сущностью.
        original.getPersistentData().putBoolean(PROCESSED_TAG, true);

        int roll = shooter.getRandom().nextInt(1000);
        int multiplier;

        if (roll == 0) {
            multiplier = 3; // 0,1%
        } else if (roll <= 10) {
            multiplier = 2; // 1%
        } else {
            return;
        }

        int copiesCreated = 0;

        for (int i = 1; i < multiplier; i++) {
            if (createCopy(level, original, shooter)) {
                copiesCreated++;
            }
        }

        /*
         * Надпись над головой пока не добавлена.
         * Подключим её к copiesCreated + 1, когда создадим
         * общий обработчик видимых множителей.
         */
    }

    private static boolean createCopy(
            ServerLevel level,
            Projectile original,
            Mob shooter
    ) {
        Entity created = original.getType().create(level);

        if (!(created instanceof Projectile copy)) {
            return false;
        }

        /*
         * Сохраняем данные исходного снаряда: положение, скорость
         * и поддерживаемые его типом дополнительные свойства.
         */
        CompoundTag data = new CompoundTag();
        original.saveWithoutId(data);

        // У копии должен быть свой UUID.
        data.remove("UUID");
        copy.load(data);
        copy.setUUID(UUID.randomUUID());

        copy.setOwner(shooter);

        // Ставим метку до добавления сущности в мир.
        copy.getPersistentData().putBoolean(PROCESSED_TAG, true);

        // Дополнительные стрелы и трезубцы нельзя подобрать в выживании.
        if (copy instanceof AbstractArrow arrow) {
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }

        Vec3 motion = original.getDeltaMovement();

        // Небольшой разброс направления.
        copy.setDeltaMovement(
                motion.add(
                        (shooter.getRandom().nextDouble() - 0.5D) * 0.08D,
                        (shooter.getRandom().nextDouble() - 0.5D) * 0.08D,
                        (shooter.getRandom().nextDouble() - 0.5D) * 0.08D
                )
        );

        return level.addFreshEntity(copy);
    }
}
