package secret.kinetic.modules.impl.render;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.PlayerAttackEvent;
import secret.kinetic.api.properties.impl.NumberProperty;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;

@ModuleInfo(label = "Hit FX", description = "Spawns particles when you hit an entity", category = ModuleCategory.RENDER)
public class HitFXModule extends Module {

    private final NumberProperty particles = new NumberProperty("Particles", 8, 1, 30, 1);

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (event.target == null || mc.theWorld == null) return;

        EntityLivingBase target = event.target;
        int count = particles.getValue().intValue();
        for (int i = 0; i < count; i++) {
            double dx = (Math.random() - 0.5) * target.width;
            double dy = Math.random() * target.height;
            double dz = (Math.random() - 0.5) * target.width;
            mc.theWorld.spawnParticle(EnumParticleTypes.CRIT,
                    target.posX + dx, target.posY + dy, target.posZ + dz,
                    (Math.random() - 0.5) * 0.4, Math.random() * 0.4, (Math.random() - 0.5) * 0.4);
        }
    }
}
