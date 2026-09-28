package net.minecraft.client.gui;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.api.gui.main.KineticMenu;
import secret.kinetic.api.gui.kinetic.KineticButton;
import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.managers.impl.LastConnectionManager;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.KineticImage;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridge;

import java.awt.Color;
import java.io.IOException;






public class GuiIngameMenu extends GuiScreen
{
    private static final int PANEL_WIDTH = 196;
    private static final int PANEL_PADDING = 12;
    private static final int HEADER_HEIGHT = 34;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 6;
    private static final int COLUMN_GAP = 6;
    private static final long OPEN_FADE_MS = 300L;

    private int field_146445_a;
    private int field_146444_f;
    private int panelX, panelY, panelHeight;
    private long openedAt;

    public void initGui()
    {
        this.field_146445_a = 0;
        this.openedAt = System.currentTimeMillis();
        this.buttonList.clear();

        int rows = 4;
        this.panelHeight = HEADER_HEIGHT + PANEL_PADDING + rows * BUTTON_HEIGHT + (rows - 1) * ROW_GAP + PANEL_PADDING;
        this.panelX = this.width - PANEL_WIDTH - Math.max(16, (int) (this.width * 0.07f));
        this.panelY = this.height / 2 - this.panelHeight / 2;

        int fullWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        int halfWidth = (fullWidth - COLUMN_GAP) / 2;
        int left = this.panelX + PANEL_PADDING;
        int right = left + halfWidth + COLUMN_GAP;
        int y = this.panelY + HEADER_HEIGHT + PANEL_PADDING;

        this.buttonList.add(new KineticButton(4, left, y, fullWidth, BUTTON_HEIGHT, I18n.format("menu.returnToGame", new Object[0])).primary());
        y += BUTTON_HEIGHT + ROW_GAP;

        this.buttonList.add(new KineticButton(5, left, y, halfWidth, BUTTON_HEIGHT, I18n.format("gui.achievements", new Object[0])));
        this.buttonList.add(new KineticButton(6, right, y, halfWidth, BUTTON_HEIGHT, I18n.format("gui.stats", new Object[0])));
        y += BUTTON_HEIGHT + ROW_GAP;

        GuiButton guibutton;
        this.buttonList.add(guibutton = new KineticButton(7, left, y, halfWidth, BUTTON_HEIGHT, I18n.format("menu.shareToLan", new Object[0])));
        this.buttonList.add(new KineticButton(0, right, y, halfWidth, BUTTON_HEIGHT, I18n.format("menu.options", new Object[0])));
        guibutton.enabled = this.mc.isSingleplayer() && !this.mc.getIntegratedServer().getPublic();
        y += BUTTON_HEIGHT + ROW_GAP;

        if (!this.mc.isIntegratedServerRunning()) {
            this.buttonList.add(new KineticButton(1, left, y, halfWidth, BUTTON_HEIGHT, I18n.format("menu.disconnect")));
            this.buttonList.add(new KineticButton(8, right, y, halfWidth, BUTTON_HEIGHT, "Reconnect"));
        } else {
            this.buttonList.add(new KineticButton(1, left, y, fullWidth, BUTTON_HEIGHT, I18n.format("menu.returnToMenu")));
        }
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        switch (button.id)
        {
            case 0:
                this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
                break;

            case 1:
                boolean flag = this.mc.isIntegratedServerRunning();
                boolean flag1 = this.mc.isConnectedToRealms();
                button.enabled = false;
                this.mc.theWorld.sendQuittingDisconnectingPacket();
                this.mc.loadWorld((WorldClient)null);

                if (flag)
                {
                    this.mc.displayGuiScreen(new KineticMenu());
                }
                else if (flag1)
                {
                    RealmsBridge realmsbridge = new RealmsBridge();
                    realmsbridge.switchToRealms(new KineticMenu());
                }
                else
                {
                    this.mc.displayGuiScreen(new GuiMultiplayer(new KineticMenu()));
                }

            case 2:
            case 3:
            default:
                break;

            case 4:
                this.mc.displayGuiScreen((GuiScreen)null);
                this.mc.setIngameFocus();
                break;

            case 5:
                this.mc.displayGuiScreen(new GuiAchievements(this, this.mc.thePlayer.getStatFileWriter()));
                break;

            case 6:
                this.mc.displayGuiScreen(new GuiStats(this, this.mc.thePlayer.getStatFileWriter()));
                break;

            case 7:
                this.mc.displayGuiScreen(new GuiShareToLan(this));
                break;

            case 8:
                this.mc.displayGuiScreen(new GuiConnecting(new GuiMultiplayer(new KineticMenu()), this.mc, new ServerData("", LastConnectionManager.ip, false)));
                break;
        }
    }

    public void updateScreen()
    {
        super.updateScreen();
        ++this.field_146444_f;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();

        float fade = KineticUi.ease(Math.min(1f, (System.currentTimeMillis() - this.openedAt) / (float) OPEN_FADE_MS));
        float slide = (1f - fade) * 8f;

        
        
        float logoH = Math.min(this.height * 0.68f, this.width * 0.26f / KineticImage.LOGO_ASPECT);
        float logoW = logoH * KineticImage.LOGO_ASPECT;
        float logoX = Math.max(4f, Math.min(this.width * 0.03f, this.panelX - logoW - 12f));
        float logoY = this.height - logoH + (1f - fade) * 14f;
        KineticImage.drawLogo(logoX, logoY, logoW, logoH, fade);

        KineticUi.beginBlur();
        KineticUi.mask(this.panelX, this.panelY + slide, PANEL_WIDTH, this.panelHeight, KineticUi.CARD_RADIUS);
        KineticUi.endBlur(12f * fade);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, slide, 0f);
        KineticUi.drawPanel(this.panelX, this.panelY, PANEL_WIDTH, this.panelHeight, KineticUi.CARD_RADIUS, fade);

        
        float avatar = 20f;
        float ax = this.panelX + PANEL_PADDING;
        float ay = this.panelY + (HEADER_HEIGHT - avatar) / 2f + 2f;
        RoundedUtils.drawSmoothShadow(ax, ay, avatar, avatar, avatar / 2f, 4f, Theme.alpha(ColorManager.getColor(), 70, fade));
        KineticImage.drawAvatar(ax, ay, avatar, fade);

        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        float textX = ax + avatar + 8f;
        bold.drawString(Kinetic.NAME + " Client", textX, ay + 1f, Theme.argb(Color.WHITE, fade));
        small.drawString("Paused", textX, ay + bold.getHeight() + 2f, Theme.argb(ColorManager.getColor(), fade));

        String version = "v" + Kinetic.VERSION;
        small.drawString(version, this.panelX + PANEL_WIDTH - PANEL_PADDING - small.getStringWidth(version), ay + 2f,
                Theme.argb(KineticUi.TEXT_DIM, fade));

        KineticUi.drawHairline(this.panelX + PANEL_PADDING, this.panelY + HEADER_HEIGHT + 3f, PANEL_WIDTH - PANEL_PADDING * 2, fade);

        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();
    }
}
