package secret.kinetic.api.gui.alt.comp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.Order;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.OrderItem;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.Product;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.StatusListener;
import secret.kinetic.api.gui.alt.comp.LocaltsClient.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PandaAltsClient {

    public static final String BASE_URL = "https://altshop.pandaservice.eu/api/user/v1";
    private static final String USER_AGENT = "Kinetic/1.8.9";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 45000;
    private static final int PURCHASE_TIMEOUT = 190000;
    // embed.md client rules: stop waiting for a delivery after 180s, poll progress every ~3s,
    // never refresh stock faster than its 30s max-age, back off 60s+ (exponential) after a 429
    private static final long PENDING_WAIT_MS = 180 * 1000L;
    private static final long STOCK_MAX_AGE_MS = 30 * 1000L;
    private static final long RATE_LIMIT_WAIT_MS = 60 * 1000L;

    private PandaAltsClient() {
    }

    public static final AltShopBackend BACKEND = new AltShopBackend() {
        @Override
        public String name() {
            return "PandaAlts";
        }

        @Override
        public String keyFile() {
            return "pandaalts.txt";
        }

        @Override
        public String deliveryFile() {
            return "pandaalts_orders.txt";
        }

        @Override
        public String keyPlaceholder() {
            return "PandaAlts API key (psu_live_…)";
        }

        @Override
        public String site() {
            return "altshop.pandaservice.eu";
        }

        @Override
        public User getMe(String apiKey) throws Exception {
            return PandaAltsClient.getMe(apiKey);
        }

        @Override
        public List<Product> getProducts(String apiKey) throws Exception {
            return PandaAltsClient.getProducts(apiKey);
        }

        @Override
        public Order purchase(String apiKey, Product product, int amount, StatusListener status) throws Exception {
            return PandaAltsClient.purchase(apiKey, product, amount, status);
        }

        @Override
        public AltShopBackend.Progress progress() {
            return live;
        }
    };

    private static volatile AltShopBackend.Progress live;
    private static final long PROGRESS_POLL_MS = 3000L;

    private static final Object STOCK_LOCK = new Object();
    private static String stockKey;
    private static long stockFetchedAt;
    private static List<Product> stockCache;
    private static volatile long rateLimitedUntil;
    private static volatile int rateLimitHits;

    

    public static User getMe(String apiKey) throws IOException {
        JsonObject data = execute("GET", "/me", apiKey, null);
        JsonObject user = object(data, "user");
        if (user == null) user = data;
        String name = string(user, "username", "name", "email", "login");
        double credits = number(user, "credits", "balance", "credit");
        return new User(name.isEmpty() ? "PandaAlts user" : name, credits);
    }

    public static List<Product> getProducts(String apiKey) throws IOException {
        synchronized (STOCK_LOCK) {
            if (stockCache != null && String.valueOf(apiKey).equals(stockKey) && System.currentTimeMillis() - stockFetchedAt < STOCK_MAX_AGE_MS) {
                return new ArrayList<>(stockCache);
            }
        }
        List<Product> products = fetchProducts(apiKey);
        synchronized (STOCK_LOCK) {
            stockKey = String.valueOf(apiKey);
            stockFetchedAt = System.currentTimeMillis();
            stockCache = new ArrayList<>(products);
            return products;
        }
    }

    private static List<Product> fetchProducts(String apiKey) throws IOException {
        JsonObject data;
        String locale = Locale.getDefault().getLanguage();
        try {
            data = execute("GET", locale.isEmpty() ? "/stock" : "/stock?locale=" + locale, apiKey, null);
        } catch (IOException e) {
            data = execute("GET", "/stock", apiKey, null);
        }
        JsonArray rows = array(data, "stock", "products", "items");
        List<Product> products = new ArrayList<>();
        if (rows == null) return products;
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            String id = string(row, "product_id", "id");
            if (id.isEmpty()) continue;
            if (row.has("price_configured") && !bool(row, "price_configured")) continue;
            String name = string(row, "product_name", "name", "title");
            String category = string(row, "category", "store", "provider");
            List<String> tags = new ArrayList<>();
            JsonArray tagArray = array(row, "tags");
            if (tagArray != null) for (JsonElement tag : tagArray) if (tag.isJsonPrimitive()) tags.add(tag.getAsString());
            products.add(new Product(id, name.isEmpty() ? "Product " + id : name, string(row, "product_description", "description"),
                    category, number(row, "price", "price_credits", "priceInCredits", "unit_price"),
                    (int) number(row, "count", "stock", "available", "quantity"), string(row, "type", "kind"),
                    Collections.unmodifiableList(tags), Collections.emptyMap()));
        }
        products.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return products;
    }

    public static Order purchase(String apiKey, Product product, int amount, StatusListener status) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        try {
            body.addProperty("product_id", Long.parseLong(product.id));
        } catch (NumberFormatException e) {
            body.addProperty("product_id", product.id);
        }
        body.addProperty("amount", amount);
        String progressId = newProgressId();
        body.addProperty("progress_id", progressId);
        synchronized (STOCK_LOCK) {
            stockCache = null;
        }

        long startedAt = System.currentTimeMillis();
        live = new AltShopBackend.Progress("queued", 0, "active", "", 0, amount, false, "", startedAt);
        AtomicBoolean done = new AtomicBoolean(false);
        Thread poller = startProgressPoller(apiKey, progressId, amount, startedAt, done);

        try {
            JsonObject data;
            try {
                if (status != null) status.update("Buying from PandaAlts...");
                data = execute("POST", "/purchase", apiKey, body, PURCHASE_TIMEOUT);
            } catch (SocketTimeoutException timeout) {
                if (status != null) status.update("PandaAlts is slow, checking order state...");
                data = reconcile(apiKey, progressId, status);
                if (data == null) throw new IOException("PandaAlts did not answer in time and no order was found. Check " + BACKEND.site() + " before buying again.");
            }

            String purchaseId = purchaseId(data);
            List<OrderItem> items = deliverables(data);

            long deadline = System.currentTimeMillis() + PENDING_WAIT_MS;
            while (items.isEmpty() && isPending(data) && System.currentTimeMillis() < deadline) {
                long waited = (System.currentTimeMillis() - startedAt) / 1000L;
                if (status != null) status.update("Paid, waiting for PandaAlts delivery (" + waited + "s)...");
                live = new AltShopBackend.Progress("delivering", 3, "pending", providerOf(data), 0, amount, true, "", startedAt);
                Thread.sleep(PROGRESS_POLL_MS);
                JsonObject lookup = null;
                if (!purchaseId.isEmpty()) {
                    try {
                        lookup = execute("GET", "/purchases/" + purchaseId, apiKey, null);
                    } catch (IOException ignored) {
                    }
                }
                if (lookup == null) {
                    try {
                        lookup = execute("GET", "/purchase/progress/" + progressId, apiKey, null);
                    } catch (IOException ignored) {
                    }
                }
                if (lookup == null) continue;
                data = lookup;
                if (purchaseId.isEmpty()) purchaseId = purchaseId(lookup);
                if (isRefunded(lookup)) {
                    live = new AltShopBackend.Progress("delivering", 3, "refunded", providerOf(lookup), 0, amount, false, string(lookup, "error"), startedAt);
                    throw new IOException("PandaAlts could not deliver order " + purchaseId + " - credits were refunded" + refundReason(lookup));
                }
                if (!isPending(lookup)) items = deliverables(lookup);
            }

            if (items.isEmpty() && isPending(data)) {
                throw new IOException("Order " + purchaseId + " is still pending at PandaAlts. It is delivered (or refunded) automatically - check " + BACKEND.site() + " later.");
            }
            live = new AltShopBackend.Progress("delivering", 3, "done", providerOf(data), items.size(), amount, false, "", startedAt);
            return new Order(purchaseId.isEmpty() ? "?" : purchaseId, items.isEmpty() ? "EMPTY" : "PACKAGED", product.name, Collections.unmodifiableList(items));
        } finally {
            done.set(true);
            poller.interrupt();
            final long shownAt = System.currentTimeMillis();
            Thread cleaner = new Thread(() -> {
                try {
                    Thread.sleep(2500L);
                } catch (InterruptedException ignored) {
                }
                AltShopBackend.Progress current = live;
                if (current != null && current.startedAt == startedAt && System.currentTimeMillis() - shownAt >= 2400L) live = null;
            }, "PandaAlts-progress-cleanup");
            cleaner.setDaemon(true);
            cleaner.start();
        }
    }

    private static Thread startProgressPoller(String apiKey, String progressId, int amount, long startedAt, AtomicBoolean done) {
        Thread thread = new Thread(() -> {
            int misses = 0;
            while (!done.get() && System.currentTimeMillis() - startedAt < PENDING_WAIT_MS) {
                try {
                    Thread.sleep(PROGRESS_POLL_MS);
                } catch (InterruptedException e) {
                    return;
                }
                if (done.get()) return;
                try {
                    JsonObject p = execute("GET", "/purchase/progress/" + progressId, apiKey, null);
                    misses = 0;
                    AltShopBackend.Progress snap = new AltShopBackend.Progress(string(p, "step"), (int) number(p, "step_index"), string(p, "status"),
                            string(p, "provider"), (int) number(p, "delivered_amount"), (int) Math.max(amount, number(p, "requested_amount")),
                            bool(p, "pending"), string(p, "error"), startedAt);
                    if (!done.get()) live = snap;
                    String state = string(p, "status");
                    if (state.equalsIgnoreCase("done") || state.equalsIgnoreCase("failed")) return;
                } catch (IOException e) {
                    if (++misses > 40) return;
                }
            }
        }, "PandaAlts-progress");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static String providerOf(JsonObject data) {
        String p = string(data, "provider");
        if (!p.isEmpty()) return p;
        JsonObject txn = object(data, "transaction");
        if (txn != null) p = string(txn, "provider");
        if (p.isEmpty()) {
            JsonObject purchase = object(data, "purchase");
            if (purchase != null) p = string(purchase, "provider");
        }
        return p;
    }

    private static String newProgressId() {
        StringBuilder sb = new StringBuilder("kin-").append(Long.toString(System.currentTimeMillis(), 36)).append('-');
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 8; i++) sb.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        return sb.toString();
    }

    private static JsonObject reconcile(String apiKey, String progressId, StatusListener status) throws InterruptedException {
        for (int attempt = 0; attempt < 6; attempt++) {
            Thread.sleep(attempt == 0 ? 2000L : 5000L);
            try {
                JsonObject progress = execute("GET", "/purchase/progress/" + progressId, apiKey, null);
                String state = string(progress, "status");
                if (state.equalsIgnoreCase("failed")) return null;
                if (state.equalsIgnoreCase("done") || state.equalsIgnoreCase("pending") || state.equalsIgnoreCase("refunded")) {
                    String txn = string(progress, "transaction_id");
                    if (!txn.isEmpty()) return execute("GET", "/purchases/" + txn, apiKey, null);
                    return progress;
                }
                if (status != null) status.update("PandaAlts is still processing (" + string(progress, "step") + ")...");
                continue;
            } catch (IOException ignored) {
            }
            try {
                JsonArray purchases = array(execute("GET", "/purchases?limit=5&offset=0", apiKey, null), "purchases");
                if (purchases != null) {
                    for (JsonElement element : purchases) {
                        if (!element.isJsonObject()) continue;
                        JsonObject row = element.getAsJsonObject();
                        if (progressId.equals(string(row, "progress_id"))) {
                            String id = string(row, "id");
                            return id.isEmpty() ? row : execute("GET", "/purchases/" + id, apiKey, null);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public static boolean isPending(JsonObject data) {
        JsonObject txn = object(data, "transaction");
        if (txn != null && txn.has("pending")) return bool(txn, "pending");
        JsonObject purchase = object(data, "purchase");
        if (purchase != null && purchase.has("pending")) return bool(purchase, "pending");
        return bool(data, "pending");
    }

    public static boolean isRefunded(JsonObject data) {
        JsonObject txn = object(data, "transaction");
        if (txn != null && bool(txn, "pending_refunded")) return true;
        return string(data, "status").equalsIgnoreCase("refunded");
    }

    private static String refundReason(JsonObject data) {
        String reason = string(data, "error");
        return reason.isEmpty() ? "." : ": " + reason;
    }

    private static boolean bool(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
    }

    
    public static JsonObject getQuote(String apiKey, String productId, int amount) throws IOException {
        JsonObject body = new JsonObject();
        try {
            body.addProperty("product_id", Long.parseLong(productId));
        } catch (NumberFormatException e) {
            body.addProperty("product_id", productId);
        }
        body.addProperty("amount", amount);
        return execute("POST", "/purchase/quote", apiKey, body);
    }

    
    public static JsonArray getPurchases(String apiKey, int limit, int offset) throws IOException {
        JsonArray rows = array(execute("GET", "/purchases?limit=" + limit + "&offset=" + offset, apiKey, null), "purchases", "items");
        return rows == null ? new JsonArray() : rows;
    }

    
    public static JsonObject getPurchase(String apiKey, String purchaseId) throws IOException {
        return execute("GET", "/purchases/" + purchaseId, apiKey, null);
    }

    
    public static JsonArray getPreorders(String apiKey) throws IOException {
        JsonArray rows = array(execute("GET", "/preorders", apiKey, null), "preorders", "items");
        return rows == null ? new JsonArray() : rows;
    }

    

    public static String purchaseId(JsonObject data) {
        JsonObject transaction = object(data, "transaction");
        String id = transaction == null ? "" : string(transaction, "id", "purchase_id");
        if (!id.isEmpty()) return id;
        JsonObject purchase = object(data, "purchase");
        if (purchase == null) purchase = data;
        return string(purchase, "purchase_id", "id");
    }

    private static String progressId(JsonObject data) {
        String id = string(data, "progress_id", "progressId");
        if (!id.isEmpty()) return id;
        JsonObject purchase = object(data, "purchase");
        if (purchase == null) purchase = object(data, "transaction");
        if (purchase == null) purchase = data;
        return string(purchase, "progress_id", "progressId");
    }

    
    public static List<OrderItem> deliverables(JsonObject data) {
        JsonObject purchase = object(data, "purchase");
        if (purchase == null) purchase = object(data, "response");
        if (purchase == null) purchase = data;
        JsonArray list = array(purchase, "products", "items", "deliverables", "accounts");
        if (list == null) {
            JsonObject inner = object(purchase, "purchase");
            if (inner != null) list = array(inner, "products");
        }
        if (list == null) list = array(data, "products", "deliverables");
        List<OrderItem> items = new ArrayList<>();
        if (list == null) return items;
        int index = 0;
        for (JsonElement element : list) {
            index++;
            String content;
            String title;
            if (element.isJsonObject()) {
                JsonObject raw = element.getAsJsonObject();
                title = string(raw, "username", "name", "email", "login", "account", "mc_username");
                content = maybeBase64(string(raw, "data", "cookie", "password", "token", "refresh_token", "value", "credentials", "combo", "access_token"));
                if (content.isEmpty()) content = raw.toString();
                // some products deliver the login in a field of its own next to data (e.g. access_token), keep it
                for (String key : new String[]{"refresh_token", "access_token", "cookie", "token"}) {
                    String extra = maybeBase64(string(raw, key)).trim();
                    if (!extra.isEmpty() && !content.contains(extra)) content = content + "\n" + extra;
                }
            } else {
                title = "";
                content = maybeBase64(element.isJsonPrimitive() ? element.getAsString() : element.toString());
            }
            items.add(new OrderItem(title.isEmpty() ? "account-" + index : title, content));
        }
        return items;
    }

    
    private static String maybeBase64(String text) {
        String value = text == null ? "" : text.trim();
        if (value.length() < 16 || value.length() % 4 != 0 || !value.matches("[A-Za-z0-9+/=]+")) return text;
        try {
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            int printable = 0;
            for (char c : decoded.toCharArray()) if (c >= 32 && c < 127 || c == '\n' || c == '\r' || c == '\t') printable++;
            return printable >= decoded.length() * 0.95 ? decoded : text;
        } catch (IllegalArgumentException e) {
            return text;
        }
    }

    

    private static JsonObject execute(String method, String path, String apiKey, JsonObject body) throws IOException {
        return execute(method, path, apiKey, body, READ_TIMEOUT);
    }

    private static JsonObject execute(String method, String path, String apiKey, JsonObject body, int readTimeout) throws IOException {
        long wait = rateLimitedUntil - System.currentTimeMillis();
        if (wait > 0) {
            // hammering through 429s gets the IP flagged, so nothing goes out until the backoff is over
            throw new IOException("PandaAlts rate limit reached, try again in " + (wait / 1000L + 1) + "s");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(readTimeout);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (apiKey != null && !apiKey.trim().isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            if (body != null) {
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = stream == null ? "" : readAll(stream);
            JsonObject json = null;
            try {
                JsonElement parsed = new JsonParser().parse(text);
                if (parsed.isJsonObject()) json = parsed.getAsJsonObject();
            } catch (RuntimeException ignored) {
            }
            JsonObject data = json == null ? null : object(json, "data");
            boolean success = json != null && json.has("success") && json.get("success").isJsonPrimitive() && json.get("success").getAsBoolean();
            if (status == 401) throw new IOException(error(data, json, "PandaAlts rejected the API key"));
            if (status == 403) throw new IOException(error(data, json, "PandaAlts blocked this request (VPN/proxy IP or blacklisted)"));
            if (status == 402) throw new IOException(error(data, json, "Not enough credits") + quoteReasons(data));
            if (status == 409) throw new IOException(error(data, json, "PandaAlts: not enough stock right now") + quoteReasons(data));
            if (status == 429) {
                long backoff = RATE_LIMIT_WAIT_MS << Math.min(4, rateLimitHits++);
                rateLimitedUntil = System.currentTimeMillis() + backoff;
                throw new IOException(error(data, json, "PandaAlts rate limit reached, try again in " + backoff / 1000L + "s"));
            }
            if (status < 400) rateLimitHits = 0;
            if (status == 503) throw new IOException(error(data, json, "PandaAlts is in maintenance or the provider is unreachable - nothing was charged"));
            if (status < 200 || status >= 300 || json == null || !success) {
                String fallback = json == null
                        ? (status >= 500 ? "PandaAlts server error (HTTP " + status + ") - nothing was charged" : "PandaAlts returned no JSON (HTTP " + status + ")")
                        : "PandaAlts request failed (HTTP " + status + ")";
                throw new IOException(error(data, json, fallback) + upstreamDetail(data));
            }
            return data != null ? data : json;
        } finally {
            connection.disconnect();
        }
    }

    private static String upstreamDetail(JsonObject data) {
        JsonObject details = object(data, "details");
        if (details == null) return "";
        String code = string(details, "code"), provider = string(details, "provider");
        if (code.isEmpty()) return "";
        return " [" + code.toLowerCase(Locale.ROOT).replace('_', ' ') + (provider.isEmpty() ? "" : " @ " + provider) + "]";
    }

    private static String quoteReasons(JsonObject data) {
        JsonObject details = object(data, "details");
        JsonArray reasons = array(details, "reasons");
        if (reasons == null || reasons.size() == 0) return "";
        StringBuilder sb = new StringBuilder(" (");
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(reasons.get(i).isJsonPrimitive() ? reasons.get(i).getAsString() : reasons.get(i).toString());
        }
        return sb.append(')').toString();
    }

    private static String error(JsonObject data, JsonObject json, String fallback) {
        String message = data == null ? "" : string(data, "error", "message");
        if (message.isEmpty() && json != null) message = string(json, "error", "message");
        return message.isEmpty() ? fallback : message;
    }

    private static String readAll(InputStream stream) throws IOException {
        try (InputStream in = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    

    private static JsonObject object(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject json, String... keys) {
        if (json == null) return null;
        for (String key : keys) {
            JsonElement value = json.get(key);
            if (value != null && value.isJsonArray()) return value.getAsJsonArray();
        }
        return null;
    }

    private static String string(JsonObject json, String... keys) {
        if (json == null) return "";
        for (String key : keys) {
            JsonElement value = json.get(key);
            if (value == null || value.isJsonNull()) continue;
            String text = value.isJsonPrimitive() ? value.getAsString() : value.toString();
            if (!text.trim().isEmpty()) return text;
        }
        return "";
    }

    private static double number(JsonObject json, String... keys) {
        if (json == null) return 0;
        for (String key : keys) {
            JsonElement value = json.get(key);
            if (value == null || !value.isJsonPrimitive()) continue;
            try {
                return Double.parseDouble(value.getAsString().trim().replace(",", ".").toLowerCase(Locale.ROOT).replace("credits", "").trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
