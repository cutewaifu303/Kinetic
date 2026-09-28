package secret.kinetic.api.gui.click.kinetic;


final class KineticFrame {

    final float x;
    final float y;
    final float width;
    final float height;
    final float mouseX;
    final float mouseY;
    final float alpha;

    KineticFrame(float x, float y, float width, float height, float mouseX, float mouseY, float alpha) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.alpha = alpha;
    }

    boolean contains(float px, float py) {
        return KineticWidgets.hovered(px, py, x, y, width, height);
    }
}
