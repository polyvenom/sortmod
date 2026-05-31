package com.tom.sortmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Client → server: the player's current set of frozen player-inventory slot indices (0..35).
 * Sent on join and on every toggle.
 */
public record FrozenSlotsPacket(Set<Integer> slots) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FrozenSlotsPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("sortmod", "frozen_slots"));

    public static final StreamCodec<FriendlyByteBuf, FrozenSlotsPacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeVarInt(pkt.slots.size());
                        for (Integer i : pkt.slots) buf.writeVarInt(i);
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        Set<Integer> s = new HashSet<>(n * 2);
                        for (int i = 0; i < n; i++) s.add(buf.readVarInt());
                        return new FrozenSlotsPacket(s);
                    }
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerServer() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
                context.server().execute(() ->
                        FrozenSlotsTracker.set(context.player(), payload.slots())
                )
        );
    }
}
