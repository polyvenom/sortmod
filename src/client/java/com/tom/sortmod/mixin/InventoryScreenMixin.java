package com.tom.sortmod.mixin;

import com.tom.sortmod.client.IconButton;
import com.tom.sortmod.client.Layout;
import com.tom.sortmod.client.SortModConfig;
import com.tom.sortmod.network.InventoryStatePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    public InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onOpen(CallbackInfo ci) {
        // Notify server the inventory screen is open — pauses hotbar auto-restock.
        ClientPlayNetworking.send(new InventoryStatePacket(true));

        boolean invSortBtn = SortModConfig.showInventorySortButton();
        boolean quickStackBtn = SortModConfig.showQuickStackButton();
        if (!invSortBtn && !quickStackBtn) return;

        int bs = 20;
        int gap = 2;

        // Group width depends on which buttons are visible. With both shown
        // it's 2 buttons wide (42px); with one it's 20px. Used when anchoring
        // the group to a right-aligned edge so the rightmost button sits flush.
        int visible = (invSortBtn ? 1 : 0) + (quickStackBtn ? 1 : 0);
        int groupW = visible == 0 ? 0 : visible * (bs + gap) - gap;

        Layout layout = SortModConfig.inventoryScreenButtonsLayout();
        int rowX = layout.x(this.leftPos, groupW);
        int rowY = layout.y(this.topPos, this.imageHeight, bs);
        int slot = 0;

        if (invSortBtn) {
            this.addRenderableWidget(new IconButton(rowX + (bs + gap) * slot, rowY, bs, IconButton.ICON_SORT,
                    Component.literal("Sort player inventory"),     () -> sendCmd("sortinv")));
            slot++;
        }

        if (quickStackBtn) {
            this.addRenderableWidget(new IconButton(rowX + (bs + gap) * slot, rowY, bs, IconButton.ICON_QUICK_STACK,
                    Component.literal("Quick stack to nearby chests"), () -> sendCmd("quickstack")));
        }
    }

    private static void sendCmd(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(command);
    }
}
