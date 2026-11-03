package com.example.multiaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class TotemMulticastHandler {

    /*
     * Эффекты применяем на следующем тике: Minecraft должен сначала
     * закончить обычное срабатывание тотема.
     */
    private static final Map<UUID, PendingTotem> PENDING = new HashMap<>();

    private TotemMulticastHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTotemUse(LivingUseTotemEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        // При x0 и x1 оставляем обычное спасение без изменений.
        if (multiplier <= 1) {
            return;
        }

        long applyAt = player.server.overworld().getGameTime() + 1L;

        PENDING.put(
                player.getUUID(),
                new PendingTotem(multiplier, applyAt)
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        PendingTotem pending = PENDING.get(player.getUUID());

        if (pending == null) {
            return;
        }

        long now = player.server.overworld().getGameTime();

        if (now < pending.applyAt()) {
            return;
        }

        PENDING.remove(player.getUUID());

        if (!player.isAlive()) {
            return;
        }

        int multiplier = pending.multiplier();

        /*
         * Длительность стандартных эффектов умножается.
         * Сила регенерации и поглощения растёт постепенно,
         * максимум до пятого уровня эффекта.
         */
        int amplifier = Math.min(4, 1 + (multiplier - 1) / 4);

        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                900 * multiplier,
                amplifier
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION,
                100 * multiplier,
                amplifier
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                800 * multiplier,
                0
        ));

        MultiplierDisplay.show(player, multiplier);

        player.displayClientMessage(
                Component.literal("§6✦ Мультитотем: x" + multiplier),
                true
        );
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    private record PendingTotem(int multiplier, long applyAt) {
    }
}
