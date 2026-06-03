# SortMod

**Your Inventory Utility Companion for Minecraft 26.1.2 (Fabric).**

SortMod is the inventory toolkit that fills the gaps [ClientSort](https://modrinth.com/mod/client-sort) doesn't cover: **Quick-Stack to nearby chests**, **persistent frozen slots**, **hotbar auto-restock**, **cross-inventory search highlighting**, and a full set of **slash commands**. It's designed to run *alongside* ClientSort rather than replace it — pair them for a complete inventory experience.

> Already running ClientSort? Set `"showContainerButtons": false` in `config/sortmod/config.json` to hide our in-screen buttons and lean on slash commands + keybinds instead.

---

## Headline features

### Quick Stack to nearby chests
One click pushes everything from your inventory into nearby chests that already contain matching items, within a 5-block radius — no container needs to be open. Terraria's best UX feature, brought to Minecraft.

- Slash: `/quickstack`
- Button: player inventory screen
- Matches by item type, so each chest gets only the items it already contains

### Persistent Frozen Slots
Middle-click any player-inventory slot to lock it. Locked slots are skipped by every automated operation in the mod — sort, dump, restock, quick-stack, hotbar auto-restock. State persists to `config/sortmod/frozen-slots.json` and syncs to the server on every world join.

Common uses:
- Lock an empty totem slot so junk doesn't auto-restock in
- Lock a row of building blocks so sorting doesn't reorganize them
- Lock a spare bucket slot so quick-stack doesn't send it away

### Hotbar Auto-Restock
When a hotbar slot empties, SortMod refills it with the same item from your main inventory. Pauses while any screen is open. Skips frozen slots so locked empties stay locked.

### Cross-inventory Search
Type into the search box in any chest screen — matching items get a yellow tint on both sides simultaneously. Query persists between containers.

### Slash commands for everything

| Command | Action |
|---|---|
| `/quickstack` | Push matching items to nearby chests |
| `/sortinv` | Sort player inventory |
| `/sortcontainer` | Sort the open container |
| `/dumpinv` | Player → container, everything |
| `/removeinv` | Container → player, everything |
| `/restockplayer` | Top up player from container |
| `/restockcontainer` | Top up container from player |

---

## Also includes: basic sorting

SortMod can sort containers and inventories by item registry ID (so oak items group with oak items rather than scattering alphabetically). It's intentionally minimal — if pure sorting is your priority, [ClientSort](https://modrinth.com/mod/client-sort) is the better-polished, more configurable option and we recommend running it alongside SortMod.

Sort order: registry ID → damage value → enchantment count → custom-name flag → custom name → stack count desc. Respects frozen slots.

---

## Pair with ClientSort

| Feature | ClientSort | SortMod |
|---|---|---|
| Sort containers | ✅ | ✅ (basic) |
| Sort inventory | ✅ | ✅ (basic) |
| Quick-Stack to nearby chests | ❌ | ✅ |
| Persistent Frozen Slots | ❌ | ✅ |
| Hotbar Auto-Restock | ❌ | ✅ |
| Cross-inventory Search highlight | ❌ | ✅ |
| Slash commands for everything | ❌ | ✅ |

Recommended setup: both installed; in SortMod's config screen turn off `showContainerButtons` and `showInventorySortButton` (so ClientSort owns those), but keep `showQuickStackButton` on for SortMod's unique feature.

---

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.1.2
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Drop the SortMod jar into your `mods` folder
4. Launch

**Server play:** install on the server too. The buttons and search box are client-side, but every inventory operation runs server-side.

---

## Configuration

`config/sortmod/config.json` (auto-created on first launch):

```json
{
  "showContainerButtons": true,
  "showInventorySortButton": true,
  "showQuickStackButton": true,
  "showSearchBox": true
}
```

- **`showContainerButtons`** — render the 5 sort/dump/restock buttons above containers. Disable when pairing with ClientSort.
- **`showInventorySortButton`** — render the single sort button on the player inventory screen.
- **`showQuickStackButton`** — render the Quick Stack button (SortMod's signature feature, recommended on).
- **`showSearchBox`** — render the chest-screen search box. Disable if you use NEI / REI / JEI.

Slash commands, keybinds, and middle-click frozen-slot toggling work regardless of any of these settings.

### Per-element layout

Each visible UI group can be anchored to a different corner of the container window with a pixel offset, so SortMod's widgets stay out of the way of other inventory mods (ClientSort, NEI, Trashslot, etc.). Open **ModMenu → SortMod → Config → Layout…** for the in-game editor, or edit `config/sortmod/config.json` directly:

```json
"containerButtonsLayout":       { "anchor": "TOP_LEFT",    "offsetX": 0,   "offsetY": -22 },
"chestPlayerSortLayout":        { "anchor": "BOTTOM_LEFT", "offsetX": 0,   "offsetY": 4   },
"searchBoxLayout":              { "anchor": "TOP_LEFT",    "offsetX": 110, "offsetY": -22 },
"inventoryScreenButtonsLayout": { "anchor": "TOP_LEFT",    "offsetX": 0,   "offsetY": -22 },
"searchBoxWidth": 66
```

Anchors: `TOP_LEFT` / `TOP_RIGHT` / `BOTTOM_LEFT` / `BOTTOM_RIGHT`. Offsets are pixels — positive X is right, positive Y is down — measured outward from the chosen corner. To put a button row above the gui top edge, use a TOP anchor with a negative Y; to put one below the bottom edge, use a BOTTOM anchor with a positive Y. The Reset Defaults button in the layout screen restores the original positions.

---

## Building

```sh
./gradlew build
```

Output jar lands in `build/libs/`.

The button icon atlas is regenerated by `tools/IconGen.java`:

```sh
java tools/IconGen.java src/client/resources/assets/sortmod/textures/gui/icons.png
```

Edit the ASCII templates in that file to tweak any icon, then rerun.

---

## Architecture

Split source sets via Fabric loom `splitEnvironmentSourceSets()`:

- `src/main/java` — server-side: inventory ops, command registration, packet handlers
- `src/client/java` — client-side: UI, mixins, config, frozen-slot store
- `src/main/resources` — `fabric.mod.json`, server mixin config, mod icon
- `src/client/resources` — client mixin config, GUI icon atlas

All inventory manipulation runs server-side for correctness and to avoid client-prediction desync.

---

## Acknowledgements

The frozen-slots concept, cross-inventory search highlighting, and the broader idea of in-container button widgets were pioneered by [EasierChests by Giselbaer (gbl)](https://github.com/gbl/EasierChests), which stopped being maintained at Minecraft 1.20.4. SortMod is an independent reimplementation for 26.1.2 with a different architecture (server-side inventory ops + client-side UI, instead of EasierChests' client-only slot-click simulation), but the polish ideas trace back there and credit is due.

[ClientSort](https://modrinth.com/mod/client-sort) is the recommended sorting companion. SortMod's basic sort exists as a fallback; ClientSort does it better.

---

## License

CC0 1.0 Universal — public domain. Do whatever you want with the code.

## Author

polyvenom — <https://github.com/polyvenom>
