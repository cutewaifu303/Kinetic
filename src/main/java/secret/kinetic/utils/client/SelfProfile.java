package secret.kinetic.utils.client;

import secret.kinetic.api.gui.kinetic.KineticUi;
import secret.kinetic.utils.render.PlayerHeads;
import net.minecraft.client.Minecraft;




public final class SelfProfile {

    private SelfProfile() {
    }

    
    public static String name() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.getSession() != null ? mc.getSession().getUsername() : "Player";
    }

    
    public static void drawAvatar(float x, float y, float size, float alpha) {
        KineticUi.drawHead(PlayerHeads.current(), x, y, size, alpha);
    }
}
