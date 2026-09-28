package secret.kinetic.modules.impl.render;

import secret.kinetic.Kinetic;
import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.player.KillEvent;
import secret.kinetic.api.events.impl.player.PlayerDamageEvent;
import secret.kinetic.api.events.impl.player.PlayerDeathEvent;
import secret.kinetic.api.events.impl.render.Render2DEvent;
import secret.kinetic.api.events.impl.render.Shader2DEvent;
import secret.kinetic.api.font.CustomFontRenderer;
import secret.kinetic.api.gui.click.classic.Theme;
import secret.kinetic.managers.impl.ColorManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;
import secret.kinetic.modules.ModuleInfo;
import secret.kinetic.utils.misc.IMinecraft;
import secret.kinetic.utils.render.DragUtils;
import secret.kinetic.utils.render.FontUtils;
import secret.kinetic.utils.render.GlassUtils;
import secret.kinetic.utils.render.KineticImage;
import secret.kinetic.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;





@ModuleInfo(label = "Kinetic Chat", description = "Displays Kinetic reacting to your actions in real time", category = ModuleCategory.RENDER)
public class KineticChatModule extends Module implements IMinecraft {

    private static final String KEY = "KineticChat";

    private static final float LOGO_SIZE = 128f;
    private static final float MIN_TEXTBOX_WIDTH = 214f;
    private static final float TEXTBOX_HEIGHT = 40f;
    private static final float TEXTBOX_RADIUS = 8f;
    private static final float PADDING = 6f;
    private static final float TEXT_PADDING = 12f;
    
    private static final float BUST_VISIBLE = 0.56f;
    private static final long FADE_MS = 260L;

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);
    private final Deque<Message> messages = new ArrayDeque<>();
    private final Random random = new Random();

    private long lastIdleCheck = 0L;
    private int kills = 0;
    private boolean welcomed;

    private static final String[] START_MESSAGES = {
            "Ara ara, you took your time. I was starting to miss you.",
            "There you are. My cute servant finally reports for duty.",
            "Stay close to me today. I have a feeling it'll be a good one.",
            "The club room felt empty without you. Shall we begin?",
            "I kept your seat warm. Don't make me wait like that again.",
            "Crimson suits you, you know. Let's go paint the server with it."
    };

    private static final String[] KILL_MESSAGES_GENERIC = {
            "Well done. That's exactly what I expect from my servant.",
            "Clean and decisive. You make your master proud.",
            "One less piece on the board. Keep going.",
            "Ara, you didn't even hesitate. I like that.",
            "They should have known better than to stand in your way.",
            "That's the power of the Gremory household.",
            "Beautiful. Almost as elegant as my Power of Destruction.",
            "Another one down. I'll reward you later.",
            "You're getting stronger every day. I can feel it.",
            "Don't let it go to your head... but that was lovely.",
            "Checkmate for them. Next move is yours.",
            "Such focus. It's almost enough to make me blush."
    };

    private static final String[] KILL_MESSAGES_MILESTONE = {
            "Five more? You really are my strongest piece.",
            "At this rate I'll have to promote you to Queen.",
            "Relentless. That's the servant I chose.",
            "They'll be whispering your name in the underworld tonight.",
            "I've stopped counting. You haven't stopped once.",
            "Keep this up and even my brother would be impressed.",
            "Magnificent. No one stands a chance against us today."
    };

    private static final String[] DAMAGE_MESSAGES = {
            "Careful! Nobody hurts my servant and gets away with it.",
            "Are you alright? Stay behind me if you have to.",
            "That one hurt, didn't it? Breathe. You're fine.",
            "Don't be reckless. I'd rather heal you than lose you.",
            "Watch your back, I can't do everything for you.",
            "Stay focused. I believe in you.",
            "They'll pay for that. Just give me a moment."
    };

    private static final String[] DEATH_MESSAGES = {
            "Don't worry. I'll bring you back, I always do.",
            "That wasn't your fault. Get up, we're not done yet.",
            "Rest for a moment... then show them what a Gremory servant is.",
            "I won't let them have the last word. Come back to me.",
            "Even the best pieces fall sometimes. Try again.",
            "I'm right here. Let's take it back together."
    };

    private static final String[] IDLE_MESSAGES = {
            "It's quiet... Shall I make us some tea?",
            "You know, you look rather cute when you concentrate.",
            "I've been thinking about our next move.",
            "Akeno says I'm spoiling you. I don't see the problem.",
            "Take your time. I'm not going anywhere.",
            "A little chess later? I promise to go easy on you. Maybe.",
            "The night is young. What should we do next?",
            "Being a devil isn't so bad with company like yours.",
            "Stay by my side and I'll show you the whole underworld.",
            "Don't forget: you belong to me now."
    };

    public KineticChatModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        kills = 0;
        messages.clear();
        lastIdleCheck = System.currentTimeMillis();
        if (!welcomed) {
            welcomed = true;
            
            pushMessage(welcomeMessage(), 8000);
            messages.peekLast().time = -1L;
        } else {
            pushMessage(START_MESSAGES[random.nextInt(START_MESSAGES.length)], 7000);
        }
    }

    @Override
    public void onDisable() {
        messages.clear();
    }

    private String welcomeMessage() {
        String name = mc.getSession() != null ? mc.getSession().getUsername() : null;
        String base = "Welcome back to " + Kinetic.NAME + " Client";
        return name == null || name.isEmpty() ? base + "." : base + ", " + name + ".";
    }

    @EventHook
    public void onKill(KillEvent event) {
        kills++;
        if (kills == 1) {
            pushMessage("First blood. I knew I chose well.", 7000);
        } else if (kills % 5 == 0) {
            pushMessage(KILL_MESSAGES_MILESTONE[random.nextInt(KILL_MESSAGES_MILESTONE.length)], 8000);
        } else {
            pushMessage(KILL_MESSAGES_GENERIC[random.nextInt(KILL_MESSAGES_GENERIC.length)], 6000);
        }
    }

    @EventHook
    public void onDamage(PlayerDamageEvent event) {
        if (random.nextFloat() < 0.4f) {
            pushMessage(DAMAGE_MESSAGES[random.nextInt(DAMAGE_MESSAGES.length)], 6000);
        }
    }

    @EventHook
    public void onDeath(PlayerDeathEvent event) {
        pushMessage(DEATH_MESSAGES[random.nextInt(DEATH_MESSAGES.length)], 9000);
    }

    private void pushMessage(String text, long lifetimeMs) {
        messages.clear();
        messages.addLast(new Message(text, System.currentTimeMillis(), lifetimeMs));
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        render(null);
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        render(event.getShaderType());
    }

    private void render(Shader2DEvent.ShaderType pass) {
        long now = System.currentTimeMillis();
        if (pass == null) {
            for (Message message : messages) {
                if (message.time < 0L) message.time = now;
            }
            messages.removeIf(m -> now - m.time >= m.lifetimeMs);
            if (messages.isEmpty() && (now - lastIdleCheck > 12_000)) {
                lastIdleCheck = now;
                if (random.nextFloat() < 0.5f) {
                    pushMessage(IDLE_MESSAGES[random.nextInt(IDLE_MESSAGES.length)], 7500);
                }
            }
        }

        CustomFontRenderer font = FontUtils.getFont("sf", 15);
        if (font == null) return;

        Message activeMessage = messages.peekFirst();
        float boxWidth = MIN_TEXTBOX_WIDTH;
        if (activeMessage != null) {
            boxWidth = Math.max(MIN_TEXTBOX_WIDTH, font.getStringWidth(activeMessage.text) + TEXT_PADDING * 2f);
        }

        float bustHeight = LOGO_SIZE * BUST_VISIBLE;
        float totalWidth = Math.max(LOGO_SIZE, boxWidth) + PADDING * 2f;
        float totalHeight = bustHeight + TEXTBOX_HEIGHT + PADDING * 2f;
        component.setWidth(totalWidth);
        component.setHeight(totalHeight);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth() - totalWidth) {
            x = sr.getScaledWidth() - totalWidth;
            component.setX(x);
        }
        if (x < 0) {
            x = 0;
            component.setX(0);
        }
        if (y > sr.getScaledHeight() - totalHeight) {
            y = sr.getScaledHeight() - totalHeight;
            component.setY(y);
        }
        if (y < 0) {
            y = 0;
            component.setY(0);
        }

        float boxX = x + PADDING + Math.max(0f, (LOGO_SIZE - boxWidth) / 2f);
        float boxY = y + PADDING + bustHeight;

        if (pass == Shader2DEvent.ShaderType.BLUR) {
            GlassUtils.drawMask(boxX, boxY, boxWidth, TEXTBOX_HEIGHT, TEXTBOX_RADIUS);
            return;
        }
        if (pass != null) return;

        
        float logoWidth = LOGO_SIZE * KineticImage.LOGO_ASPECT;
        float logoX = boxX + boxWidth / 2f - logoWidth / 2f;
        float logoY = y + PADDING;
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (logoX * scale), (int) ((sr.getScaledHeight() - (boxY + TEXTBOX_RADIUS)) * scale),
                Math.max(1, (int) (logoWidth * scale)), Math.max(1, (int) ((bustHeight + TEXTBOX_RADIUS) * scale)));
        KineticImage.drawLogo(logoX, logoY, logoWidth, LOGO_SIZE, 1f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        Color accent = ColorManager.getColor();
        GlassUtils.drawGlass(boxX, boxY, boxWidth, TEXTBOX_HEIGHT, TEXTBOX_RADIUS, 1f, accent);

        
        CustomFontRenderer small = FontUtils.getFont("sf-bold", 14);
        if (small != null) {
            String tag = Kinetic.NAME;
            float tagW = small.getStringWidth(tag) + 12f;
            float tagH = small.getHeight() + 4f;
            float tagX = boxX + 8f;
            float tagY = boxY - tagH / 2f;
            RoundedUtils.drawSmoothShadow(tagX, tagY, tagW, tagH, tagH / 2f, 5f, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));
            RoundedUtils.drawSmoothGradientRect(tagX, tagY, tagW, tagH, tagH / 2f, Theme.accentAlt(), accent);
            small.drawString(tag, tagX + 6f, tagY + (tagH - small.getHeight()) / 2f + 0.5f, Color.WHITE.getRGB());
        }

        if (activeMessage != null) {
            long age = now - activeMessage.time;
            long left = activeMessage.lifetimeMs - age;
            float alpha = Math.min(1f, Math.min(age, left) / (float) FADE_MS);
            
            int shown = Math.min(activeMessage.text.length(), (int) (age / 18L));
            String text = activeMessage.text.substring(0, shown);
            float textX = boxX + TEXT_PADDING;
            float textY = boxY + TEXTBOX_HEIGHT / 2f - font.getHeight() / 2f + 2f;
            int a = Math.max(4, Math.min(255, (int) (255 * alpha)));
            font.drawString(text, textX, textY, new Color(245, 242, 248, a).getRGB());
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static class Message {
        final String text;
        long time;
        final long lifetimeMs;

        Message(String text, long time, long lifetimeMs) {
            this.text = text;
            this.time = time;
            this.lifetimeMs = lifetimeMs;
        }
    }
}
