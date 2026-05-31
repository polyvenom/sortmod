package com.tom.sortmod.mixin;

import com.tom.sortmod.client.ClientFrozenSlots;
import com.tom.sortmod.client.IconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin extends Screen {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageHeight;
    @Shadow public AbstractContainerMenu menu;

    // Search query persists across container opens within a session.
    @Unique private static String sortmod$searchText = "";
    @Unique private EditBox sortmod$searchBox;

    protected ContainerScreenMixin(Component title) {
        super(title);
    }

    // -------------------------------------------------------------------------
    // Sort/restock/etc. buttons — skip player inventory & crafting table (those
    // are handled in InventoryScreenMixin where the bottom row makes sense).
    // -------------------------------------------------------------------------
    @Inject(method = "init", at = @At("TAIL"))
    private void addButtons(CallbackInfo ci) {
        if (menu instanceof CraftingMenu || menu instanceof InventoryMenu) return;

        int bs = 20;                                   // vanilla square-button size
        int gap = 2;
        int bx = this.leftPos;
        int topY = this.topPos - bs - 2;
        int bottomY = this.topPos + this.imageHeight + 4;

        // Top row: container-side actions
        this.addRenderableWidget(new IconButton(bx,                  topY, bs, IconButton.ICON_SORT,
                Component.literal("Sort container"),         () -> sendCmd("sortcontainer")));
        this.addRenderableWidget(new IconButton(bx + (bs + gap),     topY, bs, IconButton.ICON_DUMP_TO,
                Component.literal("Dump inventory into container"), () -> sendCmd("dumpinv")));
        this.addRenderableWidget(new IconButton(bx + (bs + gap) * 2, topY, bs, IconButton.ICON_PULL_FROM,
                Component.literal("Pull container into inventory"), () -> sendCmd("removeinv")));
        this.addRenderableWidget(new IconButton(bx + (bs + gap) * 3, topY, bs, IconButton.ICON_RESTOCK_PLAYER,
                Component.literal("Restock player from container"), () -> sendCmd("restockplayer")));
        this.addRenderableWidget(new IconButton(bx + (bs + gap) * 4, topY, bs, IconButton.ICON_RESTOCK_CONT,
                Component.literal("Restock container from player"), () -> sendCmd("restockcontainer")));

        // Bottom row: sort player inventory (positioned below the player slots)
        this.addRenderableWidget(new IconButton(bx, bottomY, bs, IconButton.ICON_SORT,
                Component.literal("Sort player inventory"),  () -> sendCmd("sortinv")));

        // Search box to the right of the top button row. Fills the remaining
        // horizontal space inside the container window's width (~176px).
        int searchX = bx + (bs + gap) * 5;
        int searchW = (this.leftPos + 176) - searchX;
        if (searchW >= 24) {
            sortmod$searchBox = new EditBox(this.font, searchX, topY, searchW, bs,
                    Component.literal("Search"));
            sortmod$searchBox.setBordered(true);
            sortmod$searchBox.setMaxLength(32);
            sortmod$searchBox.setValue(sortmod$searchText);
            sortmod$searchBox.setResponder(s -> sortmod$searchText = s);
            this.addRenderableWidget(sortmod$searchBox);
        }
    }

    private static void sendCmd(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(command);
    }

    // -------------------------------------------------------------------------
    // Frozen-slot overlay — always rendered, regardless of menu type.
    // Implemented as a custom Renderable widget so it slots into the screen's
    // normal extract-render-state pipeline.
    // -------------------------------------------------------------------------
    @Inject(method = "init", at = @At("TAIL"))
    private void addSlotOverlays(CallbackInfo ci) {
        final int FROZEN_TINT = 0x8055BBFF;   // 50% light blue
        final int SEARCH_TINT = 0x80FFEE55;   // 50% warm yellow
        this.addRenderableOnly((g, mouseX, mouseY, partial) -> {
            String q = sortmod$searchText.toLowerCase();
            boolean searching = !q.isEmpty();
            for (Slot slot : this.menu.slots) {
                int x0 = this.leftPos + slot.x;
                int y0 = this.topPos + slot.y;

                // Frozen-slot tint (player inventory slots 0..35 only)
                if (slot.container instanceof Inventory) {
                    int idx = slot.getContainerSlot();
                    if (idx >= 0 && idx < 36 && ClientFrozenSlots.isFrozen(idx)) {
                        g.fill(x0, y0, x0 + 16, y0 + 16, FROZEN_TINT);
                    }
                }

                // Search highlight (any slot in the menu, including the container side)
                if (searching) {
                    var stack = slot.getItem();
                    if (!stack.isEmpty()
                            && stack.getHoverName().getString().toLowerCase().contains(q)) {
                        g.fill(x0, y0, x0 + 16, y0 + 16, SEARCH_TINT);
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Middle-click on a player-inventory slot toggles its frozen state.
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void onMiddleClickToggleFrozen(MouseButtonEvent event, boolean doubleClick,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 2) return;                       // middle button only
        double mx = event.x(), my = event.y();
        for (Slot slot : this.menu.slots) {
            if (!(slot.container instanceof Inventory)) continue;
            int idx = slot.getContainerSlot();
            if (idx < 0 || idx >= 36) continue;
            // Slot draw rect is 16×16 at (leftPos + slot.x, topPos + slot.y)
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                ClientFrozenSlots.toggle(idx);
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
