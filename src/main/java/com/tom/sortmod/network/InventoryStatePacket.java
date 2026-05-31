package com.tom.sortmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record InventoryStatePacket(boolean open) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InventoryStatePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("sortmod", "inventory_state"));

    public static final StreamCodec<FriendlyByteBuf, InventoryStatePacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeBoolean(pkt.open),
                    buf -> new InventoryStatePacket(buf.readBoolean())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerServer() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
                context.server().execute(() ->
                        InventoryScreenTracker.setOpen(context.player(), payload.open())
                )
        );
    }
}