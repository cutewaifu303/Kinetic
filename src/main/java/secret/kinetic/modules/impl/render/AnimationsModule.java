package secret.kinetic.modules.impl.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;

@ModuleInfo(label = "Animations", category = ModuleCategory.RENDER, description = "Changes the rendered hand appearance properties")
public class AnimationsModule extends Module {

    public enum AnimationMode {
        TIMGIOH("TimGioh"),
        ASTRA("Astra");

        public final String name;
        AnimationMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final ModeProperty<AnimationMode> mode = new ModeProperty<>("Block Animations", AnimationMode.ASTRA);
    public static final NumberProperty swingSpeed = new NumberProperty("Swing Speed", 0, 0, 10, 1);
    public static final NumberProperty astraPush = new NumberProperty("Astra Push", 0.45, 0.1, 1.0, 0.05, () -> mode.getValue() == AnimationMode.ASTRA);
    public static final NumberProperty astraScale = new NumberProperty("Astra Scale", 1.0, 0.5, 1.5, 0.05, () -> mode.getValue() == AnimationMode.ASTRA);
    public static final NumberProperty astraSpin = new NumberProperty("Astra Spin Angle", 85.0, 30.0, 180.0, 5.0, () -> mode.getValue() == AnimationMode.ASTRA);
    public static final Property<Boolean> alwaysBlock = new Property<>("Always Block Animation", false);
    public static final Property<Boolean> dontResetBlock = new Property<>("Dont Reset Block", true);
    public static final Property<Boolean> fluxSwing = new Property<>("Flux Swing", false);
    public static final Property<Boolean> swingEating = new Property<>("Swing While Eating", false);

    public static boolean alwaysBlocking() {
        return alwaysBlock.getValue() && mc.thePlayer != null && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemSword;
    }

    public static void renderBlockAnimation(float swingProgress, float equipProgress) {
        float equip = dontResetBlock.getValue() ? 0.0F : equipProgress;
        float clampedSwing = swingProgress < 0.0F ? 0.0F : (swingProgress > 1.0F ? 1.0F : swingProgress);
        float sqrtSwing = MathHelper.sin(MathHelper.sqrt_float(clampedSwing) * (float) Math.PI);
        float sinSwing = MathHelper.sin(clampedSwing * (float) Math.PI);

        mc.getItemRenderer().transformFirstPersonItem(equip, 0.0F);

        switch (mode.getValue()) {
            case TIMGIOH: {
                float smooth = clampedSwing < 0.5F ? 4.0F * clampedSwing * clampedSwing * clampedSwing
                        : 1.0F - (float) Math.pow(-2.0F * clampedSwing + 2.0F, 3.0F) / 2.0F;
                GlStateManager.rotate(smooth * -35.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(sqrtSwing * 30.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(sqrtSwing * -20.0F, 0.0F, 0.0F, 1.0F);
                break;
            }
            case ASTRA:
            default: {
                float tBack = clampedSwing - 1.0F;
                float easeOutBack = 1.0F + 2.4F * tBack * tBack * tBack + 1.4F * tBack * tBack;
                float push = astraPush.getValue().floatValue();
                float scale = astraScale.getValue().floatValue();
                float spin = astraSpin.getValue().floatValue();

                GlStateManager.translate(sqrtSwing * 0.08F, sinSwing * -0.18F, sqrtSwing * -push);
                GlStateManager.scale(scale, scale, scale);
                GlStateManager.rotate(easeOutBack * -spin, 1.0F, 0.2F, 0.0F);
                GlStateManager.rotate(sqrtSwing * 45.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(sqrtSwing * -85.0F, 0.0F, 0.0F, 1.0F);
                break;
            }
        }

        mc.getItemRenderer().doBlockTransformations();
    }
}
