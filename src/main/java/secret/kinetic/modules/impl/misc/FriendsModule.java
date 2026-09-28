package secret.kinetic.modules.impl.misc;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.MiddleClickEvent;
import secret.kinetic.api.properties.Property;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.player.FriendUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(
        label = "Friends",
        description = "Manage your friends, added via friends commands or mid-click, to exclude from other modules",
        category = ModuleCategory.MISC)
public final class FriendsModule extends Module {

    public final Property<Boolean> midClickAdd = new Property<>("Mid-Click Add", true);

    @EventHook
    public void onMidClick(MiddleClickEvent event) {
        if(!midClickAdd.getValue()) return;

        if (mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
                mc.objectMouseOver.entityHit instanceof EntityPlayer) {

            EntityPlayer clickedPlayer = (EntityPlayer) mc.objectMouseOver.entityHit;

            if (clickedPlayer != mc.thePlayer) {
                if (FriendUtils.isFriend(clickedPlayer)) {
                    FriendUtils.remove(clickedPlayer.getName());
                    Kinetic.INSTANCE.getNotificationHandler().pop(getLabel(),"§fRemoved §c" + clickedPlayer.getName() + "§f from exclusion list");
                } else {
                    FriendUtils.add(clickedPlayer.getName());
                    Kinetic.INSTANCE.getNotificationHandler().pop(getLabel(),"§fAdded §c" + clickedPlayer.getName() + "§f to exclusion list");
                }
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onEnable() {
        FriendUtils.loadFriends();
    }
}
