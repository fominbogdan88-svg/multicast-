package com.example.multiaction;

import net.minecraft.world.effect.MobEffectInstance;

public final class PotionEffectScaling {

    private PotionEffectScaling() {
    }

    /**
     * Создаёт усиленную копию эффекта.
     *
     * multiplier должен быть от 1 до 16.
     * При x0 вызывающий обработчик вообще не должен применять эффект.
     */
    public static MobEffectInstance scale(
            MobEffectInstance original,
            int multiplier
    ) {
        if (multiplier < 1 || multiplier > 16) {
            throw new IllegalArgumentException(
                    "Potion multiplier must be between 1 and 16"
            );
        }

        /*
         * В Minecraft усилитель хранится с нуля:
         * 0 = уровень I, 1 = уровень II и так далее.
         *
         * Например, скорость II при x3 становится скоростью VI.
         */
        long originalLevel = (long) original.getAmplifier() + 1L;
        long scaledLevel = originalLevel * multiplier;

        // Ограничиваем усилитель, чтобы он помещался в данные эффекта.
        int amplifier = (int) Math.min(255L, scaledLevel - 1L);

        int originalDuration = original.getDuration();
        int duration;

        if (originalDuration < 0) {
            // Сохраняем бесконечный эффект как бесконечный.
            duration = originalDuration;
        } else {
            long scaledDuration = (long) originalDuration * multiplier;
            duration = (int) Math.min(
                    Integer.MAX_VALUE,
                    scaledDuration
            );
        }

        return new MobEffectInstance(
                original.getEffect(),
                duration,
                amplifier,
                original.isAmbient(),
                original.isVisible(),
                original.showIcon()
        );
    }
}
