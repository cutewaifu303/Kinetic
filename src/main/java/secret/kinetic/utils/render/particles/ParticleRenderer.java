package secret.kinetic.utils.render.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;











public class ParticleRenderer {

    
    private static final int MAX_PARTICLES = 256;

    private final List<Particle> particles = new ArrayList<Particle>();
    private final Random random = new Random();

    




    public void spawnOrbit(ParticleShape shape, ParticleColorMode colorMode,
                           float red, float green, float blue, float hue,
                           double radius, long lifetime, boolean pulse, long now) {
        Particle particle = new Particle();
        particle.shape = shape;
        particle.colorMode = colorMode;
        particle.red = red;
        particle.green = green;
        particle.blue = blue;
        particle.hue = hue;
        particle.hueSpeed = 0.05F + random.nextFloat() * 0.1F;
        particle.orbitAngle = random.nextDouble() * Math.PI * 2.0;
        particle.orbitSpeed = (0.15 + random.nextDouble() * 0.3) * (random.nextBoolean() ? 1.0 : -1.0);
        particle.orbitRadius = radius * (0.55 + random.nextDouble() * 0.45);
        particle.wobblePhase = random.nextDouble() * Math.PI * 2.0;
        particle.wobbleSpeed = 0.5 + random.nextDouble();
        particle.baseHeight = 0.2 + random.nextDouble() * 1.5;
        particle.riseSpeed = 0.05 + random.nextDouble() * 0.2;
        particle.motionX = (random.nextDouble() - 0.5) * 0.05;
        particle.motionY = (random.nextDouble() - 0.5) * 0.05;
        particle.motionZ = (random.nextDouble() - 0.5) * 0.05;
        particle.size = 0.05F + random.nextFloat() * 0.06F;
        particle.rotation = random.nextFloat() * 360.0F;
        particle.rotationSpeed = (random.nextFloat() - 0.5F) * 120.0F;
        particle.twinklePhase = random.nextFloat() * (float) Math.PI * 2.0F;
        particle.twinkleSpeed = 3.0F + random.nextFloat() * 4.0F;
        particle.pulse = pulse;
        particle.spawnTime = now;
        particle.lifetime = lifetime;

        particles.add(particle);
        while (particles.size() > MAX_PARTICLES) {
            particles.remove(0);
        }
    }

    public boolean isEmpty() {
        return particles.isEmpty();
    }

    public void clear() {
        particles.clear();
    }

    




    public void render(double anchorX, double anchorY, double anchorZ, long now, float opacity) {
        if (particles.isEmpty() || opacity <= 0.0F) return;

        Minecraft mc = Minecraft.getMinecraft();
        RenderManager renderManager = mc.getRenderManager();
        double camX = renderManager.viewerPosX;
        double camY = renderManager.viewerPosY;
        double camZ = renderManager.viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();

        Iterator<Particle> iterator = particles.iterator();

        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            if (particle.isDead(now)) {
                iterator.remove();
                continue;
            }

            float alpha = particle.alpha(now) * opacity;
            if (alpha <= 0.01F) continue;

            float scale = particle.scale(now);
            float red = particle.red;
            float green = particle.green;
            float blue = particle.blue;

            if (particle.colorMode == ParticleColorMode.RAINBOW) {
                float hue = (particle.hue + particle.hueSpeed * (float) ((now - particle.spawnTime) / 1000.0)) % 1.0F;
                int rgb = Color.HSBtoRGB(hue, 0.85F, 1.0F);
                red = ((rgb >> 16) & 255) / 255.0F;
                green = ((rgb >> 8) & 255) / 255.0F;
                blue = (rgb & 255) / 255.0F;
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(particle.x(anchorX, now) - camX,
                    particle.y(anchorY, now) - camY,
                    particle.z(anchorZ, now) - camZ);
            GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(renderManager.playerViewX, (mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F), 0.0F, 0.0F);
            GlStateManager.rotate(particle.roll(now), 0.0F, 0.0F, 1.0F);

            
            GlStateManager.color(red, green, blue, alpha * 0.2F);
            particle.shape.draw(scale * 1.8);
            GlStateManager.color(red, green, blue, alpha);
            particle.shape.draw(scale);

            GlStateManager.popMatrix();
        }

        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.resetColor();
    }
}
