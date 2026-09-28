package secret.kinetic.launcher;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;





final class Ui {

    private Ui() {
    }

    

    
    enum Theme {
        KINETIC("Kinetic"), ISRAEL("Israel"), CHRISTIAN("Christian");

        final String label;

        Theme(String label) {
            this.label = label;
        }

        static Theme parse(String value) {
            for (Theme theme : values()) if (theme.name().equalsIgnoreCase(value == null ? "" : value.trim())) return theme;
            return KINETIC;
        }
    }

    
    static Theme theme;
    
    static final Color DEFAULT_ACCENT = new Color(255, 118, 176), DEFAULT_ACCENT_DEEP = new Color(255, 205, 96);
    
    static Color customAccent, customAccentDeep;
    static Color ACCENT, ACCENT_DEEP, ACCENT_LIGHT, BASE, SHADE, TEXT, MUTED, DIM, GLASS_TINT, GLASS_STRONG, LOG_TINT,
            DIALOG, DIALOG_TOP, DANGER, SUCCESS, SELECTION, TOOLTIP, TOOLTIP_LINE, EMBER_CORE, EMBER_EDGE;

    static {
        applyTheme(Theme.KINETIC);
    }

    static void applyTheme(Theme next) {
        theme = next;
        SUCCESS = new Color(98, 222, 150);
        if (next == Theme.ISRAEL) {
            ACCENT = new Color(40, 118, 255);
            ACCENT_DEEP = new Color(16, 62, 170);
            ACCENT_LIGHT = new Color(120, 170, 255);
            BASE = new Color(4, 7, 14);
            SHADE = new Color(3, 5, 12);
            TEXT = new Color(244, 247, 255);
            MUTED = new Color(168, 180, 204);
            DIM = new Color(110, 122, 148);
            GLASS_TINT = new Color(6, 10, 24, 150);
            GLASS_STRONG = new Color(5, 9, 22, 190);
            LOG_TINT = new Color(3, 5, 12, 200);
            DIALOG = new Color(9, 14, 30);
            DIALOG_TOP = new Color(18, 30, 64);
            DANGER = new Color(255, 112, 112);
            TOOLTIP = new Color(16, 26, 56);
            TOOLTIP_LINE = new Color(60, 100, 190);
            EMBER_CORE = new Color(240, 246, 255, 255);
            EMBER_EDGE = new Color(90, 150, 255, 150);
        } else if (next == Theme.CHRISTIAN) {
            ACCENT = new Color(255, 209, 102);
            ACCENT_DEEP = new Color(196, 146, 36);
            ACCENT_LIGHT = new Color(255, 236, 176);
            BASE = new Color(14, 11, 5);
            SHADE = new Color(11, 9, 4);
            TEXT = new Color(255, 252, 244);
            MUTED = new Color(210, 198, 172);
            DIM = new Color(150, 138, 116);
            GLASS_TINT = new Color(24, 19, 8, 150);
            GLASS_STRONG = new Color(22, 17, 7, 190);
            LOG_TINT = new Color(11, 9, 4, 200);
            DIALOG = new Color(28, 23, 11);
            DIALOG_TOP = new Color(52, 42, 18);
            DANGER = new Color(255, 118, 108);
            TOOLTIP = new Color(44, 36, 16);
            TOOLTIP_LINE = new Color(150, 118, 44);
            EMBER_CORE = new Color(255, 246, 220, 255);
            EMBER_EDGE = new Color(255, 209, 102, 150);
        } else {
            Color accent = customAccent != null ? customAccent : DEFAULT_ACCENT;
            ACCENT = accent;
            ACCENT_DEEP = customAccentDeep != null ? customAccentDeep : customAccent != null ? scale(customAccent, 0.62f) : DEFAULT_ACCENT_DEEP;
            ACCENT_LIGHT = mix(accent, Color.WHITE, 0.32f);
            BASE = new Color(9, 5, 7);
            SHADE = new Color(7, 3, 5);
            TEXT = new Color(246, 240, 242);
            MUTED = new Color(186, 166, 171);
            DIM = new Color(132, 112, 118);
            GLASS_TINT = new Color(16, 6, 9, 150);
            GLASS_STRONG = new Color(14, 5, 8, 190);
            LOG_TINT = new Color(6, 3, 5, 200);
            DIALOG = new Color(22, 10, 13);
            DIALOG_TOP = new Color(34, 13, 17);
            DANGER = new Color(255, 118, 108);
            TOOLTIP = new Color(40, 18, 22);
            TOOLTIP_LINE = new Color(120, 44, 52);
            TOOLTIP_LINE = scale(accent, 0.55f);
            EMBER_CORE = mix(accent, Color.WHITE, 0.7f);
            EMBER_EDGE = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150);
        }
        SELECTION = alpha(ACCENT, 120);
    }

    private static Color scale(Color c, float f) {
        return new Color(Math.round(c.getRed() * f), Math.round(c.getGreen() * f), Math.round(c.getBlue() * f));
    }

    
    static String themeKey() {
        return theme.name() + (theme == Theme.KINETIC && customAccent != null ? ":" + Integer.toHexString(customAccent.getRGB()) : "");
    }

    static boolean israel() {
        return theme == Theme.ISRAEL;
    }

    static boolean christian() {
        return theme == Theme.CHRISTIAN;
    }
    
    static final Color[] BADGE_COLORS = {
            new Color(64, 196, 178), new Color(255, 160, 72), new Color(84, 156, 255),
            new Color(118, 206, 104), new Color(236, 196, 84), new Color(196, 196, 206)
    };
    static final Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    

    




    private static final Font MONA = load("mona"), MONA_MEDIUM = load("mona-medium"), MONA_SEMIBOLD = load("mona-semibold"),
            MONA_BOLD = load("mona-bold"), MONA_WIDE = load("mona-expanded-semibold"), HUBOT = load("hubot-logo-800");
    static final String FAMILY = MONA != null ? MONA.getFamily() : Font.DIALOG;
    private static final String MONO = pick(Font.MONOSPACED, "Cascadia Mono", "Consolas", "JetBrains Mono", "SF Mono", "Menlo", "DejaVu Sans Mono");

    private static Font load(String file) {
        try (InputStream in = Ui.class.getResourceAsStream("fonts/" + file + ".ttf")) {
            if (in == null) return null;
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            return null;
        }
    }

    private static String pick(String fallback, String... candidates) {
        Set<String> families;
        try {
            families = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        } catch (RuntimeException e) {
            return fallback;
        }
        for (String family : candidates) if (families.contains(family)) return family;
        return fallback;
    }

    private static Font face(Font face, int fallbackStyle, float size) {
        return face != null ? face.deriveFont(Font.PLAIN, size) : new Font(Font.DIALOG, fallbackStyle, 12).deriveFont(size);
    }

    static Font font(int style, float size) {
        return face((style & Font.BOLD) != 0 ? MONA_BOLD : MONA, style, size);
    }

    static Font medium(float size) {
        return face(MONA_MEDIUM, Font.PLAIN, size);
    }

    static Font semibold(float size) {
        return face(MONA_SEMIBOLD, Font.BOLD, size);
    }

    
    static Font display(float size) {
        return face(MONA_BOLD, Font.BOLD, size);
    }

    
    static Font wide(float size) {
        return face(MONA_WIDE, Font.BOLD, size);
    }

    
    static Font wordmark(float size) {
        return face(HUBOT, Font.BOLD | Font.ITALIC, size);
    }

    static Font mono(float size) {
        return new Font(MONO, Font.PLAIN, 12).deriveFont(size);
    }

    
    static Font tracked(Font font, float tracking) {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, tracking);
        return font.deriveFont(attributes);
    }

    

    private static final long START = System.nanoTime();
    
    static volatile double frozenTime = -1;

    static double now() {
        double frozen = frozenTime;
        return frozen >= 0 ? frozen : (System.nanoTime() - START) / 1e9;
    }

    

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (hints instanceof Map) g2.addRenderingHints((Map<?, ?>) hints);
        else g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g2;
    }

    



    static BufferedImage crisp(BufferedImage source, int size) {
        BufferedImage scaled = scale(source, size, size, BufferedImage.TYPE_INT_ARGB);
        if (size > 96) return scaled;
        float a = size <= 32 ? 0.28f : 0.18f;
        java.awt.image.Kernel kernel = new java.awt.image.Kernel(3, 3, new float[]{0, -a, 0, -a, 1 + 4 * a, -a, 0, -a, 0});
        BufferedImage out = new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null)
                .filter(scaled, new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB));
        
        int[] sharp = out.getRGB(0, 0, size, size, null, 0, size);
        int[] plain = scaled.getRGB(0, 0, size, size, null, 0, size);
        for (int i = 0; i < sharp.length; i++) sharp[i] = (plain[i] & 0xFF000000) | (sharp[i] & 0x00FFFFFF);
        out.setRGB(0, 0, size, size, sharp, 0, size);
        return out;
    }

    static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha, 0, 255));
    }

    static Color mix(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static double smoothstep(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    static String ellipsize(String text, FontMetrics fm, int max) {
        if (fm.stringWidth(text) <= max) return text;
        String dots = "…";
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end) + dots) > max) end--;
        return text.substring(0, end) + dots;
    }

    static void centerText(Graphics2D g2, String text, int x, int y, int w, int h) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2f, y + (h - fm.getHeight()) / 2f + fm.getAscent());
    }

    static JPanel transparent(LayoutManager layout) {
        JPanel panel = layout == null ? new JPanel() : new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JLabel label(String text, Color color, Font font) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font);
        return label;
    }

    
    static JLabel caption(String text) {
        return label(text, DIM, tracked(wide(9.5f), 0.12f));
    }

    static Btn ghostButton(String text, Runnable action) {
        Btn button = new Btn(text, Kind.GHOST);
        button.addActionListener(e -> action.run());
        return button;
    }

    
    static BufferedImage roundImage(BufferedImage source, int size) {
        int side = Math.min(source.getWidth(), source.getHeight());
        BufferedImage square = source.getSubimage((source.getWidth() - side) / 2, (source.getHeight() - side) / 2, side, side);
        BufferedImage scaled = scale(square, size, size, BufferedImage.TYPE_INT_ARGB_PRE);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(0, 0, size, size));
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }

    
    static final class TrackingPanel extends JPanel implements javax.swing.Scrollable {
        TrackingPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, visible.height - 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    static BufferedImage loadImage(String name) {
        try (InputStream in = Ui.class.getResourceAsStream("/secret/kinetic/launcher/" + name)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    
    static BufferedImage scale(BufferedImage source, int width, int height, int type) {
        width = Math.max(1, width);
        height = Math.max(1, height);
        BufferedImage current = source;
        int w = source.getWidth(), h = source.getHeight();
        do {
            w = w > width ? Math.max(width, w / 2) : width;
            h = h > height ? Math.max(height, h / 2) : height;
            BufferedImage next = new BufferedImage(w, h, type);
            Graphics2D g = next.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(current, 0, 0, w, h, null);
            g.dispose();
            current = next;
        } while (w != width || h != height);
        return current;
    }

    
    static BufferedImage blur(BufferedImage source, int factor, int radius) {
        int w = source.getWidth(), h = source.getHeight();
        boolean alpha = source.getColorModel().hasAlpha();
        int type = alpha ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_RGB;
        BufferedImage small = scale(source, Math.max(1, w / factor), Math.max(1, h / factor), type);
        int sw = small.getWidth(), sh = small.getHeight();
        int[] pixels = ((DataBufferInt) small.getRaster().getDataBuffer()).getData();
        int[] temp = new int[pixels.length];
        for (int pass = 0; pass < 3; pass++) {
            boxBlurTranspose(pixels, temp, sw, sh, radius);
            boxBlurTranspose(temp, pixels, sh, sw, radius);
        }
        if (!alpha) for (int i = 0; i < pixels.length; i++) pixels[i] |= 0xFF000000;
        return scale(small, w, h, type);
    }

    
    private static void boxBlurTranspose(int[] in, int[] out, int w, int h, int r) {
        int div = 2 * r + 1;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int a = 0, red = 0, green = 0, blue = 0;
            for (int i = -r; i <= r; i++) {
                int p = in[row + clamp(i, 0, w - 1)];
                a += p >>> 24;
                red += (p >> 16) & 255;
                green += (p >> 8) & 255;
                blue += p & 255;
            }
            for (int x = 0; x < w; x++) {
                out[x * h + y] = ((a / div) << 24) | ((red / div) << 16) | ((green / div) << 8) | (blue / div);
                int add = in[row + Math.min(x + r + 1, w - 1)];
                int sub = in[row + Math.max(x - r, 0)];
                a += (add >>> 24) - (sub >>> 24);
                red += ((add >> 16) & 255) - ((sub >> 16) & 255);
                green += ((add >> 8) & 255) - ((sub >> 8) & 255);
                blue += (add & 255) - (sub & 255);
            }
        }
    }

    private static BufferedImage radialSprite(int size, Color inner, Color outer) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        float c = size / 2f;
        g.setPaint(new RadialGradientPaint(c, c, c, new float[]{0f, 0.35f, 1f}, new Color[]{inner, outer, alpha(outer, 0)}));
        g.fillRect(0, 0, size, size);
        g.dispose();
        return image;
    }

    
    static void paintHexagram(Graphics2D g, float cx, float cy, float radius, float stroke, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
        for (int t = 0; t < 2; t++) {
            Path2D triangle = new Path2D.Float();
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(-90 + t * 180 + i * 120);
                float x = cx + (float) (Math.cos(angle) * radius), y = cy + (float) (Math.sin(angle) * radius);
                if (i == 0) triangle.moveTo(x, y);
                else triangle.lineTo(x, y);
            }
            triangle.closePath();
            g2.draw(triangle);
        }
        g2.dispose();
    }

    
    static void paintCross(Graphics2D g, float cx, float cy, float radius, float stroke, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
        float armY = cy - radius * 0.28f;
        g2.draw(new java.awt.geom.Line2D.Float(cx, cy - radius, cx, cy + radius));
        g2.draw(new java.awt.geom.Line2D.Float(cx - radius * 0.72f, armY, cx + radius * 0.72f, armY));
        g2.dispose();
    }

    

    
    static final class Fader {
        private static final List<Fader> RUNNING = new ArrayList<>();
        private static Timer timer;

        private final JComponent owner;
        private final float speed;
        private float value, target;

        Fader(JComponent owner, float speed, float initial) {
            this.owner = owner;
            this.speed = speed;
            this.value = this.target = initial;
        }

        float get() {
            return value;
        }

        void to(float target) {
            if (target == this.target && (value == target || RUNNING.contains(this))) return;
            this.target = target;
            if (frozenTime >= 0 || !owner.isShowing()) {
                set(target);
                return;
            }
            if (!RUNNING.contains(this)) RUNNING.add(this);
            if (timer == null) timer = new Timer(15, e -> tick());
            if (!timer.isRunning()) timer.start();
        }

        void set(float value) {
            this.value = this.target = value;
            RUNNING.remove(this);
            owner.repaint();
        }

        private static void tick() {
            for (Iterator<Fader> it = RUNNING.iterator(); it.hasNext(); ) {
                Fader fader = it.next();
                fader.value += (fader.target - fader.value) * fader.speed;
                if (Math.abs(fader.target - fader.value) < 0.004f) {
                    fader.value = fader.target;
                    it.remove();
                }
                fader.owner.repaint();
            }
            if (RUNNING.isEmpty()) timer.stop();
        }
    }

    

    




    static final class Backdrop extends JPanel {
        private static final float ZOOM = 1.08f;
        private static final double HOLD = 7.0, FADE = 3.5;
        private static final int EMBERS = 24;

        private final BufferedImage[] redStills;
        private final Map<String, BufferedImage[]> themedStills = new HashMap<>();
        private final Map<Theme, BufferedImage> themedArt = new HashMap<>();
        private BufferedImage[] stills;
        private BufferedImage art, ember;
        private String builtFor;
        private BufferedImage[] sharp, soft;
        private BufferedImage artSharp, artSoft, overlay, glow;
        private int cacheW = -1, cacheH = -1, pendingW = -1, pendingH = -1;
        private final Timer frames, rebuild;
        private final float[] emberX = new float[EMBERS], emberSpeed = new float[EMBERS], emberPhase = new float[EMBERS], emberSize = new float[EMBERS];
        
        boolean drawBorder;
        
        final Fader dim = new Fader(this, 0.12f, 0);

        Backdrop(LayoutManager layout) {
            super(layout);
            setOpaque(true);
            setBackground(BASE);
            List<BufferedImage> loaded = new ArrayList<>();
            for (String name : new String[]{"bg1.jpg", "bg2.jpg", "bg3.jpg"}) {
                BufferedImage image = loadImage(name);
                if (image != null) loaded.add(image);
            }
            redStills = loaded.toArray(new BufferedImage[0]);
            useTheme();
            Random random = new Random(7);
            for (int i = 0; i < EMBERS; i++) {
                emberX[i] = 0.28f + random.nextFloat() * 0.72f;
                emberSpeed[i] = 0.035f + random.nextFloat() * 0.05f;
                emberPhase[i] = random.nextFloat();
                emberSize[i] = 4 + random.nextFloat() * 9;
            }
            frames = new Timer(33, e -> repaint());
            frames.setCoalesce(true);
            rebuild = new Timer(180, e -> {
                build(getWidth(), getHeight());
                repaint();
            });
            rebuild.setRepeats(false);
        }

        
        void useTheme() {
            String key = themeKey();
            if (key.equals(builtFor)) return;
            builtFor = key;
            stills = themedStills.get(key);
            if (stills == null) {
                stills = new BufferedImage[redStills.length];
                for (int i = 0; i < redStills.length; i++) {
                    stills[i] = theme == Theme.ISRAEL ? toBlue(redStills[i])
                            : theme == Theme.CHRISTIAN ? toAccent(redStills[i], ACCENT)
                            : customAccent != null ? toAccent(redStills[i], customAccent) : redStills[i];
                }
                themedStills.put(key, stills);
            }
            art = themedArt.get(theme);
            if (art == null) {
                art = loadImage(theme == Theme.ISRAEL ? "israel_art.png" : theme == Theme.CHRISTIAN ? "christian_art.png" : "art.png");
                if (art != null) themedArt.put(theme, art);
            }
            ember = radialSprite(32, EMBER_CORE, EMBER_EDGE);
            cacheW = cacheH = pendingW = pendingH = -1;
            repaint();
        }

        
        private static BufferedImage toAccent(BufferedImage source, Color accent) {
            int w = source.getWidth(), h = source.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            int[] row = new int[w];
            int ar = accent.getRed(), ag = accent.getGreen(), ab = accent.getBlue();
            int peak = Math.max(1, Math.max(ar, Math.max(ag, ab)));
            for (int y = 0; y < h; y++) {
                source.getRGB(0, y, w, 1, row, 0, w);
                for (int x = 0; x < w; x++) {
                    int p = row[x], r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                    int white = (g + b) / 2;
                    int nr = Math.min(255, r * ar / peak + white);
                    int ng = Math.min(255, r * ag / peak + white);
                    int nb = Math.min(255, r * ab / peak + white);
                    row[x] = (nr << 16) | (ng << 8) | nb;
                }
                out.setRGB(0, y, w, 1, row, 0, w);
            }
            return out;
        }

        
        private static BufferedImage toBlue(BufferedImage source) {
            int w = source.getWidth(), h = source.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            int[] row = new int[w];
            for (int y = 0; y < h; y++) {
                source.getRGB(0, y, w, 1, row, 0, w);
                for (int x = 0; x < w; x++) {
                    int p = row[x], r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                    int white = (g + b) / 2;
                    int nr = Math.min(255, r * 16 / 100 + white);
                    int ng = Math.min(255, r * 46 / 100 + white);
                    int nb = Math.min(255, r + white);
                    row[x] = (nr << 16) | (ng << 8) | nb;
                }
                out.setRGB(0, y, w, 1, row, 0, w);
            }
            return out;
        }

        static Backdrop of(Component component) {
            for (Component c = component; c != null; c = c.getParent()) if (c instanceof Backdrop) return (Backdrop) c;
            return null;
        }

        
        void setFps(int fps) {
            if (fps <= 0) {
                frames.stop();
                return;
            }
            frames.setDelay(1000 / fps);
            if (!frames.isRunning()) frames.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            paintScene(g2, getWidth(), getHeight(), false);
            g2.dispose();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (drawBorder) {
                g.setColor(new Color(255, 255, 255, 26));
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        }

        Rectangle artRect(int w, int h) {
            if (art == null) return new Rectangle(w, h, 0, 0);
            int ah = (int) (h * 1.0);
            int aw = ah * art.getWidth() / art.getHeight();
            float shift = theme == Theme.KINETIC ? 0.84f : 0.98f;
            return new Rectangle(w - (int) (aw * shift), h - ah + (int) (h * 0.035), aw, ah);
        }

        private Rectangle stillRect(int w, int h, int index, double t) {
            BufferedImage reference = stills[0];
            double scale = Math.max(w * ZOOM / reference.getWidth(), h * ZOOM / reference.getHeight());
            int sw = (int) Math.ceil(reference.getWidth() * scale), sh = (int) Math.ceil(reference.getHeight() * scale);
            double rx = (sw - w) / 2.0, ry = (sh - h) / 2.0;
            double px = Math.sin(t * 2 * Math.PI / 46 + index * 2.1), py = Math.cos(t * 2 * Math.PI / 61 + index * 1.3);
            return new Rectangle((int) Math.round(-rx + rx * 0.85 * px), (int) Math.round(-ry + ry * 0.85 * py), sw, sh);
        }

        
        void paintScene(Graphics2D g, int w, int h, boolean soft) {
            ensureCache(w, h);
            double t = now();
            g.setColor(BASE);
            g.fillRect(0, 0, w, h);
            BufferedImage[] set = soft ? this.soft : sharp;
            if (set != null && set.length > 0) {
                int n = set.length;
                double period = HOLD + FADE;
                long cycle = (long) Math.floor(t / period);
                int a = (int) (cycle % n), b = (a + 1) % n;
                double local = t - cycle * period;
                float mix = local < HOLD ? 0f : (float) smoothstep((local - HOLD) / FADE);
                Rectangle ra = stillRect(w, h, a, t);
                g.drawImage(set[a], ra.x, ra.y, ra.width, ra.height, null);
                if (mix > 0.002f && n > 1) {
                    Composite old = g.getComposite();
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, mix));
                    Rectangle rb = stillRect(w, h, b, t);
                    g.drawImage(set[b], rb.x, rb.y, rb.width, rb.height, null);
                    g.setComposite(old);
                }
            }
            Rectangle ar = artRect(w, h);
            if (glow != null) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(t * 0.6));
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f + 0.15f * pulse));
                int gs = glow.getWidth();
                int cx = ar.x + ar.width * 46 / 100, cy = ar.y + ar.height * 40 / 100;
                g.drawImage(glow, cx - gs / 2, cy - gs / 2, null);
                g.setComposite(old);
            }
            if (overlay != null) g.drawImage(overlay, 0, 0, w, h, null);
            BufferedImage artImage = soft ? artSoft : artSharp;
            if (artImage != null) g.drawImage(artImage, ar.x, ar.y, ar.width, ar.height, null);
            if (!soft) {
                g.setPaint(new GradientPaint(0, h - 260, alpha(SHADE, 0), 0, h, alpha(SHADE, 210)));
                g.fillRect(0, h - 260, w, 260);
            }
            
            float dim = this.dim.get();
            if (dim > 0.004f) {
                g.setColor(alpha(SHADE, (int) (150 * dim)));
                g.fillRect(0, 0, w, h);
            }
        }

        private void paintEmbers(Graphics2D g, int w, int h, double t) {
            Composite old = g.getComposite();
            for (int i = 0; i < EMBERS; i++) {
                double life = (t * emberSpeed[i] + emberPhase[i]) % 1.0;
                float fade = (float) Math.sin(life * Math.PI);
                if (fade <= 0.02f) continue;
                float size = emberSize[i] * (0.55f + 0.45f * (float) (1 - life));
                double x = emberX[i] * w + Math.sin(t * 0.55 + i * 1.7) * 26 + life * 40;
                double y = h * (1.04 - life * 1.1);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f * fade));
                g.drawImage(ember, (int) (x - size / 2), (int) (y - size / 2), (int) size, (int) size, null);
            }
            g.setComposite(old);
        }

        private void ensureCache(int w, int h) {
            if (w <= 0 || h <= 0 || (w == cacheW && h == cacheH)) return;
            if (cacheW < 0 || frozenTime >= 0 || !isShowing()) {
                build(w, h);
            } else if (w != pendingW || h != pendingH) {
                
                pendingW = w;
                pendingH = h;
                rebuild.restart();
            }
        }

        private void build(int w, int h) {
            if (w <= 0 || h <= 0) return;
            cacheW = w;
            cacheH = h;
            if (stills.length > 0) {
                Rectangle r = stillRect(w, h, 0, 0);
                sharp = new BufferedImage[stills.length];
                soft = new BufferedImage[stills.length];
                for (int i = 0; i < stills.length; i++) {
                    sharp[i] = scale(stills[i], r.width, r.height, BufferedImage.TYPE_INT_RGB);
                    soft[i] = blur(sharp[i], 10, 2);
                }
            }
            if (art != null) {
                Rectangle ar = artRect(w, h);
                artSharp = scale(art, ar.width, ar.height, BufferedImage.TYPE_INT_ARGB_PRE);
                artSoft = blur(artSharp, 8, 2);
            }
            int gs = (int) (h * 1.3f);
            glow = radialSprite(gs, alpha(ACCENT, 120), alpha(ACCENT_DEEP, 60));

            overlay = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = overlay.createGraphics();
            
            g.setPaint(new LinearGradientPaint(0, 0, w, 0, new float[]{0f, 0.5f, 1f},
                    new Color[]{alpha(SHADE, 215), alpha(SHADE, 110), alpha(SHADE, 10)}));
            g.fillRect(0, 0, w, h);
            g.setPaint(new GradientPaint(0, 0, alpha(SHADE, 150), 0, 90, alpha(SHADE, 0)));
            g.fillRect(0, 0, w, 90);
            float radius = (float) Math.hypot(w, h) * 0.62f;
            g.setPaint(new RadialGradientPaint(w * 0.55f, h * 0.45f, radius, new float[]{0f, 0.6f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 30), new Color(0, 0, 0, 140)}));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

    
    static void paintGlass(JComponent c, Graphics g, int radius, Color tint, boolean glow) {
        int w = c.getWidth(), h = c.getHeight();
        if (w <= 0 || h <= 0) return;
        RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, radius, radius);
        Backdrop backdrop = Backdrop.of(c.getParent());
        if (backdrop != null) {
            Graphics2D bg = (Graphics2D) g.create();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            bg.clip(shape);
            Point p = SwingUtilities.convertPoint(c, 0, 0, backdrop);
            bg.translate(-p.x, -p.y);
            backdrop.paintScene(bg, backdrop.getWidth(), backdrop.getHeight(), true);
            bg.dispose();
        }
        Graphics2D g2 = smooth(g);
        g2.setColor(tint);
        g2.fill(shape);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(255, 255, 255, 20));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));
        g2.dispose();
    }

    static class GlassPanel extends JPanel {
        final int radius;
        Color tint;
        boolean glow;

        GlassPanel(LayoutManager layout, int radius, Color tint) {
            super(layout);
            this.radius = radius;
            this.tint = tint;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintGlass(this, g, radius, tint, glow);
        }
    }

    

    
    enum Glyph {
        PLUS, FOLDER, REFRESH, LOGOUT, COPY, TRASH, FILE, MIN, MAX, RESTORE, CLOSE, PLAY, STOP, USER, CHECK;

        void paint(Graphics2D g, float x, float y, float size, Color color) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.scale(size / 16f, size / 16f);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(this == MIN || this == MAX || this == RESTORE || this == CLOSE ? 1.1f * 16f / size : 1.5f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            switch (this) {
                case PLUS:
                    g2.draw(new Line2D.Float(8, 3, 8, 13));
                    g2.draw(new Line2D.Float(3, 8, 13, 8));
                    break;
                case FOLDER:
                    p.moveTo(2, 4.5);
                    p.lineTo(6, 4.5);
                    p.lineTo(7.5, 6);
                    p.lineTo(14, 6);
                    p.lineTo(14, 13);
                    p.lineTo(2, 13);
                    p.closePath();
                    g2.draw(p);
                    break;
                case REFRESH:
                    g2.draw(new Arc2D.Float(3, 3, 10, 10, 70, 280, Arc2D.OPEN));
                    p.moveTo(9.2, 1.9);
                    p.lineTo(11.5, 3.4);
                    p.lineTo(9.6, 5.5);
                    g2.draw(p);
                    break;
                case LOGOUT:
                    p.moveTo(9, 3);
                    p.lineTo(3, 3);
                    p.lineTo(3, 13);
                    p.lineTo(9, 13);
                    g2.draw(p);
                    g2.draw(new Line2D.Float(7, 8, 14, 8));
                    Path2D head = new Path2D.Float();
                    head.moveTo(11.5, 5.5);
                    head.lineTo(14, 8);
                    head.lineTo(11.5, 10.5);
                    g2.draw(head);
                    break;
                case COPY:
                    g2.draw(new RoundRectangle2D.Float(5.5f, 5.5f, 8, 8.5f, 2.5f, 2.5f));
                    p.moveTo(3, 11);
                    p.lineTo(3, 3.5);
                    p.quadTo(3, 2.5, 4, 2.5);
                    p.lineTo(10, 2.5);
                    g2.draw(p);
                    break;
                case TRASH:
                    g2.draw(new Line2D.Float(2.5f, 4.5f, 13.5f, 4.5f));
                    p.moveTo(6.3, 4.5);
                    p.lineTo(6.3, 2.6);
                    p.lineTo(9.7, 2.6);
                    p.lineTo(9.7, 4.5);
                    p.moveTo(4.2, 4.5);
                    p.lineTo(5, 13.6);
                    p.lineTo(11, 13.6);
                    p.lineTo(11.8, 4.5);
                    g2.draw(p);
                    break;
                case FILE:
                    p.moveTo(4, 2);
                    p.lineTo(10, 2);
                    p.lineTo(13, 5);
                    p.lineTo(13, 14);
                    p.lineTo(4, 14);
                    p.closePath();
                    p.moveTo(10, 2);
                    p.lineTo(10, 5);
                    p.lineTo(13, 5);
                    g2.draw(p);
                    break;
                case MIN:
                    g2.draw(new Line2D.Float(3, 8.5f, 13, 8.5f));
                    break;
                case MAX:
                    g2.draw(new java.awt.geom.Rectangle2D.Float(3.5f, 3.5f, 9, 9));
                    break;
                case RESTORE:
                    g2.draw(new java.awt.geom.Rectangle2D.Float(3.5f, 5.5f, 7, 7));
                    p.moveTo(5.5, 5.5);
                    p.lineTo(5.5, 3.5);
                    p.lineTo(12.5, 3.5);
                    p.lineTo(12.5, 10.5);
                    p.lineTo(10.5, 10.5);
                    g2.draw(p);
                    break;
                case CLOSE:
                    g2.draw(new Line2D.Float(3.5f, 3.5f, 12.5f, 12.5f));
                    g2.draw(new Line2D.Float(12.5f, 3.5f, 3.5f, 12.5f));
                    break;
                case PLAY:
                    p.moveTo(4.5, 2.5);
                    p.lineTo(13.5, 8);
                    p.lineTo(4.5, 13.5);
                    p.closePath();
                    g2.fill(p);
                    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(p);
                    break;
                case STOP:
                    g2.fill(new RoundRectangle2D.Float(3.5f, 3.5f, 9, 9, 3, 3));
                    break;
                case USER:
                    g2.draw(new Ellipse2D.Float(5.2f, 2.2f, 5.6f, 5.6f));
                    g2.draw(new Arc2D.Float(2.5f, 9.5f, 11, 9, 0, 180, Arc2D.OPEN));
                    break;
                case CHECK:
                    p.moveTo(3.5, 8.5);
                    p.lineTo(6.5, 11.5);
                    p.lineTo(12.5, 4.5);
                    g2.draw(p);
                    break;
                default:
                    break;
            }
            g2.dispose();
        }
    }

    

    enum Kind { PRIMARY, GHOST, LINK, DANGER, WINDOW, CLOSE }

    static class Btn extends JButton {
        final Kind kind;
        Glyph glyph;
        int radius = 10;
        final Fader hover = new Fader(this, 0.2f, 0), press = new Fader(this, 0.35f, 0);

        Btn(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(kind == Kind.PRIMARY || kind == Kind.DANGER ? semibold(13f) : medium(12.5f));
            setBorder(kind == Kind.LINK ? new EmptyBorder(6, 8, 6, 8) : new EmptyBorder(8, 14, 8, 14));
            setCursor(HAND);
            getModel().addChangeListener(e -> {
                ButtonModel model = getModel();
                hover.to(model.isRollover() && isEnabled() ? 1f : 0f);
                press.to(model.isPressed() && model.isArmed() ? 1f : 0f);
            });
        }

        Btn glyph(Glyph glyph) {
            this.glyph = glyph;
            revalidate();
            repaint();
            return this;
        }

        @Override
        public void updateUI() {
            setUI(new BasicButtonUI());
        }

        @Override
        public Dimension getPreferredSize() {
            if (isPreferredSizeSet()) return super.getPreferredSize();
            FontMetrics fm = getFontMetrics(getFont());
            Insets in = getInsets();
            String text = getText() == null ? "" : getText();
            int w = fm.stringWidth(text) + in.left + in.right;
            if (glyph != null) w += 15 + (text.isEmpty() ? 0 : 8);
            return new Dimension(w, Math.max(fm.getHeight(), 16) + in.top + in.bottom);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            if (!isEnabled()) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
            float hv = isEnabled() ? hover.get() : 0f, pr = press.get();
            int w = getWidth(), h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, radius, radius);
            RoundRectangle2D rim = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius);
            Color fg;
            switch (kind) {
                case PRIMARY:
                case DANGER: {
                    Color base = kind == Kind.PRIMARY ? ACCENT : new Color(206, 58, 58);
                    g2.setColor(mix(base, Color.WHITE, 0.12f * hv));
                    g2.fill(shape);
                    if (pr > 0) {
                        g2.setColor(new Color(0, 0, 0, (int) (50 * pr)));
                        g2.fill(shape);
                    }
                    fg = onAccent(base);
                    break;
                }
                case GHOST:
                    g2.setColor(new Color(255, 255, 255, (int) (12 + 12 * hv - 5 * pr)));
                    g2.fill(shape);
                    g2.setColor(new Color(255, 255, 255, (int) (22 + 22 * hv)));
                    g2.draw(rim);
                    fg = mix(mix(TEXT, MUTED, 0.15f), TEXT, hv);
                    break;
                case LINK:
                    if (hv > 0) {
                        g2.setColor(new Color(255, 255, 255, (int) (12 * hv)));
                        g2.fill(shape);
                    }
                    fg = mix(MUTED, TEXT, hv);
                    break;
                case CLOSE:
                    if (hv > 0) {
                        g2.setColor(new Color(232, 17, 35, (int) (235 * hv)));
                        g2.fill(shape);
                    }
                    fg = mix(MUTED, Color.WHITE, hv);
                    break;
                case WINDOW:
                default:
                    if (hv > 0) {
                        g2.setColor(new Color(255, 255, 255, (int) (24 * hv)));
                        g2.fill(shape);
                    }
                    fg = mix(MUTED, TEXT, hv);
                    break;
            }
            paintLabel(g2, fg, w, h);
            g2.dispose();
        }

        void paintLabel(Graphics2D g2, Color fg, int w, int h) {
            String text = getText() == null ? "" : getText();
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            float glyphSize = kind == Kind.WINDOW || kind == Kind.CLOSE ? 12 : 15;
            float total = fm.stringWidth(text) + (glyph != null ? glyphSize + (text.isEmpty() ? 0 : 8) : 0);
            float x = (w - total) / 2f;
            if (glyph != null) {
                glyph.paint(g2, x, (h - glyphSize) / 2f, glyphSize, fg);
                x += glyphSize + 8;
            }
            g2.setColor(fg);
            g2.drawString(text, x, (h - fm.getHeight()) / 2f + fm.getAscent());
        }
    }

    
    static Color onAccent(Color accent) {
        float luma = (0.2126f * accent.getRed() + 0.7152f * accent.getGreen() + 0.0722f * accent.getBlue()) / 255f;
        return luma > 0.66f ? new Color(14, 16, 22) : Color.WHITE;
    }

    
    static final class PlayButton extends Btn {
        private boolean running;

        PlayButton() {
            super("PLAY", Kind.PRIMARY);
            radius = 14;
            setFont(tracked(display(16f), 0.16f));
        }

        void setRunning(boolean running) {
            if (this.running == running) return;
            this.running = running;
            setText(running ? "STOP" : "PLAY");
            setToolTipText(running ? "Stop the running game" : null);
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(236, 58);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            float hv = isEnabled() ? hover.get() : 0f, pr = press.get();
            RoundRectangle2D body = new RoundRectangle2D.Float(0, 0, w, h, radius, radius);
            boolean live = isEnabled() && !running;
            Color fg;
            if (live) {
                g2.setColor(mix(ACCENT, Color.WHITE, 0.1f * hv));
                g2.fill(body);
                
                Graphics2D top = (Graphics2D) g2.create();
                top.clip(body);
                top.setColor(new Color(255, 255, 255, 46));
                top.fillRect(0, 0, w, 1);
                top.dispose();
                if (pr > 0) {
                    g2.setColor(new Color(0, 0, 0, (int) (55 * pr)));
                    g2.fill(body);
                }
                fg = onAccent(ACCENT);
            } else if (running) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(now() * 2.4));
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fill(body);
                g2.setColor(new Color(255, 255, 255, (int) (8 + 12 * hv)));
                g2.fill(body);
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(alpha(ACCENT_LIGHT, (int) (130 + 70 * pulse)));
                g2.draw(new RoundRectangle2D.Float(0.75f, 0.75f, w - 1.5f, h - 1.5f, radius, radius));
                fg = TEXT;
            } else {
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fill(body);
                g2.setColor(new Color(255, 255, 255, 24));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));
                fg = DIM;
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            float icon = 13, gap = 12;
            float total = icon + gap + fm.stringWidth(text);
            float x = (w - total) / 2f;
            (running ? Glyph.STOP : Glyph.PLAY).paint(g2, x, (h - icon) / 2f, icon, fg);
            g2.setColor(fg);
            g2.drawString(text, x + icon + gap, (h - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    

    static final class RoundField extends JTextField {
        String placeholder;
        private final Fader focus = new Fader(this, 0.25f, 0), hover = new Fader(this, 0.25f, 0);

        RoundField(String text) {
            super(text);
            setOpaque(false);
            setForeground(TEXT);
            setDisabledTextColor(MUTED);
            setCaretColor(ACCENT_LIGHT);
            setSelectionColor(SELECTION);
            setSelectedTextColor(Color.WHITE);
            setFont(font(Font.PLAIN, 13f));
            setBorder(new EmptyBorder(9, 12, 9, 12));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focus.to(1f);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focus.to(0f);
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover.to(1f);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover.to(0f);
                }
            });
        }

        @Override
        public void updateUI() {
            setUI(new BasicTextFieldUI());
        }

        @Override
        public Color getSelectionColor() {
            return SELECTION;
        }

        @Override
        public Color getCaretColor() {
            return ACCENT_LIGHT;
        }

        @Override
        public Color getForeground() {
            return TEXT;
        }

        @Override
        public Color getDisabledTextColor() {
            return MUTED;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float f = focus.get(), hv = hover.get();
            int w = getWidth(), h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10);
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fill(shape);
            g2.setColor(new Color(255, 255, 255, (int) (6 + 5 * hv + 4 * f)));
            g2.fill(shape);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(mix(new Color(255, 255, 255, (int) (22 + 18 * hv)), alpha(ACCENT_LIGHT, 210), f));
            g2.draw(shape);
            if (placeholder != null && getText().isEmpty()) {
                Insets in = getInsets();
                g2.setFont(getFont());
                g2.setColor(DIM);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, in.left, (h - fm.getHeight()) / 2f + fm.getAscent());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static final class Toggle extends JComponent {
        private boolean selected;
        private final Fader knob, hover = new Fader(this, 0.25f, 0);

        Toggle(boolean selected) {
            this.selected = selected;
            knob = new Fader(this, 0.22f, selected ? 1f : 0f);
            setPreferredSize(new Dimension(40, 22));
            setMaximumSize(new Dimension(40, 22));
            setCursor(HAND);
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) setSelected(!Toggle.this.selected);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover.to(1f);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover.to(0f);
                }
            });
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle");
            getActionMap().put("toggle", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setSelected(!Toggle.this.selected);
                }
            });
        }

        boolean isSelected() {
            return selected;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            knob.to(selected ? 1f : 0f);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float k = knob.get(), hv = hover.get();
            int w = 40, h = 22, y = (getHeight() - h) / 2;
            RoundRectangle2D track = new RoundRectangle2D.Float(0, y, w, h, h, h);
            g2.setColor(new Color(255, 255, 255, (int) (20 + 10 * hv)));
            g2.fill(track);
            if (k > 0) {
                g2.setColor(alpha(ACCENT, (int) (255 * k)));
                g2.fill(track);
            }
            int size = h - 6;
            float kx = 3 + (w - size - 6) * k;
            g2.setColor(mix(mix(MUTED, Color.WHITE, 0.4f), Color.WHITE, k));
            g2.fill(new Ellipse2D.Float(kx, y + 3, size, size));
            g2.dispose();
        }
    }

    
    static final class Segmented extends JComponent {
        private final String[] labels;
        private final boolean[] dots;
        private final IntConsumer onSelect;
        private final Fader position;
        private int selected, hoverIndex = -1;

        Segmented(String[] labels, IntConsumer onSelect) {
            this.labels = labels;
            this.dots = new boolean[labels.length];
            this.onSelect = onSelect;
            this.position = new Fader(this, 0.2f, 0);
            setFont(medium(12.5f));
            setCursor(HAND);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int index = indexAt(e.getX());
                    if (index != hoverIndex) {
                        hoverIndex = index;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverIndex = -1;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e) || !contains(e.getPoint())) return;
                    int index = indexAt(e.getX());
                    if (index >= 0) Segmented.this.onSelect.accept(index);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private int segmentWidth() {
            FontMetrics fm = getFontMetrics(getFont());
            int max = 0;
            for (String label : labels) max = Math.max(max, fm.stringWidth(label));
            return max + 48;
        }

        private int indexAt(int x) {
            int index = (x - 4) / segmentWidth();
            return index >= 0 && index < labels.length ? index : -1;
        }

        void select(int index) {
            selected = index;
            dots[index] = false;
            position.to(index);
            repaint();
        }

        boolean isSelected(int index) {
            return selected == index;
        }

        void setDot(int index, boolean dot) {
            if (dots[index] == dot) return;
            dots[index] = dot;
            repaint();
        }

        boolean hasDot(int index) {
            return dots[index];
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(segmentWidth() * labels.length + 8, 34);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight(), seg = segmentWidth();
            RoundRectangle2D track = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h);
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fill(track);
            g2.setColor(new Color(255, 255, 255, 20));
            g2.draw(track);
            float ix = 4 + position.get() * seg;
            RoundRectangle2D indicator = new RoundRectangle2D.Float(ix, 4, seg, h - 8, h - 8, h - 8);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fill(indicator);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < labels.length; i++) {
                float closeness = 1f - Math.min(1f, Math.abs(position.get() - i));
                Color color = mix(i == hoverIndex ? TEXT : MUTED, Color.WHITE, closeness);
                g2.setColor(color);
                int x = 4 + i * seg;
                int tw = fm.stringWidth(labels[i]);
                float tx = x + (seg - tw) / 2f;
                g2.drawString(labels[i], tx, (h - fm.getHeight()) / 2f + fm.getAscent());
                if (dots[i]) {
                    g2.setColor(ACCENT_LIGHT);
                    g2.fill(new Ellipse2D.Float(tx + tw + 5, h / 2f - 7, 6, 6));
                }
            }
            g2.dispose();
        }
    }

    



    static final class TextTabs extends JComponent {
        private static final int GAP = 26;
        private final String[] labels;
        private final boolean[] dots;
        private final IntConsumer onSelect;
        private final Fader position;
        private int selected, hoverIndex = -1;

        TextTabs(String[] labels, IntConsumer onSelect) {
            this.labels = labels;
            this.dots = new boolean[labels.length];
            this.onSelect = onSelect;
            this.position = new Fader(this, 0.2f, 0);
            setFont(medium(13.5f));
            setCursor(HAND);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int index = indexAt(e.getX());
                    if (index != hoverIndex) {
                        hoverIndex = index;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverIndex = -1;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e) || !contains(e.getPoint())) return;
                    int index = indexAt(e.getX());
                    if (index >= 0) TextTabs.this.onSelect.accept(index);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private int x(int index) {
            FontMetrics fm = getFontMetrics(getFont());
            int x = 0;
            for (int i = 0; i < index; i++) x += fm.stringWidth(labels[i]) + GAP;
            return x;
        }

        private int width(int index) {
            return getFontMetrics(getFont()).stringWidth(labels[index]);
        }

        private int indexAt(int px) {
            for (int i = 0; i < labels.length; i++) {
                int x = x(i);
                if (px >= x - GAP / 2 && px < x + width(i) + GAP / 2) return i;
            }
            return -1;
        }

        void select(int index) {
            selected = index;
            dots[index] = false;
            position.to(index);
            repaint();
        }

        boolean isSelected(int index) {
            return selected == index;
        }

        void setDot(int index, boolean dot) {
            if (dots[index] == dot) return;
            dots[index] = dot;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(x(labels.length - 1) + width(labels.length - 1) + 10, 40);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int h = getHeight();
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            float baseline = (h - fm.getHeight()) / 2f + fm.getAscent() - 1;
            for (int i = 0; i < labels.length; i++) {
                float closeness = 1f - Math.min(1f, Math.abs(position.get() - i));
                g2.setColor(mix(i == hoverIndex ? mix(MUTED, TEXT, 0.6f) : mix(MUTED, DIM, 0.35f), TEXT, closeness));
                int x = x(i);
                g2.drawString(labels[i], x, baseline);
                if (dots[i]) {
                    g2.setColor(ACCENT_LIGHT);
                    g2.fill(new Ellipse2D.Float(x + width(i) + 4, baseline - fm.getAscent() + 1, 5, 5));
                }
            }
            
            float p = position.get();
            int a = (int) Math.floor(p), b = Math.min(labels.length - 1, a + 1);
            float f = p - a;
            float bx = x(a) + (x(b) - x(a)) * f, bw = width(a) + (width(b) - width(a)) * f;
            g2.setColor(ACCENT);
            g2.fill(new RoundRectangle2D.Float(bx, baseline + 9, bw, 2, 2, 2));
            g2.dispose();
        }
    }

    



    static final class Intro extends JComponent {
        static final double TOTAL = 3.0, OUT = 2.3;
        private final String greeting, caption;
        private final Runnable onDone;
        private final Timer timer;
        private double start = -1, skipAt = -1;
        private boolean done;

        Intro(String greeting, String caption, Runnable onDone) {
            this.greeting = greeting;
            this.caption = caption;
            this.onDone = onDone;
            setOpaque(false);
            timer = new Timer(15, e -> {
                repaint();
                if (elapsed() >= end()) finish();
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    skip();
                }
            });
        }

        void start() {
            start = now();
            setVisible(true);
            timer.start();
        }

        void skip() {
            if (skipAt < 0 && start >= 0) skipAt = Math.min(elapsed(), OUT);
        }

        boolean running() {
            return start >= 0 && !done;
        }

        private double elapsed() {
            return now() - start;
        }

        private double end() {
            return skipAt < 0 ? TOTAL : skipAt + 0.4;
        }

        private void finish() {
            if (done) return;
            done = true;
            timer.stop();
            setVisible(false);
            if (onDone != null) onDone.run();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (start < 0) return;
            Graphics2D g2 = smooth(g);
            paintAt(g2, getWidth(), getHeight(), elapsed(), skipAt < 0 ? OUT : skipAt, end());
            g2.dispose();
        }

        
        void paintAt(Graphics2D g2, int w, int h, double t, double outStart, double outEnd) {
            float out = (float) smoothstep((t - outStart) / Math.max(0.01, outEnd - outStart));
            float alpha = 1f - out;
            if (alpha <= 0f) return;
            Composite base = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(BASE);
            g2.fillRect(0, 0, w, h);
            float glowIn = (float) smoothstep(t / 1.4);
            float cx = w / 2f, cy = h / 2f - 16;
            float r = Math.max(w, h) * 0.55f;
            g2.setPaint(new RadialGradientPaint(cx, cy, r, new float[]{0f, 1f},
                    new Color[]{alpha(ACCENT_DEEP, (int) (70 * glowIn)), alpha(ACCENT_DEEP, 0)}));
            g2.fillRect(0, 0, w, h);

            
            Graphics2D mark = (Graphics2D) g2.create();
            float scale = 1f + 0.05f * out;
            mark.translate(cx, cy - 14 * out);
            mark.scale(scale, scale);
            Font font = wordmark(Math.min(118f, w * 0.1f));
            mark.setFont(font);
            FontMetrics fm = mark.getFontMetrics();
            String word = "KINETIC";
            float tracking = font.getSize2D() * 0.02f;
            float total = 0;
            for (int i = 0; i < word.length(); i++) total += fm.charWidth(word.charAt(i)) + (i < word.length() - 1 ? tracking : 0);
            float x = -total / 2f;
            float baseline = fm.getAscent() / 2f - fm.getDescent() / 2f;
            for (int i = 0; i < word.length(); i++) {
                float a = (float) smoothstep((t - 0.25 - i * 0.1) / 0.55);
                if (a > 0f) {
                    mark.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * a));
                    mark.setColor(TEXT);
                    mark.drawString(String.valueOf(word.charAt(i)), x, baseline + (1f - a) * 22f);
                }
                x += fm.charWidth(word.charAt(i)) + tracking;
            }
            mark.dispose();

            
            float lineW = 200f * (float) smoothstep((t - 0.75) / 0.7);
            float lineY = cy + font.getSize2D() * 0.52f;
            if (lineW > 1f) {
                g2.setColor(ACCENT);
                g2.fill(new RoundRectangle2D.Float(cx - lineW / 2f, lineY, lineW, 2f, 2f, 2f));
            }

            float greet = (float) smoothstep((t - 1.05) / 0.55);
            if (greet > 0f && greeting != null) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * greet));
                g2.setFont(display(22f));
                FontMetrics gm = g2.getFontMetrics();
                g2.setColor(TEXT);
                g2.drawString(greeting, cx - gm.stringWidth(greeting) / 2f, lineY + 30 + gm.getAscent() - (1f - greet) * -8f);
            }
            float cap = (float) smoothstep((t - 1.35) / 0.55);
            if (cap > 0f && caption != null) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * cap));
                g2.setFont(tracked(wide(10f), 0.16f));
                FontMetrics cm = g2.getFontMetrics();
                g2.setColor(DIM);
                g2.drawString(caption, cx - cm.stringWidth(caption) / 2f, lineY + 70 + cm.getAscent());
            }
            if (israel() || christian()) {
                float star = (float) smoothstep((t - 0.6) / 0.6);
                if (star > 0f) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * star));
                    if (christian()) paintCross(g2, cx, cy - font.getSize2D() * 0.75f, 14f, 1.8f, TEXT);
                    else paintHexagram(g2, cx, cy - font.getSize2D() * 0.75f, 14f, 1.8f, TEXT);
                }
            }
            g2.setComposite(base);
        }
    }

    
    static final class StatusLabel extends JComponent {
        private String text = " ";
        private Color color = MUTED, dot = MUTED;
        private double progress = Double.NaN;

        StatusLabel() {
            setFont(font(Font.PLAIN, 13f));
        }

        void set(String text, Color color, Color dot) {
            this.text = text;
            this.color = color;
            this.dot = dot;
            setToolTipText(text);
            repaint();
        }

        
        void setProgress(double progress) {
            this.progress = progress;
            repaint();
        }

        void hideProgress() {
            progress = Double.NaN;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 50);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            boolean bar = !Double.isNaN(progress);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textY = bar ? h / 2 - 8 : h / 2;
            String shown = ellipsize(text, fm, Math.max(10, w - 22 - (bar && progress >= 0 ? 52 : 0)));
            g2.setColor(dot);
            g2.fill(new Ellipse2D.Float(0, textY - 3, 6, 6));
            g2.setColor(color);
            g2.drawString(shown, 16, textY - fm.getHeight() / 2f + fm.getAscent());
            if (bar) {
                float y = h / 2f + 8, bh = 3;
                RoundRectangle2D track = new RoundRectangle2D.Float(0, y, w, bh, bh, bh);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fill(track);
                float from, to;
                if (progress < 0) {
                    double cycle = (now() * 0.6) % 1.0;
                    from = (float) ((cycle * 1.4 - 0.3) * w);
                    to = from + w * 0.3f;
                } else {
                    from = 0;
                    to = (float) (w * Math.min(1, progress));
                }
                from = Math.max(0, from);
                to = Math.min(w, to);
                if (to - from > 1) {
                    Graphics2D fill = (Graphics2D) g2.create();
                    fill.clip(track);
                    RoundRectangle2D part = new RoundRectangle2D.Float(from, y, Math.max(bh, to - from), bh, bh, bh);
                    fill.setColor(ACCENT);
                    fill.fill(part);
                    fill.dispose();
                }
                if (progress >= 0) {
                    String percent = Math.round(Math.min(1, progress) * 100) + " %";
                    g2.setFont(medium(11.5f));
                    FontMetrics pm = g2.getFontMetrics();
                    g2.setColor(MUTED);
                    g2.drawString(percent, w - pm.stringWidth(percent), textY - pm.getHeight() / 2f + pm.getAscent());
                }
            }
            g2.dispose();
        }
    }

    static void styleSlider(JSlider slider) {
        slider.setUI(new BasicSliderUI(slider) {
            @Override
            protected Dimension getThumbSize() {
                return new Dimension(16, 16);
            }

            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = smooth(g);
                int cy = trackRect.y + trackRect.height / 2;
                g2.setColor(new Color(255, 255, 255, 26));
                g2.fill(new RoundRectangle2D.Float(trackRect.x, cy - 2, trackRect.width, 4, 4, 4));
                int filled = thumbRect.x + thumbRect.width / 2 - trackRect.x;
                g2.setColor(ACCENT);
                g2.fill(new RoundRectangle2D.Float(trackRect.x, cy - 2, Math.max(4, filled), 4, 4, 4));
                g2.dispose();
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = smooth(g);
                int size = 14;
                int x = thumbRect.x + (thumbRect.width - size) / 2;
                int y = trackRect.y + trackRect.height / 2 - size / 2;
                g2.setColor(new Color(0, 0, 0, 70));
                g2.fill(new Ellipse2D.Float(x, y + 1, size, size));
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Float(x, y, size, size));
                g2.dispose();
            }

            @Override
            public void paintFocus(Graphics g) {
            }
        });
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);
        slider.setSnapToTicks(true);
        slider.setMajorTickSpacing(1);
        slider.setCursor(HAND);
    }

    static JScrollPane glassScroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar bar = scroll.getVerticalScrollBar();
        bar.setUI(new GlassScrollBarUI());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 0));
        bar.setUnitIncrement(16);
        return scroll;
    }

    private static final class GlassScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(255, 255, 255, 40);
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private static JButton zeroButton() {
            JButton button = new JButton();
            Dimension zero = new Dimension(0, 0);
            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = smooth(g);
            g2.setColor(isThumbRollover() || isDragging ? alpha(ACCENT_LIGHT, 150) : thumbColor);
            g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            g2.dispose();
        }
    }

    

    



    static final class Chrome {
        private static final int EDGE = 6;
        private final JFrame frame;
        private Rectangle normalBounds;
        private boolean maximized;
        private int hoverDir, resizeDir;
        private Point pressScreen;
        private Rectangle pressBounds;
        private Runnable onStateChange;

        Chrome(JFrame frame) {
            this.frame = frame;
            Toolkit.getDefaultToolkit().addAWTEventListener(this::onEvent, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        void onStateChange(Runnable runnable) {
            onStateChange = runnable;
        }

        boolean isMaximized() {
            return maximized;
        }

        boolean isResizing() {
            return resizeDir != 0;
        }

        void minimize() {
            frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED);
        }

        void toggleMaximize() {
            if (maximized) {
                maximized = false;
                if (normalBounds != null) frame.setBounds(normalBounds);
            } else {
                normalBounds = frame.getBounds();
                maximized = true;
                frame.setBounds(usableBounds());
            }
            frame.validate();
            if (onStateChange != null) onStateChange.run();
        }

        private Rectangle usableBounds() {
            GraphicsConfiguration gc = frame.getGraphicsConfiguration();
            Rectangle b = gc.getBounds();
            Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            return new Rectangle(b.x + in.left, b.y + in.top, b.width - in.left - in.right, b.height - in.top - in.bottom);
        }

        
        void makeDraggable(JComponent bar) {
            MouseAdapter drag = new MouseAdapter() {
                private Point grab;

                @Override
                public void mousePressed(MouseEvent e) {
                    grab = isResizing() || !SwingUtilities.isLeftMouseButton(e) ? null : e.getLocationOnScreen();
                    if (grab != null) pressBounds = frame.getBounds();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (grab == null || isResizing()) return;
                    Point now = e.getLocationOnScreen();
                    if (maximized) {
                        
                        double rel = (grab.x - pressBounds.x) / (double) Math.max(1, pressBounds.width);
                        maximized = false;
                        Rectangle normal = normalBounds != null ? normalBounds : new Rectangle(0, 0, 1180, 700);
                        int x = now.x - (int) (normal.width * rel);
                        int y = now.y - (grab.y - pressBounds.y);
                        frame.setBounds(x, y, normal.width, normal.height);
                        pressBounds = frame.getBounds();
                        grab = now;
                        if (onStateChange != null) onStateChange.run();
                        return;
                    }
                    frame.setLocation(pressBounds.x + now.x - grab.x, pressBounds.y + now.y - grab.y);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) toggleMaximize();
                }
            };
            bar.addMouseListener(drag);
            bar.addMouseMotionListener(drag);
        }

        private int direction(Point p) {
            int w = frame.getWidth(), h = frame.getHeight(), dir = 0;
            if (p.x < EDGE) dir |= 1;
            if (p.x >= w - EDGE) dir |= 2;
            if (p.y < EDGE) dir |= 4;
            if (p.y >= h - EDGE) dir |= 8;
            return dir;
        }

        private static Cursor cursorFor(int dir) {
            switch (dir) {
                case 1: return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
                case 2: return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                case 4: return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                case 8: return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                case 5: return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                case 6: return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                case 9: return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                case 10: return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                default: return null;
            }
        }

        private void onEvent(AWTEvent event) {
            if (!(event instanceof MouseEvent)) return;
            MouseEvent e = (MouseEvent) event;
            Component source = e.getComponent();
            if (source == null || (source != frame && SwingUtilities.getWindowAncestor(source) != frame)) return;
            switch (e.getID()) {
                case MouseEvent.MOUSE_MOVED: {
                    int dir = maximized ? 0 : direction(SwingUtilities.convertPoint(source, e.getPoint(), frame.getRootPane()));
                    if (dir != hoverDir) {
                        hoverDir = dir;
                        frame.getRootPane().setCursor(cursorFor(dir));
                    }
                    break;
                }
                case MouseEvent.MOUSE_EXITED:
                    if (resizeDir == 0 && hoverDir != 0 && source == frame.getRootPane()) {
                        hoverDir = 0;
                        frame.getRootPane().setCursor(null);
                    }
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    if (hoverDir != 0 && !maximized && SwingUtilities.isLeftMouseButton(e)) {
                        resizeDir = hoverDir;
                        pressScreen = e.getLocationOnScreen();
                        pressBounds = frame.getBounds();
                    }
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    if (resizeDir != 0) resize(e.getLocationOnScreen());
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if (resizeDir != 0) {
                        resizeDir = 0;
                        hoverDir = 0;
                        frame.getRootPane().setCursor(null);
                    }
                    break;
                default:
                    break;
            }
        }

        private void resize(Point now) {
            int dx = now.x - pressScreen.x, dy = now.y - pressScreen.y;
            Dimension min = frame.getMinimumSize();
            Rectangle b = new Rectangle(pressBounds);
            if ((resizeDir & 2) != 0) b.width = Math.max(min.width, pressBounds.width + dx);
            if ((resizeDir & 8) != 0) b.height = Math.max(min.height, pressBounds.height + dy);
            if ((resizeDir & 1) != 0) {
                b.width = Math.max(min.width, pressBounds.width - dx);
                b.x = pressBounds.x + pressBounds.width - b.width;
            }
            if ((resizeDir & 4) != 0) {
                b.height = Math.max(min.height, pressBounds.height - dy);
                b.y = pressBounds.y + pressBounds.height - b.height;
            }
            frame.setBounds(b);
            frame.validate();
        }
    }
}
