package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PreUpdateEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Render3DEvent;
import secret.kinetic.api.events.impl.world.WorldJoinEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.properties.Property;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.render.ESPUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.Render3D;
import secret.kinetic.utils.render.RoundedUtils;
import secret.kinetic.utils.world.BedUtils;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;





@ModuleInfo(label = "Bed ESP", description = "Shows beds in their team colour with name and distance", category = ModuleCategory.RENDER, enabledByDefault = true)
public final class BedESPModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 32, 8, 64, 4);
    private final Property<Boolean> fill = new Property<>("Fill", true);
    private final Property<Boolean> labels = new Property<>("Labels", true);
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", true);

    private static final int HEIGHT = 20;
    private static final int LAYERS_PER_TICK = 2;

    
    private final Map<BlockPos, BlockPos[]> beds = new LinkedHashMap<>();
    private int scanLayer;

    @Override
    public void onDisable() {
        beds.clear();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        beds.clear();
        scanLayer = 0;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        int r = range.getValue().intValue();
        BlockPos origin = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        for (int step = 0; step < LAYERS_PER_TICK; step++) {
            int dy = scanLayer - HEIGHT;
            scanLayer = (scanLayer + 1) % (HEIGHT * 2 + 1);
            int y = origin.getY() + dy;
            if (y < 0 || y > 255) continue;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
                    IBlockState state = mc.theWorld.getBlockState(pos);
                    if (state.getBlock() == Blocks.bed && state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT && !beds.containsKey(pos)) {
                        beds.put(pos, new BlockPos[]{pos, pos.offset(state.getValue(BlockBed.FACING))});
                    }
                }
            }
        }
        beds.entrySet().removeIf(e -> mc.theWorld.getBlockState(e.getKey()).getBlock() != Blocks.bed);
    }

    private AxisAlignedBB box(BlockPos[] bed) {
        BlockPos a = bed[0], b = bed[1];
        return new AxisAlignedBB(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, a.getY() + 0.5625, Math.max(a.getZ(), b.getZ()) + 1);
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (beds.isEmpty()) return;
        Render3D.begin(throughWalls.getValue());
        try {
            for (BlockPos[] bed : beds.values()) {
                BedUtils.Team team = BedUtils.teamOf(bed[0]);
                AxisAlignedBB b = Render3D.toCamera(box(bed).expand(0.01, 0.01, 0.01));
                if (fill.getValue()) Render3D.fill(b, team.color, 0.2f);
                Render3D.outline(b, team.color, 0.95f, 1.6f);
            }
        } finally {
            Render3D.end();
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (!labels.getValue() || beds.isEmpty()) return;
        ScaledResolution sr = new ScaledResolution(mc);
        CustomFontRenderer font = FontUtils.getFont("sf-bold", 15);
        List<BlockPos[]> list = new ArrayList<>(beds.values());
        for (BlockPos[] bed : list) {
            AxisAlignedBB b = box(bed);
            double cx = (b.minX + b.maxX) / 2 - mc.getRenderManager().viewerPosX;
            double cy = b.maxY + 0.45 - mc.getRenderManager().viewerPosY;
            double cz = (b.minZ + b.maxZ) / 2 - mc.getRenderManager().viewerPosZ;
            Vector3f screen = ESPUtils.projectWorld((float) cx, (float) cy, (float) cz, sr.getScaleFactor());
            if (screen == null || screen.z < 0 || screen.z >= 1) continue;
            BedUtils.Team team = BedUtils.teamOf(bed[0]);
            int distance = (int) Math.round(mc.thePlayer.getDistance((b.minX + b.maxX) / 2, b.minY, (b.minZ + b.maxZ) / 2));
            String name = team.name;
            String dist = distance + "m";
            float w = font.getStringWidth(name) + 6 + font.getStringWidth(dist) + 12;
            float h = font.getHeight() + 7;
            float x = screen.x - w / 2f, y = screen.y - h;
            RoundedUtils.drawSmoothRect(x, y, w, h, 4f, new Color(12, 13, 16, 190));
            RoundedUtils.drawSmoothRect(x, y + h - 1.5f, w, 1.5f, 0.75f, team.color);
            font.drawString(name, x + 6, y + 3.5f, team.color.getRGB());
            font.drawString(dist, x + 6 + font.getStringWidth(name) + 6, y + 3.5f, new Color(200, 200, 210).getRGB());
        }
    }
}
