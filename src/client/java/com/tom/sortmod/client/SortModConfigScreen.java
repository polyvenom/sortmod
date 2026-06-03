package com.tom.sortmod.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Plain vanilla-widget config screen. Reachable via the ModMenu mods list
 * (if ModMenu is installed) or by binding the in-game key… well, not yet —
 * for now ModMenu is the entry point.
 *
 * Four checkboxes for the four granular UI toggles, plus a Done button that
 * returns to the previous screen. Config changes are written through to disk
 * immediately by the {@link SortModConfig} setters.
 */
public class SortModConfigScreen extends Screen {

    private static final int ROW_HEIGHT  = 24;
    private static final int CHECKBOX_W  = 280;
    private static final int BUTTON_W    = 150;
    private static final int BUTTON_H    = 20;

    private final Screen parent;

    public SortModConfigScreen(Screen parent) {
        super(Component.translatable("sortmod.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int leftX   = centerX - (CHECKBOX_W / 2);
        int topY    = 50;

        // Title label centered above the first row.
        this.addRenderableWidget(new StringWidget(
                leftX, 20, CHECKBOX_W, 20,
                this.getTitle(), this.font));

        // Row 1 — container buttons (the ClientSort overlap row)
        this.addRenderableWidget(Checkbox.builder(
                        Component.translatable("sortmod.config.showContainerButtons"), this.font)
                .pos(leftX, topY)
                .selected(SortModConfig.showContainerButtons())
                .tooltip(Tooltip.create(Component.translatable("sortmod.config.showContainerButtons.tip")))
                .onValueChange((cb, v) -> SortModConfig.setShowContainerButtons(v))
                .build());

        // Row 2 — inventory sort button
        this.addRenderableWidget(Checkbox.builder(
                        Component.translatable("sortmod.config.showInventorySortButton"), this.font)
                .pos(leftX, topY + ROW_HEIGHT)
                .selected(SortModConfig.showInventorySortButton())
                .tooltip(Tooltip.create(Component.translatable("sortmod.config.showInventorySortButton.tip")))
                .onValueChange((cb, v) -> SortModConfig.setShowInventorySortButton(v))
                .build());

        // Row 3 — quick stack button (the unique-feature button)
        this.addRenderableWidget(Checkbox.builder(
                        Component.translatable("sortmod.config.showQuickStackButton"), this.font)
                .pos(leftX, topY + ROW_HEIGHT * 2)
                .selected(SortModConfig.showQuickStackButton())
                .tooltip(Tooltip.create(Component.translatable("sortmod.config.showQuickStackButton.tip")))
                .onValueChange((cb, v) -> SortModConfig.setShowQuickStackButton(v))
                .build());

        // Row 4 — search box
        this.addRenderableWidget(Checkbox.builder(
                        Component.translatable("sortmod.config.showSearchBox"), this.font)
                .pos(leftX, topY + ROW_HEIGHT * 3)
                .selected(SortModConfig.showSearchBox())
                .tooltip(Tooltip.create(Component.translatable("sortmod.config.showSearchBox.tip")))
                .onValueChange((cb, v) -> SortModConfig.setShowSearchBox(v))
                .build());

        // Footer — Layout sub-screen on the left, Done on the right.
        int footerY = this.height - 30;
        int gap = 10;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("sortmod.config.layout"),
                        btn -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new SortModLayoutScreen(this));
                            }
                        })
                .bounds(centerX - BUTTON_W - gap / 2, footerY, BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("sortmod.config.layout.tip")))
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("sortmod.config.done"),
                        btn -> this.onClose())
                .bounds(centerX + gap / 2, footerY, BUTTON_W, BUTTON_H)
                .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}
