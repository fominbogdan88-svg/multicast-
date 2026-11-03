package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class BreedingHandler {

    private BreedingHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getCausedByPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        AgeableMob parentA = event.getParentA();
        AgeableMob parentB = event.getParentB();
        AgeableMob originalBaby = event.getChild();

        if (parentA == null || parentB == null || originalBaby == null) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier <= 1) {
            return;
        }

        int spawned = 0;

        // Одного детёныша создаст обычная механика игры.
        for (int i = 1; i < multiplier; i++) {
            AgeableMob extra = parentA.getBreedOffspring(level, parentB);

            if (extra == null) {
                continue;
            }

            extra.setBaby(true);
            extra.moveTo(
                    parentA.getX(),
                    parentA.getY(),
                    parentA.getZ(),
                    parentA.getYRot(),
                    0.0F
            );

            if (level.addFreshEntity(extra)) {
                spawned++;
            }
        }

        if (spawned > 0) {
            player.displayClientMessage(
                    Component.literal(
                            "§a✦ Мультиразмножение: дополнительно "
                                    + spawned
                            ),
                    true
            );

            player.playNotifySound(
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    0.2F,
                    1.4F
            );
        }
    }
}
