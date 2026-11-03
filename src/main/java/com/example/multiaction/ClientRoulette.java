package com.example.multiaction;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientRoulette {

    private ClientRoulette() {
    }

    public static void open(int multiplier) {
        Minecraft.getInstance().setScreen(
                new RouletteScreen(multiplier)
        );
    }
}
