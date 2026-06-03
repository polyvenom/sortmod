package com.tom.sortmod;

import com.tom.sortmod.client.ClientFrozenSlots;
import com.tom.sortmod.client.SortModConfig;
import com.tom.sortmod.client.SortModKeybinds;
import com.tom.sortmod.network.InventoryStatePacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class SortModClient implements ClientModInitializer {

    // Track whether the inventory screen was open last tick
    private boolean wasInventoryOpen = false;

    @Override
    public void onInitializeClient() {
        // Packet types are registered server-side in SortMod.onInitialize();
        // the client-side registration happens automatically when we send.

        // Touch the config so the default file is written on first launch
        // and discoverable without having to open a chest first.
        SortModConfig.showContainerButtons();

        // Register vanilla-style keybinds. All default to UNBOUND so they don't
        // collide with other mods — the user binds whichever they want via
        // Options → Controls → Key Binds → SortMod.
        SortModKeybinds.register();

        // Load frozen-slot state from disk
        ClientFrozenSlots.loadIfNeeded();

        // Push frozen-slot state to the server on every world join so server-side
        // operations (sort, restock, hotbar restock) respect it immediately.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientFrozenSlots.sendToServer()
        );

        // Watch for the inventory screen closing — send "closed" packet on transition
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen current = Minecraft.getInstance().screen;
            boolean isInventoryOpen = current instanceof InventoryScreen;
            if (wasInventoryOpen && !isInventoryOpen) {
                if (Minecraft.getInstance().getConnection() != null) {
                    ClientPlayNetworking.send(new InventoryStatePacket(false));
                }
            }
            wasInventoryOpen = isInventoryOpen;
        });
    }
}