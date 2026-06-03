package com.tom.sortmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Vanilla-style keybinds. All default to UNBOUND so they don't collide with
 * other mods or vanilla controls — the user binds whichever ones they want
 * via <em>Options → Controls → Key Binds → SortMod</em>.
 *
 * Each binding fires its equivalent slash command on press so the existing
 * command-handling code stays the single source of truth.
 */
public final class SortModKeybinds {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sortmod", "main"));

    private static KeyMapping quickStack;
    private static KeyMapping sortInv;
    private static KeyMapping sortContainer;
    private static KeyMapping dumpInv;
    private static KeyMapping removeInv;
    private static KeyMapping restockPlayer;
    private static KeyMapping restockContainer;

    private SortModKeybinds() {}

    public static void register() {
        quickStack       = register("quickstack");
        sortInv          = register("sortinv");
        sortContainer    = register("sortcontainer");
        dumpInv          = register("dumpinv");
        removeInv        = register("removeinv");
        restockPlayer    = register("restockplayer");
        restockContainer = register("restockcontainer");

        ClientTickEvents.END_CLIENT_TICK.register(SortModKeybinds::onTick);
    }

    private static KeyMapping register(String name) {
        // InputConstants.UNKNOWN.getValue() is -1 — meaning "unbound by default".
        KeyMapping km = new KeyMapping(
                "key.sortmod." + name,
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        );
        return KeyMappingHelper.registerKeyMapping(km);
    }

    private static void onTick(Minecraft client) {
        if (client.player == null) return;

        // consumeClick() handles auto-repeat correctly: each press is one fire.
        while (quickStack.consumeClick())        sendCmd(client, "quickstack");
        while (sortInv.consumeClick())           sendCmd(client, "sortinv");
        while (sortContainer.consumeClick())     sendCmd(client, "sortcontainer");
        while (dumpInv.consumeClick())           sendCmd(client, "dumpinv");
        while (removeInv.consumeClick())         sendCmd(client, "removeinv");
        while (restockPlayer.consumeClick())     sendCmd(client, "restockplayer");
        while (restockContainer.consumeClick())  sendCmd(client, "restockcontainer");
    }

    private static void sendCmd(Minecraft client, String command) {
        if (client.player != null) client.player.connection.sendCommand(command);
    }
}
