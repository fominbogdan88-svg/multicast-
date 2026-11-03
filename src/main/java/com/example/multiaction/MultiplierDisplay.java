package com.example.multiaction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class MultiplierDisplay {

    private static final String DISPLAY_TAG = "MultiActionDisplay";
    private static final String OWNER_TAG = "MultiActionDisplayOwner";
    private static final String AGE_TAG = "MultiActionDisplayAge";

    // 40 тиков = примерно 2 секунды.
    private static final int LIFETIME_TICKS = 40;

    private MultiplierDisplay() {
    }

    /**
     * Показывает результат всем игрокам поблизости.
     * Вызывать только на сервере.
     */
    public static void show(Entity owner, int multiplier) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }

        // Для обычного действия надпись не нужна.
        if (multiplier == 1) {
            return;
        }

        ArmorStand display = new ArmorStand(
                level,
                owner.getX(),
                owner.getY() + owner.getBbHeight() + 0.3D,
                owner.getZ()
        );

        display.setInvisible(true);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setMarker(true);
        display.setCustomNameVisible(true);

        Component text = multiplier == 0
                ? Component.literal("§c✘ Срыв!")
                : Component.literal("§6✦ x" + multiplier);

        display.setCustomName(text);

        CompoundTag data = display.getPersistentData();
        data.putBoolean(DISPLAY_TAG, true);
        data.putUUID(OWNER_TAG, owner.getUUID());
        data.putInt(AGE_TAG, 0);

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

        CompoundTag data = display.getPersistentData();

        if (!data.getBoolean(DISPLAY_TAG)) {
            return;
        }

        int age = data.getInt(AGE_TAG) + 1;
        data.putInt(AGE_TAG, age);

        if (age >= LIFETIME_TICKS || !data.hasUUID(OWNER_TAG)) {
            display.discard();
            return;
        }

        UUID ownerId = data.getUUID(OWNER_TAG);
        Entity owner = level.getEntity(ownerId);

        if (owner == null || owner.isRemoved()) {
            display.discard();
            return;
        }

        // Надпись следует за владельцем и немного поднимается вверх.
        double rise = age * 0.01D;

        display.setPos(
                owner.getX(),
                owner.getY() + owner.getBbHeight() + 0.3D + rise,
                owner.getZ()
        );
    }
}
