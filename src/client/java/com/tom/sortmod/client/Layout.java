package com.tom.sortmod.client;

/**
 * Anchor + offset pair describing where a SortMod UI element should be drawn
 * within a container screen. See {@link Anchor} for the coordinate convention.
 *
 * Mutable so the config screen can edit fields in place without thrashing
 * object identity. The fields are read only on the client tick thread (mixin
 * init), so no synchronization is needed.
 */
public final class Layout {
    public Anchor anchor;
    public int offsetX;
    public int offsetY;

    public Layout(Anchor anchor, int offsetX, int offsetY) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public Layout copy() {
        return new Layout(anchor, offsetX, offsetY);
    }

    /**
     * Compute the top-left screen-pixel coordinates for an element of the
     * given size positioned by this layout, relative to a container gui whose
     * upper-left corner is (leftPos, topPos) and whose interior is 176×imageHeight.
     */
    public int x(int leftPos, int elementWidth) {
        int base = anchor.isRight() ? leftPos + 176 : leftPos;
        int withOffset = base + offsetX;
        return anchor.isRight() ? withOffset - elementWidth : withOffset;
    }

    public int y(int topPos, int imageHeight, int elementHeight) {
        int base = anchor.isBottom() ? topPos + imageHeight : topPos;
        return base + offsetY;
    }
}
