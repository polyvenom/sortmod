# SortMod — Your Inventory Utility Companion

**The inventory toolkit ClientSort doesn't replace.**

SortMod is a small, server-friendly Fabric mod that adds the inventory conveniences Minecraft *should* have shipped with: Terraria-style Quick Stack to nearby chests, persistent frozen slots, automatic hotbar restocking, and a cross-inventory search box. Designed to slot in alongside [ClientSort](https://modrinth.com/mod/client-sort) — run them together for a complete inventory setup.

> **Tip:** SortMod plays nicely with ClientSort. If you want to skip our in-screen buttons and rely on ClientSort's UI, flip `"showContainerButtons": false` (and optionally `"showInventorySortButton": false`) in `config/sortmod/config.json` — every feature still works through slash commands and keybinds.

---

## What makes SortMod unique

### Quick Stack to Nearby Chests
Like Terraria. One click pushes everything from your inventory into nearby chests that already contain matching items — within a 5-block radius, no container open required. The single fastest way to come back from a mining trip and empty your bags.

- Command: `/quickstack`
- Button: in the player inventory screen
- Smart matching: only sends to chests that already hold that item type, so a chest of cobblestone gets cobble, the chest of redstone gets redstone, and nothing gets dumped into your tool chest by accident.

### Persistent Frozen Slots
Middle-click any player inventory slot to lock it. Locked slots are skipped by every automated operation in the mod — sort, dump, restock, quick-stack, hotbar auto-restock. Lock state survives across worlds, servers, and game restarts.

**Why this matters:**
- Lock an empty totem-of-undying slot so junk doesn't auto-restock into it
- Lock a row of building blocks so sorting doesn't reorganize them
- Lock a spare-bucket slot so it never gets quick-stacked to a chest

### Hotbar Auto-Restock
When a hotbar slot runs out, SortMod automatically pulls a matching item up from your main inventory. Cobble while bridging, arrows while shooting, torches while spelunking — they just keep coming back. Pauses while any inventory screen is open so you can rearrange freely. Respects frozen slots, so locked empty slots stay empty.

### Cross-Inventory Search
Open any chest, type into the search box, and matching items get a yellow highlight on **both sides** simultaneously — your inventory and the container. The query persists between chests too, so scanning a row of storage barrels for "diamond" is two keystrokes and a glance.

### Slash Commands for Everything
Every feature has a slash command. Bind them to keys, chain them in macros, fire them from a remote, whatever you like:

| Command | Action |
|---|---|
| `/quickstack` | Push matching items to nearby chests |
| `/sortinv` | Sort your inventory |
| `/sortcontainer` | Sort the open container |
| `/dumpinv` | Dump player → container |
| `/removeinv` | Pull container → player |
| `/restockplayer` | Top up your inventory from the container |
| `/restockcontainer` | Top up the container from your inventory |

---

## Also includes: basic sort

Yes, SortMod can sort. Container and player inventory, grouped by item registry ID — so oak logs end up next to oak planks, not scattered alphabetically across the chest. If you're after pure sorting UX, ClientSort handles it beautifully and we strongly recommend installing it alongside.

SortMod's sort respects frozen slots and uses sensible tiebreakers (damage value, enchantment count, custom names, stack size) so chests stay readable.

---

## Pair with ClientSort

[ClientSort](https://modrinth.com/mod/client-sort) is the gold standard for inventory sorting on Fabric. It has 3.7M+ downloads for a reason — it's a beautifully focused mod. SortMod is built to fill the gaps ClientSort doesn't touch:

| Feature | ClientSort | SortMod |
|---|---|---|
| Sort containers | ✅ | ✅ (basic) |
| Sort inventory | ✅ | ✅ (basic) |
| **Quick-Stack to nearby chests** | ❌ | ✅ |
| **Persistent Frozen Slots** | ❌ | ✅ |
| **Hotbar Auto-Restock** | ❌ | ✅ |
| **Cross-inventory Search highlight** | ❌ | ✅ |
| Slash commands for everything | ❌ | ✅ |

Install both. Turn off SortMod's overlap-zone buttons via the in-game config screen (or `"showContainerButtons": false` and `"showInventorySortButton": false` in `config/sortmod/config.json`) to avoid visual overlap, and you get ClientSort's polished sorting alongside SortMod's logistics toolkit. Keep `showQuickStackButton` on — that's SortMod's unique feature.

---

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.1.2
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Drop the SortMod jar into your `mods` folder
4. (Recommended) Add [ClientSort](https://modrinth.com/mod/client-sort)
5. Launch

**For servers:** SortMod must be installed on the server too. The UI is client-side, but every inventory operation runs server-side for correctness and anti-cheat compatibility.

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

- **`showContainerButtons`** — the 5 buttons above containers (overlap zone with ClientSort).
- **`showInventorySortButton`** — the single sort button on the player inventory screen.
- **`showQuickStackButton`** — the Quick Stack button (recommended on).
- **`showSearchBox`** — the chest-screen search box (disable if you use NEI / REI / JEI).

Each toggle is independent. Slash commands, keybinds, and middle-click frozen-slot toggling keep working regardless.

### Per-element layout

Every visible UI group can also be **repositioned**. Pick a corner (top-left / top-right / bottom-left / bottom-right) and a pixel offset for each element, in-game via **ModMenu → SortMod → Config → Layout…** or by editing JSON directly. Move the search box to the bottom-right to dodge NEI; pin the container buttons to the top-right so ClientSort can keep the top-left. The Reset Defaults button in the layout screen restores everything if you lose track.

`config/sortmod/frozen-slots.json` is managed automatically — your locked slot list lives here and syncs to the server on join.

---

## Compatibility

- Minecraft 26.1.2 / Fabric Loader 0.19.2+
- Fabric API required
- Designed to coexist with other inventory mods — use the per-element layout settings to position SortMod's widgets around them
- Server-side ops are vanilla-safe — no NBT mangling, no item duplication paths

---

## Source & License

- GitHub: <https://github.com/polyvenom/sortmod>
- License: CC0 1.0 Universal (public domain)
- Author: polyvenom
- Issues / suggestions: GitHub issues

## Support

If SortMod saves you a few hours of chest-organizing, a tip on [Ko-fi](https://ko-fi.com/polyvenom) is always appreciated. No pressure — the mod is and will always be free.
