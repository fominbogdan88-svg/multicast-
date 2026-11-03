package com.example.multiaction;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "multiaction")
public class PlayerProjectileHandler {

    /*
     * Защита от повторного копирования уже созданных копий.
     */
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

        if (!(event.getEntity() instanceof Projectile projectile)) {
            return;
        }

        /*
         * Работаем только со стрелами, трезубцами и ракетами.
         * Рыболовные поплавки и остальные снаряды не копируются.
         */
        if (!(projectile instanceof AbstractArrow)
                && !(projectile instanceof FireworkRocketEntity)) {
            return;
        }

        if (!(projectile.getOwner() instanceof ServerPlayer player)) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 1) {
            return;
        }

        MultiplierDisplay.show(player, multiplier);

        if (multiplier <= 0) {
            event.setCanceled(true);
            return;
        }

        CompoundTag projectileData = new CompoundTag();
        projectile.saveWithoutId(projectileData);

        CREATING_COPIES.set(true);

        try {
            for (int i = 1; i < multiplier; i++) {
                Entity created = projectile.getType().create(level);

                if (!(created instanceof Projectile copy)) {
                    continue;
                }

                CompoundTag copyData = projectileData.copy();

                /*
                 * Каждому снаряду нужен собственный UUID.
                 */
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

                /*
                 * Копии стрел и трезубцев нельзя подобрать
                 * в выживании, чтобы не получать бесплатные предметы.
                 * Оригинальный снаряд сохраняет обычные правила подбора.
                 */
                if (copy instanceof AbstractArrow arrowCopy) {
                    arrowCopy.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }

                level.addFreshEntity(copy);
            }
        } finally {
            CREATING_COPIES.remove();
        }
    }
}
