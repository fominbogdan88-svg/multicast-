package com.example.multiaction;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL =
            NetworkRegistry.ChannelBuilder
                    .named(new ResourceLocation(
                            MultiActionMod.MODID,
                            "main"
                    ))
                    .networkProtocolVersion(() -> PROTOCOL_VERSION)
                    .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                    .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                    .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                0,
                RoulettePacket.class,
                RoulettePacket::encode,
                RoulettePacket::decode,
                RoulettePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendRoulette(
            ServerPlayer player,
            int multiplier
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RoulettePacket(multiplier)
        );
    }

    public static final class RoulettePacket {

        private final int multiplier;

        public RoulettePacket(int multiplier) {
            this.multiplier = multiplier;
        }

        public static void encode(
                RoulettePacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeVarInt(packet.multiplier);
        }

        public static RoulettePacket decode(FriendlyByteBuf buffer) {
            return new RoulettePacket(buffer.readVarInt());
        }

        public static void handle(
                RoulettePacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> ClientRoulette.open(
                                    packet.multiplier
                            )
                    )
            );

            context.setPacketHandled(true);
        }
    }
}
