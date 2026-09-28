package secret.kinetic.utils.render.particles;





public enum ParticleColorMode {
    THEME("Theme"),
    RAINBOW("Rainbow"),
    CUSTOM("Custom");

    public final String name;

    ParticleColorMode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
