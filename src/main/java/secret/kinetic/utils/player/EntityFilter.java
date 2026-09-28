package secret.kinetic.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StringUtils;






public final class EntityFilter {

    private EntityFilter() {
    }

    public static boolean isNpc(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (player == null || player == mc.thePlayer) return false;
        if (player.getUniqueID() != null && player.getUniqueID().version() == 2) return true;
        if (mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info == null) return true;
        }
        String name = StringUtils.stripControlCodes(player.getDisplayName().getUnformattedText());
        return name.contains("[NPC]") || name.isEmpty();
    }
}
