package net.minecraft.client.settings;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import secret.kinetic.Kinetic;
import secret.kinetic.modules.impl.player.InvMoveModule;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IntHashMap;

import java.util.List;
import java.util.Set;

public class KeyBinding implements Comparable<KeyBinding>
{
    private static final List<KeyBinding> keybindArray = Lists.<KeyBinding>newArrayList();
    private static final IntHashMap<KeyBinding> hash = new IntHashMap();
    private static final Set<String> keybindSet = Sets.<String>newHashSet();
    private final String keyDescription;
    private final int keyCodeDefault;
    private final String keyCategory;
    private int keyCode;
    @Setter
    public boolean pressed;
    private int pressTime;

    public static void onTick(int keyCode)
    {
        if (keyCode != 0)
        {
            KeyBinding keybinding = (KeyBinding)hash.lookup(keyCode);

            if (keybinding != null)
            {
                ++keybinding.pressTime;
            }
        }
    }

    public static void setKeyBindState(int keyCode, boolean pressed)
    {
        if (keyCode != 0)
        {
            if (keyCode == Minecraft.getMinecraft().gameSettings.keyBindJump.getKeyCode()
                    && shouldBlockJumpInCurrentScreen())
            {
                pressed = false;
            }

            KeyBinding keybinding = (KeyBinding)hash.lookup(keyCode);

            if (keybinding != null)
            {
                keybinding.pressed = pressed;
            }
        }
    }

    public static void unPressAllKeys()
    {
        for (KeyBinding keybinding : keybindArray)
        {
            keybinding.unpressKey();
        }
    }

    public static void resetKeyBindingArrayAndHash()
    {
        hash.clearMap();

        for (KeyBinding keybinding : keybindArray)
        {
            hash.addKey(keybinding.keyCode, keybinding);
        }
    }

    public static Set<String> getKeybinds()
    {
        return keybindSet;
    }

    public KeyBinding(String description, int keyCode, String category)
    {
        this.keyDescription = description;
        this.keyCode = keyCode;
        this.keyCodeDefault = keyCode;
        this.keyCategory = category;
        keybindArray.add(this);
        hash.addKey(keyCode, this);
        keybindSet.add(category);
    }

    public boolean isKeyDown()
    {
        if (this == Minecraft.getMinecraft().gameSettings.keyBindJump
                && shouldBlockJumpInCurrentScreen())
        {
            return false;
        }

        return this.pressed;
    }

    public String getKeyCategory()
    {
        return this.keyCategory;
    }

    public boolean isPressed()
    {
        if (this == Minecraft.getMinecraft().gameSettings.keyBindJump
                && shouldBlockJumpInCurrentScreen())
        {
            return false;
        }

        if (this.pressTime == 0)
        {
            return false;
        }
        else
        {
            --this.pressTime;
            return true;
        }
    }

    private void unpressKey()
    {
        this.pressTime = 0;
        this.pressed = false;
    }

    public String getKeyDescription()
    {
        return this.keyDescription;
    }

    public int getKeyCodeDefault()
    {
        return this.keyCodeDefault;
    }

    public int getKeyCode()
    {
        return this.keyCode;
    }

    public void setKeyCode(int keyCode)
    {
        this.keyCode = keyCode;
    }

    public int compareTo(KeyBinding p_compareTo_1_)
    {
        int i = I18n.format(this.keyCategory, new Object[0]).compareTo(I18n.format(p_compareTo_1_.keyCategory, new Object[0]));

        if (i == 0)
        {
            i = I18n.format(this.keyDescription, new Object[0]).compareTo(I18n.format(p_compareTo_1_.keyDescription, new Object[0]));
        }

        return i;
    }

    private static boolean shouldBlockJumpInCurrentScreen()
    {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen == null) {
            return false;
        }

        if (screen instanceof GuiChat
                || screen instanceof GuiEditSign
                || screen instanceof GuiScreenBook) {
            return true;
        }

        InvMoveModule inventoryMoveModule = Kinetic.INSTANCE.getModuleManager().getModule(InvMoveModule.class);
        return inventoryMoveModule == null || !inventoryMoveModule.isEnabled();
    }
}