package secret.kinetic.api.gui.alt.comp;

import secret.kinetic.api.gui.alt.comp.LocaltsClient.Order;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.Product;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.StatusListener;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.User;

import java.util.List;

public interface AltShopBackend {

    default Progress progress() {
        return null;
    }

    final class Progress {
        public static final String[] STEPS = {"Queued", "Validating", "Provider", "Delivering"};
        public final String step, status, provider, error;
        public final int stepIndex, delivered, requested;
        public final boolean pending;
        public final long startedAt;

        public Progress(String step, int stepIndex, String status, String provider, int delivered, int requested, boolean pending, String error, long startedAt) {
            this.step = step == null ? "" : step;
            this.stepIndex = Math.max(0, Math.min(STEPS.length - 1, stepIndex));
            this.status = status == null ? "" : status;
            this.provider = provider == null ? "" : provider;
            this.delivered = delivered;
            this.requested = requested;
            this.pending = pending;
            this.error = error == null ? "" : error;
            this.startedAt = startedAt;
        }

        public long elapsedMs() {
            return System.currentTimeMillis() - startedAt;
        }

        public boolean finished() {
            return status.equalsIgnoreCase("done") || status.equalsIgnoreCase("failed") || status.equalsIgnoreCase("refunded");
        }
    }

    
    String name();

    
    String keyFile();

    
    String deliveryFile();

    String keyPlaceholder();

    
    String site();

    User getMe(String apiKey) throws Exception;

    List<Product> getProducts(String apiKey) throws Exception;

    
    Order purchase(String apiKey, Product product, int amount, StatusListener status) throws Exception;

    
    AltShopBackend LOCALTS = new AltShopBackend() {
        @Override
        public String name() {
            return "Localts";
        }

        @Override
        public String keyFile() {
            return "localts.txt";
        }

        @Override
        public String deliveryFile() {
            return "localts_orders.txt";
        }

        @Override
        public String keyPlaceholder() {
            return "Localts API key (lc.…)";
        }

        @Override
        public String site() {
            return "localts.store";
        }

        @Override
        public User getMe(String apiKey) throws Exception {
            return LocaltsClient.getMe(apiKey);
        }

        @Override
        public List<Product> getProducts(String apiKey) throws Exception {
            return LocaltsClient.getProducts(apiKey);
        }

        @Override
        public Order purchase(String apiKey, Product product, int amount, StatusListener status) throws Exception {
            String orderId = LocaltsClient.purchase(apiKey, product.id, amount);
            return LocaltsClient.waitForOrder(apiKey, orderId, status);
        }
    };
}
