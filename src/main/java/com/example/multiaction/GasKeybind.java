package com.example.multiaction;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class GasKeybind {

    public static final KeyMapping GAS_KEY = new KeyMapping(
            "key.multiaction.gas",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.multiaction"
    );

    private GasKeybind() {
    }

    @Mod.EventBusSubscriber(
            modid = MultiActionMod.MODID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD
    )
    public static class Registration {

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(GAS_KEY);
        }
    }

    @Mod.EventBusSubscriber(
            modid = MultiActionMod.MODID,
            value = Dist.CLIENT
    )
    public static class ClientEvents {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            while (GAS_KEY.consumeClick()) {
                ModNetwork.sendGasRequest();
            }
        }
    }
}
