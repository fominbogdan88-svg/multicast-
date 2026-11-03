package com.example.multiaction;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiActionMod.MODID)
public final class LingeringPotionHandler {

    private LingeringPotionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionImpact(ProjectileImpactEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getProjectile() instanceof ThrownPotion potion)) {
            return;
        }

        if (!(potion.level() instanceof ServerLevel level)) {
            return;
        }

        // Используем персональный множитель бросившего игрока.
        if (!(potion.getOwner() instanceof ServerPlayer thrower)) {
            return;
        }

        ItemStack originalStack = potion.getItem();

        if (!originalStack.is(Items.LINGERING_POTION)) {
            return;
        }

        List<MobEffectInstance> effects =
                PotionUtils.getMobEffects(originalStack);

        // Зелья без эффектов оставляем обычной механике Minecraft.
        if (effects.isEmpty()) {
            return;
        }

        int multiplier = MulticastManager.roll(thrower);

        if (multiplier == 0) {
            // Облако не создаётся.
            event.setCanceled(true);

            level.levelEvent(
                    2002,
                    potion.blockPosition(),
                    PotionUtils.getColor(originalStack)
            );

            potion.discard();

            MultiplierDisplay.show(thrower, 0);
            thrower.displayClientMessage(
                    Component.literal("§c✘ Оседающее зелье: срыв!"),
                    true
            );
            return;
        }

        if (multiplier == 1) {
            return;
        }

        int originalColor = PotionUtils.getColor(originalStack);
        ItemStack scaledStack = originalStack.copy();

        CompoundTag tag = scaledStack.getOrCreateTag();
        ListTag scaledEffects = new ListTag();

        for (MobEffectInstance effect : effects) {
            MobEffectInstance scaled =
                    PotionEffectScaling.scale(effect, multiplier);

            scaledEffects.add(scaled.save(new CompoundTag()));
        }

        /*
         * Переносим все эффекты в список пользовательских эффектов.
         * Базовое зелье заменяем пустым, чтобы обычные эффекты
         * не добавились второй раз.
         */
        tag.putString("Potion", "minecraft:empty");
        tag.put("CustomPotionEffects", scaledEffects);
        tag.putInt("CustomPotionColor", originalColor);

        potion.setItem(scaledStack);

        MultiplierDisplay.show(thrower, multiplier);
        thrower.displayClientMessage(
                Component.literal("§d✦ Оседающее мультизелье: x" + multiplier),
                true
        );

        // Обычное создание облака выполняет Minecraft.
    }
}
