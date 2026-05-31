package com.tom.sortmod.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry of which player-inventory slot indices each player has frozen.
 * Frozen slots are skipped by sort, restock, dump, quick-stack, and hotbar auto-restock.
 */
public class FrozenSlotsTracker {

    private static final Set<Integer> EMPTY = Collections.emptySet();
    private static final ConcurrentHashMap<UUID, Set<Integer>> byPlayer = new ConcurrentHashMap<>();

    public static void set(ServerPlayer player, Set<Integer> slots) {
        byPlayer.put(player.getUUID(), new HashSet<>(slots));
    }

    public static Set<Integer> get(ServerPlayer player) {
        return byPlayer.getOrDefault(player.getUUID(), EMPTY);
    }

    public static boolean isFrozen(ServerPlayer player, int slot) {
        return get(player).contains(slot);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        byPlayer.remove(player.getUUID());
    }
}
