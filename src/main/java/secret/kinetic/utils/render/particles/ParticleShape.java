package secret.kinetic.utils.render.particles;

import org.lwjgl.opengl.GL11;

import java.util.Random;








public enum ParticleShape {
    STAR("Star"),
    HEART("Heart"),
    DIAMOND("Diamond"),
    SPARKLE("Sparkle"),
    NOTE("Note"),
    CIRCLE("Circle");

    public final String name;

    ParticleShape(String name) {
        this.name = name;
    }

    private static final ParticleShape[] VALUES = values();

    private static final double[] STAR_X = new double[8];
    private static final double[] STAR_Y = new double[8];
    private static final double[] SPARK_X = new double[16];
    private static final double[] SPARK_Y = new double[16];
    private static final double[] CIRCLE_X = new double[16];
    private static final double[] CIRCLE_Y = new double[16];
    private static final double[] HEART_X = new double[25];
    private static final double[] HEART_Y = new double[25];

    static {
        
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 2.0 + i * Math.PI / 4.0;
            double radius = i % 2 == 0 ? 1.0 : 0.28;
            STAR_X[i] = Math.cos(angle) * radius;
            STAR_Y[i] = Math.sin(angle) * radius;
        }
        
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI / 2.0 + i * Math.PI / 8.0;
            double radius = i % 2 == 1 ? 0.08 : (i % 4 == 0 ? 1.0 : 0.5);
            SPARK_X[i] = Math.cos(angle) * radius;
            SPARK_Y[i] = Math.sin(angle) * radius;
        }
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8.0;
            CIRCLE_X[i] = Math.cos(angle);
            CIRCLE_Y[i] = Math.sin(angle);
        }
        
        for (int i = 0; i <= 24; i++) {
            double t = i / 24.0 * Math.PI * 2.0;
            double sin = Math.sin(t);
            HEART_X[i] = 16.0 * sin * sin * sin / 18.0;
            HEART_Y[i] = (13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t)
                    - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t) + 6.0) / 18.0;
        }
    }

    



    public void draw(double size) {
        switch (this) {
            case STAR:
                drawStar(size);
                break;
            case HEART:
                drawHeart(size);
                break;
            case DIAMOND:
                drawDiamond(size);
                break;
            case SPARKLE:
                drawSparkle(size);
                break;
            case NOTE:
                drawNote(size);
                break;
            case CIRCLE:
            default:
                drawCircle(size);
                break;
        }
    }

    
    public static ParticleShape random(Random random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    private static void drawStar(double size) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        for (int i = 0; i <= 8; i++) {
            int index = i % 8;
            GL11.glVertex3d(STAR_X[index] * size, STAR_Y[index] * size, 0.0);
        }
        GL11.glEnd();

        
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        for (int i = 0; i <= 16; i++) {
            int index = i % 16;
            GL11.glVertex3d(CIRCLE_X[index] * size * 0.3, CIRCLE_Y[index] * size * 0.3, 0.0);
        }
        GL11.glEnd();
    }

    private static void drawSparkle(double size) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        for (int i = 0; i <= 16; i++) {
            int index = i % 16;
            GL11.glVertex3d(SPARK_X[index] * size, SPARK_Y[index] * size, 0.0);
        }
        GL11.glEnd();
    }

    private static void drawHeart(double size) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0.0, 0.05 * size, 0.0);
        for (int i = 0; i <= 24; i++) {
            GL11.glVertex3d(HEART_X[i] * size, HEART_Y[i] * size, 0.0);
        }
        GL11.glEnd();
    }

    
    private static void drawDiamond(double size) {
        double width = 0.72 * size;

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        GL11.glVertex3d(0.0, size, 0.0);
        GL11.glVertex3d(width, 0.0, 0.0);
        GL11.glVertex3d(0.0, -size, 0.0);
        GL11.glVertex3d(-width, 0.0, 0.0);
        GL11.glVertex3d(0.0, size, 0.0);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(0.0, size, 0.0);
        GL11.glVertex3d(width, 0.0, 0.0);
        GL11.glVertex3d(0.0, -size, 0.0);
        GL11.glVertex3d(-width, 0.0, 0.0);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(-width, 0.0, 0.0);
        GL11.glVertex3d(width, 0.0, 0.0);
        GL11.glEnd();
    }

    
    private static void drawNote(double size) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(-0.30 * size, -0.45 * size, 0.0);
        for (int i = 0; i <= 16; i++) {
            int index = i % 16;
            GL11.glVertex3d((-0.30 + CIRCLE_X[index] * 0.34) * size,
                    (-0.45 + CIRCLE_Y[index] * 0.26) * size, 0.0);
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(0.0, -0.52 * size, 0.0);
        GL11.glVertex3d(0.14 * size, -0.52 * size, 0.0);
        GL11.glVertex3d(0.14 * size, 0.75 * size, 0.0);
        GL11.glVertex3d(0.0, 0.75 * size, 0.0);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3d(0.14 * size, 0.75 * size, 0.0);
        GL11.glVertex3d(0.62 * size, 0.28 * size, 0.0);
        GL11.glVertex3d(0.14 * size, 0.32 * size, 0.0);
        GL11.glEnd();
    }

    private static void drawCircle(double size) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        for (int i = 0; i <= 16; i++) {
            int index = i % 16;
            GL11.glVertex3d(CIRCLE_X[index] * size * 0.75, CIRCLE_Y[index] * size * 0.75, 0.0);
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 16; i++) {
            GL11.glVertex3d(CIRCLE_X[i] * size, CIRCLE_Y[i] * size, 0.0);
        }
        GL11.glEnd();
    }

    @Override
    public String toString() {
        return name;
    }
}
