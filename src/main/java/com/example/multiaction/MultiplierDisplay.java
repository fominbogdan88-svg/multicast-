package com.example.multiaction;

import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "multiaction")
public class MultiplierDisplay {

    private static final String DISPLAY_TAG = "MultiActionDisplay";
    private static final String OWNER_TAG = "MultiActionDisplayOwner";
    private static final String EXPIRES_TAG = "MultiActionDisplayExpires";

    public static void show(Entity owner, int multiplier) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }

        if (multiplier == 1) {
            return;
        }

        ArmorStand display = new ArmorStand(
                level,
                owner.getX(),
                owner.getBoundingBox().maxY + 0.35D,
                owner.getZ()
        );

        display.setInvisible(true);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.noPhysics = true;

        if (multiplier <= 0) {
            display.setCustomName(Component.literal("§c✘ Срыв!"));
        } else {
            display.setCustomName(Component.literal("§6✦ x" + multiplier));
        }

        display.setCustomNameVisible(true);

        display.getPersistentData().putBoolean(DISPLAY_TAG, true);
        display.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        display.getPersistentData().putLong(
                EXPIRES_TAG,
                level.getGameTime() + 40L
        );

        level.addFreshEntity(display);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ArmorStand display)) {
            return;
        }

        if (!(display.level() instanceof ServerLevel level)) {
            return;
        }

        if (!display.getPersistentData().getBoolean(DISPLAY_TAG)) {
            return;
        }

        long expires = display.getPersistentData().getLong(EXPIRES_TAG);

        if (level.getGameTime() >= expires
                || !display.getPersistentData().hasUUID(OWNER_TAG)) {
            display.discard();
            return;
        }

        UUID ownerId = display.getPersistentData().getUUID(OWNER_TAG);
        Entity owner = level.getEntity(ownerId);

        if (owner == null || owner.isRemoved()) {
            display.discard();
            return;
        }

        display.setPos(
                owner.getX(),
                owner.getBoundingBox().maxY + 0.35D,
                owner.getZ()
        );
    }
}
