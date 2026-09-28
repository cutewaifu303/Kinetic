package secret.kinetic.utils.client;

import net.minecraft.util.ChatComponentText;

import static secret.kinetic.utils.misc.IMinecraft.mc;

public class LoggingUtils {
    public static void sendChatMessage(String message) {
        if (mc.thePlayer != null) {
            String msg = "§cKinetic §8» §7" + message;
            mc.thePlayer.addChatMessage(new ChatComponentText(msg));
        }
    }
}
