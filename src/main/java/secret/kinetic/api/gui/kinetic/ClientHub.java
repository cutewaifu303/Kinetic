package secret.kinetic.api.gui.kinetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.alt.KineticAltMenu;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.gui.click.kinetic.KineticClickGui;
import secret.kinetic.api.gui.main.KineticMenu;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;

import java.awt.Color;







public final class ClientHub {

    public enum Tab {
        ALT_MANAGER("Alt Manager"), THEMES("Themes"), MODULES("Modules"), CONFIGS("Configs");

        public final String title;

        Tab(String title) {
            this.title = title;
        }
    }

    public static final int BAR_HEIGHT = 38;
    private static final int TAB_GAP = 22;

    private static KineticClickGui clickGui;
    private static GuiScreen parent;

    
    private static float indicatorX = Float.NaN, indicatorW;
    private static long lastFrame;

    
    private static final float[] tabX = new float[Tab.values().length];
    private static final float[] tabW = new float[Tab.values().length];
    private static float backX, backY, backW, backH;

    private ClientHub() {
    }

    
    public static void open(Tab tab, GuiScreen from) {
        parent = from;
        show(tab);
    }

    private static void show(Tab tab) {
        Minecraft mc = Minecraft.getMinecraft();
        if (tab == Tab.ALT_MANAGER) {
            mc.displayGuiScreen(new KineticAltMenu());
            return;
        }
        if (clickGui == null) clickGui = KineticClickGui.createHub();
        clickGui.showHubTab(tab);
        mc.displayGuiScreen(clickGui);
    }

    
    public static void close() {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen target = parent;
        parent = null;
        if (target == null && mc.theWorld == null) target = new KineticMenu();
        mc.displayGuiScreen(target);
    }

    private static CustomFontRenderer tabFont() {
        return FontUtils.getFont("sf", 18);
    }

    
    public static void drawBar(float width, Tab active, int mouseX, int mouseY) {
        KineticUi.blend();
        RoundedUtils.drawSmoothRect(0f, 0f, width, BAR_HEIGHT, 0f, new Color(11, 12, 15, 205));
        KineticUi.drawHairline(0f, BAR_HEIGHT - 0.6f, width, 1f);

        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 20);
        float wordY = (BAR_HEIGHT - (bold == null ? 10 : bold.getHeight())) / 2f;
        KineticUi.drawInlineWordmark(14f, wordY, 1f);

        
        CustomFontRenderer font = tabFont();
        Tab[] tabs = Tab.values();
        float total = 0f;
        for (int i = 0; i < tabs.length; i++) {
            tabW[i] = font.getStringWidth(tabs[i].title);
            total += tabW[i] + (i > 0 ? TAB_GAP : 0);
        }
        float cursor = width / 2f - total / 2f;
        float textY = (BAR_HEIGHT - font.getHeight()) / 2f;
        for (int i = 0; i < tabs.length; i++) {
            tabX[i] = cursor;
            boolean on = tabs[i] == active;
            boolean hovered = !on && KineticUi.inside(mouseX, mouseY, cursor - 6f, 0f, tabW[i] + 12f, BAR_HEIGHT);
            float hover = KineticUi.hover("hub.tab." + i, hovered);
            Color color = on ? Color.WHITE : RenderUtils.interpolateColorC(KineticUi.TEXT_MUTED, KineticUi.TEXT, hover);
            font.drawString(tabs[i].title, cursor, textY, color.getRGB());
            cursor += tabW[i] + TAB_GAP;
        }

        
        int index = active.ordinal();
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : Math.min(100f, now - lastFrame);
        lastFrame = now;
        if (Float.isNaN(indicatorX) || dt >= 100f) {
            indicatorX = tabX[index];
            indicatorW = tabW[index];
        } else {
            indicatorX = KineticUi.approach(indicatorX, tabX[index], 16f, dt);
            indicatorW = KineticUi.approach(indicatorW, tabW[index], 16f, dt);
        }
        RoundedUtils.drawSmoothRect(indicatorX, BAR_HEIGHT - 2.4f, indicatorW, 2f, 1f, Theme.accent());

        
        CustomFontRenderer small = FontUtils.getFont("sf", 16);
        backH = 22f;
        backW = Math.max(52f, small.getStringWidth("Back") + 24f);
        backX = width - 12f - backW;
        backY = (BAR_HEIGHT - backH) / 2f;
        boolean backHovered = KineticUi.inside(mouseX, mouseY, backX, backY, backW, backH);
        KineticUi.drawButton(small, backX, backY, backW, backH, "Back", KineticUi.hover("hub.back", backHovered), true, false, 1f);

        Minecraft mc = Minecraft.getMinecraft();
        String name = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        float nameW = small.getStringWidth(name);
        float head = 16f;
        float chipW = head + 7f + nameW + 16f;
        float chipX = backX - 10f - chipW;
        if (chipX > tabX[tabs.length - 1] + tabW[tabs.length - 1] + 16f) {
            float chipY = backY;
            RoundedUtils.drawSmoothBorderedRect(chipX, chipY, chipW, backH, backH / 2f, KineticUi.SURFACE_2, 0.6f, KineticUi.BORDER);
            KineticUi.drawPlayerHead(chipX + 4f, chipY + (backH - head) / 2f, head, 1f);
            small.drawString(name, chipX + 4f + head + 7f, chipY + (backH - small.getHeight()) / 2f + 0.5f, KineticUi.TEXT.getRGB());
        }
    }

    
    public static boolean barClicked(Tab active, int mouseX, int mouseY, int button) {
        if (mouseY < 0 || mouseY > BAR_HEIGHT) return false;
        if (button != 0) return true;
        if (KineticUi.inside(mouseX, mouseY, backX, backY, backW, backH)) {
            click();
            close();
            return true;
        }
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (KineticUi.inside(mouseX, mouseY, tabX[i] - 6f, 0f, tabW[i] + 12f, BAR_HEIGHT)) {
                if (tabs[i] != active) {
                    click();
                    show(tabs[i]);
                }
                return true;
            }
        }
        return true;
    }

    private static void click() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}
