package secret.kinetic.utils.render;

import secret.kinetic.managers.impl.ColorManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;















public final class VideoBackground {

    private static final String DIR = "/assets/minecraft/kinetic/video/";
    
    private static final float SOURCE_HUE = 0.975f;
    private static final int RING = 4;
    
    private static final long IDLE_MS = 1500L;

    private static VideoBackground instance;

    private final Object idleLock = new Object();
    private final ArrayBlockingQueue<Frame> free = new ArrayBlockingQueue<>(RING);
    private final ArrayBlockingQueue<Frame> ready = new ArrayBlockingQueue<>(RING);

    private int frames;
    private int fps = 24;
    private int videoWidth;
    private int videoHeight;
    private boolean available;
    private boolean loaded;

    private volatile boolean running;
    private volatile long wanted;
    private volatile long lastRequest;
    private Thread decoder;

    
    private final int[] textures = {-1, -1};
    private final long[] textureSeq = {-1L, -1L};
    private long shownSeq = -1L;
    private long startTime;
    private int current, next = -1;
    private float blend;

    private int program;
    private boolean shaderFailed;
    private int uTexture, uTexture2, uMix, uHueShift, uSaturation, uAlpha, uVignette, uViewport, uTime;

    private VideoBackground() {
    }

    public static VideoBackground get() {
        if (instance == null) {
            instance = new VideoBackground();
        }
        return instance;
    }

    

    
    public void render(float width, float height) {
        render(0f, 0f, width, height, 1f);
    }

    public void render(float width, float height, float alpha) {
        render(0f, 0f, width, height, alpha);
    }

    



    public void render(float x, float y, float width, float height, float alpha) {
        if (width <= 0f || height <= 0f || alpha <= 0f) return;
        if (!ensureLoaded()) {
            fallback(x, y, width, height, alpha);
            return;
        }
        updateTexture();
        if (textures[current] < 0 || shownSeq < 0L) {
            fallback(x, y, width, height, alpha);
            return;
        }

        
        float target = width / height;
        float source = videoWidth / (float) videoHeight;
        float u0 = 0f, u1 = 1f, v0 = 0f, v1 = 1f;
        if (target > source) {
            float span = source / target;
            v0 = (1f - span) / 2f;
            v1 = v0 + span;
        } else {
            float span = target / source;
            u0 = (1f - span) / 2f;
            u1 = u0 + span;
        }

        Color accent = ColorManager.getColor();
        float[] hsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
        float hueShift = hsb[0] - SOURCE_HUE;
        
        float saturation = Math.min(1f, hsb[1] / 0.7f);

        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1f, 1f, 1f, Math.min(1f, alpha));
        GlStateManager.bindTexture(textures[current]);

        boolean shaded = useShader();
        if (shaded) {
            Minecraft mc = Minecraft.getMinecraft();
            int second = next >= 0 ? textures[next] : textures[current];
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.bindTexture(second);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL20.glUniform1i(uTexture, 0);
            GL20.glUniform1i(uTexture2, 1);
            GL20.glUniform1f(uMix, next >= 0 ? blend : 0f);
            GL20.glUniform1f(uHueShift, hueShift);
            GL20.glUniform1f(uSaturation, saturation);
            GL20.glUniform1f(uAlpha, Math.min(1f, alpha));
            GL20.glUniform1f(uVignette, 0.55f);
            GL20.glUniform2f(uViewport, Math.max(1, mc.displayWidth), Math.max(1, mc.displayHeight));
            GL20.glUniform1f(uTime, (System.currentTimeMillis() - startTime) % 100000L / 1000f);
        } else {
            
            GlStateManager.color(0.7f, 0.7f, 0.7f, Math.min(1f, alpha));
        }

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u0, v0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(u0, v1);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(u1, v1);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(u1, v0);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();

        if (shaded) {
            GL20.glUseProgram(0);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.bindTexture(0);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        GlStateManager.bindTexture(0);
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    
    public void shutdown() {
        running = false;
        loaded = false;
        available = false;
        Thread thread = decoder;
        decoder = null;
        if (thread != null) {
            thread.interrupt();
            synchronized (idleLock) {
                idleLock.notifyAll();
            }
        }
        for (int i = 0; i < textures.length; i++) {
            if (textures[i] >= 0) GL11.glDeleteTextures(textures[i]);
            textures[i] = -1;
            textureSeq[i] = -1L;
        }
        next = -1;
        if (program != 0) {
            GL20.glDeleteProgram(program);
            program = 0;
        }
        ready.clear();
        free.clear();
        shownSeq = -1L;
        loaded = false;
    }

    

    private boolean ensureLoaded() {
        if (loaded) return available;
        loaded = true;
        available = false;
        try (InputStream in = VideoBackground.class.getResourceAsStream(DIR + "info.txt")) {
            if (in == null) {
                System.err.println("[Kinetic] Background video frames are missing");
                return false;
            }
            String info = new String(readAll(in, new ByteArrayOutputStream()), "UTF-8");
            for (String token : info.trim().split("\\s+")) {
                int eq = token.indexOf('=');
                if (eq <= 0) continue;
                String key = token.substring(0, eq);
                int value = Integer.parseInt(token.substring(eq + 1).trim());
                if ("frames".equals(key)) frames = value;
                else if ("fps".equals(key)) fps = value;
                else if ("width".equals(key)) videoWidth = value;
                else if ("height".equals(key)) videoHeight = value;
            }
        } catch (Exception e) {
            System.err.println("[Kinetic] Could not read the background video info: " + e);
            return false;
        }
        if (frames <= 0 || fps <= 0 || videoWidth <= 0 || videoHeight <= 0) return false;

        int bytes = videoWidth * videoHeight * 3;
        free.clear();
        ready.clear();
        for (int i = 0; i < RING; i++) {
            free.offer(new Frame(BufferUtils.createByteBuffer(bytes)));
        }

        for (int i = 0; i < textures.length; i++) {
            textures[i] = GL11.glGenTextures();
            textureSeq[i] = -1L;
            GlStateManager.bindTexture(textures[i]);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, videoWidth, videoHeight, 0, GL12.GL_BGR,
                    GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        }
        current = 0;
        next = -1;
        GlStateManager.bindTexture(textures[0]);

        
        Frame first = free.poll();
        FrameDecoder sync = new FrameDecoder();
        try {
            if (first != null && sync.decode(0, first.data)) {
                upload(first);
                textureSeq[0] = 0L;
                shownSeq = 0L;
            }
        } finally {
            sync.dispose();
            if (first != null) free.offer(first);
        }
        GlStateManager.bindTexture(0);

        startTime = System.currentTimeMillis();
        available = true;
        running = true;
        decoder = new Thread(this::decodeLoop, "Kinetic-Video");
        decoder.setDaemon(true);
        decoder.setPriority(Thread.NORM_PRIORITY - 1);
        decoder.start();
        return true;
    }

    private boolean useShader() {
        if (shaderFailed) return false;
        if (program == 0) {
            program = compileProgram();
            if (program == 0) {
                shaderFailed = true;
                return false;
            }
            uTexture = GL20.glGetUniformLocation(program, "tex");
            uTexture2 = GL20.glGetUniformLocation(program, "tex2");
            uMix = GL20.glGetUniformLocation(program, "mixAmt");
            uHueShift = GL20.glGetUniformLocation(program, "hueShift");
            uSaturation = GL20.glGetUniformLocation(program, "saturation");
            uAlpha = GL20.glGetUniformLocation(program, "alpha");
            uVignette = GL20.glGetUniformLocation(program, "vignette");
            uViewport = GL20.glGetUniformLocation(program, "viewport");
            uTime = GL20.glGetUniformLocation(program, "time");
        }
        GL20.glUseProgram(program);
        return true;
    }

    private static int compileProgram() {
        int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX);
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT);
        if (vertex == 0 || fragment == 0) return 0;
        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vertex);
        GL20.glAttachShader(prog, fragment);
        GL20.glLinkProgram(prog);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("[Kinetic] Video shader link failed: " + GL20.glGetProgramInfoLog(prog, 4096));
            GL20.glDeleteProgram(prog);
            return 0;
        }
        return prog;
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("[Kinetic] Video shader compile failed: " + GL20.glGetShaderInfoLog(shader, 4096));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void fallback(float x, float y, float width, float height, float alpha) {
        Color accent = ColorManager.getColor();
        int base = new Color(10, 9, 13, Math.round(255 * Math.min(1f, alpha))).getRGB();
        RenderUtils.drawGradientRect(x, y, x + width, y + height, false, base,
                new Color(accent.getRed() / 4, accent.getGreen() / 4, accent.getBlue() / 4,
                        Math.round(255 * Math.min(1f, alpha))).getRGB());
    }

    

    




    private void updateTexture() {
        long now = System.currentTimeMillis();
        double position = (now - startTime) * fps / 1000.0;
        long seq = (long) position;
        wanted = seq;
        long previous = lastRequest;
        lastRequest = now;
        if (now - previous > IDLE_MS / 2) {
            synchronized (idleLock) {
                idleLock.notifyAll();
            }
        }

        Frame head;
        while ((head = ready.peek()) != null && head.seq <= seq + 1) {
            ready.poll();
            
            if (head.seq >= seq || head.seq > Math.max(textureSeq[0], textureSeq[1])) place(head);
            free.offer(head);
        }

        
        int a = -1;
        for (int i = 0; i < 2; i++) {
            if (textureSeq[i] >= 0 && textureSeq[i] <= seq && (a < 0 || textureSeq[i] > textureSeq[a])) a = i;
        }
        if (a < 0) a = textureSeq[0] <= textureSeq[1] || textureSeq[1] < 0 ? 0 : 1;
        int b = 1 - a;
        current = a;
        shownSeq = textureSeq[a];
        if (textureSeq[a] == seq && textureSeq[b] == seq + 1) {
            next = b;
            blend = (float) (position - seq);
        } else {
            next = -1;
            blend = 0f;
        }
    }

    
    private void place(Frame frame) {
        if (textureSeq[0] == frame.seq || textureSeq[1] == frame.seq) return;
        int slot = textureSeq[0] <= textureSeq[1] ? 0 : 1;
        GlStateManager.bindTexture(textures[slot]);
        upload(frame);
        textureSeq[slot] = frame.seq;
    }

    private void upload(Frame frame) {
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        frame.data.position(0);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, videoWidth, videoHeight, GL12.GL_BGR,
                GL11.GL_UNSIGNED_BYTE, frame.data);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
    }

    

    private void decodeLoop() {
        FrameDecoder decoder = new FrameDecoder();
        long next = 1L;
        try {
            while (running) {
                if (System.currentTimeMillis() - lastRequest > IDLE_MS) {
                    synchronized (idleLock) {
                        if (running && System.currentTimeMillis() - lastRequest > IDLE_MS) {
                            idleLock.wait(1000L);
                        }
                    }
                    continue;
                }
                long target = wanted;
                
                if (next <= target) next = target + 1L;

                Frame frame = free.poll(100L, TimeUnit.MILLISECONDS);
                if (frame == null) continue;
                if (decoder.decode((int) (next % frames), frame.data)) {
                    frame.seq = next++;
                    if (!ready.offer(frame)) free.offer(frame);
                } else {
                    free.offer(frame);
                    next++;
                    Thread.sleep(20L);
                }
            }
        } catch (InterruptedException ignored) {
            
        } catch (Throwable t) {
            System.err.println("[Kinetic] Background video decoder stopped: " + t);
        } finally {
            decoder.dispose();
        }
    }

    private static byte[] readAll(InputStream in, ByteArrayOutputStream out) throws IOException {
        out.reset();
        byte[] chunk = new byte[16384];
        int n;
        while ((n = in.read(chunk)) != -1) {
            out.write(chunk, 0, n);
        }
        return out.toByteArray();
    }

    private static final class Frame {
        final ByteBuffer data;
        long seq;

        Frame(ByteBuffer data) {
            this.data = data;
        }
    }

    
    private final class FrameDecoder {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream(64 * 1024);
        private ImageReader reader;
        private BufferedImage target;

        boolean decode(int index, ByteBuffer out) {
            String name = DIR + String.format("%04d.jpg", index);
            try (InputStream in = VideoBackground.class.getResourceAsStream(name)) {
                if (in == null) return false;
                byte[] data = readAll(in, bytes);
                BufferedImage image = read(data);
                if (image == null) return false;
                if (image.getWidth() != videoWidth || image.getHeight() != videoHeight
                        || image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                    BufferedImage converted = target();
                    Graphics2D g = converted.createGraphics();
                    g.drawImage(image, 0, 0, videoWidth, videoHeight, null);
                    g.dispose();
                    image = converted;
                }
                byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                out.clear();
                out.put(pixels, 0, Math.min(pixels.length, out.capacity()));
                out.flip();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private BufferedImage target() {
            if (target == null) target = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_3BYTE_BGR);
            return target;
        }

        private BufferedImage read(byte[] data) throws IOException {
            if (reader == null) {
                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jpeg");
                if (!readers.hasNext()) return ImageIO.read(new ByteArrayInputStream(data));
                reader = readers.next();
            }
            MemoryCacheImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(data));
            try {
                reader.setInput(stream, true, true);
                ImageReadParam param = reader.getDefaultReadParam();
                
                param.setDestination(target());
                try {
                    return reader.read(0, param);
                } catch (IllegalArgumentException incompatible) {
                    return reader.read(0);
                }
            } finally {
                reader.setInput(null);
                stream.close();
            }
        }

        void dispose() {
            if (reader != null) {
                reader.dispose();
                reader = null;
            }
        }
    }

    

    private static final String VERTEX =
            "#version 120\n" +
            "void main() {\n" +
            "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "    gl_Position = ftransform();\n" +
            "}\n";

    private static final String FRAGMENT =
            "#version 120\n" +
            "uniform sampler2D tex;\n" +
            "uniform sampler2D tex2;\n" +
            "uniform float mixAmt;\n" +
            "uniform float hueShift;\n" +
            "uniform float saturation;\n" +
            "uniform float alpha;\n" +
            "uniform float vignette;\n" +
            "uniform vec2 viewport;\n" +
            "uniform float time;\n" +
            "vec3 rgb2hsv(vec3 c) {\n" +
            "    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
            "    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
            "    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
            "    float d = q.x - min(q.w, q.y);\n" +
            "    float e = 1.0e-10;\n" +
            "    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
            "}\n" +
            "vec3 hsv2rgb(vec3 c) {\n" +
            "    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
            "    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
            "    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
            "}\n" +
            "void main() {\n" +
            "    vec3 source = mix(texture2D(tex, gl_TexCoord[0].st).rgb, texture2D(tex2, gl_TexCoord[0].st).rgb, mixAmt);\n" +
            "    vec3 hsv = rgb2hsv(source);\n" +
            "    hsv.x = fract(hsv.x + hueShift + 1.0);\n" +
            "    hsv.y *= saturation;\n" +
            "    vec3 color = hsv2rgb(hsv);\n" +
            
            "    vec3 luma = vec3(0.2126, 0.7152, 0.0722);\n" +
            "    color *= clamp(pow((dot(source, luma) + 0.002) / (dot(color, luma) + 0.002), 0.65), 0.7, 2.0);\n" +
            "    vec2 uv = gl_FragCoord.xy / viewport;\n" +
            "    vec2 d = (uv - 0.5) * vec2(1.0, 0.85);\n" +
            "    float edge = smoothstep(0.18, 0.78, length(d));\n" +
            "    color *= 1.0 - vignette * edge;\n" +
            "    color *= 0.82 + 0.18 * smoothstep(0.0, 0.5, uv.y);\n" +
            "    float n = fract(sin(dot(gl_FragCoord.xy + vec2(time * 61.0, time * 17.0), vec2(12.9898, 78.233))) * 43758.5453);\n" +
            "    color += (n - 0.5) / 255.0;\n" +
            "    gl_FragColor = vec4(color, alpha);\n" +
            "}\n";
}
