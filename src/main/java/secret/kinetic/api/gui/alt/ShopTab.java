package secret.kinetic.api.gui.alt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import static secret.kinetic.api.gui.alt.KineticAltMenu.*;





final class ShopTab extends AltTab {

    private static final String PROVIDER_FILE = "alt_provider.txt";
    private static final String[] PROVIDERS = {"Localts", "NiceAlts", "PandaAlts"};
    private static final String[] PROVIDER_IDS = {"localts", "nicealts", "pandaalts"};
    private static final int SWITCH_HEIGHT = 20;

    private final AltTab[] providers;
    private int provider;
    private int switchX, switchY, switchWidth;

    ShopTab(KineticAltMenu menu) {
        super(menu);
        providers = new AltTab[]{new LocaltsTab(menu, secret.kinetic.api.gui.alt.comp.AltShopBackend.LOCALTS), new NiceAltsTab(menu),
                new PandaShopTab(menu)};
        provider = loadProvider();
    }

    @Override
    String title() {
        return "Alt Shop";
    }

    private AltTab current() {
        return providers[provider];
    }

    @Override
    void onShow() {
        current().onShow();
    }

    @Override
    void layout(int x, int y, int w, int h) {
        super.layout(x, y, w, h);
        current().layout(x, y, w, h);
        int panelWidth = (int) (menu.width * ADD_PANEL_RATIO);
        switchX = x + PADDING;
        switchY = y + PADDING - 5;
        switchWidth = panelWidth - PADDING * 2;
    }

    @Override
    void draw(int mouseX, int mouseY) {
        current().draw(mouseX, mouseY);
        menu.drawSegmented("shop.provider", switchX, switchY, switchWidth, SWITCH_HEIGHT, PROVIDERS, provider, mouseX, mouseY);
    }

    @Override
    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int segment = menu.segmentAt(switchX, switchY, switchWidth, SWITCH_HEIGHT, PROVIDERS.length, mouseX, mouseY);
        if (segment >= 0) {
            if (segment != provider) {
                provider = segment;
                saveProvider();
                current().layout(contentX, contentY, contentWidth, contentHeight);
                current().onShow();
            }
            return true;
        }
        return current().mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    void mouseReleased(int mouseX, int mouseY, int state) {
        current().mouseReleased(mouseX, mouseY, state);
    }

    @Override
    void mouseClickMove(int mouseX, int mouseY) {
        current().mouseClickMove(mouseX, mouseY);
    }

    @Override
    void mouseScrolled(int wheel) {
        current().mouseScrolled(wheel);
    }

    @Override
    void keyTyped(char typedChar, int keyCode) {
        current().keyTyped(typedChar, keyCode);
    }

    private int loadProvider() {
        File file = new File(menu.getKineticDir(), PROVIDER_FILE);
        if (!file.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                for (int i = 0; i < PROVIDER_IDS.length; i++) {
                    if (PROVIDER_IDS[i].equalsIgnoreCase(line.trim())) return i;
                }
            }
        } catch (IOException ignored) {
        }
        return 0;
    }

    private void saveProvider() {
        try (PrintWriter out = new PrintWriter(new File(menu.getKineticDir(), PROVIDER_FILE))) {
            out.println(PROVIDER_IDS[provider]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
