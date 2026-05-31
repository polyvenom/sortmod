import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Generates the SortMod button icon atlas at runtime.
 *
 * Output: 96×16 PNG containing six 16×16 monochrome glyph icons in this order:
 *   0  SORT             — two opposing horizontal arrows (sort)
 *   1  DUMP_TO          — bold up arrow      (player → container, all items)
 *   2  PULL_FROM        — bold down arrow    (container → player, all items)
 *   3  RESTOCK_PLAYER   — down arrow + curve (only items the player already has)
 *   4  RESTOCK_CONT     — up arrow + curve   (only items the container already has)
 *   5  QUICK_STACK      — outward arrows     (to multiple nearby chests)
 *
 * Each icon is drawn pixel-by-pixel from a string template, where '#' is opaque
 * white and any other character is transparent. This way the source is editable
 * by eye and version control diffs are meaningful.
 *
 * Build/run with: java tools/IconGen.java   (single-file source, JEP 330+)
 */
public class IconGen {

    private static final int W = 16, H = 16, COUNT = 6;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int CLEAR = 0x00000000;

    private static final String[] SORT = {
        "................",
        "................",
        "................",
        ".....#..........",
        "....##..........",
        "...#############",
        "....##..........",
        ".....#..........",
        "..........#.....",
        ".........##.....",
        "#############...",
        ".........##.....",
        "..........#.....",
        "................",
        "................",
        "................",
    };

    private static final String[] DUMP_TO = {
        "................",
        "................",
        ".......##.......",
        "......####......",
        ".....######.....",
        "....########....",
        "...##########...",
        "......####......",
        "......####......",
        "......####......",
        "......####......",
        "......####......",
        "................",
        "..############..",
        "..############..",
        "................",
    };

    private static final String[] PULL_FROM = {
        "................",
        "..############..",
        "..############..",
        "................",
        "......####......",
        "......####......",
        "......####......",
        "......####......",
        "......####......",
        "...##########...",
        "....########....",
        ".....######.....",
        "......####......",
        ".......##.......",
        "................",
        "................",
    };

    private static final String[] RESTOCK_PLAYER = {
        "................",
        "...########.....",
        "..##........##..",
        "..#..........#..",
        "..#..........#..",
        "..#..........#..",
        "..............#.",
        "..............#.",
        "..............#.",
        ".....#.......#..",
        "....##......##..",
        "...###########..",
        "....##..........",
        ".....#..........",
        "................",
        "................",
    };

    private static final String[] RESTOCK_CONT = {
        "................",
        "................",
        ".....#..........",
        "....##..........",
        "...###########..",
        "....##......##..",
        ".....#.......#..",
        "..............#.",
        "..............#.",
        "..............#.",
        "..#..........#..",
        "..#..........#..",
        "..#..........#..",
        "..##........##..",
        "...########.....",
        "................",
    };

    private static final String[] QUICK_STACK = {
        "................",
        ".......##.......",
        "......####......",
        ".....######.....",
        "....##.##.##....",
        "...##..##..##...",
        "..##...##...##..",
        ".##....##....##.",
        "................",
        ".......##.......",
        "......####......",
        ".....######.....",
        "....##.##.##....",
        "...##..##..##...",
        "..##...##...##..",
        ".##....##....##.",
    };

    public static void main(String[] args) throws Exception {
        String[][] all = { SORT, DUMP_TO, PULL_FROM, RESTOCK_PLAYER, RESTOCK_CONT, QUICK_STACK };
        BufferedImage atlas = new BufferedImage(W * COUNT, H, BufferedImage.TYPE_INT_ARGB);

        for (int i = 0; i < all.length; i++) {
            String[] glyph = all[i];
            if (glyph.length != H) {
                throw new IllegalStateException("icon " + i + " has " + glyph.length + " rows, expected " + H);
            }
            for (int y = 0; y < H; y++) {
                String row = glyph[y];
                if (row.length() != W) {
                    throw new IllegalStateException("icon " + i + " row " + y + " has " + row.length() + " cols, expected " + W);
                }
                for (int x = 0; x < W; x++) {
                    atlas.setRGB(i * W + x, y, row.charAt(x) == '#' ? WHITE : CLEAR);
                }
            }
        }

        File out = new File(args.length > 0 ? args[0] : "icons.png");
        ImageIO.write(atlas, "PNG", out);
        System.out.println("wrote " + out.getAbsolutePath());
    }
}
