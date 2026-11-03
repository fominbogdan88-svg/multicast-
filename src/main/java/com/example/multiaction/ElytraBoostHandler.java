package com.example.multiaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class ElytraBoostHandler {

    // Примерная базовая длительность: 1,5 секунды.
    private static final int BASE_DURATION_TICKS = 30;

    // Если игрок почти неподвижен, используем минимальную начальную скорость.
    private static final double MIN_START_SPEED = 0.5D;

    private static final Map<UUID, Boost> ACTIVE_BOOSTS = new HashMap<>();

    private ElytraBoostHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.isFallFlying()) {
            return;
        }

        if (!event.getItemStack().is(Items.FIREWORK_ROCKET)) {
            return;
        }

        // Не обрабатываем вторую руку, если ракета есть в основной.
        if (event.getHand() == InteractionHand.OFF_HAND
                && player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
            return;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            MultiplierDisplay.show(player, 0);

            player.displayClientMessage(
                    Component.literal("§c✘ Дополнительное ускорение сорвалось!"),
                    true
            );

            // Обычную ракету не отменяем.
            return;
        }

        if (multiplier == 1) {
            return;
        }

        double currentSpeed = player.getDeltaMovement().length();
        double startingSpeed = Math.max(MIN_START_SPEED, currentSpeed);

        // Без верхнего ограничения, как ты попросил.
        double targetSpeed = startingSpeed * multiplier;
        int duration = BASE_DURATION_TICKS * multiplier;

        ACTIVE_BOOSTS.put(
                player.getUUID(),
                new Boost(targetSpeed, duration)
        );

        MultiplierDisplay.show(player, multiplier);

        player.displayClientMessage(
                Component.literal(
                        "§b✦ Мультиускорение: x" + multiplier
                                + " §7(скорость и время)"
                ),
                true
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

        UUID id = player.getUUID();
        Boost boost = ACTIVE_BOOSTS.get(id);

        if (boost == null) {
            return;
        }

        // Прекращаем ускорение после приземления или смерти.
        if (!player.isAlive() || !player.isFallFlying()) {
            ACTIVE_BOOSTS.remove(id);
            return;
        }

        Vec3 direction = player.getLookAngle().normalize();

        // Скорость задаётся относительно направления взгляда.
        // Она не умножается повторно каждый тик.
        player.setDeltaMovement(direction.scale(boost.speed()));
        player.hurtMarked = true;

        int remaining = boost.remainingTicks() - 1;

        if (remaining <= 0) {
            ACTIVE_BOOSTS.remove(id);
        } else {
            ACTIVE_BOOSTS.put(
                    id,
                    new Boost(boost.speed(), remaining)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_BOOSTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_BOOSTS.clear();
    }

    private record Boost(double speed, int remainingTicks) {
    }
}
