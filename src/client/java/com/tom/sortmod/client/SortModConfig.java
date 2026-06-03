package com.tom.sortmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-side config for SortMod.
 *
 * Lives at <code>config/sortmod/config.json</code>. Lazy-loaded on first access
 * and written back with defaults if missing.
 *
 * Toggles control which UI elements render; layouts control where each one
 * sits relative to the container gui. Both are user-editable directly in the
 * JSON or via the in-game config screen (ModMenu → SortMod → Config / Layout).
 */
public final class SortModConfig {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("sortmod").resolve("config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean loaded = false;

    // ------------------------------------------------------------------------
    // Visibility toggles
    // ------------------------------------------------------------------------

    private static boolean showContainerButtons    = true;
    private static boolean showInventorySortButton = true;
    private static boolean showQuickStackButton    = true;
    private static boolean showSearchBox           = true;

    // ------------------------------------------------------------------------
    // Layouts — anchor + offset per element group
    // Defaults reproduce v1.5.0's visual layout exactly.
    // ------------------------------------------------------------------------

    private static final Layout DEFAULT_CONTAINER_BTNS  = new Layout(Anchor.TOP_LEFT,    0,   -22);
    private static final Layout DEFAULT_CHEST_SORT      = new Layout(Anchor.BOTTOM_LEFT, 0,    4);
    private static final Layout DEFAULT_SEARCH          = new Layout(Anchor.TOP_LEFT,    110, -22);
    private static final Layout DEFAULT_INV_BTNS        = new Layout(Anchor.TOP_LEFT,    0,   -22);
    private static final int    DEFAULT_SEARCH_WIDTH    = 66;

    private static Layout containerButtonsLayout       = DEFAULT_CONTAINER_BTNS.copy();
    private static Layout chestPlayerSortLayout        = DEFAULT_CHEST_SORT.copy();
    private static Layout searchBoxLayout              = DEFAULT_SEARCH.copy();
    private static Layout inventoryScreenButtonsLayout = DEFAULT_INV_BTNS.copy();
    private static int    searchBoxWidth               = DEFAULT_SEARCH_WIDTH;

    private SortModConfig() {}

    // ------------------------------------------------------------------------
    // Toggle accessors
    // ------------------------------------------------------------------------

    public static boolean showContainerButtons()    { loadIfNeeded(); return showContainerButtons; }
    public static boolean showInventorySortButton() { loadIfNeeded(); return showInventorySortButton; }
    public static boolean showQuickStackButton()    { loadIfNeeded(); return showQuickStackButton; }
    public static boolean showSearchBox()           { loadIfNeeded(); return showSearchBox; }

    public static void setShowContainerButtons(boolean v)    { loadIfNeeded(); showContainerButtons    = v; save(); }
    public static void setShowInventorySortButton(boolean v) { loadIfNeeded(); showInventorySortButton = v; save(); }
    public static void setShowQuickStackButton(boolean v)    { loadIfNeeded(); showQuickStackButton    = v; save(); }
    public static void setShowSearchBox(boolean v)           { loadIfNeeded(); showSearchBox           = v; save(); }

    // ------------------------------------------------------------------------
    // Layout accessors — returned objects are live; modifying them and calling
    // save() persists changes. The config screen uses this directly.
    // ------------------------------------------------------------------------

    public static Layout containerButtonsLayout()       { loadIfNeeded(); return containerButtonsLayout; }
    public static Layout chestPlayerSortLayout()        { loadIfNeeded(); return chestPlayerSortLayout; }
    public static Layout searchBoxLayout()              { loadIfNeeded(); return searchBoxLayout; }
    public static Layout inventoryScreenButtonsLayout() { loadIfNeeded(); return inventoryScreenButtonsLayout; }

    public static int  searchBoxWidth()             { loadIfNeeded(); return searchBoxWidth; }
    public static void setSearchBoxWidth(int v)     { loadIfNeeded(); searchBoxWidth = Math.max(24, Math.min(176, v)); save(); }

    /** Persist any layout edits made through the returned references. */
    public static void saveLayouts() { save(); }

    /** Reset every layout (and the search-box width) to defaults. */
    public static void resetLayouts() {
        loadIfNeeded();
        containerButtonsLayout       = DEFAULT_CONTAINER_BTNS.copy();
        chestPlayerSortLayout        = DEFAULT_CHEST_SORT.copy();
        searchBoxLayout              = DEFAULT_SEARCH.copy();
        inventoryScreenButtonsLayout = DEFAULT_INV_BTNS.copy();
        searchBoxWidth               = DEFAULT_SEARCH_WIDTH;
        save();
    }

    // ------------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------------

    private static synchronized void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                JsonElement el = JsonParser.parseReader(r);
                if (!el.isJsonObject()) return;
                JsonObject obj = el.getAsJsonObject();

                // Migration from v1.4.0's single showButtons flag.
                if (obj.has("showButtons")) {
                    boolean legacy = obj.get("showButtons").getAsBoolean();
                    showContainerButtons    = legacy;
                    showInventorySortButton = legacy;
                    showQuickStackButton    = legacy;
                }

                if (obj.has("showContainerButtons"))    showContainerButtons    = obj.get("showContainerButtons").getAsBoolean();
                if (obj.has("showInventorySortButton")) showInventorySortButton = obj.get("showInventorySortButton").getAsBoolean();
                if (obj.has("showQuickStackButton"))    showQuickStackButton    = obj.get("showQuickStackButton").getAsBoolean();
                if (obj.has("showSearchBox"))           showSearchBox           = obj.get("showSearchBox").getAsBoolean();
                if (obj.has("searchBoxWidth"))          searchBoxWidth          = obj.get("searchBoxWidth").getAsInt();

                readLayoutInto(obj, "containerButtonsLayout",       containerButtonsLayout);
                readLayoutInto(obj, "chestPlayerSortLayout",        chestPlayerSortLayout);
                readLayoutInto(obj, "searchBoxLayout",              searchBoxLayout);
                readLayoutInto(obj, "inventoryScreenButtonsLayout", inventoryScreenButtonsLayout);
            }
            // Rewrite to reflect the current schema (drops legacy keys, fills new ones).
            save();
        } catch (Exception ignored) {
            // best-effort load; corrupted files fall back to defaults
        }
    }

    private static void readLayoutInto(JsonObject root, String key, Layout target) {
        if (!root.has(key) || !root.get(key).isJsonObject()) return;
        JsonObject obj = root.getAsJsonObject(key);
        if (obj.has("anchor")) {
            try {
                target.anchor = Anchor.valueOf(obj.get("anchor").getAsString());
            } catch (IllegalArgumentException ignored) {
                // bad anchor value → keep default
            }
        }
        if (obj.has("offsetX")) target.offsetX = obj.get("offsetX").getAsInt();
        if (obj.has("offsetY")) target.offsetY = obj.get("offsetY").getAsInt();
    }

    private static JsonObject writeLayout(Layout l) {
        JsonObject obj = new JsonObject();
        obj.addProperty("anchor",  l.anchor.name());
        obj.addProperty("offsetX", l.offsetX);
        obj.addProperty("offsetY", l.offsetY);
        return obj;
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("showContainerButtons",    showContainerButtons);
            obj.addProperty("showInventorySortButton", showInventorySortButton);
            obj.addProperty("showQuickStackButton",    showQuickStackButton);
            obj.addProperty("showSearchBox",           showSearchBox);
            obj.addProperty("searchBoxWidth",          searchBoxWidth);
            obj.add("containerButtonsLayout",       writeLayout(containerButtonsLayout));
            obj.add("chestPlayerSortLayout",        writeLayout(chestPlayerSortLayout));
            obj.add("searchBoxLayout",              writeLayout(searchBoxLayout));
            obj.add("inventoryScreenButtonsLayout", writeLayout(inventoryScreenButtonsLayout));
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(obj, w);
            }
        } catch (IOException ignored) {
        }
    }
}
