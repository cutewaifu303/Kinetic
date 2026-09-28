package secret.kinetic.modules.impl.misc;

import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

@ModuleInfo(label = "Anti Bot", description = "Blacklists bots from being targeted", category = ModuleCategory.MISC)
public final class AntiBotModule extends Module {

    public final Property<Boolean> checkInTablist = new Property<Boolean>("Tablist Check", true);
    public final Property<Boolean> checkValidName = new Property<Boolean>("Check Valid Name", true);
    public final Property<Boolean> checkAge = new Property<Boolean>("Check Age", true);
    public final Property<Boolean> checkLatency = new Property<Boolean>("Check Latency", true);

    public final Property<Boolean> checkFov = new Property<Boolean>("FOV Check", true);
    public final NumberProperty maxFov = new NumberProperty("Max FOV", 90f, 0f, 180f, 1f);

    private static final String VALID_USERNAME_REGEX = "^[a-zA-Z0-9_]{1,16}$";

    public boolean isBot(EntityPlayer player) {
        if (player == null || mc.thePlayer == null)
            return true;

        if (player == mc.thePlayer || !isEnabled())
            return false;

        NetworkPlayerInfo entry = mc.getNetHandler().getPlayerInfo(player.getUniqueID());

        if (checkAge.getValue() && player.ticksExisted < 20)
            return true;

        if (checkInTablist.getValue() && entry == null)
            return true;

        if (checkLatency.getValue() && entry != null && entry.getResponseTime() == 0)
            return true;

        if (checkValidName.getValue() && entry != null) {
            String name = entry.getGameProfile().getName();

            if (name == null ||
                    !name.matches(VALID_USERNAME_REGEX) ||
                    name.contains(" ") ||
                    name.contains("NPC")) {
                return true;
            }
        }

        if (checkFov.getValue() && !isWithinFov(player, (float) maxFov.getValue().doubleValue())) {
            return true;
        }

        return false;
    }

    private boolean isWithinFov(EntityPlayer target, float maxFov) {
        EntityPlayer self = mc.thePlayer;

        Vec3 look = self.getLook(1.0F).normalize();

        double dx = target.posX - self.posX;
        double dy = (target.posY + target.getEyeHeight()) - (self.posY + self.getEyeHeight());
        double dz = target.posZ - self.posZ;

        Vec3 toTarget = new Vec3(dx, dy, dz).normalize();

        double dot = look.xCoord * toTarget.xCoord +
                look.yCoord * toTarget.yCoord +
                look.zCoord * toTarget.zCoord;

        double angle = Math.acos(MathHelper.clamp_double(dot, -1.0, 1.0)) * (180.0 / Math.PI);

        return angle <= maxFov;
    }
}