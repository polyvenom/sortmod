package com.tom.sortmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.tom.sortmod.network.FrozenSlotsPacket;
import com.tom.sortmod.network.FrozenSlotsTracker;
import com.tom.sortmod.network.InventoryScreenTracker;
import com.tom.sortmod.network.InventoryStatePacket;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SortMod implements ModInitializer {

    private static final int QUICK_STACK_RADIUS = 5;

    // Tracks what item type was in each hotbar slot last tick, keyed by player UUID
    private final Map<UUID, Item[]> previousHotbar = new HashMap<>();

    @Override
    public void onInitialize() {

        // Register inventory screen open/close packet
        InventoryStatePacket.registerServer();

        // Register frozen-slots packet (client → server sync)
        FrozenSlotsPacket.registerServer();

        // Clean up trackers on player disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            InventoryScreenTracker.onPlayerLeave(handler.player);
            FrozenSlotsTracker.onPlayerLeave(handler.player);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickHotbarRestock(player);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(Commands.literal("sortinv").executes(context -> {
                ServerPlayer player = context.getSource().getPlayer();
                if (player != null) sortPlayerInventory(player);
                return 1;
            }));

            dispatcher.register(Commands.literal("sortcontainer").executes(context -> {
                ServerPlayer player = context.getSource().getPlayer();
                if (player != null) sortOpenContainer(player);
                return 1;
            }));

            dispatcher.register(Commands.literal("dumpinv").executes(context -> {
                ServerPlayer player = context.getSource().getPlayer();
                if (player != null) dumpToContainer(player);
                return 1;
            }));

            dispatcher.register(Commands.literal("removeinv").executes(context -> {
                ServerPlayer player = context.getSource().getPlayer();
                if (player != null) removeFromContainer(player);
                return 1;
            }));

            dispatcher.register(Commands.literal("restockplayer").executes(context -> {
                ServerPlayer player = context.getSource().getPlayer();
                if (player != null) restockPlayer(player);
                return 1;
            }));

            dispatcher.register(Commands.literal("restockcontainer").executes(context -> {
                ServerPlayer player = context.getSource().getPlayer();
                if (player != null) restockContainer(player);
                return 1;
            }));

            dispatcher.register(Commands.literal("quickstack").executes(context -> {
                ServerPlayer player = context.getSource().getPlayer();
                if (player != null) quickStackToNearbyChests(player);
                return 1;
            }));
        });
    }

    // -------------------------------------------------------------------------
    // Hotbar restock — reactive, fires only when a slot transitions to empty
    // -------------------------------------------------------------------------

    private void tickHotbarRestock(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        UUID uuid = player.getUUID();

        // Get or create previous state for this player
        Item[] prev = previousHotbar.computeIfAbsent(uuid, k -> new Item[9]);

        // Restock is gated while any screen is open:
        // - Container screens (chest etc.) detected server-side via containerMenu
        // - Player inventory screen detected via client packet (InventoryScreenTracker)
        // We still walk the slots while gated so prev[] tracks user-initiated
        // moves — otherwise an item the player dragged out of the hotbar would
        // get pulled back the instant the screen closes.
        boolean gated = !(player.containerMenu instanceof InventoryMenu)
                     || InventoryScreenTracker.isInventoryOpen(player);

        Set<Integer> frozen = FrozenSlotsTracker.get(player);

        for (int slot = 0; slot < 9; slot++) {
            ItemStack current = inventory.getItem(slot);
            Item prevItem = prev[slot];

            // Auto-restock fires only if:
            //  - not gated by an open screen
            //  - the hotbar slot just transitioned to empty
            //  - the hotbar slot itself is NOT frozen (a frozen slot is meant to stay empty)
            if (!gated && current.isEmpty() && prevItem != null && !frozen.contains(slot)) {
                // Find first matching item in main inventory, skipping frozen source slots
                for (int i = 9; i < 36; i++) {
                    if (frozen.contains(i)) continue;
                    ItemStack candidate = inventory.getItem(i);
                    if (!candidate.isEmpty() && candidate.getItem() == prevItem) {
                        // Move entire stack to hotbar slot
                        inventory.setItem(slot, candidate.copy());
                        inventory.setItem(i, ItemStack.EMPTY);
                        break;
                    }
                }
            }

            // Always update previous state, even while gated, so user-driven
            // moves through the UI are observed and don't trigger restock on close.
            ItemStack updated = inventory.getItem(slot);
            prev[slot] = updated.isEmpty() ? null : updated.getItem();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Container getOpenContainer(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof InventoryMenu || menu instanceof CraftingMenu) return null;
        for (int i = 0; i < menu.slots.size(); i++) {
            if (!(menu.slots.get(i).container instanceof Inventory)) {
                return (Container) menu.slots.get(i).container;
            }
        }
        return null;
    }

    private int getContainerSize(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        int count = 0;
        for (int i = 0; i < menu.slots.size(); i++) {
            if (!(menu.slots.get(i).container instanceof Inventory)) count++;
        }
        return count;
    }

    /**
     * Sort order:
     *   1. Item ID (primary — keeps logs/planks/etc. grouped by type, not alphabetized)
     *   2. Damage value asc (less-damaged tools first)
     *   3. Enchantment count asc (unenchanted variants before enchanted ones)
     *   4. Custom-named status (no custom name before named)
     *   5. Custom name alphabetical (only matters among named items)
     *   6. Stack count desc (fuller stacks first so consolidation reads cleaner)
     */
    private void sortItems(List<ItemStack> items) {
        items.sort(Comparator
                .<ItemStack>comparingInt(s -> BuiltInRegistries.ITEM.getId(s.getItem()))
                .thenComparingInt(ItemStack::getDamageValue)
                .thenComparingInt(s -> s.getEnchantments().size())
                .thenComparingInt(s -> s.has(DataComponents.CUSTOM_NAME) ? 1 : 0)
                .thenComparing(s -> s.has(DataComponents.CUSTOM_NAME) ? s.getHoverName().getString() : "")
                .thenComparingInt(s -> -s.getCount())
        );
    }

    // Consolidate partial stacks into fewest stacks possible.
    // Items merge only when they share the same item AND the same components
    // (so different enchanted books / named items stay distinct).
    private List<ItemStack> consolidateStacks(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>();
        outer:
        for (ItemStack incoming : items) {
            ItemStack remaining = incoming.copy();
            for (ItemStack existing : result) {
                if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue;
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space <= 0) continue;
                int transfer = Math.min(space, remaining.getCount());
                existing.grow(transfer);
                remaining.shrink(transfer);
                if (remaining.isEmpty()) continue outer;
            }
            if (!remaining.isEmpty()) result.add(remaining);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Sort
    // -------------------------------------------------------------------------

    private void sortPlayerInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        Set<Integer> frozen = FrozenSlotsTracker.get(player);
        List<ItemStack> items = new ArrayList<>();
        // Extract only from non-frozen main-inventory slots
        for (int i = 9; i < 36; i++) {
            if (frozen.contains(i)) continue;
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        items = consolidateStacks(items);
        sortItems(items);
        // Place into non-frozen slots only — count matches since we only extracted from non-frozen
        java.util.Iterator<ItemStack> iter = items.iterator();
        for (int slot = 9; slot < 36 && iter.hasNext(); slot++) {
            if (frozen.contains(slot)) continue;
            inventory.setItem(slot, iter.next());
        }
    }

    private void sortOpenContainer(ServerPlayer player) {
        Container container = getOpenContainer(player);
        if (container == null) return;
        int size = getContainerSize(player);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
                container.setItem(i, ItemStack.EMPTY);
            }
        }
        items = consolidateStacks(items);
        sortItems(items);
        int slot = 0;
        for (ItemStack item : items) container.setItem(slot++, item);
        player.containerMenu.broadcastChanges();
    }

    // -------------------------------------------------------------------------
    // Dump
    // -------------------------------------------------------------------------

    private void dumpToContainer(ServerPlayer player) {
        Container container = getOpenContainer(player);
        if (container == null) return;
        int size = getContainerSize(player);
        Inventory inventory = player.getInventory();
        Set<Integer> frozen = FrozenSlotsTracker.get(player);
        for (int pi = 9; pi < 36; pi++) {
            if (frozen.contains(pi)) continue;
            ItemStack stack = inventory.getItem(pi);
            if (stack.isEmpty()) continue;
            stack = tryInsertIntoContainer(container, size, stack);
            inventory.setItem(pi, stack);
        }
        player.containerMenu.broadcastChanges();
    }

    private void removeFromContainer(ServerPlayer player) {
        Container container = getOpenContainer(player);
        if (container == null) return;
        int size = getContainerSize(player);
        Inventory inventory = player.getInventory();
        Set<Integer> frozen = FrozenSlotsTracker.get(player);
        for (int ci = 0; ci < size; ci++) {
            ItemStack stack = container.getItem(ci);
            if (stack.isEmpty()) continue;
            stack = tryInsertIntoPlayerInventory(inventory, stack, frozen);
            container.setItem(ci, stack);
        }
        player.containerMenu.broadcastChanges();
    }

    // -------------------------------------------------------------------------
    // Restock
    // -------------------------------------------------------------------------

    private void restockPlayer(ServerPlayer player) {
        Container container = getOpenContainer(player);
        if (container == null) return;
        int size = getContainerSize(player);
        Inventory inventory = player.getInventory();
        Set<Integer> frozen = FrozenSlotsTracker.get(player);

        // Only consider non-frozen player slots when deciding what to restock
        Map<Item, Integer> playerItems = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            if (frozen.contains(i)) continue;
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) playerItems.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        for (int ci = 0; ci < size; ci++) {
            ItemStack stack = container.getItem(ci);
            if (stack.isEmpty() || !playerItems.containsKey(stack.getItem())) continue;
            stack = tryInsertIntoPlayerInventory(inventory, stack, frozen);
            container.setItem(ci, stack);
        }
        player.containerMenu.broadcastChanges();
    }

    private void restockContainer(ServerPlayer player) {
        Container container = getOpenContainer(player);
        if (container == null) return;
        int size = getContainerSize(player);
        Inventory inventory = player.getInventory();
        Set<Integer> frozen = FrozenSlotsTracker.get(player);

        Map<Item, Integer> containerItems = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) containerItems.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        for (int pi = 9; pi < 36; pi++) {
            if (frozen.contains(pi)) continue;
            ItemStack stack = inventory.getItem(pi);
            if (stack.isEmpty() || !containerItems.containsKey(stack.getItem())) continue;
            stack = tryInsertIntoContainer(container, size, stack);
            inventory.setItem(pi, stack);
        }
        player.containerMenu.broadcastChanges();
    }

    // -------------------------------------------------------------------------
    // Quick stack to nearby chests
    // -------------------------------------------------------------------------

    private void quickStackToNearbyChests(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        Inventory inventory = player.getInventory();
        Set<Integer> frozen = FrozenSlotsTracker.get(player);

        Map<Item, Integer> playerItems = new HashMap<>();
        for (int i = 9; i < 36; i++) {
            if (frozen.contains(i)) continue;
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) playerItems.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -QUICK_STACK_RADIUS; x <= QUICK_STACK_RADIUS; x++) {
            for (int y = -QUICK_STACK_RADIUS; y <= QUICK_STACK_RADIUS; y++) {
                for (int z = -QUICK_STACK_RADIUS; z <= QUICK_STACK_RADIUS; z++) {
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (!(be instanceof Container chest)) continue;

                    int chestSize = chest.getContainerSize();
                    boolean hasMatch = false;
                    for (int i = 0; i < chestSize; i++) {
                        ItemStack stack = chest.getItem(i);
                        if (!stack.isEmpty() && playerItems.containsKey(stack.getItem())) {
                            hasMatch = true;
                            break;
                        }
                    }
                    if (!hasMatch) continue;

                    for (int pi = 9; pi < 36; pi++) {
                        if (frozen.contains(pi)) continue;
                        ItemStack stack = inventory.getItem(pi);
                        if (stack.isEmpty() || !playerItems.containsKey(stack.getItem())) continue;
                        boolean chestHasItem = false;
                        for (int ci = 0; ci < chestSize; ci++) {
                            if (ItemStack.isSameItem(chest.getItem(ci), stack)) {
                                chestHasItem = true;
                                break;
                            }
                        }
                        if (!chestHasItem) continue;
                        stack = tryInsertIntoContainer(chest, chestSize, stack);
                        inventory.setItem(pi, stack);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Insertion helpers
    // -------------------------------------------------------------------------

    private ItemStack tryInsertIntoContainer(Container container, int size, ItemStack stack) {
        // Pass 1: merge into existing partial stacks
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                if (space <= 0) continue;
                int transfer = Math.min(space, stack.getCount());
                slot.grow(transfer);
                stack.shrink(transfer);
                container.setItem(i, slot);
            }
        }
        // Pass 2: fill empty slots
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack.copy());
                stack = ItemStack.EMPTY;
            }
        }
        return stack;
    }

    private ItemStack tryInsertIntoPlayerInventory(Inventory inventory, ItemStack stack, Set<Integer> frozen) {
        // Pass 1: merge into existing partial stacks (frozen slots can still receive merges
        // of the same item — freezing locks the slot's contents, but topping up a stack
        // of the same item doesn't change "what's there").
        // Actually, safer to skip merging into frozen slots entirely so the slot's count
        // also stays exactly where the player left it.
        for (int i = 0; i < 36 && !stack.isEmpty(); i++) {
            if (frozen.contains(i)) continue;
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                if (space <= 0) continue;
                int transfer = Math.min(space, stack.getCount());
                slot.grow(transfer);
                stack.shrink(transfer);
                inventory.setItem(i, slot);
            }
        }
        // Pass 2: main inventory first, then hotbar — skip frozen empty slots
        for (int i = 9; i < 36 && !stack.isEmpty(); i++) {
            if (frozen.contains(i)) continue;
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, stack.copy());
                stack = ItemStack.EMPTY;
            }
        }
        for (int i = 0; i < 9 && !stack.isEmpty(); i++) {
            if (frozen.contains(i)) continue;
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, stack.copy());
                stack = ItemStack.EMPTY;
            }
        }
        return stack;
    }
}