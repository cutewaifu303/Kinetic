package secret.kinetic.utils.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;





public final class BedUtils {

    public static final class Team {
        public final String name;
        public final Color color;
        public final char code;

        Team(String name, Color color, char code) {
            this.name = name;
            this.color = color;
            this.code = code;
        }
    }

    public static final Team UNKNOWN = new Team("Bed", new Color(220, 60, 60), 'c');
    private static final int RADIUS = 5;
    private static final Map<BlockPos, Team> cache = new HashMap<>();
    private static Object cacheWorld;

    private BedUtils() {
    }

    public static boolean isBed(BlockPos pos) {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld != null && mc.theWorld.getBlockState(pos).getBlock() == Blocks.bed;
    }

    
    public static BlockPos[] halves(BlockPos pos) {
        Minecraft mc = Minecraft.getMinecraft();
        IBlockState state = mc.theWorld.getBlockState(pos);
        if (state.getBlock() != Blocks.bed) return null;
        EnumFacingHelper facing = new EnumFacingHelper(state);
        return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT
                ? new BlockPos[]{pos, pos.offset(facing.facing)}
                : new BlockPos[]{pos.offset(facing.facing.getOpposite()), pos};
    }

    private static final class EnumFacingHelper {
        final net.minecraft.util.EnumFacing facing;

        EnumFacingHelper(IBlockState state) {
            this.facing = state.getValue(BlockBed.FACING);
        }
    }

    
    public static Team teamOf(BlockPos pos) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return UNKNOWN;
        if (cacheWorld != mc.theWorld) {
            cache.clear();
            cacheWorld = mc.theWorld;
        }
        BlockPos[] bed = halves(pos);
        BlockPos key = bed == null ? pos : bed[0];
        Team cached = cache.get(key);
        if (cached != null) return cached;

        int[] counts = new int[16];
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    IBlockState state = mc.theWorld.getBlockState(key.add(x, y, z));
                    Block block = state.getBlock();
                    if (block == Blocks.stained_hardened_clay || block == Blocks.wool || block == Blocks.stained_glass
                            || block == Blocks.carpet || block == Blocks.stained_glass_pane) {
                        EnumDyeColor dye = EnumDyeColor.byMetadata(block.getMetaFromState(state));
                        
                        counts[dye.getMetadata()] += dye == EnumDyeColor.WHITE ? 0 : 1;
                    }
                }
            }
        }
        int best = -1;
        for (int i = 0; i < 16; i++) if (counts[i] > 0 && (best < 0 || counts[i] > counts[best])) best = i;
        Team team = best < 0 ? UNKNOWN : fromDye(EnumDyeColor.byMetadata(best));
        cache.put(key, team);
        return team;
    }

    private static Team fromDye(EnumDyeColor dye) {
        switch (dye) {
            case RED:
                return new Team("Red Bed", new Color(255, 85, 85), 'c');
            case BLUE:
                return new Team("Blue Bed", new Color(85, 85, 255), '9');
            case LIME:
            case GREEN:
                return new Team("Green Bed", new Color(85, 255, 85), 'a');
            case YELLOW:
                return new Team("Yellow Bed", new Color(255, 255, 85), 'e');
            case CYAN:
            case LIGHT_BLUE:
                return new Team("Aqua Bed", new Color(85, 255, 255), 'b');
            case PINK:
            case MAGENTA:
                return new Team("Pink Bed", new Color(255, 85, 255), 'd');
            case GRAY:
            case SILVER:
                return new Team("Gray Bed", new Color(170, 170, 170), '7');
            case ORANGE:
                return new Team("Orange Bed", new Color(255, 170, 0), '6');
            case PURPLE:
                return new Team("Purple Bed", new Color(170, 0, 170), '5');
            case BLACK:
                return new Team("Black Bed", new Color(90, 90, 90), '0');
            case BROWN:
                return new Team("Brown Bed", new Color(150, 100, 60), '6');
            default:
                return new Team("White Bed", new Color(240, 240, 240), 'f');
        }
    }
}
