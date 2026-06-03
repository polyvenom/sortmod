package com.tom.sortmod.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Layout-positioning sub-screen. Lets the user assign each SortMod UI group
 * its own anchor (corner of the container gui) and X/Y offset relative to
 * that anchor.
 *
 * Use cases: avoiding overlap with ClientSort, NEI/REI search bars, Trashslot,
 * or any other mod that also injects widgets into container screens.
 *
 * Edits write through to {@link SortModConfig} immediately and persist on
 * every change — there's no "Apply" button. The Done button just navigates
 * back to the parent screen.
 */
public class SortModLayoutScreen extends Screen {

    private static final int ROW_H        = 24;
    private static final int LABEL_W      = 140;
    private static final int ANCHOR_BTN_W = 100;
    private static final int OFFSET_BOX_W = 40;
    private static final int LETTER_W     = 12;
    private static final int FOOTER_BTN_W = 110;
    private static final int FOOTER_BTN_H = 20;

    private final Screen parent;

    public SortModLayoutScreen(Screen parent) {
        super(Component.translatable("sortmod.layout.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Total row width: label + anchor + " X: " + box + " Y: " + box.
        int rowW = LABEL_W + ANCHOR_BTN_W + LETTER_W + OFFSET_BOX_W + LETTER_W + OFFSET_BOX_W + 16;
        int leftX = (this.width / 2) - (rowW / 2);
        int topY  = 50;

        // Title.
        this.addRenderableWidget(new StringWidget(
                leftX, 20, rowW, 20, this.getTitle(), this.font));

        addRow(leftX, topY + ROW_H * 0,
                "sortmod.layout.containerButtons",
                SortModConfig.containerButtonsLayout());

        addRow(leftX, topY + ROW_H * 1,
                "sortmod.layout.chestPlayerSort",
                SortModConfig.chestPlayerSortLayout());

        addRow(leftX, topY + ROW_H * 2,
                "sortmod.layout.searchBox",
                SortModConfig.searchBoxLayout());

        addRow(leftX, topY + ROW_H * 3,
                "sortmod.layout.inventoryScreenButtons",
                SortModConfig.inventoryScreenButtonsLayout());

        // Footer — Reset Defaults on the left, Done on the right.
        int footerY = this.height - 30;
        int gap = 10;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("sortmod.layout.reset"),
                        btn -> {
                            SortModConfig.resetLayouts();
                            // Rebuild the screen so the new values show up in
                            // every EditBox / anchor button.
                            this.rebuildWidgets();
                        })
                .bounds(this.width / 2 - FOOTER_BTN_W - gap / 2, footerY,
                        FOOTER_BTN_W, FOOTER_BTN_H)
                .tooltip(Tooltip.create(Component.translatable("sortmod.layout.reset.tip")))
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("sortmod.config.done"),
                        btn -> this.onClose())
                .bounds(this.width / 2 + gap / 2, footerY,
                        FOOTER_BTN_W, FOOTER_BTN_H)
                .build());
    }

    private void addRow(int leftX, int rowY, String labelKey, Layout layout) {
        int x = leftX;

        // Label.
        this.addRenderableWidget(new StringWidget(
                x, rowY + 5, LABEL_W, 20,
                Component.translatable(labelKey), this.font));
        x += LABEL_W;

        // Anchor cycle button — clicking advances through TL → TR → BR → BL.
        Button anchorBtn = Button.builder(
                        anchorLabel(layout.anchor),
                        btn -> {
                            layout.anchor = layout.anchor.next();
                            btn.setMessage(anchorLabel(layout.anchor));
                            SortModConfig.saveLayouts();
                        })
                .bounds(x, rowY, ANCHOR_BTN_W, 20)
                .tooltip(Tooltip.create(Component.translatable("sortmod.layout.anchor.tip")))
                .build();
        this.addRenderableWidget(anchorBtn);
        x += ANCHOR_BTN_W + 4;

        // X label + EditBox.
        this.addRenderableWidget(new StringWidget(
                x, rowY + 5, LETTER_W, 20, Component.literal("X:"), this.font));
        x += LETTER_W;

        EditBox xBox = new EditBox(this.font, x, rowY, OFFSET_BOX_W, 20,
                Component.translatable("sortmod.layout.offsetX"));
        xBox.setValue(Integer.toString(layout.offsetX));
        xBox.setMaxLength(5);
        xBox.setResponder(s -> {
            Integer v = parseSignedInt(s);
            if (v != null) {
                layout.offsetX = v;
                SortModConfig.saveLayouts();
            }
        });
        this.addRenderableWidget(xBox);
        x += OFFSET_BOX_W + 4;

        // Y label + EditBox.
        this.addRenderableWidget(new StringWidget(
                x, rowY + 5, LETTER_W, 20, Component.literal("Y:"), this.font));
        x += LETTER_W;

        EditBox yBox = new EditBox(this.font, x, rowY, OFFSET_BOX_W, 20,
                Component.translatable("sortmod.layout.offsetY"));
        yBox.setValue(Integer.toString(layout.offsetY));
        yBox.setMaxLength(5);
        yBox.setResponder(s -> {
            Integer v = parseSignedInt(s);
            if (v != null) {
                layout.offsetY = v;
                SortModConfig.saveLayouts();
            }
        });
        this.addRenderableWidget(yBox);
    }

    private static Component anchorLabel(Anchor anchor) {
        return Component.translatable("sortmod.layout.anchor." + anchor.name());
    }

    /** Parse "" / "-" leniently as "not yet a valid number" (returns null). */
    private static Integer parseSignedInt(String s) {
        if (s == null || s.isEmpty() || s.equals("-")) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}
