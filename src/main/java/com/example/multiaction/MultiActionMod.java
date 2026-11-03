package com.example.multiaction;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.loot.IGlobalLootModifier;
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

    // Регистрация предметов.
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> HAND_OF_MIDAS =
            ITEMS.register("hand_of_midas", HandOfMidasItem::new);

    public static final RegistryObject<Item> MAGIC_WAND =
            ITEMS.register("magic_wand", MagicWandItem::new);

    // Регистрация модификатора дропа блоков.
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>>
            LOOT_MODIFIERS = DeferredRegister.create(
                    ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    MODID
            );

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>>
            BLOCK_DROPS_MODIFIER = LOOT_MODIFIERS.register(
                    "block_drops",
                    () -> BlockDropsModifier.CODEC
            );

    public MultiActionMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modBus);
        LOOT_MODIFIERS.register(modBus);

        // Сеть для интерфейса рулетки.
        ModNetwork.register();
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class ServerEvents {

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            // Текущие настройки множителей пока хранятся в памяти.
            MulticastManager.clearAll();
        }
    }
}
