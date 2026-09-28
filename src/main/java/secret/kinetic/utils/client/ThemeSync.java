package secret.kinetic.utils.client;

import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.impl.render.ClickGUIModule;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;







public final class ThemeSync {

    private static final long CHECK_MS = 1000L;
    private static long lastCheck;
    private static String lastWritten;

    private ThemeSync() {
    }

    
    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < CHECK_MS) return;
        lastCheck = now;

        ClickGUIModule.Color mode = ClickGUIModule.color == null ? null : ClickGUIModule.color.getValue();
        if (mode == null) return;
        Color[] pair = ColorManager.presetColors(mode);
        if (pair == null) pair = new Color[]{ColorManager.DEFAULT_FIRST, ColorManager.DEFAULT_SECOND};
        String content = "preset=" + mode.name() + "\naccent=" + hex(pair[0]) + "\naccent2=" + hex(pair[1]) + "\n";
        if (content.equals(lastWritten)) return;
        lastWritten = content;

        File dir = launcherDir();
        if (dir == null || !dir.isDirectory()) return;
        Thread thread = new Thread(() -> write(new File(dir, "theme.properties"), content), "Kinetic Theme Sync");
        thread.setDaemon(true);
        thread.start();
    }

    private static void write(File target, String content) {
        try {
            File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
            Files.write(tmp.toPath(), content.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException ignored) {
            
        }
    }

    private static String hex(Color color) {
        return String.format(Locale.ROOT, "#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    
    private static File launcherDir() {
        String env = System.getenv("KINETIC_LAUNCHER_DIR");
        if (env != null && !env.trim().isEmpty()) return new File(env.trim());
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            return new File(local != null ? local : home + "\\AppData\\Local", "KineticClient");
        }
        if (os.contains("mac")) return new File(home, "Library/Application Support/KineticClient");
        String xdg = System.getenv("XDG_DATA_HOME");
        return new File(xdg != null && !xdg.isEmpty() ? xdg : home + "/.local/share", "KineticClient");
    }
}
