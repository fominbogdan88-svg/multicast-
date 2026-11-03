package com.example.multiaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class TntHandler {

    private static final String PROCESSED_TAG = "MultiActionTntProcessed";

    private TntHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();

        if (!(entity instanceof PrimedTnt tnt)) {
            return;
        }

        // Не запускаем мультикаст повторно для созданных копий.
        if (tnt.getPersistentData().getBoolean(PROCESSED_TAG)) {
            return;
        }

        if (!(tnt.getOwner() instanceof ServerPlayer player)) {
            return;
        }

        tnt.getPersistentData().putBoolean(PROCESSED_TAG, true);

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            event.setCanceled(true);

            MultiplierDisplay.show(player, 0);

            player.displayClientMessage(
                    Component.literal("§c✘ Поджигание TNT сорвалось!"),
                    true
            );

            return;
        }

        if (multiplier == 1) {
            return;
        }

        int created = 0;

        for (int i = 1; i < multiplier; i++) {
            PrimedTnt extra = new PrimedTnt(
                    level,
                    tnt.getX(),
                    tnt.getY(),
                    tnt.getZ(),
                    player
            );

            // Метка ставится до добавления в мир.
            extra.getPersistentData().putBoolean(PROCESSED_TAG, true);

            // Небольшая задержка между взрывами.
            extra.setFuse(tnt.getFuse() + player.getRandom().nextInt(21));

            extra.setDeltaMovement(
                    (player.getRandom().nextDouble() - 0.5D) * 0.3D,
                    0.15D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.3D
            );

            if (level.addFreshEntity(extra)) {
                created++;
            }
        }

        if (created > 0) {
            // Дополнительные заряды плюс исходный.
            MultiplierDisplay.show(player, created + 1);

            player.displayClientMessage(
                    Component.literal(
                            "§6✦ Мульти-TNT: "
                                    + (created + 1)
                                    + " зарядов"
                    ),
                    true
            );

            player.playNotifySound(
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    0.2F,
                    1.2F
            );
        }
    }
}
