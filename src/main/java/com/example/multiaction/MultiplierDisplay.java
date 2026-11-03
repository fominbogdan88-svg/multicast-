package com.example.multiaction;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "multiaction")
public final class MultiplierDisplay {

    private static final String DISPLAY_TAG = "MultiActionDisplay";
    private static final String OWNER_TAG = "MultiActionDisplayOwner";
    private static final String EXPIRES_TAG = "MultiActionDisplayExpires";

    // Время, когда сущности снова разрешено воспроизвести звук.
    private static final String NEXT_SOUND_TAG = "MultiActionNextDisplaySound";

    // Надпись показывается около двух секунд.
    private static final long DISPLAY_TICKS = 40L;

    // Минимальная задержка между звуками — полсекунды.
    private static final long SOUND_COOLDOWN_TICKS = 10L;

    private static final double HEIGHT_OFFSET = 0.15D;

    private MultiplierDisplay() {
    }

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
                owner.getBoundingBox().maxY + HEIGHT_OFFSET,
                owner.getZ()
        );

        /*
         * Устанавливаем свойства через NBT: прямые методы setMarker
         * и setSmall недоступны в этой версии.
         */
        CompoundTag standData = new CompoundTag();
        display.saveWithoutId(standData);
        standData.putBoolean("Marker", true);
        standData.putBoolean("Small", true);
        standData.putBoolean("NoBasePlate", true);
        display.load(standData);

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
                level.getGameTime() + DISPLAY_TICKS
        );

        if (level.addFreshEntity(display)) {
            playLimitedSound(level, owner, multiplier);
        }
    }

    private static void playLimitedSound(
            ServerLevel level,
            Entity owner,
            int multiplier
    ) {
        long now = level.getGameTime();
        CompoundTag ownerData = owner.getPersistentData();

        if (ownerData.contains(NEXT_SOUND_TAG)
                && now < ownerData.getLong(NEXT_SOUND_TAG)) {
            return;
        }

        ownerData.putLong(NEXT_SOUND_TAG, now + SOUND_COOLDOWN_TICKS);

        level.playSound(
                null,
                owner.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.2F,
                multiplier <= 0 ? 0.7F : 1.3F
        );
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
                owner.getBoundingBox().maxY + HEIGHT_OFFSET,
                owner.getZ()
        );
    }
}
