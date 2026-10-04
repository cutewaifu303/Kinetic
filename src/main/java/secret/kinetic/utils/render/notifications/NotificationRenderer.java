package secret.kinetic.utils.render.notifications;

import secret.kinetic.Kinetic;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.utils.misc.IMinecraft;
import secret.kinetic.utils.misc.NotificationHandler;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

public final class NotificationRenderer implements IMinecraft {

    private static final NotificationHandler notificationManager = Kinetic.INSTANCE.getNotificationHandler();
    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final Color SUB_COLOR = new Color(190, 190, 190);
    private static final float RADIUS = 6f;

    public static void update() {
        // the Sigma theme drives its notifications by time in SigmaHud instead
        if (secret.kinetic.utils.render.KineticImage.isSigmaTheme()) return;
        if (!notificationManager.getNotifications().isEmpty()) {
            notificationManager.update();
        }
    }

    public static void draw() {
        if (secret.kinetic.utils.render.KineticImage.isSigmaTheme()) {
            secret.kinetic.api.gui.sigma.SigmaHud.notifications(notificationManager.getNotifications(), notificationManager::remove);
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);

        for (Notification notification : notificationManager.getNotifications()) {
            renderNotification(notification, resolution);
        }
    }

    private static void renderNotification(Notification notification, ScaledResolution resolution) {
        float padding = 12f;
        float height = 28f;

        String title = notification.getCallReason() == null ? "Notification" : notification.getCallReason();
        String message = notification.getMessage();

        CustomFontRenderer nameFont = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer bodyFont = FontUtils.getFont("sf", 14);

        if (nameFont == null || bodyFont == null) return;

        float titleWidth = nameFont.getStringWidth(title);
        float messageWidth = bodyFont.getStringWidth(message);
        float width = Math.max(130f, Math.max(titleWidth, messageWidth) + padding * 2f);

        float x = (resolution.getScaledWidth() - width) / 2f;
        float y = (float) notification.getY();

        Color accentColor = ColorManager.getColor();

        RoundedUtils.drawRoundOutline(x, y, width, height, RADIUS, -0.4f, BG_COLOR, accentColor);

        float titleX = x + (width - titleWidth) / 2f;
        float messageX = x + (width - messageWidth) / 2f;

        nameFont.drawStringWithShadow(title, titleX, y + 4f, Color.WHITE.getRGB());
        bodyFont.drawStringWithShadow(message, messageX, y + 15f, SUB_COLOR.getRGB());
    }
}