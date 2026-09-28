package secret.kinetic.utils.render.moonlight;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import secret.kinetic.utils.misc.IMinecraft;
import secret.kinetic.utils.render.RenderUtils;

import java.awt.Color;







public final class ColorUtils implements IMinecraft {

    private ColorUtils() {
    }

    

    
    public static Color interpolateColor(Color color1, Color color2, float amount) {
        amount = Math.min(1f, Math.max(0f, amount));
        return new Color(Math.round(color1.getRed() + (color2.getRed() - color1.getRed()) * amount),
                Math.round(color1.getGreen() + (color2.getGreen() - color1.getGreen()) * amount),
                Math.round(color1.getBlue() + (color2.getBlue() - color1.getBlue()) * amount),
                Math.round(color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * amount));
    }

    
    public static int interpolateColor(int color1, int color2, float amount) {
        return interpolateColor(new Color(color1, true), new Color(color2, true), amount).getRGB();
    }

    
    public static int interpolateColor2(Color color1, Color color2, float fraction) {
        fraction = Math.min(1f, Math.max(0f, fraction));
        try {
            return interpolateColor(color1, color2, fraction).getRGB();
        } catch (RuntimeException ex) {
            return 0xFFFFFFFF;
        }
    }

    
    public static int fadeTo(int startColour, int endColour, double progress) {
        double invert = 1.0 - progress;
        int r = (int) ((startColour >> 16 & 0xFF) * invert + (endColour >> 16 & 0xFF) * progress);
        int g = (int) ((startColour >> 8 & 0xFF) * invert + (endColour >> 8 & 0xFF) * progress);
        int b = (int) ((startColour & 0xFF) * invert + (endColour & 0xFF) * progress);
        int a = (int) ((startColour >> 24 & 0xFF) * invert + (endColour >> 24 & 0xFF) * progress);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    
    public static int getOverallColorFrom(int color1, int color2, float percentTo2) {
        percentTo2 = Math.min(1f, Math.max(0f, percentTo2));
        int r = (int) ((color1 >> 16 & 0xFF) + ((color2 >> 16 & 0xFF) - (color1 >> 16 & 0xFF)) * percentTo2);
        int g = (int) ((color1 >> 8 & 0xFF) + ((color2 >> 8 & 0xFF) - (color1 >> 8 & 0xFF)) * percentTo2);
        int b = (int) ((color1 & 0xFF) + ((color2 & 0xFF) - (color1 & 0xFF)) * percentTo2);
        int a = (int) ((color1 >> 24 & 0xFF) + ((color2 >> 24 & 0xFF) - (color1 >> 24 & 0xFF)) * percentTo2);
        return getColor(r, g, b, a);
    }

    



    public static Color getGradientOffset(Color color1, Color color2, double offset) {
        double inversePercent;
        int redPart;
        if (offset < 0.0D) offset = -offset;
        if (offset > 1.0D) {
            inversePercent = offset % 1.0D;
            redPart = (int) offset;
            offset = redPart % 2 == 0 ? inversePercent : 1.0D - inversePercent;
        }
        inversePercent = 1.0D - offset;
        redPart = (int) ((double) color1.getRed() * inversePercent + (double) color2.getRed() * offset);
        int greenPart = (int) ((double) color1.getGreen() * inversePercent + (double) color2.getGreen() * offset);
        int bluePart = (int) ((double) color1.getBlue() * inversePercent + (double) color2.getBlue() * offset);
        return new Color(redPart, greenPart, bluePart);
    }

    
    public static Color colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed) {
        return colorSwitch(firstColor, secondColor, time, index, timePerIndex, speed, 255.0D);
    }

    
    public static Color colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed, double alpha) {
        long now = (long) (speed * (double) System.currentTimeMillis() + (double) ((long) index * timePerIndex));
        float redDiff = (float) (firstColor.getRed() - secondColor.getRed()) / time;
        float greenDiff = (float) (firstColor.getGreen() - secondColor.getGreen()) / time;
        float blueDiff = (float) (firstColor.getBlue() - secondColor.getBlue()) / time;
        int red = Math.round(secondColor.getRed() + redDiff * (float) (now % (long) time));
        int green = Math.round(secondColor.getGreen() + greenDiff * (float) (now % (long) time));
        int blue = Math.round(secondColor.getBlue() + blueDiff * (float) (now % (long) time));
        float redInverseDiff = (float) (secondColor.getRed() - firstColor.getRed()) / time;
        float greenInverseDiff = (float) (secondColor.getGreen() - firstColor.getGreen()) / time;
        float blueInverseDiff = (float) (secondColor.getBlue() - firstColor.getBlue()) / time;
        int inverseRed = Math.round(firstColor.getRed() + redInverseDiff * (float) (now % (long) time));
        int inverseGreen = Math.round(firstColor.getGreen() + greenInverseDiff * (float) (now % (long) time));
        int inverseBlue = Math.round(firstColor.getBlue() + blueInverseDiff * (float) (now % (long) time));

        return now % ((long) time * 2L) < (long) time
                ? new Color(inverseRed, inverseGreen, inverseBlue, (int) alpha)
                : new Color(red, green, blue, (int) alpha);
    }

    

    
    public static int darker(int color, float factor) {
        int r = (int) ((color >> 16 & 0xFF) * factor);
        int g = (int) ((color >> 8 & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        int a = color >> 24 & 0xFF;
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF | (a & 0xFF) << 24;
    }

    
    public static Color brighter(Color color, float factor) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int alpha = color.getAlpha();

        int i = (int) (1.0 / (1.0 - factor));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;

        return new Color(Math.min((int) (r / factor), 255),
                Math.min((int) (g / factor), 255),
                Math.min((int) (b / factor), 255),
                alpha);
    }

    
    public static Color hueShift(Color color, float shift) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float hue = (hsb[0] + shift) % 1f;
        if (hue < 0f) hue += 1f;
        Color out = Color.getHSBColor(hue, hsb[1], hsb[2]);
        return new Color(out.getRed(), out.getGreen(), out.getBlue(), color.getAlpha());
    }

    

    public static Color applyOpacity(Color color, float opacity) {
        return RenderUtils.applyOpacity(color, opacity);
    }

    public static int applyOpacity(int color, float opacity) {
        return RenderUtils.applyOpacity(color, opacity);
    }

    
    public static Color reAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    

    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= alpha << 24;
        color |= red << 16;
        color |= green << 8;
        return color | blue;
    }

    
    public static int swapAlpha(int color, float alpha) {
        return getColor(getRedFromColor(color), getGreenFromColor(color), getBlueFromColor(color), (int) alpha);
    }

    public static int getRedFromColor(int color) {
        return color >> 16 & 0xFF;
    }

    public static int getGreenFromColor(int color) {
        return color >> 8 & 0xFF;
    }

    public static int getBlueFromColor(int color) {
        return color & 0xFF;
    }

    public static int getAlphaFromColor(int color) {
        return color >> 24 & 0xFF;
    }

    

    
    public static int getColorFromPercentage(float percentage) {
        return Color.HSBtoRGB(Math.min(1.0F, Math.max(0.0F, percentage)) / 3, 0.9F, 0.9F);
    }

    
    public static int getHealthColor(EntityLivingBase player) {
        float health = player.getHealth();
        float max = player.getMaxHealth();
        float ratio = Math.max(0.0F, Math.min(health, max) / max);
        return Color.HSBtoRGB(ratio / 3.0F, 0.75F, 1.0F) | 0xFF000000;
    }

    
    public static Color getRainbow(long speed, long offset, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() + offset) % speed) / (float) speed;
        return Color.getHSBColor(hue, saturation, brightness);
    }

    

    



    public static Color getTeamColor(EntityLivingBase entity) {
        if (entity == null || mc.theWorld == null) return null;
        Team team = entity.getTeam();
        if (!(team instanceof ScorePlayerTeam)) return null;
        String prefix = ((ScorePlayerTeam) team).getColorPrefix();
        if (prefix == null) return null;
        char code = 0;
        for (int i = 0; i + 1 < prefix.length(); i++) {
            if (prefix.charAt(i) == '\u00A7') {
                char c = Character.toLowerCase(prefix.charAt(i + 1));
                if ("0123456789abcdef".indexOf(c) >= 0) code = c;
            }
        }
        if (code == 0) return null;
        return new Color(mc.fontRendererObj.getColorCode(code));
    }
}
