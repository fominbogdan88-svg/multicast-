package com.example.multiaction;

import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MultiActionMod.MODID)
public class MultiActionMod {

    public static final String MODID = "multiaction";

    public MultiActionMod() {
        // Регистрацию предметов и сети добавим в следующих частях.
    }

    /**
     * Данные текущей реализации MulticastManager хранятся в памяти.
     * Очищаем их после остановки сервера.
     */
    @Mod.EventBusSubscriber(modid = MODID)
    public static class ServerEvents {

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            MulticastManager.clearAll();
        }
    }
}
