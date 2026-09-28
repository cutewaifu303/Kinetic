package secret.kinetic.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;






public final class PlayerHeads {

    public static final ResourceLocation PLACEHOLDER = new ResourceLocation("kinetic/gui/steve.png");
    private static final int MAX_TRIES = 4;

    private static final Map<String, ResourceLocation> heads = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> loading = new ConcurrentHashMap<>();
    private static final Map<String, Integer> tries = new ConcurrentHashMap<>();

    private PlayerHeads() {
    }

    
    public static ResourceLocation get(String nameOrUuid) {
        if (nameOrUuid == null || nameOrUuid.trim().isEmpty()) return PLACEHOLDER;
        String key = nameOrUuid.trim().toLowerCase(Locale.ROOT).replace("-", "");
        if (!key.matches("[a-z0-9_]{1,36}")) return PLACEHOLDER;
        ResourceLocation head = heads.get(key);
        if (head != null) return head;
        if (!loading.containsKey(key) && tries.getOrDefault(key, 0) < MAX_TRIES) load(key);
        return PLACEHOLDER;
    }

    
    public static ResourceLocation current() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.getSession() == null ? PLACEHOLDER : get(mc.getSession().getUsername());
    }

    public static void draw(ResourceLocation head, float x, float y, float size, float radius) {
        RoundedUtils.drawRoundedImage(head, x, y, size, size, radius);
    }

    private static void load(String key) {
        loading.put(key, true);
        tries.merge(key, 1, Integer::sum);
        Thread thread = new Thread(() -> {
            try {
                URLConnection connection = URI.create("https://mc-heads.net/avatar/" + key + "/64").toURL().openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Accept", "image/png");
                BufferedImage image = ImageIO.read(connection.getInputStream());
                if (image == null) throw new IOException("not an image");
                Minecraft mc = Minecraft.getMinecraft();
                mc.addScheduledTask(() -> {
                    ResourceLocation location = mc.getTextureManager().getDynamicTextureLocation("kinetic-head-" + key, new DynamicTexture(image));
                    heads.put(key, location);
                    loading.remove(key);
                });
            } catch (IOException | RuntimeException e) {
                loading.remove(key);
            }
        }, "Kinetic-Head");
        thread.setDaemon(true);
        thread.start();
    }
}
