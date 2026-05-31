package com.tom.sortmod.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Square button with a vanilla button frame and a single 16×16 icon drawn from
 * the SortMod icon atlas. The atlas is a 96×16 PNG laid out as six 16×16 tiles;
 * {@code iconIndex} selects which tile to draw.
 */
public class IconButton extends AbstractButton {

    public static final Identifier ICONS =
            Identifier.fromNamespaceAndPath("sortmod", "textures/gui/icons.png");

    public static final int ICON_SORT           = 0;
    public static final int ICON_DUMP_TO        = 1;
    public static final int ICON_PULL_FROM      = 2;
    public static final int ICON_RESTOCK_PLAYER = 3;
    public static final int ICON_RESTOCK_CONT   = 4;
    public static final int ICON_QUICK_STACK    = 5;

    private static final int ATLAS_W = 96;
    private static final int ATLAS_H = 16;
    private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED;

    private final int iconIndex;
    private final Runnable action;

    public IconButton(int x, int y, int size, int iconIndex, Component tooltip, Runnable action) {
        super(x, y, size, size, tooltip);
        this.iconIndex = iconIndex;
        this.action = action;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        // Center the 16×16 icon inside the button.
        int iconX = getX() + (width  - 16) / 2;
        int iconY = getY() + (height - 16) / 2;
        g.blit(PIPELINE, ICONS, iconX, iconY, iconIndex * 16f, 0f, 16, 16, ATLAS_W, ATLAS_H);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
