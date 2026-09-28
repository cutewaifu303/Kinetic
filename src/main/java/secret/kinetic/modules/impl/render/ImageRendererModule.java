package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Shader2DEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.RenderUtils.GifTexture;
import secret.kinetic.utils.render.KineticImage;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@ModuleInfo(label = "Image Renderer", description = "Shows Kinetic (or a custom image or GIF) on screen", category = ModuleCategory.RENDER)
public final class ImageRendererModule extends Module {

    
    public enum Images {
        KINETIC("Theme", null, KineticImage.LOGO_ASPECT, false),
        MARIN("Marin Kitagawa", new ResourceLocation("kinetic/images/marin.png"), 640f / 800f, true),
        ICHIKA("Ichika", new ResourceLocation("kinetic/images/ichika.png"), 640f / 800f, true),
        NINO("Nino", new ResourceLocation("kinetic/images/nino.png"), 640f / 800f, true),
        MIKU("Miku", new ResourceLocation("kinetic/images/miku.png"), 640f / 800f, true),
        YOTSUBA("Yotsuba", new ResourceLocation("kinetic/images/yotsuba.png"), 640f / 800f, true),
        ITSUKI("Itsuki", new ResourceLocation("kinetic/images/itsuki.png"), 640f / 800f, true),
        SAHUR("Sahur", KineticImage.ISRAEL_LOGO, 640f / 800f, true),
        CUSTOM("Custom", null, 1f, false);

        public final String name;
        public final ResourceLocation texture;
        
        public final float aspect;
        
        public final boolean plain;

        Images(String name, ResourceLocation texture, float aspect, boolean plain) {
            this.name = name;
            this.texture = texture;
            this.aspect = aspect;
            this.plain = plain;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final ModeProperty<Images> image = new ModeProperty<>("Image", Images.KINETIC);
    public final Property<String> customUrl = new Property<>("URL", "https://i.imgur.com/example.gif", () -> image.getValue() == Images.CUSTOM);
    public static NumberProperty size = new NumberProperty("Size", 200, 100, 1000, 50);

    public static final ImageRendererModule INSTANCE = new ImageRendererModule();

    private final DragUtils.DraggableComponent draggable = new DragUtils.DraggableComponent(100, 100);

    private String lastLoadedUrl = "";
    private ResourceLocation customTextureLocation = null;
    private GifTexture customGifTexture = null;
    private DynamicTexture dynamicTexture = null;
    private boolean isLoading = false;

    public ImageRendererModule() {
        DragUtils.registerComponent("ImageRenderer", draggable);
    }

    @EventHook
    public void onRender(Render2DEvent event) {
        render();
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        if (event.getShaderType() == Shader2DEvent.ShaderType.BLUR) return;
        render();
    }

    private void render() {
        Images selectedMode = image.getValue();
        int height = size.getValue().intValue();
        int width = Math.round(height * selectedMode.aspect);
        draggable.setWidth(width);
        draggable.setHeight(height);

        int renderX = (int) draggable.getX();
        int renderY = (int) draggable.getY();

        if (selectedMode == Images.CUSTOM) {
            updateCustomTexture(customUrl.getValue());

            if (customGifTexture != null && customGifTexture.getFrameCount() > 0) {
                RoundedUtils.drawRoundedGif(customGifTexture, renderX, renderY, width, height, 6f);
            } else if (customTextureLocation != null) {
                RoundedUtils.drawRoundedImage(customTextureLocation, renderX, renderY, width, height, 6f);
            }
        } else {
            if (selectedMode == Images.KINETIC) {
                
                KineticImage.drawLogo(renderX, renderY, width, height, 1f);
            } else if (selectedMode.plain) {
                KineticImage.drawPlain(selectedMode.texture, renderX, renderY, width, height, 1f);
            } else {
                KineticImage.draw(selectedMode.texture, renderX, renderY, width, height, 1f);
            }
        }
    }

    private void updateCustomTexture(String urlString) {
        if (urlString == null || urlString.trim().isEmpty() || urlString.equals(lastLoadedUrl) || isLoading) {
            return;
        }

        lastLoadedUrl = urlString;
        isLoading = true;

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream is = connection.getInputStream()) {
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                }
                byte[] bytes = buffer.toByteArray();

                boolean isGif = bytes.length >= 4 &&
                        bytes[0] == 0x47 && bytes[1] == 0x49 &&
                        bytes[2] == 0x46 && bytes[3] == 0x38;

                if (isGif) {
                    GifTexture gif = new GifTexture(new ByteArrayInputStream(bytes));
                    if (gif.getFrameCount() > 0) {
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            cleanupTextures();
                            customGifTexture = gif;
                            isLoading = false;
                        });
                        return;
                    }
                }

                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bufferedImage != null) {
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        cleanupTextures();
                        dynamicTexture = new DynamicTexture(bufferedImage);
                        customTextureLocation = Minecraft.getMinecraft().getTextureManager()
                                .getDynamicTextureLocation("custom_image_renderer", dynamicTexture);
                        isLoading = false;
                    });
                } else {
                    isLoading = false;
                }

            } catch (Exception e) {
                isLoading = false;
            }
        });
    }

    private void cleanupTextures() {
        if (dynamicTexture != null) {
            dynamicTexture.deleteGlTexture();
            dynamicTexture = null;
        }
        customTextureLocation = null;
        if (customGifTexture != null) {
            customGifTexture.clear();
            customGifTexture = null;
        }
    }
}