package secret.kinetic.modules.impl.movement;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.client.PacketSendEvent;
import secret.kinetic.api.events.impl.player.*;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.modules.impl.movement.noslow.NoSlowMode;
import secret.kinetic.modules.impl.movement.noslow.impl.HypixelNoSlow;
import secret.kinetic.modules.impl.movement.noslow.impl.NCPNoSlow;
import net.minecraft.item.*;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "No Slow", category = ModuleCategory.MOVEMENT, description = "Prevents you from slowing down while using items")
public final class NoSlowModule extends Module {

    public enum Mode {
        VANILLA("Vanilla"),
        NCP("NCP"),
        HYPIXEL("Hypixel");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VANILLA);
    public final Property<Boolean> food = new Property<Boolean>("Food Items", true);
    public final Property<Boolean> potion = new Property<Boolean>("Potion Items", true);
    public final Property<Boolean> sword = new Property<Boolean>("Sword Items", true);
    public final Property<Boolean> bow = new Property<Boolean>("Bow Items", true);
    public final Property<Boolean> autoJump = new Property<Boolean>("Air Start Jump", true, () -> mode.getValue() == Mode.HYPIXEL);

    private final Map<Mode, NoSlowMode> noSlowModes;

    {
        noSlowModes = new EnumMap<>(Mode.class);
        noSlowModes.put(Mode.NCP, new NCPNoSlow());
        noSlowModes.put(Mode.HYPIXEL, new HypixelNoSlow(this));
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;

        setSuffix(mode.getValue().toString());

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketSend(event);
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacketReceived(event);
        }
    }

    @EventHook
    public void onRightClick(RightClickEvent event) {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onRightClick(event);
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onStrafe(event);
        }
    }

    @EventHook
    public void onPreMotion(MotionEvent event) {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onMotion(event);
        }
    }

    @EventHook
    public void onSlowdown(ItemSlowdownEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || !mc.thePlayer.isUsingItem() || mc.thePlayer.getHeldItem() == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode instanceof HypixelNoSlow) {
            (currentMode).onSlowdown(event);
            return;
        }

        Item item = mc.thePlayer.getHeldItem().getItem();

        if (food.getValue() && item instanceof ItemFood) event.setCancelled(true);
        if (potion.getValue() && item instanceof ItemPotion) event.setCancelled(true);
        if (sword.getValue() && item instanceof ItemSword) event.setCancelled(true);
        if (bow.getValue() && item instanceof ItemBow) event.setCancelled(true);
    }

    public boolean isFoodActive() {
        return food.getValue() && isUsing(ItemFood.class);
    }

    public boolean isPotionActive() {
        return potion.getValue() && isUsing(ItemPotion.class);
    }

    public boolean isSwordActive() {
        return sword.getValue() && isUsing(ItemSword.class);
    }

    public boolean isBowActive() {
        return bow.getValue() && isUsing(ItemBow.class);
    }

    public boolean isActive() {
        return isFoodActive() || isPotionActive() || isSwordActive() || isBowActive();
    }

    private boolean isUsing(Class<?> itemClass) {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem()) return false;
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && itemClass.isInstance(held.getItem());
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

        NoSlowMode currentMode = noSlowModes.get(mode.getValue());
        if (currentMode instanceof HypixelNoSlow) {
            ((HypixelNoSlow) currentMode).reset();
        }
    }
}
