package com.tom.sortmod.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.tom.sortmod.client.SortModConfigScreen;

/**
 * ModMenu entry point. Exposes a single config screen factory that opens the
 * {@link SortModConfigScreen} when the user clicks "Config" on SortMod's tile
 * in the ModMenu mods list.
 *
 * Wired in fabric.mod.json under entrypoints.modmenu. Loaded only if ModMenu
 * is present on the classpath, so this whole class is dead weight otherwise —
 * harmless because Fabric only resolves entrypoint classes when their target
 * entrypoint is actually requested.
 */
public class SortModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SortModConfigScreen::new;
    }
}
