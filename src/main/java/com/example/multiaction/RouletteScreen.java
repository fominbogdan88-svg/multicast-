package com.example.multiaction;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RouletteScreen extends Screen {

    // 20 тиков = примерно одна секунда.
    private static final int SPIN_TICKS = 40;
    private static final int TOTAL_TICKS = 70;

    private final int result;

    private int ticks;
    private int displayedMultiplier;
    private boolean resultSoundPlayed;

    public RouletteScreen(int result) {
        super(Component.literal("Рулетка удачи"));

        this.result = Math.max(0, Math.min(16, result));
        this.displayedMultiplier = 0;
    }

    @Override
    public void tick() {
        super.tick();

        ticks++;

        if (ticks < SPIN_TICKS) {
            // Числа меняются во время вращения.
            // Это только визуальная анимация.
            if (ticks % 2 == 0 && minecraft != null) {
                displayedMultiplier = minecraft.player != null
                        ? minecraft.player.getRandom().nextInt(17)
                        : 0;
            }
        } else {
            displayedMultiplier = result;

            if (!resultSoundPlayed) {
                resultSoundPlayed = true;

                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playNotifySound(
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS,
                            0.25F,
                            1.4F
                    );
                }
            }
        }

        if (ticks >= TOTAL_TICKS) {
            onClose();
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        int centerX = width / 2;
        int centerY = height / 2;

        // Тёмная панель с золотой рамкой.
        graphics.fill(
                centerX - 105,
                centerY - 58,
                centerX + 105,
                centerY + 58,
                0xFFB88724
        );

        graphics.fill(
                centerX - 103,
                centerY - 56,
                centerX + 103,
                centerY + 56,
                0xEE17110B
        );

        graphics.drawCenteredString(
                font,
                Component.literal("§6§lРУЛЕТКА УДАЧИ"),
                centerX,
                centerY - 42,
                0xFFFFFF
        );

        String status = ticks < SPIN_TICKS
                ? "§eВыбираем множитель..."
                : "§aВаш результат!";

        graphics.drawCenteredString(
                font,
                Component.literal(status),
                centerX,
                centerY - 20,
                0xFFFFFF
        );

        String valueColor = ticks < SPIN_TICKS
                ? "§f"
                : result == 0
                        ? "§c"
                        : result >= 10
                                ? "§6"
                                : "§a";

        graphics.drawCenteredString(
                font,
                Component.literal(
                        valueColor + "§l" + displayedMultiplier + "x"
                ),
                centerX,
                centerY,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                font,
                Component.literal("§7Действует 4 минуты 11 секунд"),
                centerX,
                centerY + 24,
                0xFFFFFF
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
