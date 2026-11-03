package com.example.multiaction;

import net.minecraft.world.item.Item;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(MultiActionMod.MODID)
public class MultiActionMod {

    public static final String MODID = "multiaction";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> HAND_OF_MIDAS =
            ITEMS.register("hand_of_midas", HandOfMidasItem::new);

    public MultiActionMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class ServerEvents {

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            MulticastManager.clearAll();
        }
    }
}
