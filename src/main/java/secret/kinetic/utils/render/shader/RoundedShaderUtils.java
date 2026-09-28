package secret.kinetic.utils.render.shader;

import static secret.kinetic.utils.misc.IMinecraft.mc;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform2i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import secret.kinetic.utils.client.FileUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class RoundedShaderUtils {
    private final int programID;

    public RoundedShaderUtils(String fragmentShaderLoc, String vertexShaderLoc) {
        int program = glCreateProgram();
        try {
            int fragmentShaderID;
            switch (fragmentShaderLoc) {
                case "roundRectTexture":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundRectTexture.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "roundRectOutline":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundRectOutline.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "roundedRect":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundedRect.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "roundedRectGradient":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundedRectGradient.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "roundedSoft":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundedSoft.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "liquid":
                    fragmentShaderID = createShader(new ByteArrayInputStream(liquid.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                default:
                    fragmentShaderID = createShader(mc.getResourceManager().getResource(new ResourceLocation(fragmentShaderLoc)).getInputStream(), GL_FRAGMENT_SHADER);
                    break;
            }
            glAttachShader(program, fragmentShaderID);

            int vertexShaderID = createShader(mc.getResourceManager().getResource(new ResourceLocation(vertexShaderLoc)).getInputStream(), GL_VERTEX_SHADER);
            glAttachShader(program, vertexShaderID);


        } catch (IOException e) {
            e.printStackTrace();
        }

        glLinkProgram(program);
        int status = glGetProgrami(program, GL_LINK_STATUS);

        if (status == 0) {
            throw new IllegalStateException("Shader failed to link!");
        }
        this.programID = program;
    }

    public RoundedShaderUtils(String fragmentShadersrc, boolean notUsed) {
        int program = glCreateProgram();
        int fragmentShaderID = createShader(new ByteArrayInputStream(fragmentShadersrc.getBytes()), GL_FRAGMENT_SHADER);
        int vertexShaderID = 0;
        try {
            vertexShaderID = createShader(mc.getResourceManager().getResource(new ResourceLocation("simp/shaders/vertex.vsh")).getInputStream(), GL_VERTEX_SHADER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        glAttachShader(program, fragmentShaderID);
        glAttachShader(program, vertexShaderID);


        glLinkProgram(program);
        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status == 0) {
            throw new IllegalStateException("Shader failed to link!");
        }
        this.programID = program;

    }

    public RoundedShaderUtils(String fragmentShaderLoc) {
        this(fragmentShaderLoc, "kinetic/shaders/vertex.vsh");
    }


    public void init() {
        glUseProgram(programID);
    }

    public void unload() {
        glUseProgram(0);
    }

    public int getUniform(String name) {
        return glGetUniformLocation(programID, name);
    }


    public void setUniformf(String name, float... args) {
        int loc = glGetUniformLocation(programID, name);
        switch (args.length) {
            case 1:
                glUniform1f(loc, args[0]);
                break;
            case 2:
                glUniform2f(loc, args[0], args[1]);
                break;
            case 3:
                glUniform3f(loc, args[0], args[1], args[2]);
                break;
            case 4:
                glUniform4f(loc, args[0], args[1], args[2], args[3]);
                break;
        }
    }

    public void setUniformi(String name, int... args) {
        int loc = glGetUniformLocation(programID, name);
        if (args.length > 1) glUniform2i(loc, args[0], args[1]);
        else glUniform1i(loc, args[0]);
    }

    public static void drawQuads(float x, float y, float width, float height) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(x, y);
        glTexCoord2f(0, 1);
        glVertex2f(x, y + height);
        glTexCoord2f(1, 1);
        glVertex2f(x + width, y + height);
        glTexCoord2f(1, 0);
        glVertex2f(x + width, y);
        glEnd();
    }

    public static void drawQuads() {
        ScaledResolution sr = new ScaledResolution(mc);
        float width = (float) sr.getScaledWidth_double();
        float height = (float) sr.getScaledHeight_double();
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(0, 0);
        glTexCoord2f(0, 0);
        glVertex2f(0, height);
        glTexCoord2f(1, 0);
        glVertex2f(width, height);
        glTexCoord2f(1, 1);
        glVertex2f(width, 0);
        glEnd();
    }

    public static void drawQuads(float width, float height) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(0, 0);
        glTexCoord2f(0, 0);
        glVertex2f(0, height);
        glTexCoord2f(1, 0);
        glVertex2f(width, height);
        glTexCoord2f(1, 1);
        glVertex2f(width, 0);
        glEnd();
    }

    private int createShader(InputStream inputStream, int shaderType) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, FileUtils.readInputStream(inputStream));
        glCompileShader(shader);


        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            System.out.println(glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException(String.format("Shader (%s) failed to compile!", shaderType));
        }

        return shader;
    }

    private String roundRectTexture = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform sampler2D textureIn;\n" +
            "uniform float radius, alpha;\n" +
            "\n" +
            "float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {\n" +
            "    return length(max(abs(centerPos) -size, 0.)) - radius;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "    float safeRadius = min(radius, min(rectSize.x, rectSize.y) * 0.5);\n" +
            "    float distance = roundedBoxSDF((rectSize * .5) - (gl_TexCoord[0].st * rectSize), (rectSize * .5) - safeRadius - 1., safeRadius);\n" +
            "    float smoothedAlpha =  (1.0-smoothstep(0.0, 2.0, distance)) * alpha;\n" +
            "    gl_FragColor = vec4(texture2D(textureIn, gl_TexCoord[0].st).rgb, smoothedAlpha);\n" +
            "}";

    private String roundRectOutline = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform vec4 color, outlineColor;\n" +
            "uniform float radius, outlineThickness;\n" +
            "\n" +
            "float roundedSDF(vec2 centerPos, vec2 size, float radius) {\n" +
            "    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    float safeRadius = min(radius, min(rectSize.x, rectSize.y) * 0.5);\n" +
            "    float distance = roundedSDF(gl_FragCoord.xy - location - (rectSize * .5), (rectSize * .5) + (outlineThickness *.5) - 1.0, safeRadius);\n" +
            "\n" +
            "    float blendAmount = smoothstep(0., 2., abs(distance) - (outlineThickness * .5));\n" +
            "\n" +
            "    vec4 insideColor = (distance < 0.) ? color : vec4(outlineColor.rgb,  0.0);\n" +
            "    gl_FragColor = mix(outlineColor, insideColor, blendAmount);\n" +
            "\n" +
            "}";
    private String roundedRectGradient = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform vec4 color1, color2, color3, color4;\n" +
            "uniform float radius;\n" +
            "\n" +
            "#define NOISE .5/255.0\n" +
            "\n" +
            "float roundSDF(vec2 p, vec2 b, float r) {\n" +
            "    return length(max(abs(p) - b , 0.0)) - r;\n" +
            "}\n" +
            "\n" +
            "vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){\n" +
            "    vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);\n" +
            "    //Dithering the color\n" +
            "    // from https://shader-tutorial.dev/advanced/color-banding-dithering/\n" +
            "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 st = gl_TexCoord[0].st;\n" +
            "    vec2 halfSize = rectSize * .5;\n" +
            "    \n" +
            "   // use the bottom leftColor as the alpha\n" +
            "    float safeRadius = min(radius, min(halfSize.x, halfSize.y));\n" +
            "    float smoothedAlpha =  (1.0-smoothstep(0.0, 2., roundSDF(halfSize - (gl_TexCoord[0].st * rectSize), halfSize - safeRadius - 1., safeRadius)));\n" +
            "    vec4 gradient = createGradient(st, color1, color2, color3, color4);" +
            "    gl_FragColor = vec4(gradient.rgb, gradient.a * smoothedAlpha);\n" +
            "}";
    private final String roundedRect =
            "#version 120\n" +
                    "\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform vec4 color;\n" +
                    "uniform float radius;\n" +
                    "uniform bool blur;\n" +
                    "uniform vec4 corners;\n" + 
                    "\n" +
                    "float getRadius(vec2 st) {\n" +
                    "    // gl_TexCoord[0].st ranges from (0,0) top-left to (1,1) bottom-right\n" +
                    "    if (st.x < 0.5 && st.y < 0.5) return radius * corners.x;\n" +
                    "    if (st.x >= 0.5 && st.y < 0.5) return radius * corners.y;\n" +
                    "    if (st.x >= 0.5 && st.y >= 0.5) return radius * corners.z;\n" +
                    "    if (st.x < 0.5 && st.y >= 0.5) return radius * corners.w;\n" +
                    "    return radius;\n" +
                    "}\n" +
                    "\n" +
                    "float roundSDF(vec2 p, vec2 b, float r) {\n" +
                    "    return length(max(abs(p) - b, 0.0)) - r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 st = gl_TexCoord[0].st;\n" +
                    "    vec2 rectHalf = rectSize * 0.5;\n" +
                    "    float currentRadius = min(getRadius(st), min(rectHalf.x, rectHalf.y));\n" +
                    "    float dist = roundSDF(rectHalf - (st * rectSize), rectHalf - currentRadius - 1.0, currentRadius);\n" +
                    "    gl_FragColor = vec4(color.rgb, (1.0 - smoothstep(0.0, 1.0, dist)) * color.a);\n" +
                    "}";


    




    private final String roundedSoft =
            "#version 120\n" +
                    "\n" +
                    "uniform vec2 quadSize, rectHalf;\n" +
                    "uniform vec4 color1, color2, outlineColor;\n" +
                    "uniform float radius, softness, outlineWidth;\n" +
                    "\n" +
                    "float roundSDF(vec2 p, vec2 b, float r) {\n" +
                    "    vec2 q = abs(p) - b + r;\n" +
                    "    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 st = gl_TexCoord[0].st;\n" +
                    "    vec2 p = (st - 0.5) * quadSize;\n" +
                    "    float r = min(radius, min(rectHalf.x, rectHalf.y));\n" +
                    "    float d = roundSDF(p, rectHalf, r);\n" +
                    "    float aa = max(fwidth(d), 0.0001);\n" +
                    "    float cover;\n" +
                    "    if (softness > 0.0) {\n" +
                    "        float t = clamp(d / softness, 0.0, 1.0);\n" +
                    "        cover = (1.0 - t) * (1.0 - t);\n" +
                    "    } else {\n" +
                    "        cover = clamp(0.5 - d / aa, 0.0, 1.0);\n" +
                    "    }\n" +
                    "    vec4 fill = mix(color1, color2, clamp(st.x, 0.0, 1.0));\n" +
                    "    fill.a *= cover;\n" +
                    "    if (outlineWidth > 0.0) {\n" +
                    "        float band = clamp(0.5 - d / aa, 0.0, 1.0) - clamp(0.5 - (d + outlineWidth) / aa, 0.0, 1.0);\n" +
                    "        float oa = outlineColor.a * band;\n" +
                    "        float outA = oa + fill.a * (1.0 - oa);\n" +
                    "        vec3 rgb = outA > 0.0 ? (outlineColor.rgb * oa + fill.rgb * fill.a * (1.0 - oa)) / outA : fill.rgb;\n" +
                    "        gl_FragColor = vec4(rgb, outA);\n" +
                    "    } else {\n" +
                    "        gl_FragColor = fill;\n" +
                    "    }\n" +
                    "}";


    






    private final String liquid =
            "#version 120\n" +
                    "\n" +
                    "uniform vec2 quadSize, rectHalf, seed;\n" +
                    "uniform vec4 color1, color2;\n" +
                    "uniform float radius, time, scale, fade, gloss, outline;\n" +
                    "\n" +
                    "float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123); }\n" +
                    "float noise(vec2 p) {\n" +
                    "    vec2 i = floor(p);\n" +
                    "    vec2 f = fract(p);\n" +
                    "    vec2 u = f * f * (3.0 - 2.0 * f);\n" +
                    "    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),\n" +
                    "               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);\n" +
                    "}\n" +
                    "float fbm(vec2 p) {\n" +
                    "    float v = 0.0;\n" +
                    "    float a = 0.5;\n" +
                    "    for (int i = 0; i < 4; i++) {\n" +
                    "        v += a * noise(p);\n" +
                    "        p = p * 2.03 + vec2(1.7, 9.2);\n" +
                    "        a *= 0.5;\n" +
                    "    }\n" +
                    "    return v;\n" +
                    "}\n" +
                    "float roundSDF(vec2 p, vec2 b, float r) {\n" +
                    "    vec2 q = abs(p) - b + r;\n" +
                    "    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 st = gl_TexCoord[0].st;\n" +
                    "    vec2 p = (st - 0.5) * quadSize;\n" +
                    "    float r = min(radius, min(rectHalf.x, rectHalf.y));\n" +
                    "    float d = roundSDF(p, rectHalf, r);\n" +
                    "    float aa = max(fwidth(d), 0.0001);\n" +
                    "    float cover = clamp(0.5 - d / aa, 0.0, 1.0);\n" +
                    "    if (outline > 0.0) cover -= clamp(0.5 - (d + outline) / aa, 0.0, 1.0);\n" +
                    "    if (cover <= 0.0) discard;\n" +
                    "\n" +
                    "    vec2 uv = (p + seed) / max(scale, 1.0);\n" +
                    "    vec2 q = vec2(fbm(uv + vec2(0.0, time * 0.32)), fbm(uv + vec2(5.2, 1.3) - vec2(time * 0.27, 0.0)));\n" +
                    "    float f = fbm(uv + 1.9 * q + vec2(time * 0.11, -time * 0.07));\n" +
                    "    vec4 col = mix(color2, color1, smoothstep(0.28, 0.78, f));\n" +
                    "    float ridge = smoothstep(0.60, 0.86, f) * gloss;\n" +
                    "    col.rgb = mix(col.rgb, vec3(1.0), ridge * 0.30);\n" +
                    "    float top = p.y + rectHalf.y;\n" +
                    "    float spec = (1.0 - smoothstep(0.0, max(1.5, rectHalf.y * 0.55), top)) * gloss;\n" +
                    "    col.rgb = mix(col.rgb, vec3(1.0), spec * 0.22);\n" +
                    "    if (fade > 0.0) col.a *= pow(clamp(1.0 - top / (2.0 * rectHalf.y), 0.0, 1.0), fade);\n" +
                    "    col.a *= cover;\n" +
                    "    gl_FragColor = col;\n" +
                    "}";

}
