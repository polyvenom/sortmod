package com.tom.sortmod.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.tom.sortmod.network.FrozenSlotsPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Client-side store of which player-inventory slots (0..35) the user has frozen.
 * Persists across sessions in <code>config/sortmod/frozen-slots.json</code>.
 * On any change we send the updated set to the server via {@link FrozenSlotsPacket}.
 */
public class ClientFrozenSlots {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("sortmod").resolve("frozen-slots.json");
    private static final Gson GSON = new Gson();

    private static final Set<Integer> frozen = new HashSet<>();
    private static boolean loaded = false;

    public static boolean isFrozen(int slot) {
        return frozen.contains(slot);
    }

    public static Set<Integer> snapshot() {
        return Collections.unmodifiableSet(new HashSet<>(frozen));
    }

    public static void toggle(int slot) {
        if (slot < 0 || slot >= 36) return;
        if (!frozen.remove(slot)) frozen.add(slot);
        save();
        sendToServer();
    }

    /** Called on world join — push current state to server. */
    public static void sendToServer() {
        if (Minecraft.getInstance().getConnection() == null) return;
        ClientPlayNetworking.send(new FrozenSlotsPacket(new HashSet<>(frozen)));
    }

    public static void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(CONFIG_PATH)) return;
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                JsonElement el = JsonParser.parseReader(r);
                if (!el.isJsonObject()) return;
                JsonArray arr = el.getAsJsonObject().getAsJsonArray("slots");
                if (arr == null) return;
                for (JsonElement e : arr) {
                    int v = e.getAsInt();
                    if (v >= 0 && v < 36) frozen.add(v);
                }
            }
        } catch (Exception ignored) {
            // best-effort load; corrupted files are silently ignored
        }
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            // sorted for stable diffs
            for (Integer i : new TreeSet<>(frozen)) arr.add(i);
            root.add("slots", arr);
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, w);
            }
        } catch (IOException ignored) {
        }
    }
}
