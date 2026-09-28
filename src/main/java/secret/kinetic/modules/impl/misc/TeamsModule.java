package secret.kinetic.modules.impl.misc;

import secret.kinetic.Kinetic;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.ModeProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.awt.Color;






@ModuleInfo(label = "Teams", description = "Detects teams from the tab list colour (Hypixel) for Aura, Name Tags and ESP", category = ModuleCategory.MISC, enabledByDefault = true)
public final class TeamsModule extends Module {

    public enum Mode {
        TAB_COLOR("Tab Color"), SCOREBOARD("Scoreboard Team"), ARMOR("Armor Color");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.TAB_COLOR);
    public final Property<Boolean> colorVisuals = new Property<>("Team Colors In Visuals", true);

    private static TeamsModule active() {
        if (Kinetic.INSTANCE.getModuleManager() == null) return null;
        TeamsModule module = Kinetic.INSTANCE.getModuleManager().getModule(TeamsModule.class);
        return module != null && module.isEnabled() ? module : null;
    }

    public static boolean isActive() {
        return active() != null;
    }

    
    public static boolean isTeammate(EntityPlayer a, EntityPlayer b) {
        TeamsModule module = active();
        if (module == null || a == null || b == null) return false;
        if (module.mode.getValue() == Mode.ARMOR) {
            int ca = armorColor(a), cb = armorColor(b);
            return ca != -1 && ca == cb;
        }
        char ta = teamCode(a, module.mode.getValue()), tb = teamCode(b, module.mode.getValue());
        return ta != 0 && ta == tb;
    }

    
    public static Color visualColor(EntityPlayer player) {
        TeamsModule module = active();
        if (module == null || !module.colorVisuals.getValue() || player == null) return null;
        if (module.mode.getValue() == Mode.ARMOR) {
            int rgb = armorColor(player);
            return rgb == -1 ? null : new Color(rgb);
        }
        char code = teamCode(player, module.mode.getValue());
        return code == 0 ? null : colorOf(code);
    }

    
    public static Color colorOf(char code) {
        int index = "0123456789abcdef".indexOf(Character.toLowerCase(code));
        if (index < 0) return Color.WHITE;
        int rgb = Minecraft.getMinecraft().fontRendererObj.getColorCode("0123456789abcdef".charAt(index));
        return new Color(rgb);
    }

    
    public static char teamCode(EntityPlayer player, Mode mode) {
        if (mode == Mode.SCOREBOARD) {
            ScorePlayerTeam team = (ScorePlayerTeam) player.getTeam();
            return team == null ? 0 : lastColor(team.getColorPrefix(), -1);
        }
        Minecraft mc = Minecraft.getMinecraft();
        String formatted = null;
        NetworkPlayerInfo info = mc.getNetHandler() == null ? null : mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info != null && mc.ingameGUI != null) formatted = mc.ingameGUI.getTabList().getPlayerName(info);
        if (formatted == null || formatted.isEmpty()) formatted = player.getDisplayName().getFormattedText();
        int at = formatted.indexOf(player.getName());
        char code = lastColor(formatted, at < 0 ? -1 : at);
        if (code == 0) {
            ScorePlayerTeam team = (ScorePlayerTeam) player.getTeam();
            if (team != null) code = lastColor(team.getColorPrefix(), -1);
        }
        return code;
    }

    
    private static char lastColor(String text, int end) {
        if (text == null) return 0;
        int limit = end < 0 ? text.length() : Math.min(end, text.length());
        char found = 0;
        for (int i = 0; i < limit && i + 1 < text.length(); i++) {
            if (text.charAt(i) == '§') {
                char c = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdef".indexOf(c) >= 0) found = c;
            }
        }
        return found;
    }

    private static int armorColor(EntityPlayer player) {
        for (int slot = 3; slot >= 0; slot--) {
            ItemStack stack = player.getCurrentArmor(slot);
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER && armor.hasColor(stack)) {
                    return armor.getColor(stack) & 0xFFFFFF;
                }
            }
        }
        return -1;
    }
}
