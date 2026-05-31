package com.tom.sortmod.mixin;

import com.tom.sortmod.client.IconButton;
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

        int bs = 20;
        int gap = 2;
        int bx = this.leftPos;
        int topY = this.topPos - bs - 2;

        // Sort player inventory
        this.addRenderableWidget(new IconButton(bx, topY, bs, IconButton.ICON_SORT,
                Component.literal("Sort player inventory"),     () -> sendCmd("sortinv")));

        // Quick-stack hotbar/inventory to nearby chests
        this.addRenderableWidget(new IconButton(bx + bs + gap, topY, bs, IconButton.ICON_QUICK_STACK,
                Component.literal("Quick stack to nearby chests"), () -> sendCmd("quickstack")));
    }

    private static void sendCmd(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(command);
    }
}
