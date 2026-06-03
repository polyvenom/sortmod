package com.tom.sortmod.client;

/**
 * Which corner of the container GUI a SortMod UI element attaches to.
 *
 * The base position is the corner pixel of the standard 176px-wide vanilla
 * container window. Offsets are then applied in screen-pixel coordinates
 * (positive X = right, positive Y = down):
 *
 *   TOP_LEFT     base = (leftPos,         topPos                ) — element top-LEFT  at base + offset
 *   TOP_RIGHT    base = (leftPos + 176,   topPos                ) — element top-RIGHT at base + offset
 *   BOTTOM_LEFT  base = (leftPos,         topPos + imageHeight  ) — element top-LEFT  at base + offset
 *   BOTTOM_RIGHT base = (leftPos + 176,   topPos + imageHeight  ) — element top-RIGHT at base + offset
 *
 * For LEFT anchors, positive offsetX pushes the element rightward (into the
 * gui). For RIGHT anchors, negative offsetX pushes the element leftward
 * (also into the gui).
 *
 * For TOP anchors, negative offsetY puts the element above the gui (default
 * behavior for button rows). For BOTTOM anchors, positive offsetY puts the
 * element below the gui.
 */
public enum Anchor {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    public boolean isRight()  { return this == TOP_RIGHT  || this == BOTTOM_RIGHT; }
    public boolean isBottom() { return this == BOTTOM_LEFT || this == BOTTOM_RIGHT; }

    /** Cycle to the next anchor in TL → TR → BR → BL → TL order. */
    public Anchor next() {
        return switch (this) {
            case TOP_LEFT     -> TOP_RIGHT;
            case TOP_RIGHT    -> BOTTOM_RIGHT;
            case BOTTOM_RIGHT -> BOTTOM_LEFT;
            case BOTTOM_LEFT  -> TOP_LEFT;
        };
    }
}
