package secret.kinetic.api.gui.alt;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.alt.comp.FileDialogs;
import secret.kinetic.api.gui.alt.comp.SkinChanger;
import secret.kinetic.api.gui.alt.comp.SkinChanger.Profile;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import static secret.kinetic.api.gui.alt.KineticAltMenu.*;

final class SkinTab extends AltTab {

    private static final int PREVIEW_SCALE = 5;

    private File chosenFile;
    private String variant = SkinChanger.VARIANT_CLASSIC;
    private ResourceLocation chosenTexture, currentTexture;
    private boolean chosenIs64, currentIs64;
    private String currentVariant = SkinChanger.VARIANT_CLASSIC;
    private String loadedForToken = "";

    private String status = "Pick a PNG skin file";
    private boolean statusIsError;
    private volatile boolean busy;

    private int panelX, panelY, panelWidth, panelHeight;
    private int previewX, previewY, previewWidth, previewHeight;
    private Rect chooseButton = new Rect(), uploadButton = new Rect(), resetButton = new Rect();
    private int classicX, slimX, variantY, variantWidth;
    private int fileY, statusY, tipsY;

    SkinTab(KineticAltMenu menu) {
        super(menu);
    }

    @Override
    String title() {
        return "Skins";
    }

    @Override
    void onShow() {
        loadCurrentSkin();
    }

    @Override
    void layout(int x, int y, int w, int h) {
        super.layout(x, y, w, h);

        panelX = x;
        panelY = y;
        panelWidth = (int) (menu.width * ADD_PANEL_RATIO);
        panelHeight = h;

        previewX = panelX + panelWidth + PADDING;
        previewY = y;
        previewWidth = x + w - previewX;
        previewHeight = h;

        int fontHeight = FontUtils.getFont("sf", 18).getHeight();
        int startY = panelY + PADDING + fontHeight + PADDING + 6;

        int right = panelX + panelWidth - PADDING;
        int innerWidth = panelWidth - PADDING * 2;

        
        chooseButton = rowRight(right, startY, BUTTON_HEIGHT, "Choose PNG file")[0];
        fileY = startY + (BUTTON_HEIGHT - fontHeight) / 2;

        variantY = chooseButton.y + BUTTON_HEIGHT + PADDING;
        variantWidth = (innerWidth - BUTTON_SPACING) / 2;
        classicX = panelX + PADDING;
        slimX = classicX + variantWidth + BUTTON_SPACING;

        Rect[] actions = rowRight(right, variantY + BUTTON_HEIGHT + PADDING, BUTTON_HEIGHT, "Reset to default", busy ? "Working..." : "Upload skin");
        resetButton = actions[0];
        uploadButton = actions[1];

        statusY = uploadButton.y + BUTTON_HEIGHT + PADDING + 10;
        tipsY = statusY + fontHeight + PADDING * 2;
    }

    @Override
    void draw(int mouseX, int mouseY) {
        drawControlPanel(mouseX, mouseY);
        drawPreviewPanel();
    }

    private void drawControlPanel(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);

        menu.drawPanel(panelX, panelY, panelWidth, panelHeight);
        menu.drawSectionHeader(panelX + PADDING, panelY + PADDING, "Skin", new ChatComponentText(" changer").getFormattedText());

        menu.drawButton(chooseButton, "Choose PNG file", mouseX, mouseY, chosenFile == null, !busy);

        String fileLabel = chosenFile == null ? "No file selected" : chosenFile.getName();
        int maxWidth = chooseButton.x - BUTTON_SPACING - (panelX + PADDING);
        while (regular.getStringWidth(fileLabel) > maxWidth && fileLabel.length() > 4) fileLabel = fileLabel.substring(0, fileLabel.length() - 4) + "...";
        regular.drawStringWithShadow(fileLabel, panelX + PADDING, fileY, chosenFile == null ? 0x999999 : Color.WHITE.getRGB());

        drawToggle(classicX, variantY, variantWidth, "Classic", SkinChanger.VARIANT_CLASSIC.equals(variant), mouseX, mouseY);
        drawToggle(slimX, variantY, variantWidth, "Slim", SkinChanger.VARIANT_SLIM.equals(variant), mouseX, mouseY);

        boolean premium = SkinChanger.isPremiumToken(mc.getSession().getToken());
        menu.drawButton(resetButton, "Reset to default", mouseX, mouseY, false, premium && !busy);
        menu.drawButton(uploadButton, busy ? "Working..." : "Upload skin", mouseX, mouseY, true, chosenFile != null && premium && !busy);

        menu.drawStatusPill(panelX, panelWidth, statusY, status, statusIsError);

        Gui.drawRect(panelX + PADDING, tipsY - 10, panelX + panelWidth - PADDING, tipsY - 9, RenderUtils.withAlpha(Color.WHITE, 20));
        String tips = premium ? "Changes apply to " + mc.getSession().getUsername() : "Log in with a Microsoft account first";
        small.drawString(tips, panelX + (panelWidth - small.getStringWidth(tips)) / 2f, tipsY, premium ? 0x777777 : DANGER.getRGB());
    }

    private void drawToggle(int x, int y, int w, String label, boolean active, int mouseX, int mouseY) {
        boolean hovered = menu.isMouseOverButton(mouseX, mouseY, x, y, w, BUTTON_HEIGHT);
        float hover = KineticUi.hover("skin.toggle." + label, hovered && !active);
        KineticUi.blend();
        KineticAltMenu.glassButton(FontUtils.getFont("inter-medium", 18), x, y, w, BUTTON_HEIGHT, label, hover, true, active);
    }

    private void drawPreviewPanel() {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        int fontHeight = regular.getHeight();

        menu.drawPanel(previewX, previewY, previewWidth, previewHeight);
        menu.drawSectionHeader(previewX + PADDING, previewY + PADDING, "Preview", "");

        int dividerY = previewY + PADDING + fontHeight + 10;
        Gui.drawRect(previewX + PADDING, dividerY, previewX + previewWidth - PADDING, dividerY + 1, RenderUtils.withAlpha(Color.WHITE, 20));

        int areaY = dividerY + PADDING;
        int areaHeight = previewY + previewHeight - areaY - PADDING;
        int half = previewWidth / 2;

        drawFigure(previewX + half / 2, areaY, areaHeight, "Current", currentTexture, currentIs64, currentVariant);
        Gui.drawRect(previewX + half, areaY, previewX + half + 1, areaY + areaHeight, RenderUtils.withAlpha(Color.WHITE, 20));
        drawFigure(previewX + half + half / 2, areaY, areaHeight, "New", chosenTexture, chosenIs64, variant);
    }

    private void drawFigure(int centerX, int top, int areaHeight, String label, ResourceLocation texture, boolean is64, String skinVariant) {
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        regular.drawCenteredStringWithShadow(label, centerX, top, Color.WHITE.getRGB());

        int figureHeight = 32 * PREVIEW_SCALE;
        int scale = PREVIEW_SCALE;
        if (figureHeight > areaHeight - regular.getHeight() - PADDING * 2) {
            scale = Math.max(2, (areaHeight - regular.getHeight() - PADDING * 2) / 32);
            figureHeight = 32 * scale;
        }
        int y = top + regular.getHeight() + PADDING + Math.max(0, (areaHeight - regular.getHeight() - PADDING - figureHeight) / 2);

        if (texture == null) {
            small.drawCenteredStringWithShadow(label.equals("New") ? "Choose a file" : "Not loaded", centerX, y + figureHeight / 2f, 0x777777);
            return;
        }

        boolean slim = SkinChanger.VARIANT_SLIM.equals(skinVariant);
        int armWidth = slim ? 3 : 4;
        int totalWidth = 8 + armWidth * 2;
        int x = centerX - totalWidth * scale / 2;

        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();

        int headX = x + armWidth * scale;
        int bodyY = y + 8 * scale;
        int legY = y + 20 * scale;

        part(headX, y, 8, 8, 8, 8, scale);
        part(headX, bodyY, 20, 20, 8, 12, scale);
        part(x, bodyY, 44, 20, armWidth, 12, scale);
        if (is64) part(headX + 8 * scale, bodyY, 36, 52, armWidth, 12, scale);
        else part(headX + 8 * scale, bodyY, 44, 20, armWidth, 12, scale);
        part(headX, legY, 4, 20, 4, 12, scale);
        if (is64) part(headX + 4 * scale, legY, 20, 52, 4, 12, scale);
        else part(headX + 4 * scale, legY, 4, 20, 4, 12, scale);

        part(headX, y, 40, 8, 8, 8, scale);
        if (is64) {
            part(headX, bodyY, 20, 36, 8, 12, scale);
            part(x, bodyY, 44, 36, armWidth, 12, scale);
            part(headX + 8 * scale, bodyY, 52, 52, armWidth, 12, scale);
            part(headX, legY, 4, 36, 4, 12, scale);
            part(headX + 4 * scale, legY, 4, 52, 4, 12, scale);
        }
        GlStateManager.disableBlend();
    }

    private static void part(int x, int y, int u, int v, int w, int h, int scale) {
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, w, h, w * scale, h * scale, 64f, 64f);
    }

    @Override
    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (chooseButton.contains(mouseX, mouseY)) {
            chooseFile();
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, classicX, variantY, variantWidth, BUTTON_HEIGHT)) {
            variant = SkinChanger.VARIANT_CLASSIC;
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, slimX, variantY, variantWidth, BUTTON_HEIGHT)) {
            variant = SkinChanger.VARIANT_SLIM;
            return true;
        }
        if (uploadButton.contains(mouseX, mouseY)) {
            upload();
            return true;
        }
        if (resetButton.contains(mouseX, mouseY)) {
            reset();
            return true;
        }
        return false;
    }

    private void setStatus(String message, boolean isError) {
        status = message;
        statusIsError = isError;
    }

    private void chooseFile() {
        if (busy) return;
        busy = true;
        setStatus("Waiting for file dialog...", false);
        new Thread(() -> {
            File file = FileDialogs.openFile("Choose Minecraft skin", "PNG skins", "png");
            if (file == null) {
                mc.addScheduledTask(() -> {
                    setStatus(chosenFile == null ? "Pick a PNG skin file" : "Kept " + chosenFile.getName(), false);
                    busy = false;
                });
                return;
            }
            try {
                BufferedImage image = SkinChanger.readSkin(file);
                boolean modern = image.getHeight() == 64;
                BufferedImage padded = modern ? image : padLegacy(image);
                mc.addScheduledTask(() -> {
                    chosenFile = file;
                    chosenIs64 = modern;
                    chosenTexture = replaceTexture("kinetic-skin-new", chosenTexture, padded);
                    setStatus("Ready to upload " + file.getName(), false);
                    busy = false;
                });
            } catch (SkinChanger.SkinException e) {
                mc.addScheduledTask(() -> {
                    setStatus(e.getMessage(), true);
                    busy = false;
                });
            }
        }, "Skin File Dialog").start();
    }

    private void upload() {
        if (busy) return;
        if (chosenFile == null) {
            setStatus("Choose a file first!", true);
            return;
        }
        Session session = mc.getSession();
        if (!SkinChanger.isPremiumToken(session.getToken())) {
            setStatus("Log in with a Microsoft account first!", true);
            return;
        }
        busy = true;
        setStatus("Uploading skin...", false);
        File file = chosenFile;
        String chosenVariant = variant;

        new Thread(() -> {
            try {
                Profile profile = SkinChanger.upload(session.getToken(), file, chosenVariant);
                mc.addScheduledTask(() -> {
                    setStatus("Skin updated for " + (profile.name.isEmpty() ? session.getUsername() : profile.name) + "!", false);
                    busy = false;
                    loadedForToken = "";
                    loadCurrentSkin();
                });
            } catch (SkinChanger.SkinException e) {
                mc.addScheduledTask(() -> {
                    setStatus(e.getMessage(), true);
                    busy = false;
                });
            }
        }, "Skin Upload Worker").start();
    }

    private void reset() {
        if (busy) return;
        Session session = mc.getSession();
        if (!SkinChanger.isPremiumToken(session.getToken())) {
            setStatus("Log in with a Microsoft account first!", true);
            return;
        }
        busy = true;
        setStatus("Resetting skin...", false);
        new Thread(() -> {
            try {
                SkinChanger.reset(session.getToken());
                mc.addScheduledTask(() -> {
                    setStatus("Skin reset to default!", false);
                    busy = false;
                    loadedForToken = "";
                    loadCurrentSkin();
                });
            } catch (SkinChanger.SkinException e) {
                mc.addScheduledTask(() -> {
                    setStatus(e.getMessage(), true);
                    busy = false;
                });
            }
        }, "Skin Reset Worker").start();
    }

    private void loadCurrentSkin() {
        Session session = mc.getSession();
        String token = session.getToken();
        if (!SkinChanger.isPremiumToken(token) || token.equals(loadedForToken)) return;
        loadedForToken = token;

        new Thread(() -> {
            try {
                Profile profile = SkinChanger.fetchProfile(token);
                BufferedImage image = null;
                if (!profile.skinUrl.isEmpty()) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(profile.skinUrl).openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    image = ImageIO.read(connection.getInputStream());
                }
                if (image == null) {
                    image = ImageIO.read(mc.getResourceManager().getResource(new ResourceLocation("textures/entity/steve.png")).getInputStream());
                }
                boolean modern = image.getHeight() == 64;
                BufferedImage padded = modern ? image : padLegacy(image);
                mc.addScheduledTask(() -> {
                    currentIs64 = modern;
                    currentVariant = profile.variant;
                    currentTexture = replaceTexture("kinetic-skin-current", currentTexture, padded);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Skin Fetch Worker").start();
    }

    private ResourceLocation replaceTexture(String name, ResourceLocation previous, BufferedImage image) {
        if (previous != null) mc.getTextureManager().deleteTexture(previous);
        return mc.getTextureManager().getDynamicTextureLocation(name, new DynamicTexture(image));
    }

    private static BufferedImage padLegacy(BufferedImage legacy) {
        BufferedImage padded = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        padded.getGraphics().drawImage(legacy, 0, 0, null);
        return padded;
    }
}
