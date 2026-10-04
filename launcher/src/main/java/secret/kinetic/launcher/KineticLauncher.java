package secret.kinetic.launcher;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicTextAreaUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static secret.kinetic.launcher.Ui.*;

public final class KineticLauncher {

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean WINDOWS = OS_NAME.contains("win");
    private static final boolean MAC = OS_NAME.contains("mac");

    private static final String KINETIC = "Kinetic";
    private static final String DEFAULT_MAIN_CLASS = "net.minecraft.client.main.Main";

    private final File launcherDir = locateLauncherDir();
    
    private final File localDir = locateLocalDir(launcherDir);
    private final boolean local = localDir != null;
    
    private final File dataDir = locateDataDir();
    private final File baseDir = local ? localDir : dataDir;
    private final Properties settings = new Properties();
    private final List<Client> clients = new ArrayList<>();

    private JFrame frame;
    private Ui.Chrome chrome;
    private Backdrop backdrop;
    private JPanel titleBar;
    private Btn maximizeButton;
    private Client selected;
    private JPanel clientList;
    private HeroPanel hero;
    private JTextField gameDirField, javaField;
    private JSlider ramSlider;
    private JLabel ramLabel;
    private StatusLabel statusLabel;
    private Toggle closeAfterLaunch;
    private PlayButton playButton;
    private JTextArea log;
    private CardLayout cardLayout;
    private JPanel cards;
    private TextTabs tabs;
    private ClientSwitcher switcher;
    private JPopupMenu clientPopup;
    private Btn ramChip;
    private volatile Process game;
    private volatile Client runningClient;
    private volatile boolean stopRequested;
    private volatile boolean verifying;
    private JLabel filesLabel;
    private Btn verifyButton;
    private JLabel brandIcon;
    private JComponent brandStar;
    private Segmented themeSwitch;
    
    private final List<String> startupNotes = new ArrayList<>();

    
    static final String WM_CLASS = "kinetic-client";

    private KineticLauncher() {
    }

    public static void main(String[] args) {
        
        System.setProperty("awt.useSystemAAFontSettings", WINDOWS ? "lcd" : "on");
        System.setProperty("swing.aatext", "true");
        setX11AppClassName(WM_CLASS);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("ToolTip.font", font(Font.PLAIN, 12f));
        SwingUtilities.invokeLater(() -> new KineticLauncher().show());
    }

    

    private void prepare() {
        if (!baseDir.isDirectory()) baseDir.mkdirs();
        loadSettings();
        loadClients();
        sanitizePaths();
        String saved = settings.getProperty("theme");
        Ui.customAccent = parseColor(settings.getProperty("theme.accent", ""));
        Ui.customAccentDeep = parseColor(settings.getProperty("theme.accent2", ""));
        Ui.applyTheme(saved != null ? Theme.parse(saved) : detectClientTheme());
        applyTooltipColors();
    }

    
    private static Color parseColor(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (!v.matches("#[0-9a-fA-F]{6}")) return null;
        return new Color(Integer.parseInt(v.substring(1), 16));
    }

    

    private long clientThemeStamp = -1L;

    
    private File clientThemeFile() {
        return new File(dataDir, "theme.properties");
    }

    

    private void watchClientTheme() {
        clientThemeStamp = clientThemeFile().lastModified();
        javax.swing.Timer timer = new javax.swing.Timer(1500, e -> {
            File file = clientThemeFile();
            long stamp = file.lastModified();
            if (stamp == 0L || stamp == clientThemeStamp) return;
            clientThemeStamp = stamp;
            Properties theme = new Properties();
            try (InputStream in = new FileInputStream(file)) {
                theme.load(in);
            } catch (IOException ex) {
                return;
            }
            String preset = theme.getProperty("preset", "MARIN").trim();
            if ("ISRAEL".equalsIgnoreCase(preset)) {
                applyLook(Theme.ISRAEL, null, null);
            } else if ("CHRISTIAN".equalsIgnoreCase(preset)) {
                applyLook(Theme.CHRISTIAN, null, null);
            } else {
                Color accent = parseColor(theme.getProperty("accent"));
                boolean plain = accent == null || "MARIN".equalsIgnoreCase(preset);
                applyLook(Theme.KINETIC, plain ? null : accent, plain ? null : parseColor(theme.getProperty("accent2")));
            }
            appendLog("[Kinetic] Theme from the client: " + preset);
        });
        timer.start();
    }

    
    private void applyLook(Theme next, Color accent, Color deep) {
        boolean sameAccent = accent == null ? Ui.customAccent == null : accent.equals(Ui.customAccent);
        if (next == Ui.theme && sameAccent) return;
        Color[] before = {TEXT, MUTED, DIM, ACCENT, ACCENT_LIGHT, DANGER};
        Ui.customAccent = accent;
        Ui.customAccentDeep = deep;
        Ui.applyTheme(next);
        Color[] after = {TEXT, MUTED, DIM, ACCENT, ACCENT_LIGHT, DANGER};
        applyTooltipColors();
        recolor(backdrop, before, after);
        settings.setProperty("theme", next.name());
        if (accent != null) settings.setProperty("theme.accent", String.format("#%06x", accent.getRGB() & 0xFFFFFF));
        else settings.remove("theme.accent");
        if (deep != null) settings.setProperty("theme.accent2", String.format("#%06x", deep.getRGB() & 0xFFFFFF));
        else settings.remove("theme.accent2");
        saveSettings();
        refreshTheme();
    }

    private static void applyTooltipColors() {
        UIManager.put("ToolTip.background", TOOLTIP);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TOOLTIP_LINE), new EmptyBorder(5, 9, 5, 9)));
    }

    

    private void sanitizePaths() {
        String gameDir = settings.getProperty("gameDir", "").trim();
        if (!gameDir.isEmpty() && !usableDirectory(gameDir)) {
            settings.remove("gameDir");
            startupNotes.add("[Kinetic] Saved game folder " + gameDir + " does not exist on this system - using " + defaultGameDir());
        }
        String java = settings.getProperty("java", "").trim();
        if (!java.isEmpty() && (foreignPath(java) || !new File(java).exists())) {
            settings.remove("java");
            startupNotes.add("[Kinetic] Saved Java path " + java + " does not exist on this system - using the bundled Java");
        }
    }

    
    static boolean foreignPath(String path) {
        boolean windowsStyle = path.matches("^[A-Za-z]:[\\/].*") || path.startsWith("\\\\");
        return WINDOWS ? path.startsWith("/") && !windowsStyle : windowsStyle;
    }

    
    private static boolean usableDirectory(String path) {
        if (foreignPath(path)) return false;
        File dir = new File(path);
        if (dir.isDirectory()) return true;
        File parent = dir.getAbsoluteFile().getParentFile();
        return !dir.exists() && parent != null && parent.isDirectory();
    }

    

    private Theme detectClientTheme() {
        String gameDir = settings.getProperty("gameDir", "").trim();
        List<File> candidates = new ArrayList<>(Arrays.asList(new File(baseDir, "Kinetic/visuals.json"), new File(dataDir, "Kinetic/visuals.json")));
        if (!gameDir.isEmpty()) candidates.add(new File(gameDir, "Kinetic/visuals.json"));
        candidates.add(new File(defaultGameDir(), "Kinetic/visuals.json"));
        for (File file : candidates) {
            if (!file.isFile()) continue;
            try {
                Object json = Json.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
                Map<String, Object> clickGui = Json.object(Json.object(json, "Modules"), "ClickGUI");
                String color = Json.string(Json.object(clickGui, "Properties"), "Color");
                if (color != null) {
                    String value = color.trim();
                    if ("ISRAEL".equalsIgnoreCase(value)) return Theme.ISRAEL;
                    if ("CHRISTIAN".equalsIgnoreCase(value)) return Theme.CHRISTIAN;
                    return Theme.KINETIC;
                }
            } catch (IOException | RuntimeException ignored) {
                
            }
        }
        return Theme.KINETIC;
    }

    
    private void setTheme(Theme next) {
        
        if (next == Ui.theme && Ui.customAccent == null) return;
        applyLook(next, null, null);
        appendLog("[Kinetic] Theme: " + next.label);
    }

    private static void recolor(Component component, Color[] before, Color[] after) {
        if (component instanceof JLabel) {
            Color fg = component.getForeground();
            for (int i = 0; i < before.length; i++) {
                if (before[i].equals(fg)) {
                    component.setForeground(after[i]);
                    break;
                }
            }
        }
        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) recolor(child, before, after);
        }
    }

    private void refreshTheme() {
        backdrop.useTheme();
        if (frame != null) frame.setIconImages(themeIcons());
        brandIcon.setIcon(new ImageIcon(kineticIcon(22)));
        brandStar.setVisible(israel());
        themeSwitch.select(theme == Theme.KINETIC ? 0 : theme == Theme.ISRAEL ? 1 : 2);
        ramLabel.setForeground(ACCENT_LIGHT);
        log.setForeground(mix(TEXT, MUTED, 0.3f));
        log.setSelectionColor(SELECTION);
        log.setCaretColor(TEXT);
        hero.display(selected);
        refreshFilesLabel();
        updatePlayState();
        backdrop.revalidate();
        backdrop.repaint();
    }

    private void show() {
        prepare();
        JPanel root = buildUi();

        boolean nativeFrame = Boolean.getBoolean("kinetic.nativeFrame");
        frame = new JFrame("Kinetic Launcher");
        frame.setUndecorated(!nativeFrame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImages(themeIcons());
        frame.setBackground(BASE);
        frame.setContentPane(root);
        frame.setMinimumSize(new Dimension(1040, 640));
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(1180, screen.width), Math.min(720, screen.height));
        frame.setLocationRelativeTo(null);
        if (!nativeFrame) {
            chrome = new Ui.Chrome(frame);
            chrome.makeDraggable(titleBar);
            chrome.onStateChange(() -> {
                maximizeButton.glyph(chrome.isMaximized() ? Glyph.RESTORE : Glyph.MAX);
                maximizeButton.setToolTipText(chrome.isMaximized() ? "Restore" : "Maximise");
                backdrop.drawBorder = !chrome.isMaximized();
                backdrop.repaint();
            });
            backdrop.drawBorder = true;
        }
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                backdrop.setFps(30);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                backdrop.setFps(12);
            }

            @Override
            public void windowIconified(WindowEvent e) {
                backdrop.setFps(0);
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                backdrop.setFps(30);
            }
        });

        finishSetup(nativeFrame);
        frame.setVisible(true);
        backdrop.setFps(30);
        if (!Boolean.getBoolean("kinetic.noIntro")) startIntro();
        watchClientTheme();

        appendLog("[Kinetic] Log file: " + logFile().getAbsolutePath());
        appendLog("[Kinetic] Base directory: " + baseDir.getAbsolutePath() + (local ? " (local client)" : ""));
        appendLog("[Kinetic] Data directory: " + dataDir.getAbsolutePath());
        File jar = findClientJar();
        appendLog(jar != null ? "[Kinetic] Client: " + jar.getAbsolutePath() : "[Kinetic] client/Kinetic.jar not found next to the launcher");
        for (String note : startupNotes) appendLog(note);
        refreshFilesLabel();
        installDesktopEntry();
    }

    
    private void startIntro() {
        java.awt.KeyboardFocusManager focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager();
        java.awt.KeyEventDispatcher[] skipper = new java.awt.KeyEventDispatcher[1];
        Intro intro = new Intro("Welcome to Kinetic", "MINECRAFT 1.8.9", () -> focus.removeKeyEventDispatcher(skipper[0]));
        
        skipper[0] = e -> {
            if (!intro.running()) return false;
            if (e.getID() == KeyEvent.KEY_PRESSED) intro.skip();
            return true;
        };
        focus.addKeyEventDispatcher(skipper[0]);
        frame.setGlassPane(intro);
        intro.start();
    }

    private JPanel buildUi() {
        backdrop = new Backdrop(new BorderLayout());
        titleBar = titleBar();
        backdrop.add(titleBar, BorderLayout.NORTH);
        backdrop.add(homeArea(), BorderLayout.CENTER);
        return backdrop;
    }

    private void finishSetup(boolean nativeFrame) {
        rebuildClientList();
        Client initial = findClient(settings.getProperty("selectedClient", KINETIC));
        select(initial != null ? initial : clients.get(0), false);
        showCard("play");
        for (Component c : windowButtonList) c.setVisible(!nativeFrame);
    }

    private final List<Component> windowButtonList = new ArrayList<>();

    
    private static final String[] PAGES = {"play", "settings", "log"};
    private static final int LOG_TAB = 2;

    private JPanel titleBar() {
        JPanel bar = transparent(new BorderLayout());
        bar.setBorder(new EmptyBorder(0, 24, 0, 8));
        bar.setPreferredSize(new Dimension(0, 60));

        JPanel brand = transparent(null);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        
        brandIcon = new JLabel(new ImageIcon(kineticIcon(22)));
        brand.add(brandIcon);
        brand.add(Box.createHorizontalStrut(10));
        brand.add(label("KINETIC", TEXT, wordmark(15f)));
        brandStar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                if (christian()) paintCross(g2, 12, getHeight() / 2f, 8f, 1.6f, TEXT);
                else paintHexagram(g2, 12, getHeight() / 2f, 8f, 1.6f, TEXT);
                g2.dispose();
            }
        };
        brandStar.setPreferredSize(new Dimension(24, 24));
        brandStar.setMaximumSize(new Dimension(24, 24));
        brandStar.setToolTipText("Israel / Christian theme");
        brandStar.setVisible(israel() || christian());
        brand.add(Box.createHorizontalStrut(2));
        brand.add(brandStar);
        brand.add(Box.createHorizontalStrut(40));
        tabs = new TextTabs(new String[]{"Play", "Settings", "Console"}, i -> showCard(PAGES[i]));
        brand.add(tabs);
        JPanel brandWrap = transparent(new GridBagLayout());
        brandWrap.add(brand);
        bar.add(brandWrap, BorderLayout.WEST);

        JPanel right = transparent(null);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        Btn minimize = windowButton(Glyph.MIN, "Minimise", false, () -> {
            if (chrome != null) chrome.minimize();
        });
        maximizeButton = windowButton(Glyph.MAX, "Maximise", false, () -> {
            if (chrome != null) chrome.toggleMaximize();
        });
        Btn close = windowButton(Glyph.CLOSE, "Close", true, () -> {
            if (frame != null) frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        });
        for (Btn button : new Btn[]{minimize, maximizeButton, close}) {
            right.add(button);
            right.add(Box.createHorizontalStrut(2));
            windowButtonList.add(button);
        }
        JPanel rightWrap = transparent(new GridBagLayout());
        rightWrap.add(right);
        bar.add(rightWrap, BorderLayout.EAST);
        return bar;
    }

    private static Btn windowButton(Glyph glyph, String tip, boolean close, Runnable action) {
        Btn button = new Btn("", close ? Kind.CLOSE : Kind.WINDOW).glyph(glyph);
        button.radius = 10;
        button.setPreferredSize(new Dimension(42, 32));
        button.setToolTipText(tip);
        button.setFocusable(false);
        button.addActionListener(e -> action.run());
        return button;
    }

    private JPanel homeArea() {
        cardLayout = new CardLayout();
        cards = transparent(cardLayout);
        clientList = transparent(null);
        clientList.setLayout(new BoxLayout(clientList, BoxLayout.Y_AXIS));
        
        JPanel settingsPage = settingsPage();
        cards.add(playPage(), "play");
        cards.add(settingsPage, "settings");
        cards.add(logPage(), "log");
        return cards;
    }

    
    private JPanel playPage() {
        JPanel page = transparent(new GridBagLayout());
        page.setBorder(new EmptyBorder(0, 60, 44, 40));
        JPanel column = transparent(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        switcher = new ClientSwitcher();
        column.add(switcher);
        column.add(Box.createVerticalStrut(12));
        hero = new HeroPanel();
        column.add(hero);
        column.add(Box.createVerticalStrut(30));

        playButton = new PlayButton();
        playButton.addActionListener(e -> launch());
        Btn folder = new Btn("", Kind.GHOST).glyph(Glyph.FOLDER);
        folder.radius = 14;
        folder.setPreferredSize(new Dimension(58, 58));
        folder.setToolTipText("Open the game folder (saves, screenshots, resource packs)");
        folder.addActionListener(e -> openGameFolder());
        ramChip = new Btn(ramText(), Kind.GHOST) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(Math.max(96, super.getPreferredSize().width + 12), 58);
            }
        };
        ramChip.radius = 14;
        ramChip.setToolTipText("Memory for the game - change it in the settings");
        ramChip.addActionListener(e -> showCard("settings"));
        ramSlider.addChangeListener(e -> {
            ramChip.setText(ramText());
            ramChip.revalidate();
        });
        JPanel actions = transparent(null);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(playButton);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(folder);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(ramChip);
        column.add(actions);
        column.add(Box.createVerticalStrut(18));

        statusLabel = new StatusLabel();
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setPreferredSize(new Dimension(440, 44));
        statusLabel.setMaximumSize(new Dimension(440, 44));
        column.add(statusLabel);

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.SOUTHWEST;
        page.add(column, c);
        return page;
    }

    private String ramText() {
        return ramSlider.getValue() + " GB RAM";
    }

    private void showCard(String name) {
        cardLayout.show(cards, name);
        tabs.select(Math.max(0, Arrays.asList(PAGES).indexOf(name)));
        if (backdrop != null) backdrop.dim.to("play".equals(name) ? 0f : 1f);
    }

    
    private static JComponent pageTitle(String title, String subtitle) {
        JPanel head = transparent(null);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel big = label(title, TEXT, display(30f));
        big.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(big);
        head.add(Box.createVerticalStrut(4));
        JLabel small = label(subtitle, MUTED, font(Font.PLAIN, 13.5f));
        small.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(small);
        return head;
    }

    
    private static JComponent section(String title, JComponent... rows) {
        JPanel block = new JPanel(new BorderLayout(0, 9)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(new EmptyBorder(0, 0, 24, 0));
        JLabel cap = caption(title.toUpperCase(Locale.ROOT));
        cap.setBorder(new EmptyBorder(0, 2, 0, 0));
        block.add(cap, BorderLayout.NORTH);
        GlassPanel card = new GlassPanel(null, 16, GLASS_TINT);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) card.add(hairline());
            rows[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(rows[i]);
        }
        block.add(card, BorderLayout.CENTER);
        return block;
    }

    private static JComponent hairline() {
        JComponent line = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 14));
                g.fillRect(18, 0, getWidth() - 36, 1);
            }
        };
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setPreferredSize(new Dimension(10, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return line;
    }

    
    private static JComponent row(String name, String hint, JComponent control, boolean fill) {
        JPanel row = transparent(new BorderLayout(24, 0));
        row.setBorder(new EmptyBorder(13, 18, 13, 18));
        JPanel text = transparent(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label(name, TEXT, medium(13.5f)));
        if (hint != null) {
            text.add(Box.createVerticalStrut(3));
            text.add(label(hint, DIM, font(Font.PLAIN, 12f)));
        }
        JPanel textWrap = transparent(new GridBagLayout());
        GridBagConstraints t = new GridBagConstraints();
        t.anchor = GridBagConstraints.WEST;
        t.weightx = 1;
        textWrap.add(text, t);
        textWrap.setPreferredSize(new Dimension(212, text.getPreferredSize().height));
        row.add(textWrap, BorderLayout.WEST);
        JPanel wrap = transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.anchor = GridBagConstraints.EAST;
        if (fill) c.fill = GridBagConstraints.HORIZONTAL;
        wrap.add(control, c);
        row.add(wrap, BorderLayout.CENTER);
        return row;
    }

    
    private static JPanel withSide(JComponent main, JComponent side) {
        JPanel panel = transparent(new BorderLayout(10, 0));
        panel.add(main, BorderLayout.CENTER);
        if (side != null) {
            JPanel sideWrap = transparent(new GridBagLayout());
            sideWrap.add(side);
            panel.add(sideWrap, BorderLayout.EAST);
        }
        return panel;
    }

    private JPanel settingsPage() {
        gameDirField = new RoundField(settings.getProperty("gameDir", defaultGameDir().getAbsolutePath()));
        javaField = new RoundField(settings.getProperty("java", ""));
        ((RoundField) javaField).placeholder = "Bundled Java (automatic)";
        javaField.setToolTipText("Leave empty: bundled client/jre, then the Java 8 downloaded by the launcher, then JAVA_HOME, then java on PATH");

        int maxRam = (int) Math.max(2, Math.min(32, totalMemoryGb()));
        ramSlider = new JSlider(1, maxRam, clamp(parseInt(settings.getProperty("ramGb", "2"), 2), 1, maxRam));
        styleSlider(ramSlider);
        ramLabel = label(ramSlider.getValue() + " GB", ACCENT_LIGHT, semibold(13.5f));
        ramLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        ramLabel.setPreferredSize(new Dimension(56, 26));
        ramSlider.addChangeListener(e -> ramLabel.setText(ramSlider.getValue() + " GB"));

        closeAfterLaunch = new Toggle(Boolean.parseBoolean(settings.getProperty("closeAfterLaunch", "false")));

        themeSwitch = new Segmented(new String[]{Theme.KINETIC.label, Theme.ISRAEL.label, Theme.CHRISTIAN.label},
                i -> setTheme(i == 0 ? Theme.KINETIC : i == 1 ? Theme.ISRAEL : Theme.CHRISTIAN));
        themeSwitch.select(theme == Theme.KINETIC ? 0 : theme == Theme.ISRAEL ? 1 : 2);
        themeSwitch.setToolTipText("Colours and artwork of the launcher");

        filesLabel = label(" ", MUTED, font(Font.PLAIN, 13f));
        verifyButton = iconButton("Verify assets", Glyph.REFRESH, () -> startAssetCheck(true));
        verifyButton.setToolTipText("Check the Minecraft assets and download missing files from Mojang");

        JPanel files = transparent(new BorderLayout(14, 0));
        files.setBorder(new EmptyBorder(14, 18, 14, 18));
        files.add(filesLabel, BorderLayout.CENTER);
        JPanel verifyWrap = transparent(new GridBagLayout());
        verifyWrap.add(verifyButton);
        files.add(verifyWrap, BorderLayout.EAST);

        JPanel column = new JPanel(null) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int available = getParent() != null ? getParent().getWidth() : 700;
                return new Dimension(Math.min(700, Math.max(440, available)), d.height);
            }
        };
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(new EmptyBorder(4, 0, 30, 0));
        column.add(pageTitle("Settings", "How the launcher starts the game."));
        column.add(Box.createVerticalStrut(28));
        column.add(section("Game",
                row("Game folder", "Saves, resource packs, screenshots", withSide(gameDirField, iconButton("Browse", Glyph.FOLDER, () -> browseDirectory(gameDirField))), true),
                row("Java", "Empty uses the bundled Java", withSide(javaField, iconButton("Browse", Glyph.FOLDER, () -> browseFile(javaField, null))), true),
                row("Memory", "RAM the game may use", withSide(ramSlider, ramLabel), true),
                row("Close after launch", "Hide the launcher while you play", closeAfterLaunch, false)));
        column.add(section("Appearance",
                row("Theme", "Colours and artwork of the launcher", themeSwitch, false)));
        column.add(section("Client files", files));
        column.add(Box.createVerticalGlue());

        Ui.TrackingPanel wrap = new Ui.TrackingPanel(new BorderLayout());
        wrap.add(column, BorderLayout.WEST);
        
        javax.swing.JScrollPane scroll = glassScroll(wrap);
        scroll.setPreferredSize(new Dimension(700 + 14, 10));
        JPanel page = transparent(new BorderLayout());
        page.setBorder(new EmptyBorder(8, 60, 0, 40));
        page.add(scroll, BorderLayout.WEST);
        return page;
    }

    private static Btn iconButton(String text, Glyph glyph, Runnable action) {
        Btn button = ghostButton(text, action);
        button.glyph(glyph);
        return button;
    }

    
    private static void settingRow(JPanel panel, int row, String name, JComponent main, JComponent side) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(5, 0, 5, 12);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel label = label(name, MUTED, medium(12.5f));
        label.setPreferredSize(new Dimension(104, 26));
        c.gridx = 0;
        c.weightx = 0;
        panel.add(label, c);

        c.gridx = 1;
        c.weightx = 1;
        if (side == null) {
            c.gridwidth = 2;
            c.insets = new Insets(5, 0, 5, 0);
        }
        panel.add(main, c);

        if (side != null) {
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(5, 0, 5, 0);
            panel.add(side, c);
        }
    }

    private JPanel logPage() {
        JPanel page = transparent(new BorderLayout(0, 20));
        page.setBorder(new EmptyBorder(8, 60, 36, 40));

        JPanel head = transparent(new BorderLayout());
        head.add(pageTitle("Console", "Everything the launcher and the game print."), BorderLayout.WEST);
        JPanel toolbar = transparent(null);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.add(iconButton("Copy", Glyph.COPY, () -> {
            StringSelection selection = new StringSelection(log.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            setStatus("Log copied to clipboard", false);
        }));
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(iconButton("Clear", Glyph.TRASH, () -> log.setText("")));
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(iconButton("Open log file", Glyph.FILE, this::openLogFile));
        JPanel toolWrap = transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.weighty = 1;
        toolWrap.add(toolbar, c);
        head.add(toolWrap, BorderLayout.EAST);
        page.add(head, BorderLayout.NORTH);

        GlassPanel panel = new GlassPanel(new BorderLayout(), 16, LOG_TINT);
        panel.setBorder(new EmptyBorder(12, 16, 12, 10));
        log = new JTextArea() {
            @Override
            public void updateUI() {
                setUI(new BasicTextAreaUI());
            }
        };
        log.setOpaque(false);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(false);
        log.setForeground(mix(TEXT, MUTED, 0.3f));
        log.setCaretColor(TEXT);
        log.setSelectionColor(SELECTION);
        log.setSelectedTextColor(Color.WHITE);
        log.setFont(mono(12f));
        log.setBorder(new EmptyBorder(2, 2, 2, 6));
        panel.add(glassScroll(log), BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    
    private void showClientPopup(Component anchor) {
        if (clientPopup == null) {
            clientPopup = new JPopupMenu() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = smooth(g);
                    int w = getWidth(), h = getHeight();
                    g2.setColor(mix(DIALOG, BASE, 0.3f));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));
                    g2.setColor(new Color(255, 255, 255, 26));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 16, 16));
                    g2.dispose();
                }
            };
            clientPopup.setOpaque(false);
            clientPopup.setBorder(new EmptyBorder(6, 6, 6, 6));
            clientPopup.setLayout(new BorderLayout(0, 4));
            JLabel head = caption("CLIENTS");
            head.setBorder(new EmptyBorder(6, 10, 4, 10));
            clientPopup.add(head, BorderLayout.NORTH);
            clientPopup.add(clientList, BorderLayout.CENTER);
            Btn add = new Btn("Add a client jar", Kind.LINK).glyph(Glyph.PLUS);
            add.setToolTipText("Launch another client jar with Kinetic's natives, assets and game folder");
            add.addActionListener(e -> {
                clientPopup.setVisible(false);
                showAddDialog();
            });
            JPanel addWrap = transparent(new BorderLayout());
            addWrap.add(add, BorderLayout.WEST);
            clientPopup.add(addWrap, BorderLayout.SOUTH);
        }
        clientPopup.setPreferredSize(null);
        Dimension size = clientPopup.getPreferredSize();
        clientPopup.setPreferredSize(new Dimension(Math.max(280, size.width), size.height));
        clientPopup.show(anchor, 0, anchor.getHeight() + 6);
    }

    

    private static final class Client {
        final String name, jar, mainClass, version;
        final boolean builtin;

        Client(String name, String jar, String mainClass, String version, boolean builtin) {
            this.name = name;
            this.jar = jar;
            this.mainClass = mainClass == null || mainClass.trim().isEmpty() ? DEFAULT_MAIN_CLASS : mainClass.trim();
            this.version = version == null ? "" : version.trim();
            this.builtin = builtin;
        }

        String initial() {
            return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        }

        Color color() {
            if (builtin) return ACCENT;
            return BADGE_COLORS[(name.toLowerCase(Locale.ROOT).hashCode() & 0x7fffffff) % BADGE_COLORS.length];
        }

        String subtitle() {
            String v = version.isEmpty() ? "" : version + "  ·  ";
            return v + (builtin ? "Built-in" : "Custom");
        }
    }

    private Client findClient(String name) {
        for (Client client : clients) if (client.name.equalsIgnoreCase(name.trim())) return client;
        return null;
    }

    private void loadClients() {
        clients.clear();
        clients.add(new Client("Kinetic Client", null, DEFAULT_MAIN_CLASS, "1.8.9", true));
        int count = parseInt(settings.getProperty("clients.count", "0"), 0);
        for (int i = 0; i < count; i++) {
            String name = settings.getProperty("client." + i + ".name", "").trim();
            String jar = settings.getProperty("client." + i + ".jar", "").trim();
            if (name.isEmpty() || jar.isEmpty() || findClient(name) != null) continue;
            clients.add(new Client(name, jar, settings.getProperty("client." + i + ".main"),
                    settings.getProperty("client." + i + ".version", ""), false));
        }
    }

    private void storeClients() {
        for (String key : settings.stringPropertyNames()) if (key.startsWith("client.")) settings.remove(key);
        int i = 0;
        for (Client client : clients) {
            if (client.builtin) continue;
            settings.setProperty("client." + i + ".name", client.name);
            settings.setProperty("client." + i + ".jar", client.jar);
            settings.setProperty("client." + i + ".main", client.mainClass);
            settings.setProperty("client." + i + ".version", client.version);
            i++;
        }
        settings.setProperty("clients.count", String.valueOf(i));
        if (selected != null) settings.setProperty("selectedClient", selected.name);
    }

    private void rebuildClientList() {
        clientList.removeAll();
        for (Client client : clients) {
            clientList.add(new ClientItem(client));
            clientList.add(Box.createVerticalStrut(4));
        }
        clientList.revalidate();
        clientList.repaint();
    }

    private void select(Client client, boolean persist) {
        selected = client;
        hero.display(client);
        clientList.repaint();
        updatePlayState();
        if (persist) saveSettings();
    }

    private void removeClient(Client client) {
        if (client.builtin) return;
        if (!confirm("Remove " + client.name + "?", "Only the launcher entry is removed, the jar file stays on disk.", "Remove")) return;
        clients.remove(client);
        rebuildClientList();
        select(selected == client ? clients.get(0) : selected, true);
        appendLog("[Kinetic] Removed client " + client.name);
    }

    private void showAddDialog() {
        JDialog dialog = dialog("Add client");
        JPanel content = (JPanel) dialog.getContentPane();

        JPanel head = transparent(null);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(label("Add a client", TEXT, display(22f)));
        head.add(Box.createVerticalStrut(4));
        head.add(label("Launches the jar with Kinetic's natives, assets, Java and game folder.", MUTED, font(Font.PLAIN, 12.5f)));
        content.add(head, BorderLayout.NORTH);

        JPanel form = transparent(new GridBagLayout());
        RoundField name = new RoundField("");
        RoundField version = new RoundField("1.8.9");
        RoundField jar = new RoundField("");
        RoundField main = new RoundField(DEFAULT_MAIN_CLASS);
        settingRow(form, 0, "Name", name, null);
        settingRow(form, 1, "Version", version, null);
        settingRow(form, 2, "Client jar", jar, iconButton("Browse", Glyph.FOLDER, () -> {
            browseFile(jar, new FileNameExtensionFilter("Java archives (*.jar)", "jar"));
            if (name.getText().trim().isEmpty() && !jar.getText().trim().isEmpty()) {
                String file = new File(jar.getText().trim()).getName();
                name.setText(file.toLowerCase(Locale.ROOT).endsWith(".jar") ? file.substring(0, file.length() - 4) : file);
            }
        }));
        settingRow(form, 3, "Main class", main, null);
        main.setToolTipText("Class with the main method, default " + DEFAULT_MAIN_CLASS);
        JLabel error = label(" ", DANGER, font(Font.PLAIN, 12f));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 0, 0);
        form.add(error, c);
        content.add(form, BorderLayout.CENTER);

        Btn cancel = ghostButton("Cancel", dialog::dispose);
        Btn add = new Btn("Add client", Kind.PRIMARY).glyph(Glyph.PLUS);
        add.addActionListener(e -> {
            String n = name.getText().trim();
            String path = jar.getText().trim();
            String mainClass = main.getText().trim();
            File file = new File(path);
            if (n.isEmpty()) error.setText("Enter a name for the client");
            else if (findClient(n) != null) error.setText("A client called " + n + " already exists");
            else if (path.isEmpty()) error.setText("Choose the client jar");
            else if (!file.isFile()) error.setText("File not found: " + path);
            else if (!isReadableZip(file)) error.setText(file.getName() + " is not a readable jar");
            else if (!mainClass.isEmpty() && !mainClass.matches("[\\p{L}_$][\\p{L}\\p{N}_$]*(\\.[\\p{L}_$][\\p{L}\\p{N}_$]*)*"))
                error.setText("Main class must be a fully qualified class name");
            else {
                Client client = new Client(n, file.getAbsolutePath(), mainClass, version.getText(), false);
                clients.add(client);
                rebuildClientList();
                select(client, true);
                appendLog("[Kinetic] Added client " + n + " (" + file.getAbsolutePath() + ")");
                dialog.dispose();
            }
        });
        JPanel buttons = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancel);
        buttons.add(add);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(add);
        dialog.pack();
        dialog.setSize(Math.max(560, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private boolean confirm(String title, String message, String action) {
        JDialog dialog = dialog(title);
        JPanel content = (JPanel) dialog.getContentPane();
        JPanel head = transparent(null);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(label(title, TEXT, display(19f)));
        head.add(Box.createVerticalStrut(6));
        head.add(label(message, MUTED, font(Font.PLAIN, 13f)));
        content.add(head, BorderLayout.CENTER);

        boolean[] result = {false};
        Btn ok = new Btn(action, Kind.DANGER).glyph(Glyph.TRASH);
        ok.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });
        JPanel buttons = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(ghostButton("Cancel", dialog::dispose));
        buttons.add(ok);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setSize(Math.max(430, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        return result[0];
    }

    

    private boolean isRunning() {
        Process process = game;
        return process != null && process.isAlive();
    }

    
    private String refreshPlayButton() {
        if (isRunning()) {
            playButton.setRunning(true);
            playButton.setEnabled(true);
            return null;
        }
        playButton.setRunning(false);
        if (verifying) {
            playButton.setEnabled(false);
            return "Checking files...";
        }
        String problem = clientProblem(selected);
        playButton.setEnabled(problem == null);
        return problem;
    }

    private void updatePlayState() {
        String problem = refreshPlayButton();
        if (isRunning() || verifying) return;
        if (problem != null) setStatus(problem, true);
        else setStatus("Ready to launch " + selected.name, false);
    }

    private String clientProblem(Client client) {
        if (client == null) return "No client selected";
        if (client.builtin) {
            if (findClientJar() == null) return "client/Kinetic.jar not found next to the launcher";
        } else if (!new File(client.jar).isFile()) {
            return "Client jar not found: " + new File(client.jar).getName();
        }
        if (findNatives() == null) return "client/natives/" + nativesFolder() + " not found next to the launcher";
        return null;
    }

    private void refreshFilesLabel() {
        if (filesLabel == null) return;
        File jar = findClientJar();
        String text = jar != null ? "Kinetic.jar in " + jar.getParentFile().getAbsolutePath() : "client/Kinetic.jar not found next to the launcher";
        String state = (findNatives() != null ? "natives ready" : "natives missing")
                + "  ·  " + (findAssets() != null ? "assets ready" : "assets missing");
        filesLabel.setText(twoLines(text, state));
        filesLabel.setToolTipText("Downloaded files are stored in " + dataDir.getAbsolutePath());
    }

    private void setStatus(String text, boolean error) {
        statusLabel.set(text, error ? DANGER : MUTED, error ? DANGER : ACCENT);
    }

    private void setRunningStatus(String text) {
        statusLabel.set(text, TEXT, SUCCESS);
    }

    

    private File logFile() {
        return new File(baseDir, "launcher.log");
    }

    private void openLogFile() {
        File file = logFile();
        if (!file.isFile()) {
            setStatus("No log file yet", true);
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            new ProcessBuilder(WINDOWS ? "notepad" : MAC ? "open" : "xdg-open", file.getAbsolutePath()).start();
        } catch (IOException e) {
            setStatus("Could not open log: " + e.getMessage(), true);
        }
    }

    private void browseDirectory(JTextField target) {
        JFileChooser chooser = new JFileChooser(target.getText().isEmpty() ? null : target.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) target.setText(chooser.getSelectedFile().getAbsolutePath());
    }

    private void browseFile(JTextField target, FileNameExtensionFilter filter) {
        JFileChooser chooser = new JFileChooser();
        String current = target.getText().trim();
        if (!current.isEmpty() && new File(current).getParentFile() != null) chooser.setCurrentDirectory(new File(current).getParentFile());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (filter != null) chooser.setFileFilter(filter);
        Component parent = SwingUtilities.getWindowAncestor(target);
        if (chooser.showOpenDialog(parent != null ? parent : frame) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void openGameFolder() {
        if (gameDirField.getText().trim().isEmpty()) gameDirField.setText(defaultGameDir().getAbsolutePath());
        File dir = new File(gameDirField.getText().trim()).getAbsoluteFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            setStatus("Cannot create " + dir, true);
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            new ProcessBuilder(WINDOWS ? "explorer" : MAC ? "open" : "xdg-open", dir.getAbsolutePath()).start();
        } catch (IOException e) {
            setStatus("Could not open folder: " + e.getMessage(), true);
        }
    }

    private void appendLog(String line) {
        writeLogFile(line);
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendLogUi(line));
            return;
        }
        appendLogUi(line);
    }

    private static final int LOG_LIMIT = 400_000, LOG_TRIM = 100_000;

    private void appendLogUi(String line) {
        
        if (log.getDocument().getLength() > LOG_LIMIT) {
            try {
                log.getDocument().remove(0, LOG_TRIM);
            } catch (javax.swing.text.BadLocationException ignored) {
            }
        }
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
        if (tabs != null && !tabs.isSelected(LOG_TAB)) tabs.setDot(LOG_TAB, true);
    }

    private PrintWriter logWriter;

    private synchronized void writeLogFile(String line) {
        try {
            if (logWriter == null) {
                logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile(), false), StandardCharsets.UTF_8), true);
            }
            logWriter.println(line);
        } catch (IOException ignored) {
        }
    }

    

    private void launch() {
        if (isRunning()) {
            stopRequested = true;
            appendLog("[Kinetic] Stopping the game...");
            setStatus("Stopping...", false);
            game.destroy();
            return;
        }
        if (verifying) return;

        Client client = selected;
        if (client == null) { setStatus("No client selected", true); return; }
        File jar = client.builtin ? findClientJar() : new File(client.jar).getAbsoluteFile();
        if (client.builtin && jar != null) jar = stableClientJar(jar);
        File natives = findNatives();
        File assets = findAssets();
        File java = findJava();
        if (gameDirField.getText().trim().isEmpty()) gameDirField.setText(defaultGameDir().getAbsolutePath());
        File gameDir = new File(gameDirField.getText().trim()).getAbsoluteFile();

        if (jar == null || !jar.isFile()) { setStatus(client.builtin ? "client/Kinetic.jar not found next to the launcher" : "Client jar not found: " + client.jar, true); return; }
        if (natives == null) { setStatus("natives/" + nativesFolder() + " not found", true); return; }
        if (assets == null || java == null) {
            prepareDownloadsThenLaunch(assets == null, java == null);
            return;
        }
        if (!gameDir.isDirectory() && !gameDir.mkdirs()) { setStatus("Cannot create game folder", true); return; }

        if (!WINDOWS && !MAC && !isOnPath("xrandr")) {
            setStatus("xrandr is missing - install xorg-xrandr (or your distro's xrandr package)", true);
            appendLog("[Kinetic] xrandr not found on PATH. Minecraft 1.8 needs it on Linux: sudo pacman -S xorg-xrandr / apt install x11-xserver-utils");
            return;
        }

        saveSettings();

        List<String> command = new ArrayList<>();
        command.add(java.getAbsolutePath());
        command.add("-Xmx" + ramSlider.getValue() + "G");
        command.add("-Xms" + Math.max(1, ramSlider.getValue() / 2) + "G");
        
        
        command.addAll(Arrays.asList("-XX:+UnlockExperimentalVMOptions", "-XX:+UseG1GC", "-XX:G1NewSizePercent=20",
                "-XX:G1ReservePercent=20", "-XX:MaxGCPauseMillis=20", "-XX:G1HeapRegionSize=32M", "-XX:+ParallelRefProcEnabled",
                "-XX:-UsePerfData", "-Dsun.java2d.opengl=false"));
        command.add("-Djava.library.path=" + natives.getAbsolutePath());
        command.add("-Dorg.lwjgl.librarypath=" + natives.getAbsolutePath());
        command.add("-DLWJGL_WM_CLASS=" + WM_CLASS);
        if (client.builtin) {
            command.add("-jar");
            command.add(jar.getAbsolutePath());
        } else {
            String problem = addCustomClasspath(client, jar, command);
            if (problem != null) {
                setStatus(problem, true);
                appendLog("[Kinetic] Cannot launch " + client.name + ": " + problem);
                return;
            }
        }
        command.add("--version"); command.add(client.builtin ? KINETIC : client.name);
        command.add("--accessToken"); command.add("0");
        command.add("--assetsDir"); command.add(assets.getAbsolutePath());
        command.add("--assetIndex"); command.add(ASSET_INDEX);
        command.add("--gameDir"); command.add(gameDir.getAbsolutePath());
        command.add("--userProperties"); command.add("{}");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(baseDir);
        builder.redirectErrorStream(true);

        String pathVar = WINDOWS ? "PATH" : MAC ? "DYLD_LIBRARY_PATH" : "LD_LIBRARY_PATH";
        StringBuilder libraryPath = new StringBuilder(natives.getAbsolutePath());
        File javaLib = new File(java.getParentFile().getParentFile(), WINDOWS ? "bin" : "lib/amd64");
        if (!javaLib.isDirectory()) javaLib = new File(java.getParentFile().getParentFile(), "lib");
        if (javaLib.isDirectory()) libraryPath.append(File.pathSeparator).append(javaLib.getAbsolutePath());
        String existing = System.getenv(pathVar);
        if (existing != null && !existing.isEmpty()) libraryPath.append(File.pathSeparator).append(existing);
        builder.environment().put(pathVar, libraryPath.toString());
        
        builder.environment().put("KINETIC_LAUNCHER_DIR", dataDir.getAbsolutePath());

        appendLog("[Kinetic] Client: " + client.name + " (" + jar.getAbsolutePath() + ")");
        appendLog("[Kinetic] Java: " + java.getAbsolutePath());
        appendLog("[Kinetic] Game dir: " + gameDir.getAbsolutePath());
        appendLog("[Kinetic] Launching with " + ramSlider.getValue() + " GB...");
        setStatus("Starting...", false);
        playButton.setEnabled(false);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            setStatus("Failed to start: " + e.getMessage(), true);
            appendLog("[Kinetic] Failed to start: " + e);
            refreshPlayButton();
            return;
        }
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }
        game = process;
        runningClient = client;
        stopRequested = false;

        Thread reader = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) appendLog(line);
            } catch (IOException ignored) {
            }
            int exit;
            try {
                exit = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exit = -1;
            }
            int code = exit;
            SwingUtilities.invokeLater(() -> {
                appendLog("[Kinetic] Game exited with code " + code);
                boolean stopped = stopRequested;
                if (game == process) {
                    game = null;
                    runningClient = null;
                }
                refreshPlayButton();
                clientList.repaint();
                if (stopped) setStatus("Game stopped", false);
                else setStatus(code == 0 ? "Game closed" : "Game exited with code " + code, code != 0);
            });
        }, "Kinetic Game Output");
        reader.setDaemon(true);
        reader.start();

        refreshPlayButton();
        clientList.repaint();
        setRunningStatus(client.name + " is running");
        if (closeAfterLaunch.isSelected()) {
            new Thread(() -> {
                try {
                    Thread.sleep(4000L);
                } catch (InterruptedException ignored) {
                }
                if (process.isAlive()) System.exit(0);
            }, "Kinetic Launcher Exit").start();
        }
    }

    

    private String addCustomClasspath(Client client, File jar, List<String> command) {
        if (!isReadableZip(jar)) return jar.getName() + " is not a readable jar";

        List<File> classpath = new ArrayList<>();
        classpath.add(jar);
        File libraries = firstDir(new File(jar.getParentFile(), "libraries"), new File(baseDir, "libraries"), new File(baseDir, "launch/libraries"));
        if (libraries != null) collectJars(libraries, classpath);

        String mainClass = client.mainClass;
        if (!anyJarContains(classpath, mainClass.replace('.', '/') + ".class")) {
            return "Main class " + mainClass + " not found in " + jar.getName();
        }
        if (!anyJarContains(classpath, "org/lwjgl/opengl/Display.class")) {
            return jar.getName() + " does not bundle LWJGL - use a fat jar or put its libraries in a libraries/ folder";
        }

        StringBuilder cp = new StringBuilder();
        for (File file : classpath) {
            if (cp.length() > 0) cp.append(File.pathSeparator);
            cp.append(file.getAbsolutePath());
        }
        if (libraries != null) appendLog("[Kinetic] Libraries: " + libraries.getAbsolutePath() + " (" + (classpath.size() - 1) + " jars)");
        appendLog("[Kinetic] Main class: " + mainClass);
        command.add("-cp");
        command.add(cp.toString());
        command.add(mainClass);
        return null;
    }

    private static void collectJars(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File file : files) {
            if (file.isDirectory()) collectJars(file, out);
            else if (file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) out.add(file);
        }
    }

    private static boolean anyJarContains(List<File> jars, String entry) {
        for (File jar : jars) {
            try (ZipFile zip = new ZipFile(jar)) {
                if (zip.getEntry(entry) != null) return true;
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    private static boolean isReadableZip(File file) {
        try (ZipFile ignored = new ZipFile(file)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    

    
    private static File locateLauncherDir() {
        try {
            File self = new File(KineticLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return self.isFile() ? self.getParentFile() : self;
        } catch (URISyntaxException | RuntimeException e) {
            return new File(".").getAbsoluteFile();
        }
    }

    
    private static File locateLocalDir(File launcherDir) {
        for (File probe = launcherDir; probe != null; probe = probe.getParentFile()) {
            if (clientJarIn(probe) != null || new File(probe, "build.gradle").isFile() && new File(probe, "launch").isDirectory()) {
                return probe;
            }
        }
        return null;
    }

    private static File clientJarIn(File dir) {
        File inClient = new File(dir, "client/Kinetic.jar");
        if (inClient.isFile()) return inClient;
        File beside = new File(dir, "Kinetic.jar");
        return beside.isFile() && !isLauncherSelf(beside) ? beside : null;
    }

    private static boolean isLauncherSelf(File file) {
        try {
            return file.getCanonicalFile().equals(launcherJar().getCanonicalFile());
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    
    private static File locateDataDir() {
        String home = System.getProperty("user.home");
        if (WINDOWS) {
            String local = System.getenv("LOCALAPPDATA");
            return new File(local != null && !local.isEmpty() ? new File(local) : new File(home, "AppData/Local"), "KineticClient");
        }
        if (MAC) return new File(home, "Library/Application Support/KineticClient");
        String xdg = System.getenv("XDG_DATA_HOME");
        return new File(xdg != null && !xdg.isEmpty() ? new File(xdg) : new File(home, ".local/share"), "KineticClient");
    }

    
    private File findClientJar() {
        if (!local) return null;
        File jar = clientJarIn(baseDir);
        if (jar != null) return jar;
        return firstFile(new File(baseDir, "build/libs/Kinetic.jar"), new File(baseDir, "launch/Kinetic.jar"));
    }

    private File stableClientJar(File source) {
        if (source == null || !source.isFile()) return source;
        try {
            File dir = new File(baseDir, "launch-cache");
            if (!dir.isDirectory() && !dir.mkdirs()) return source;
            String stamp = Long.toHexString(source.length()) + "-" + Long.toHexString(source.lastModified());
            File copy = new File(dir, "Kinetic-" + stamp + ".jar");
            if (!copy.isFile() || copy.length() != source.length()) {
                Files.copy(source.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                File[] old = dir.listFiles((d, n) -> n.startsWith("Kinetic-") && n.endsWith(".jar") && !n.equals(copy.getName()));
                if (old != null) {
                    for (File file : old) {
                        boolean ignored = file.delete();
                    }
                }
            }
            return copy;
        } catch (IOException e) {
            return source;
        }
    }

    private static String nativesFolder() {
        return WINDOWS ? "windows" : MAC ? "macos" : "linux";
    }

    private File findNatives() {
        if (!local) return null;
        return firstDir(new File(baseDir, "client/natives/" + nativesFolder()), new File(baseDir, "natives/" + nativesFolder()), new File(baseDir, "launch/natives/" + nativesFolder()));
    }

    
    private File findAssets() {
        File bundled = local ? firstDir(new File(baseDir, "client/assets"), new File(baseDir, "assets"), new File(baseDir, "launch/assets"), new File(baseDir, "run/assets")) : null;
        if (bundled != null) return bundled;
        File downloaded = new File(dataDir, "assets");
        return new File(downloaded, "indexes/" + ASSET_INDEX + ".json").isFile() ? downloaded : null;
    }

    private File findJava() {
        String custom = javaField.getText().trim();
        if (!custom.isEmpty()) {
            File file = new File(custom);
            if (file.isFile()) return file;
            File inHome = new File(file, "bin/" + javaBinary());
            if (inHome.isFile()) return inHome;
            return null;
        }

        File bundled = firstFile(
                new File(baseDir, "client/jre/bin/" + javaBinary()),
                new File(baseDir, "client/jre/jre/bin/" + javaBinary()),
                new File(baseDir, "jre/bin/" + javaBinary()),
                new File(baseDir, "jre/jre/bin/" + javaBinary()),
                new File(baseDir, "launch/jre/bin/" + javaBinary()));
        if (bundled != null) return bundled;
        File downloaded = downloadedJava(dataDir);
        if (downloaded != null) return downloaded;

        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            File current = new File(javaHome, "bin/" + javaBinary());
            if (current.isFile() && isJava8(current) && hasJavaFx(current)) return current;
        }
        String envHome = System.getenv("JAVA_HOME");
        if (envHome != null) {
            File fromEnv = new File(envHome, "bin/" + javaBinary());
            if (fromEnv.isFile() && isJava8(fromEnv) && hasJavaFx(fromEnv)) return fromEnv;
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String entry : path.split(File.pathSeparator)) {
                File candidate = new File(entry, javaBinary());
                if (candidate.isFile() && isJava8(candidate) && hasJavaFx(candidate)) return candidate;
            }
        }
        return null;
    }

    /** The PandaAlts shop runs in a JavaFX WebView, so a system Java 8 without JavaFX is not good enough. */
    private static boolean hasJavaFx(File java) {
        File bin = java.getParentFile();
        File home = bin == null ? null : bin.getParentFile();
        if (home == null) return false;
        return new File(home, "lib/ext/jfxrt.jar").isFile() || new File(home, "jre/lib/ext/jfxrt.jar").isFile();
    }

    private static boolean isJava8(File java) {
        try {
            Process process = new ProcessBuilder(java.getAbsolutePath(), "-version").redirectErrorStream(true).start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) output.append(line).append('\n');
            }
            process.waitFor();
            return output.toString().contains("version \"1.8");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isOnPath(String binary) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String entry : path.split(File.pathSeparator)) {
            if (new File(entry, binary).isFile()) return true;
        }
        return false;
    }

    private static String javaBinary() {
        return WINDOWS ? "java.exe" : "java";
    }

    private static File firstFile(File... candidates) {
        for (File candidate : candidates) if (candidate.isFile()) return candidate;
        return null;
    }

    private static File firstDir(File... candidates) {
        for (File candidate : candidates) if (candidate != null && candidate.isDirectory()) return candidate;
        return null;
    }

    private static File defaultGameDir() {
        if (WINDOWS) {
            String appData = System.getenv("APPDATA");
            return new File(appData != null ? appData : System.getProperty("user.home"), ".minecraft");
        }
        if (MAC) return new File(System.getProperty("user.home"), "Library/Application Support/minecraft");
        return new File(System.getProperty("user.home"), ".minecraft");
    }

    

    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES = "https://resources.download.minecraft.net/";
    private static final String MINECRAFT_VERSION = "1.8.9";
    private static final String ASSET_INDEX = "1.8";
    private static final int DOWNLOAD_THREADS = 5;
    private static final String USER_AGENT = "KineticLauncher/1.0";

    
    interface ProgressSink {
        void report(String text, double fraction);
    }

    static final class AssetObject {
        final String hash;
        final long size;

        AssetObject(String hash, long size) {
            this.hash = hash;
            this.size = size;
        }

        String path() {
            return hash.substring(0, 2) + "/" + hash;
        }
    }

    

    private void prepareDownloadsThenLaunch(boolean needAssets, boolean needJava) {
        if (verifying || isRunning()) return;
        verifying = true;
        refreshPlayButton();
        verifyButton.setEnabled(false);
        Thread thread = new Thread(() -> {
            String failure = null;
            try {
                if (needJava) {
                    appendLog("[Kinetic] No Java 8 with JavaFX found - downloading Azul Zulu 8 (FX) to " + new File(dataDir, "jre").getAbsolutePath());
                    ensureJava(dataDir, this::progress);
                    appendLog("[Kinetic] Java 8 ready");
                }
                if (needAssets) {
                    File assets = new File(dataDir, "assets");
                    appendLog("[Kinetic] Assets not found next to the launcher - downloading to " + assets.getAbsolutePath());
                    int count = ensureAssets(assets, true, this::progress);
                    appendLog("[Kinetic] Assets ready (" + count + " files downloaded)");
                }
            } catch (Exception e) {
                appendLog("[Kinetic] Download failed: " + e);
                failure = "Download failed: " + describe(e);
            }
            String problem = failure;
            verifying = false;
            SwingUtilities.invokeLater(() -> {
                statusLabel.hideProgress();
                verifyButton.setEnabled(true);
                refreshFilesLabel();
                refreshPlayButton();
                if (problem != null) {
                    setStatus(problem, true);
                } else {
                    launch();
                }
            });
        }, "Kinetic Downloads");
        thread.setDaemon(true);
        thread.start();
    }

    // Zulu FX: Java 8 with JavaFX (Corretto 8 no longer bundles it), needed for the PandaAlts WebView
    private static final String JRE_VERSION = "zulu8.96.0.205-ca-fx-jre8.0.504";

    private static File downloadedJava(File dataDir) {
        File jre = new File(dataDir, "jre");
        File marker = new File(jre, ".kinetic-jre");
        if (!marker.isFile()) return null;
        try {
            // older launchers downloaded Corretto without JavaFX, replace it
            if (!JRE_VERSION.equals(new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim())) return null;
        } catch (IOException e) {
            return null;
        }
        return firstFile(new File(jre, "bin/" + javaBinary()), new File(jre, "jre/bin/" + javaBinary()),
                new File(jre, "Contents/Home/bin/" + javaBinary()), new File(jre, "Contents/Home/jre/bin/" + javaBinary()));
    }

    private static String jreUrl() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        String base = "https://cdn.azul.com/zulu/bin/" + JRE_VERSION;
        if (WINDOWS) return base + "-win_x64.zip";
        if (MAC) return base + (arm ? "-macosx_aarch64.tar.gz" : "-macosx_x64.tar.gz");
        return base + "-linux_x64.tar.gz";
    }

    static File ensureJava(File dataDir, ProgressSink sink) throws IOException {
        File existing = downloadedJava(dataDir);
        if (existing != null) return existing;
        String url = jreUrl();
        boolean zip = url.endsWith(".zip");
        File archive = new File(dataDir, "jre-download" + (zip ? ".zip" : ".tar.gz"));
        downloadFile(url, archive, "Java 8 (" + JRE_VERSION + ")", sink);
        File temp = new File(dataDir, "jre.tmp");
        File target = new File(dataDir, "jre");
        deleteTree(temp);
        if (sink != null) sink.report("Unpacking Java 8...", 0.0);
        if (zip) extractZipStripTop(archive, temp);
        else extractTarGzStripTop(archive, temp);
        deleteTree(target);
        if (!temp.renameTo(target)) throw new IOException("Cannot move " + temp + " to " + target);
        Files.write(new File(target, ".kinetic-jre").toPath(), JRE_VERSION.getBytes(StandardCharsets.UTF_8));
        boolean ignored = archive.delete();
        File java = downloadedJava(dataDir);
        if (java == null) throw new IOException("Java binary missing after unpacking " + url);
        if (!WINDOWS) {
            File bin = java.getParentFile();
            File[] tools = bin.listFiles();
            if (tools != null) for (File tool : tools) tool.setExecutable(true, false);
        }
        return java;
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        boolean ignored = file.delete();
    }

    private static String stripTop(String name) {
        while (name.startsWith("./")) name = name.substring(2);
        int slash = name.indexOf('/');
        if (slash < 0) return null;
        String rest = name.substring(slash + 1);
        return rest.isEmpty() ? null : rest;
    }

    private static File safeChild(File root, String relative) throws IOException {
        File file = new File(root, relative);
        if (!file.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator) && !file.getCanonicalPath().equals(root.getCanonicalPath())) {
            throw new IOException("Archive entry escapes target directory: " + relative);
        }
        return file;
    }

    private static void extractZipStripTop(File archive, File target) throws IOException {
        if (!target.isDirectory() && !target.mkdirs()) throw new IOException("Cannot create " + target);
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            byte[] chunk = new byte[64 * 1024];
            while ((entry = in.getNextEntry()) != null) {
                String relative = stripTop(entry.getName());
                if (relative == null) continue;
                File file = safeChild(target, relative);
                if (entry.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) throw new IOException("Cannot create " + file);
                    continue;
                }
                File parent = file.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                    int n;
                    while ((n = in.read(chunk)) != -1) out.write(chunk, 0, n);
                }
            }
        }
    }

    private static void extractTarGzStripTop(File archive, File target) throws IOException {
        if (!target.isDirectory() && !target.mkdirs()) throw new IOException("Cannot create " + target);
        try (InputStream in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(archive), 64 * 1024))) {
            byte[] header = new byte[512];
            byte[] chunk = new byte[64 * 1024];
            String pendingLongName = null;
            while (readFully(in, header, 512)) {
                boolean empty = true;
                for (byte b : header) if (b != 0) { empty = false; break; }
                if (empty) continue;
                String name = tarString(header, 0, 100);
                int mode = (int) tarNumber(header, 100, 8);
                long size = tarNumber(header, 124, 12);
                char type = (char) header[156];
                String link = tarString(header, 157, 100);
                String magic = tarString(header, 257, 6);
                String prefix = magic.startsWith("ustar") ? tarString(header, 345, 155) : "";
                if (!prefix.isEmpty()) name = prefix + "/" + name;
                if (pendingLongName != null) { name = pendingLongName; pendingLongName = null; }
                long padded = (size + 511) / 512 * 512;
                if (type == 'L') {
                    byte[] data = new byte[(int) size];
                    if (!readFully(in, data, (int) size)) throw new IOException("Truncated tar");
                    pendingLongName = new String(data, StandardCharsets.UTF_8).trim();
                    while (pendingLongName.endsWith("\u0000")) pendingLongName = pendingLongName.substring(0, pendingLongName.length() - 1);
                    skipFully(in, padded - size);
                    continue;
                }
                String relative = stripTop(name);
                if (relative == null) { skipFully(in, padded); continue; }
                File file = safeChild(target, relative);
                if (type == '5') {
                    if (!file.isDirectory() && !file.mkdirs()) throw new IOException("Cannot create " + file);
                    skipFully(in, padded);
                    continue;
                }
                File parent = file.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
                if (type == '2') {
                    try {
                        Files.deleteIfExists(file.toPath());
                        Files.createSymbolicLink(file.toPath(), new File(link).toPath());
                    } catch (IOException | UnsupportedOperationException ignored) {
                    }
                    skipFully(in, padded);
                    continue;
                }
                if (type != '0' && type != '\0' && type != '7') { skipFully(in, padded); continue; }
                long remaining = size;
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                    while (remaining > 0) {
                        int n = in.read(chunk, 0, (int) Math.min(chunk.length, remaining));
                        if (n < 0) throw new IOException("Truncated tar entry " + name);
                        out.write(chunk, 0, n);
                        remaining -= n;
                    }
                }
                skipFully(in, padded - size);
                if ((mode & 0111) != 0) file.setExecutable(true, false);
            }
        }
    }

    private static boolean readFully(InputStream in, byte[] buffer, int length) throws IOException {
        int read = 0;
        while (read < length) {
            int n = in.read(buffer, read, length - read);
            if (n < 0) return read == 0 ? false : fail("Truncated tar header");
            read += n;
        }
        return true;
    }

    private static boolean fail(String message) throws IOException {
        throw new IOException(message);
    }

    private static void skipFully(InputStream in, long count) throws IOException {
        while (count > 0) {
            long skipped = in.skip(count);
            if (skipped <= 0) {
                if (in.read() < 0) throw new IOException("Truncated tar");
                skipped = 1;
            }
            count -= skipped;
        }
    }

    private static String tarString(byte[] header, int offset, int length) {
        int end = offset;
        while (end < offset + length && header[end] != 0) end++;
        return new String(header, offset, end - offset, StandardCharsets.UTF_8);
    }

    private static long tarNumber(byte[] header, int offset, int length) {
        String text = tarString(header, offset, length).trim();
        if (text.isEmpty()) return 0L;
        if ((header[offset] & 0x80) != 0) { 
            long value = 0;
            for (int i = 1; i < length; i++) value = (value << 8) | (header[offset + i] & 0xFF);
            return value;
        }
        try {
            return Long.parseLong(text, 8);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void startAssetCheck(boolean refreshIndex) {
        if (verifying || isRunning()) return;
        verifying = true;
        refreshPlayButton();
        verifyButton.setEnabled(false);
        Thread thread = new Thread(() -> {
            String note;
            try {
                File assets = findAssets();
                boolean downloaded = assets == null || assets.equals(new File(dataDir, "assets"));
                if (assets == null) assets = new File(dataDir, "assets");
                int count = ensureAssets(assets, downloaded && refreshIndex, this::progress);
                appendLog("[Kinetic] Assets verified in " + assets.getAbsolutePath() + (count > 0 ? " (" + count + " files downloaded)" : ""));
                note = count > 0 ? count + " asset files downloaded" : "All assets are present";
            } catch (Exception e) {
                appendLog("[Kinetic] Asset check failed: " + e);
                note = "Asset check failed: " + describe(e);
            }
            String result = note;
            verifying = false;
            SwingUtilities.invokeLater(() -> {
                statusLabel.hideProgress();
                verifyButton.setEnabled(true);
                refreshFilesLabel();
                hero.display(selected);
                String problem = refreshPlayButton();
                setStatus(result, problem != null);
            });
        }, "Kinetic Asset Check");
        thread.setDaemon(true);
        thread.start();
    }

    private void progress(String text, double fraction) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.set(text, MUTED, ACCENT);
            statusLabel.setProgress(fraction);
        });
    }

    private static String describe(Exception e) {
        if (e instanceof UnknownHostException) return "no internet connection (" + e.getMessage() + ")";
        if (e instanceof java.net.ConnectException) return "connection refused (" + e.getMessage() + ")";
        if (e instanceof java.net.SocketTimeoutException) return "connection timed out";
        String message = e.getMessage();
        return message == null || message.isEmpty() ? e.getClass().getSimpleName() : message;
    }

    
    static String resolveAssetIndexUrl() throws IOException {
        Object manifest = Json.parse(fetchText(VERSION_MANIFEST));
        String versionUrl = null;
        List<Object> versions = Json.array(manifest, "versions");
        if (versions != null) {
            for (Object version : versions) {
                if (MINECRAFT_VERSION.equals(Json.string(version, "id"))) {
                    versionUrl = Json.string(version, "url");
                    break;
                }
            }
        }
        if (versionUrl == null) throw new IOException("Minecraft " + MINECRAFT_VERSION + " is not in Mojang's version manifest");
        Object version = Json.parse(fetchText(versionUrl));
        String url = Json.string(Json.object(version, "assetIndex"), "url");
        if (url == null) throw new IOException("Minecraft " + MINECRAFT_VERSION + " has no asset index");
        return url;
    }

    
    static List<AssetObject> parseAssetIndex(String json) {
        Map<String, Object> objects = Json.object(Json.parse(json), "objects");
        if (objects == null) throw new IllegalArgumentException("Asset index has no objects");
        Map<String, AssetObject> unique = new LinkedHashMap<>();
        for (Object entry : objects.values()) {
            String hash = Json.string(entry, "hash");
            if (hash == null || hash.length() < 2) continue;
            if (!unique.containsKey(hash)) unique.put(hash, new AssetObject(hash, Json.number(entry, "size", -1)));
        }
        return new ArrayList<>(unique.values());
    }

    

    static int ensureAssets(File assetsDir, boolean refreshIndex, ProgressSink sink) throws IOException {
        File index = new File(assetsDir, "indexes/" + ASSET_INDEX + ".json");
        if (refreshIndex || !index.isFile()) {
            sink.report("Fetching Minecraft " + MINECRAFT_VERSION + " metadata...", -1);
            try {
                downloadFile(resolveAssetIndexUrl(), index, "asset index", sink);
            } catch (IOException e) {
                if (!index.isFile()) throw e;
            }
        }
        List<AssetObject> objects = parseAssetIndex(new String(Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8));
        File objectsDir = new File(assetsDir, "objects");
        List<AssetObject> missing = new ArrayList<>();
        for (AssetObject object : objects) {
            File file = new File(objectsDir, object.path());
            if (!file.isFile() || object.size >= 0 && file.length() != object.size) missing.add(object);
        }
        if (missing.isEmpty()) return 0;

        int total = missing.size();
        sink.report("Downloading assets 0 / " + total, 0);
        AtomicInteger done = new AtomicInteger();
        ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();
        ExecutorService pool = Executors.newFixedThreadPool(DOWNLOAD_THREADS, runnable -> {
            Thread thread = new Thread(runnable, "Kinetic Asset Download");
            thread.setDaemon(true);
            return thread;
        });
        for (AssetObject object : missing) {
            pool.execute(() -> {
                File file = new File(objectsDir, object.path());
                IOException last = null;
                for (int attempt = 0; attempt < 3; attempt++) {
                    try {
                        downloadFile(RESOURCES + object.path(), file, null, null);
                        verifyAsset(file, object);
                        last = null;
                        break;
                    } catch (IOException e) {
                        file.delete();
                        last = e;
                    }
                }
                if (last != null) failures.add(object.hash + ": " + last.getMessage());
                int count = done.incrementAndGet();
                sink.report("Downloading assets " + count + " / " + total, count / (double) total);
            });
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(2, TimeUnit.HOURS)) throw new IOException("Asset download timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
            throw new IOException("Asset download interrupted");
        }
        if (!failures.isEmpty()) throw new IOException(failures.size() + " of " + total + " assets failed (" + failures.peek() + ") - press Verify assets to retry");
        return total;
    }

    
    private static void verifyAsset(File file, AssetObject object) throws IOException {
        if (object.size >= 0 && file.length() != object.size) {
            throw new IOException(object.hash + " has " + file.length() + " bytes, expected " + object.size);
        }
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] chunk = new byte[16 * 1024];
            int n;
            while ((n = in.read(chunk)) != -1) sha1.update(chunk, 0, n);
            if (!hex(sha1.digest()).equalsIgnoreCase(object.hash)) throw new IOException(object.hash + " failed its sha1 check");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        return out.toString();
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    static String fetchText(String url) throws IOException {
        HttpURLConnection connection = open(url);
        int code = connection.getResponseCode();
        if (code != 200) {
            connection.disconnect();
            throw new IOException("HTTP " + code + " from " + url);
        }
        try (InputStream in = connection.getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[16 * 1024];
            int n;
            while ((n = in.read(chunk)) != -1) buffer.write(chunk, 0, n);
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    
    static void downloadFile(String url, File target, String label, ProgressSink sink) throws IOException {
        HttpURLConnection connection = open(url);
        int code = connection.getResponseCode();
        if (code != 200) {
            connection.disconnect();
            throw new IOException("HTTP " + code + " from " + url);
        }
        File dir = target.getAbsoluteFile().getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) throw new IOException("Cannot create " + dir);
        File part = new File(dir, target.getName() + ".part");
        long total = connection.getContentLengthLong();
        long read = 0, lastReport = 0;
        boolean done = false;
        try {
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(part))) {
                byte[] chunk = new byte[64 * 1024];
                int n;
                while ((n = in.read(chunk)) != -1) {
                    out.write(chunk, 0, n);
                    read += n;
                    if (sink != null && label != null) {
                        long now = System.currentTimeMillis();
                        if (now - lastReport >= 150 || read == total) {
                            lastReport = now;
                            sink.report("Downloading " + label + "  " + megabytes(read) + (total > 0 ? " / " + megabytes(total) : "") + " MB",
                                    total > 0 ? Math.min(1, read / (double) total) : -1);
                        }
                    }
                }
            }
            if (total > 0 && read != total) {
                throw new IOException("Incomplete download of " + target.getName() + " (" + read + " of " + total + " bytes)");
            }
            Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            done = true;
        } finally {
            if (!done) part.delete();
        }
    }

    private static String megabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / (1024.0 * 1024.0));
    }

    

    private static final int[] ICON_SIZES = {16, 32, 64, 128};

    
    private static List<Image> kineticIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : ICON_SIZES) {
            try (InputStream in = KineticLauncher.class.getResourceAsStream("/secret/kinetic/launcher/icon_" + size + ".png")) {
                if (in != null) icons.add(javax.imageio.ImageIO.read(in));
            } catch (IOException ignored) {
            }
        }
        if (icons.isEmpty()) icons.addAll(Arrays.<Image>asList(badgeImage(16), badgeImage(32), badgeImage(64)));
        return icons;
    }

    
    private static void setX11AppClassName(String name) {
        if (WINDOWS || MAC) return;
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            java.lang.reflect.Field field = toolkit.getClass().getDeclaredField("awtAppClassName");
            field.setAccessible(true);
            field.set(toolkit, name);
        } catch (Exception ignored) {
        }
    }

    

    private void installDesktopEntry() {
        if (WINDOWS || MAC) return;
        try {
            File icon = new File(dataDir, "icon_128.png");
            try (InputStream in = KineticLauncher.class.getResourceAsStream("/secret/kinetic/launcher/icon_128.png")) {
                if (in == null) return;
                if (!dataDir.isDirectory()) dataDir.mkdirs();
                Files.copy(in, icon.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            String content = "[Desktop Entry]\n"
                    + "Type=Application\n"
                    + "Name=Kinetic Client\n"
                    + "Comment=Kinetic Client launcher\n"
                    + "Icon=" + icon.getAbsolutePath() + "\n"
                    + "Exec=" + desktopExec() + "\n"
                    + "Path=" + baseDir.getAbsolutePath() + "\n"
                    + "Terminal=false\n"
                    + "Categories=Game;\n"
                    + "StartupNotify=false\n"
                    + "StartupWMClass=" + WM_CLASS + "\n";
            File apps = new File(System.getProperty("user.home"), ".local/share/applications");
            if (!apps.isDirectory() && !apps.mkdirs()) return;
            File entry = new File(apps, "kinetic-client.desktop");
            String existing = entry.isFile() ? new String(Files.readAllBytes(entry.toPath()), StandardCharsets.UTF_8) : "";
            if (!content.equals(existing)) {
                Files.write(entry.toPath(), content.getBytes(StandardCharsets.UTF_8));
                appendLog("[Kinetic] Desktop entry written to " + entry.getAbsolutePath());
            }
        } catch (IOException | RuntimeException e) {
            appendLog("[Kinetic] Could not install the desktop entry: " + e);
        }
    }

    
    private static String desktopExec() {
        File java = new File(System.getProperty("java.home"), "bin/java");
        return quoteExec(java.getAbsolutePath()) + " -jar " + quoteExec(launcherJar().getAbsolutePath());
    }

    
    private static File launcherJar() {
        try {
            return new File(KineticLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | RuntimeException e) {
            return new File(locateLauncherDir(), "Kinetic.jar");
        }
    }

    
    private static String quoteExec(String arg) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : arg.toCharArray()) {
            if (c == '"' || c == '`' || c == '$' || c == '\\') out.append('\\');
            if (c == '%') out.append('%');
            out.append(c);
        }
        return out.append('"').toString();
    }

    

    private File settingsFile() {
        return new File(baseDir, "launcher.properties");
    }

    private void loadSettings() {
        File file = settingsFile();
        if (!file.isFile()) return;
        try (FileInputStream in = new FileInputStream(file)) {
            settings.load(in);
        } catch (IOException ignored) {
        }
    }

    private void saveSettings() {
        settings.setProperty("gameDir", gameDirField.getText().trim());
        settings.setProperty("java", javaField.getText().trim());
        settings.setProperty("ramGb", String.valueOf(ramSlider.getValue()));
        settings.setProperty("closeAfterLaunch", String.valueOf(closeAfterLaunch.isSelected()));
        storeClients();
        try (OutputStream out = new FileOutputStream(settingsFile())) {
            settings.store(out, "Kinetic Launcher");
        } catch (IOException e) {
            appendLog("[Kinetic] Could not save settings: " + e.getMessage());
        }
    }

    private static long totalMemoryGb() {
        try {
            java.lang.management.OperatingSystemMXBean bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            java.lang.reflect.Method method = bean.getClass().getMethod("getTotalPhysicalMemorySize");
            method.setAccessible(true);
            long bytes = (Long) method.invoke(bean);
            return Math.max(2, bytes / (1024L * 1024L * 1024L));
        } catch (Exception e) {
            return 8;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    

    private static final Map<String, BufferedImage> ICON_CACHE = new java.util.HashMap<>();
    private static final BufferedImage ICON_SOURCE = loadImage("icon_128.png");
    private static final BufferedImage ISRAEL_AVATAR = loadImage("israel_avatar.png");
    private static final BufferedImage CHRISTIAN_AVATAR = loadImage("christian_avatar.png");

    
    private static BufferedImage kineticIcon(int size) {
        BufferedImage source = israel() && ISRAEL_AVATAR != null ? ISRAEL_AVATAR
                : christian() && CHRISTIAN_AVATAR != null ? CHRISTIAN_AVATAR : ICON_SOURCE;
        if (source == null) return null;
        String key = (source == ISRAEL_AVATAR ? "israel:" : source == CHRISTIAN_AVATAR ? "christian:" : "kinetic:") + size;
        BufferedImage cached = ICON_CACHE.get(key);
        if (cached == null) {
            cached = Ui.crisp(source, size);
            ICON_CACHE.put(key, cached);
        }
        return cached;
    }

    
    private static List<Image> themeIcons() {
        if ((!israel() && !christian()) || (israel() && ISRAEL_AVATAR == null) || (christian() && CHRISTIAN_AVATAR == null)) return kineticIcons();
        List<Image> icons = new ArrayList<>();
        for (int size : ICON_SIZES) icons.add(kineticIcon(size));
        return icons;
    }

    private static void paintBadge(Graphics2D g2, String letter, Color color, int x, int y, int size) {
        float arc = size * 0.36f;
        RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, size, size, arc, arc);
        g2.setPaint(new GradientPaint(x, y, color, x + size, y + size, color.darker().darker()));
        g2.fill(shape);
        g2.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 70), x, y + size, new Color(255, 255, 255, 10)));
        g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, size - 1, size - 1, arc, arc));
        g2.setFont(semibold(size * 0.46f));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(letter, x + (size - fm.stringWidth(letter)) / 2f, y + (size - fm.getHeight()) / 2f + fm.getAscent());
    }

    
    private static void paintClientBadge(Graphics2D g2, Client client, int x, int y, int size) {
        BufferedImage icon = client.builtin ? kineticIcon(size) : null;
        if (icon == null) {
            paintBadge(g2, client.initial(), client.color(), x, y, size);
            return;
        }
        g2.drawImage(icon, x, y, null);
    }

    private static Image badgeImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = smooth(image.getGraphics());
        paintBadge(g2, "K", ACCENT, 0, 0, size - 1);
        g2.dispose();
        return image;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    
    private static String twoLines(String main, String sub) {
        return "<html><font color='" + htmlColor(TEXT) + "'>" + escapeHtml(main) + "</font><br><font size='-1' face='" + FAMILY + "' color='" + htmlColor(DIM) + "'>"
                + escapeHtml(sub) + "</font></html>";
    }

    private static String htmlColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    

    
    private JDialog dialog(String title) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                int w = getWidth(), h = getHeight();
                g2.setColor(mix(DIALOG, BASE, 0.3f));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRect(0, 0, w - 1, h - 1);
                g2.dispose();
            }
        };
        content.setBorder(new EmptyBorder(22, 24, 18, 24));
        dialog.setContentPane(content);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        MouseAdapter drag = new MouseAdapter() {
            private Point grab, origin;

            @Override
            public void mousePressed(MouseEvent e) {
                grab = e.getLocationOnScreen();
                origin = dialog.getLocation();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (grab == null) return;
                Point now = e.getLocationOnScreen();
                dialog.setLocation(origin.x + now.x - grab.x, origin.y + now.y - grab.y);
            }
        };
        content.addMouseListener(drag);
        content.addMouseMotionListener(drag);
        return dialog;
    }

    
    private final class HeroPanel extends JPanel {
        private static final float MARK = 108f;
        private Client client;
        private final JComponent mark;
        private final JLabel description;

        HeroPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            mark = new JComponent() {
                @Override
                public Dimension getPreferredSize() {
                    if (client == null) return new Dimension(10, 10);
                    if (client.builtin) {
                        FontMetrics fm = getFontMetrics(wordmark(MARK));
                        return new Dimension(fm.stringWidth("KINETIC") + (int) (MARK * 0.12f) + (israel() ? 70 : 0), (int) (MARK * 0.78f));
                    }
                    FontMetrics fm = getFontMetrics(Ui.display(58f));
                    return new Dimension(fm.stringWidth(client.name) + 8, (int) (58f * 1.1f));
                }

                @Override
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }

                @Override
                protected void paintComponent(Graphics g) {
                    if (client == null) return;
                    Graphics2D g2 = smooth(g);
                    g2.setColor(TEXT);
                    if (client.builtin) {
                        g2.setFont(wordmark(MARK));
                        float baseline = MARK * 0.74f;
                        g2.drawString("KINETIC", -MARK * 0.02f, baseline);
                        if (israel() || christian()) {
                            float x = g2.getFontMetrics().stringWidth("KINETIC") + MARK * 0.18f;
                            if (christian()) paintCross(g2, x + 22, baseline - MARK * 0.36f, 24f, 3f, TEXT);
                            else paintHexagram(g2, x + 22, baseline - MARK * 0.36f, 24f, 3f, TEXT);
                        }
                    } else {
                        g2.setFont(Ui.display(58f));
                        g2.drawString(client.name, 0, 58f * 0.9f);
                    }
                    g2.dispose();
                }
            };
            mark.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(mark);
            add(Box.createVerticalStrut(6));
            description = label(" ", MUTED, font(Font.PLAIN, 15f));
            description.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(description);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        void display(Client client) {
            this.client = client;
            if (client.builtin) {
                File jar = findClientJar();
                description.setText("Minecraft 1.8.9  \u00b7  " + (jar != null ? "Ready to play" : "Kinetic.jar not found"));
                description.setToolTipText(jar != null ? jar.getAbsolutePath() : "Put Kinetic.jar into the client/ folder next to the launcher");
            } else {
                String main = DEFAULT_MAIN_CLASS.equals(client.mainClass) ? "" : "  \u00b7  " + client.mainClass;
                description.setText((client.version.isEmpty() ? "" : "Minecraft " + client.version + "  \u00b7  ") + new File(client.jar).getName() + main);
                description.setToolTipText(client.jar);
            }
            mark.revalidate();
            mark.repaint();
            if (switcher != null) {
                switcher.revalidate();
                switcher.repaint();
            }
            revalidate();
            repaint();
        }
    }

    
    private final class ClientSwitcher extends JComponent {
        private final Fader hover = new Fader(this, 0.22f, 0);

        ClientSwitcher() {
            setCursor(HAND);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setToolTipText("Switch or add a client");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover.to(1f);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover.to(0f);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) showClientPopup(ClientSwitcher.this);
                }
            });
        }

        private String name() {
            return selected == null ? "No client" : selected.name;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(8 + 22 + 10 + getFontMetrics(semibold(13f)).stringWidth(name()) + 10 + 10 + 12, 36);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            float hv = hover.get();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h);
            g2.setColor(new Color(255, 255, 255, (int) (10 + 10 * hv)));
            g2.fill(shape);
            g2.setColor(new Color(255, 255, 255, (int) (18 + 22 * hv)));
            g2.draw(shape);
            if (selected != null) paintClientBadge(g2, selected, 8, (h - 22) / 2, 22);
            g2.setFont(semibold(13f));
            FontMetrics fm = g2.getFontMetrics();
            int tx = 8 + 22 + 10;
            g2.setColor(TEXT);
            g2.drawString(name(), tx, (h - fm.getHeight()) / 2f + fm.getAscent());
            float cx = tx + fm.stringWidth(name()) + 14, cy = h / 2f;
            g2.setColor(mix(MUTED, TEXT, hv));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            java.awt.geom.Path2D chevron = new java.awt.geom.Path2D.Float();
            chevron.moveTo(cx - 4, cy - 2);
            chevron.lineTo(cx, cy + 2);
            chevron.lineTo(cx + 4, cy - 2);
            g2.draw(chevron);
            g2.dispose();
        }
    }

    
    private final class ClientItem extends JComponent {
        private final Client client;
        private final Fader hover = new Fader(this, 0.22f, 0);
        private boolean hoverRemove;

        ClientItem(Client client) {
            this.client = client;
            setPreferredSize(new Dimension(268, 50));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(HAND);
            setToolTipText(client.builtin ? "Bundled Kinetic client" : client.jar);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover.to(1f);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover.to(0f);
                    hoverRemove = false;
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    boolean over = !client.builtin && removeBounds().contains(e.getPoint());
                    if (over != hoverRemove) {
                        hoverRemove = over;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e) || !contains(e.getPoint())) return;
                    if (clientPopup != null) clientPopup.setVisible(false);
                    if (!client.builtin && removeBounds().contains(e.getPoint())) removeClient(client);
                    else if (selected != client) select(client, true);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private Rectangle removeBounds() {
            return new Rectangle(getWidth() - 32, (getHeight() - 22) / 2, 22, 22);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            float hv = hover.get();
            boolean active = selected == client;
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w, h, 12, 12);
            if (active || hv > 0) {
                g2.setColor(new Color(255, 255, 255, (int) ((active ? 16 : 0) + 12 * hv)));
                g2.fill(shape);
            }
            int badgeSize = 30, bx = 10, by = (h - badgeSize) / 2;
            paintClientBadge(g2, client, bx, by, badgeSize);
            if (runningClient == client) {
                g2.setColor(SHADE);
                g2.fill(new Ellipse2D.Float(bx + badgeSize - 9, by + badgeSize - 9, 12, 12));
                g2.setColor(SUCCESS);
                g2.fill(new Ellipse2D.Float(bx + badgeSize - 7, by + badgeSize - 7, 8, 8));
            }
            boolean showRemove = !client.builtin && hv > 0.5f;
            int textX = bx + badgeSize + 12;
            int textMax = w - textX - (showRemove ? 38 : 34);
            g2.setFont(semibold(13f));
            FontMetrics nameFm = g2.getFontMetrics();
            g2.setColor(TEXT);
            g2.drawString(ellipsize(client.name, nameFm, textMax), textX, h / 2f - 2);
            g2.setFont(font(Font.PLAIN, 11.5f));
            FontMetrics subFm = g2.getFontMetrics();
            g2.setColor(MUTED);
            g2.drawString(ellipsize(client.subtitle(), subFm, textMax), textX, h / 2f + subFm.getAscent());
            if (active && !showRemove) Glyph.CHECK.paint(g2, w - 30, (h - 14) / 2f, 14, ACCENT_LIGHT);
            if (showRemove) {
                Rectangle r = removeBounds();
                g2.setColor(hoverRemove ? alpha(DANGER, 220) : new Color(255, 255, 255, 22));
                g2.fill(new Ellipse2D.Float(r.x, r.y, r.width, r.height));
                g2.setColor(hoverRemove ? Color.WHITE : MUTED);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = r.x + r.width / 2, cy = r.y + r.height / 2;
                g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
            }
            g2.dispose();
        }
    }

    

    
    static final class Json {
        private final String text;
        private int pos;

        private Json(String text) {
            this.text = text;
        }

        static Object parse(String text) {
            Json parser = new Json(text);
            Object value = parser.value();
            parser.whitespace();
            if (parser.pos != text.length()) throw parser.error("Trailing data");
            return value;
        }

        @SuppressWarnings("unchecked")
        private static Object field(Object container, String key) {
            return container instanceof Map ? ((Map<String, Object>) container).get(key) : null;
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> object(Object container, String key) {
            Object value = field(container, key);
            return value instanceof Map ? (Map<String, Object>) value : null;
        }

        @SuppressWarnings("unchecked")
        static List<Object> array(Object container, String key) {
            Object value = field(container, key);
            return value instanceof List ? (List<Object>) value : null;
        }

        static String string(Object container, String key) {
            Object value = field(container, key);
            return value instanceof String ? (String) value : null;
        }

        static long number(Object container, String key, long fallback) {
            Object value = field(container, key);
            return value instanceof Number ? ((Number) value).longValue() : fallback;
        }

        private Object value() {
            whitespace();
            switch (peek()) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': literal("true"); return Boolean.TRUE;
                case 'f': literal("false"); return Boolean.FALSE;
                case 'n': literal("null"); return null;
                default: return number();
            }
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            whitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                whitespace();
                if (peek() != '"') throw error("Expected a string key");
                String key = string();
                whitespace();
                expect(':');
                map.put(key, value());
                whitespace();
                char c = next();
                if (c == '}') return map;
                if (c != ',') throw error("Expected , or }");
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            pos++;
            whitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(value());
                whitespace();
                char c = next();
                if (c == ']') return list;
                if (c != ',') throw error("Expected , or ]");
            }
        }

        private String string() {
            pos++;
            StringBuilder out = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') return out.toString();
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                char escaped = next();
                switch (escaped) {
                    case '"': case '\\': case '/': out.append(escaped); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'u':
                        if (pos + 4 > text.length()) throw error("Bad unicode escape");
                        try {
                            out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException e) {
                            throw error("Bad unicode escape");
                        }
                        pos += 4;
                        break;
                    default: throw error("Bad escape \\" + escaped);
                }
            }
        }

        private Number number() {
            int start = pos;
            while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) pos++;
            if (start == pos) throw error("Unexpected character");
            String token = text.substring(start, pos);
            try {
                if (token.matches("-?\\d{1,18}")) return Long.parseLong(token);
                return Double.parseDouble(token);
            } catch (NumberFormatException e) {
                throw error("Bad number " + token);
            }
        }

        private void whitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
        }

        private char peek() {
            if (pos >= text.length()) throw error("Unexpected end of JSON");
            return text.charAt(pos);
        }

        private char next() {
            char c = peek();
            pos++;
            return c;
        }

        private void expect(char c) {
            if (next() != c) throw error("Expected " + c);
        }

        private void literal(String word) {
            if (!text.startsWith(word, pos)) throw error("Unexpected token");
            pos += word.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException("Invalid JSON: " + message + " at offset " + pos);
        }
    }
}
