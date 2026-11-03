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

        // Поплавки удочек не копируем.
        if (original instanceof FishingHook) {
            return;
        }

        if (original.getPersistentData().getBoolean(PROCESSED_TAG)) {
            return;
        }

        // Обрабатываем только снаряды, владельцем которых указан моб.
        if (!(original.getOwner() instanceof Mob shooter)) {
            return;
        }

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

        // Показываем фактическое количество снарядов над головой моба.
        if (copiesCreated > 0) {
            MultiplierDisplay.show(shooter, copiesCreated + 1);
        }
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

        CompoundTag data = new CompoundTag();
        original.saveWithoutId(data);

        // Копии нужен собственный UUID.
        data.remove("UUID");
        copy.load(data);
        copy.setUUID(UUID.randomUUID());

        copy.setOwner(shooter);

        // Метка ставится до добавления копии в мир.
        copy.getPersistentData().putBoolean(PROCESSED_TAG, true);

        // Дополнительные стрелы и трезубцы нельзя подобрать в выживании.
        if (copy instanceof AbstractArrow arrow) {
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }

        Vec3 motion = original.getDeltaMovement();

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
