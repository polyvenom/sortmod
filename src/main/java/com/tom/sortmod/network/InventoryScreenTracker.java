package com.tom.sortmod.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry of which players currently have their inventory screen open.
 */
public class InventoryScreenTracker {

    private static final Set<UUID> openPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void setOpen(ServerPlayer player, boolean open) {
        if (open) {
            openPlayers.add(player.getUUID());
        } else {
            openPlayers.remove(player.getUUID());
        }
    }

    public static boolean isInventoryOpen(ServerPlayer player) {
        return openPlayers.contains(player.getUUID());
    }

    /** Clean up when a player disconnects. */
    public static void onPlayerLeave(ServerPlayer player) {
        openPlayers.remove(player.getUUID());
    }
}