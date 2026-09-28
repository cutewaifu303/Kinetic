package net.minecraft.client.gui;

import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.render.RenderUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;

public class GuiLockIconButton extends GuiButton
{
    private boolean field_175231_o = false;

    public GuiLockIconButton(int p_i45538_1_, int p_i45538_2_, int p_i45538_3_)
    {
        super(p_i45538_1_, p_i45538_2_, p_i45538_3_, 20, 20, "");
    }

    public boolean func_175230_c()
    {
        return this.field_175231_o;
    }

    public void func_175229_b(boolean p_175229_1_)
    {
        this.field_175231_o = p_175229_1_;
    }

    
    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            this.hoverAnim = KineticUi.hover("lock-" + this.id, this.hovered && this.enabled);
            KineticUi.blend();
            KineticUi.drawButton(null, this.xPosition, this.yPosition, this.width, this.height, null, this.hoverAnim, this.enabled, false, 1f);

            Color color = !this.enabled ? KineticUi.DISABLED_TEXT
                    : this.field_175231_o ? ColorManager.getColor() : RenderUtils.interpolateColorC(KineticUi.TEXT_MUTED, Color.WHITE, this.hoverAnim);
            float cx = this.xPosition + this.width / 2f;
            float cy = this.yPosition + this.height / 2f;
            float bodyW = 8f;
            float bodyH = 6f;
            float bodyY = cy - 1f;
            
            float shackleW = 5.5f;
            float shackleH = 7f;
            float shackleX = cx - shackleW / 2f + (this.field_175231_o ? 0f : 2.5f);
            float shackleY = bodyY - shackleH + 2.5f - (this.field_175231_o ? 0f : 1.5f);
            RoundedUtils.drawSmooth(shackleX, shackleY, shackleW, shackleH, shackleW / 2f, 0f,
                    KineticUi.CLEAR, KineticUi.CLEAR, 1.3f, color);
            RoundedUtils.drawSmoothRect(cx - bodyW / 2f, bodyY, bodyW, bodyH, 1.5f, color);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    static enum Icon
    {
        LOCKED(0, 146),
        LOCKED_HOVER(0, 166),
        LOCKED_DISABLED(0, 186),
        UNLOCKED(20, 146),
        UNLOCKED_HOVER(20, 166),
        UNLOCKED_DISABLED(20, 186);

        private final int field_178914_g;
        private final int field_178920_h;

        private Icon(int p_i45537_3_, int p_i45537_4_)
        {
            this.field_178914_g = p_i45537_3_;
            this.field_178920_h = p_i45537_4_;
        }

        public int func_178910_a()
        {
            return this.field_178914_g;
        }

        public int func_178912_b()
        {
            return this.field_178920_h;
        }
    }
}
