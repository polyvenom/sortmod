import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the SortMod Modrinth icon (256×256 PNG).
 * Run: java tools/ModIcon.java <output-path>
 */
public class ModIcon {
    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "icon.png";

        int S = 256;
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

        // ── Background ──────────────────────────────────────────────────────
        g.setColor(new Color(0x24263a));
        g.fillRoundRect(0, 0, S, S, 32, 32);

        // ── Chest geometry ──────────────────────────────────────────────────
        int chX = 12, chY = 10;
        int chW = S - 2 * chX;   // 232
        int chH = S - 2 * chY;   // 236
        int lidH  = 64;
        int hngeH = 8;
        int bY = chY + lidH + hngeH;   // body top
        int bH = chH - lidH - hngeH;   // body height (164)

        // ── Wood palette ────────────────────────────────────────────────────
        Color wb  = new Color(0x3d1f05);  // outline/border
        Color wd  = new Color(0x7a4018);  // dark plank grain
        Color wm  = new Color(0x9a5520);  // mid (base fill)
        Color wl  = new Color(0xb86e2e);  // left-edge highlight
        Color wh  = new Color(0xd08840);  // top-edge highlight
        Color hg  = new Color(0xc8921c);  // hardware gold
        Color hgl = new Color(0xf0c040);  // hardware gold highlight

        // ── Draw a wood panel (lid or body) ─────────────────────────────────
        // Body
        drawPanel(g, chX, bY, chW, bH, wb, wm, wd, wl, wh, false);
        // Lid
        drawPanel(g, chX, chY, chW, lidH, wb, wm, wd, wl, wh, true);

        // ── Hinge bar ───────────────────────────────────────────────────────
        g.setColor(wb);
        g.fillRect(chX, chY + lidH, chW, hngeH);
        for (int hx : new int[]{ chX + 30, chX + chW - 30 }) {
            g.setColor(wb);  g.fillOval(hx - 7, chY + lidH - 1, 14, hngeH + 2);
            g.setColor(hg);  g.fillOval(hx - 5, chY + lidH + 1, 10, hngeH - 2);
            g.setColor(hgl); g.fillOval(hx - 3, chY + lidH + 2,  6,  4);
        }

        // ── Clasp ───────────────────────────────────────────────────────────
        int clW = 26, clH = 34;
        int clX = chX + chW / 2 - clW / 2;
        int clY = chY + lidH + hngeH / 2 - clH / 2;
        g.setColor(wb);  g.fillRoundRect(clX - 2, clY - 2, clW + 4, clH + 4, 6, 6);
        g.setColor(hg);  g.fillRoundRect(clX,     clY,     clW,     clH,     4, 4);
        g.setColor(hgl); g.fillRoundRect(clX + 3, clY + 3, clW - 6, clH / 2 - 2, 3, 3);
        g.setColor(wb);  g.fillOval(clX + clW / 2 - 4, clY + clH / 2 - 4, 8, 8);
        g.setColor(wb);  g.fillRect(clX + clW / 2 - 2, clY + clH / 2 + 2, 5, 7);

        // ── 3×3 item grid inside body ────────────────────────────────────────
        int cell = 44, gap = 7;
        int gridW = 3 * cell + 2 * gap;   // 146
        int gridH = 3 * cell + 2 * gap;   // 146
        int gX = chX + (chW - gridW) / 2;
        int gY = bY  + (bH  - gridH) / 2;

        // Each colour loosely suggests a different item type
        int[][] palette = {
            { 0xc0392b, 0xe67e22, 0xf1c40f },  // red, orange, yellow
            { 0x27ae60, 0x2980b9, 0x8e44ad },  // green, blue, purple
            { 0x16a085, 0x95a5a6, 0xe8b84a },  // teal, silver, gold
        };

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int ix = gX + col * (cell + gap);
                int iy = gY + row * (cell + gap);
                Color c  = new Color(palette[row][col]);
                Color cd = c.darker();
                Color cl = c.brighter();

                // drop shadow
                g.setColor(new Color(0, 0, 0, 70));
                g.fillRoundRect(ix + 3, iy + 3, cell, cell, 7, 7);
                // item border
                g.setColor(cd.darker());
                g.fillRoundRect(ix, iy, cell, cell, 6, 6);
                // item face
                g.setColor(c);
                g.fillRoundRect(ix + 2, iy + 2, cell - 4, cell - 4, 4, 4);
                // specular highlight
                g.setColor(new Color(
                        Math.min(cl.getRed(),   255),
                        Math.min(cl.getGreen(), 255),
                        Math.min(cl.getBlue(),  255), 170));
                g.fillRoundRect(ix + 5, iy + 5, cell - 12, 9, 3, 3);
            }
        }

        g.dispose();
        ImageIO.write(img, "png", new File(out));
        System.out.println("wrote " + out);
    }

    static void drawPanel(Graphics2D g,
                          int x, int y, int w, int h,
                          Color border, Color mid, Color dark, Color leftEdge, Color topEdge,
                          boolean roundTop) {
        int r = roundTop ? 8 : 4;
        // outline
        g.setColor(border);
        g.fillRoundRect(x, y, w, h, r, r);
        // base fill
        g.setColor(mid);
        g.fillRect(x + 3, y + 3, w - 6, h - 3);
        // horizontal plank lines
        g.setColor(dark);
        for (int py = y + 3; py < y + h - 3; py += 22)
            g.fillRect(x + 3, py, w - 6, 2);
        // left-edge bevel
        g.setColor(leftEdge);
        g.fillRect(x + 3, y + 3, 4, h - 6);
        // top-edge bevel
        g.setColor(topEdge);
        g.fillRect(x + 3, y + 3, w - 6, 3);
    }
}
