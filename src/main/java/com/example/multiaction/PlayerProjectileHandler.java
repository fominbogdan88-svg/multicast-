package com.example.multiaction;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "multiaction")
public class PlayerProjectileHandler {

    // Не обрабатываем созданные копии повторно.
    private static final ThreadLocal<Boolean> CREATING_COPIES =
            ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (event.loadedFromDisk() || CREATING_COPIES.get()) {
            return;
        }

        // AbstractArrow включает стрелы и трезубцы.
        if (!(event.getEntity() instanceof AbstractArrow projectile)) {
            return;
        }

        if (!(projectile.getOwner() instanceof ServerPlayer player)) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 1) {
            return;
        }

        if (multiplier <= 0) {
            event.setCanceled(true);
            MultiplierDisplay.show(player, 0);
            return;
        }

        CompoundTag projectileData = new CompoundTag();
        projectile.saveWithoutId(projectileData);

        int createdCount = 0;
        CREATING_COPIES.set(true);

        try {
            for (int i = 1; i < multiplier; i++) {
                Entity created = projectile.getType().create(level);

                if (!(created instanceof Projectile copy)) {
                    continue;
                }

                CompoundTag copyData = projectileData.copy();

                // Каждой копии нужен собственный UUID.
                copyData.remove("UUID");
                copy.load(copyData);
                copy.setUUID(UUID.randomUUID());

                copy.setOwner(player);
                copy.setPos(
                        projectile.getX(),
                        projectile.getY(),
                        projectile.getZ()
                );
                copy.setDeltaMovement(projectile.getDeltaMovement());

                // Дополнительные снаряды нельзя подобрать в выживании.
                if (copy instanceof AbstractArrow arrowCopy) {
                    arrowCopy.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }

                if (level.addFreshEntity(copy)) {
                    createdCount++;
                }
            }
        } finally {
            CREATING_COPIES.remove();
        }

        if (createdCount > 0) {
            // Копии плюс исходный снаряд.
            MultiplierDisplay.show(player, createdCount + 1);
        }
    }
}
