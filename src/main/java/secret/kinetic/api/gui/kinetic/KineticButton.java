package secret.kinetic.api.gui.kinetic;

import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.utils.render.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;






public class KineticButton extends GuiButton {

    private boolean primary;
    private float hover;
    private long lastFrame;

    public KineticButton(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, buttonText);
    }

    public KineticButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    
    public KineticButton primary() {
        this.primary = true;
        return this;
    }

    public float getHover() {
        return hover;
    }

    
    public void drawBlurMask() {
        if (!this.visible) return;
        KineticUi.mask(this.xPosition, this.yPosition, this.width, this.height, Math.min(KineticUi.BUTTON_RADIUS, this.height / 2f));
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 16f : now - lastFrame;
        lastFrame = now;
        hover = KineticUi.approach(hover, this.hovered && this.enabled ? 1f : 0f, KineticUi.HOVER_SPEED, dt);

        KineticUi.blend();
        GlStateManager.pushMatrix();
        
        GlStateManager.translate(0f, -1.2f * hover, 0f);
        CustomFontRenderer font = FontUtils.getFont("sf", 18);
        KineticUi.drawButton(font, this.xPosition, this.yPosition, this.width, this.height, this.displayString,
                hover, this.enabled, primary, 1f);
        GlStateManager.popMatrix();

        this.mouseDragged(mc, mouseX, mouseY);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
