package secret.kinetic.api.gui.alt.comp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;






public final class NiceAltsClient {

    public static final String BASE_URL = "https://app.nicealts.com";
    private static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    private static final String USER_AGENT = "Kinetic/1.8.9";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 45000;

    
    public static final String[] PRODUCT_IDS = {"1", "2", "3", "9", "5", "4", "6", "7", "8"};
    
    private static final String API_PURCHASABLE = "1 2 3 4 5 6 9";
    public static final double DEFAULT_CUSTOM_PRICE = 5;

    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern REFRESH = Pattern.compile("M\\.[A-Za-z0-9_!*$.-]{20,}");

    private NiceAltsClient() {
    }

    public enum Endpoint {
        STOCK, BALANCE, HISTORY, PURCHASE, CUSTOM, GENERATE, DOWNLOAD
    }

    

    public static String productName(String id) {
        switch (id) {
            case "1": return "Unbanned 1-7";
            case "2": return "Unbanned 8+";
            case "3": return "Unbanned Ranked";
            case "4": return "Donut Cookies";
            case "5": return "Donut";
            case "6": return "Banned";
            case "7": return "MFA";
            case "8": return "MFA High Value";
            case "9": return "Unbanned Cookies";
            default: return "Product " + id;
        }
    }

    public static boolean isApiPurchasable(String id) {
        return API_PURCHASABLE.contains(id) && id.length() == 1;
    }

    public static boolean isCookieProduct(String id) {
        return "4".equals(id) || "9".equals(id);
    }

    

    public static Stock getStock() throws NiceAltsException {
        JsonObject json = parseObject(Endpoint.STOCK, request(Endpoint.STOCK, "GET", BASE_URL + "/public/stock", null));
        Map<String, Integer> stock = new LinkedHashMap<>();
        Map<String, Double> prices = new LinkedHashMap<>();
        JsonElement stockElement = json.get("stock");
        if (stockElement != null && stockElement.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : stockElement.getAsJsonObject().entrySet()) {
                try {
                    stock.put(entry.getKey(), entry.getValue().getAsInt());
                } catch (RuntimeException ignored) {
                }
            }
        }
        JsonElement pricesElement = json.get("prices");
        if (pricesElement != null && pricesElement.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : pricesElement.getAsJsonObject().entrySet()) {
                try {
                    prices.put(entry.getKey(), entry.getValue().getAsDouble());
                } catch (RuntimeException ignored) {
                }
            }
        }
        if (stock.isEmpty()) throw new NiceAltsException(200, "NiceAlts returned no stock data", 0L);
        return new Stock(Collections.unmodifiableMap(stock), Collections.unmodifiableMap(prices));
    }

    public static Account getBalance(String apiKey) throws NiceAltsException {
        JsonObject json = post(Endpoint.BALANCE, "/api/balance", keyBody(apiKey));
        return new Account(optString(json, "username"), optDouble(json, "balance"), optString(json, "sub_status"),
                optString(json, "sub_expiry"));
    }

    public static List<HistoryEntry> getHistory(String apiKey) throws NiceAltsException {
        JsonObject json = post(Endpoint.HISTORY, "/api/history", keyBody(apiKey));
        List<HistoryEntry> history = new ArrayList<>();
        JsonElement array = json.get("history");
        if (array != null && array.isJsonArray()) {
            for (JsonElement element : array.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();
                history.add(new HistoryEntry(optString(entry, "purchase_id"), optString(entry, "product_id"),
                        optString(entry, "timestamp"), strings(entry.get("items")), strings(entry.get("mc_usernames")),
                        optString(entry, "refund_status")));
            }
        }
        return history;
    }

    public static Purchase purchase(String apiKey, String productId) throws NiceAltsException {
        JsonObject body = keyBody(apiKey);
        body.addProperty("product_id", productId);
        JsonObject json = post(Endpoint.PURCHASE, "/api/purchase", body);
        return new Purchase(optString(json, "order_id"), strings(json.get("items")));
    }

    public static List<String> customPurchase(String apiKey, String server, String protocol) throws NiceAltsException {
        JsonObject body = keyBody(apiKey);
        body.addProperty("server", server);
        body.addProperty("protocol", protocol);
        JsonObject json = post(Endpoint.CUSTOM, "/api/custompurchase", body);
        List<String> tokens = strings(json.get("tokens"));
        if (tokens.isEmpty()) throw new NiceAltsException(200, "NiceAlts delivered no token", 0L);
        return tokens;
    }

    public static String generate(String apiKey, String category) throws NiceAltsException {
        JsonObject body = keyBody(apiKey);
        body.addProperty("category", category);
        JsonObject json = post(Endpoint.GENERATE, "/api/generate", body);
        String token = optString(json, "token");
        if (token.isEmpty()) throw new NiceAltsException(200, "NiceAlts generated no token", 0L);
        return token;
    }

    
    public static File downloadFile(String apiKey, String orderId, File destination) throws NiceAltsException {
        String url = BASE_URL + "/api/download-file?api_key=" + urlEncode(apiKey) + "&order_id=" + urlEncode(orderId);
        HttpURLConnection connection = null;
        try {
            connection = open("GET", url);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String text = connection.getErrorStream() == null ? "" : new String(readAll(connection.getErrorStream()), StandardCharsets.UTF_8);
                throw error(Endpoint.DOWNLOAD, status, text, connection.getHeaderField("Retry-After"));
            }
            byte[] data = readAll(connection.getInputStream());
            if (data.length < 4 || data[0] != 'P' || data[1] != 'K') {
                String text = new String(data, StandardCharsets.UTF_8).trim();
                throw new NiceAltsException(status, "Download is not a zip file" + (isShortText(text) ? ": " + text : ""), 0L);
            }
            File parent = destination.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (OutputStream out = new FileOutputStream(destination)) {
                out.write(data);
            }
            return destination;
        } catch (IOException e) {
            throw new NiceAltsException(0, "Could not reach NiceAlts: " + e.getMessage(), 0L);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    

    
    public static Profile fetchProfile(String accessToken) throws NiceAltsException {
        HttpURLConnection connection = null;
        try {
            connection = open("GET", PROFILE_URL);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = stream == null ? "" : new String(readAll(stream), StandardCharsets.UTF_8);
            if (status == 401) throw new NiceAltsException(status, "Token is invalid or expired (tokens last about 24h)", 0L);
            if (status == 404) throw new NiceAltsException(status, "This account does not own Minecraft", 0L);
            if (status == 429) throw new NiceAltsException(status, "Mojang rate limit, try again in a minute", 60000L);
            if (status < 200 || status >= 300) throw new NiceAltsException(status, "Mojang profile lookup failed (HTTP " + status + ")", 0L);
            JsonObject json = parseObject(Endpoint.STOCK, text);
            String name = optString(json, "name");
            String id = optString(json, "id");
            if (name.isEmpty() || id.isEmpty()) throw new NiceAltsException(status, "Mojang returned an empty profile", 0L);
            return new Profile(name, id);
        } catch (IOException e) {
            throw new NiceAltsException(0, "Could not reach Mojang: " + e.getMessage(), 0L);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    
    public static String findAccessToken(String text) {
        if (text == null) return "";
        Matcher matcher = JWT.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    
    public static String findRefreshToken(String text) {
        if (text == null) return "";
        Matcher matcher = REFRESH.matcher(text);
        String best = "";
        while (matcher.find()) {
            String candidate = matcher.group();
            if (MicrosoftOAuthTranslation.isRefreshToken(candidate) && candidate.length() > best.length()) best = candidate;
        }
        return best;
    }

    
    public static long jwtExpiry(String token) {
        if (token == null) return -1L;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return -1L;
        try {
            String payload = parts[1];
            while (payload.length() % 4 != 0) payload += "=";
            String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject()) return -1L;
            JsonElement exp = parsed.getAsJsonObject().get("exp");
            return exp == null || !exp.isJsonPrimitive() ? -1L : exp.getAsLong() * 1000L;
        } catch (RuntimeException e) {
            return -1L;
        }
    }

    

    private static JsonObject keyBody(String apiKey) {
        JsonObject body = new JsonObject();
        body.addProperty("api_key", apiKey == null ? "" : apiKey.trim());
        return body;
    }

    private static JsonObject post(Endpoint endpoint, String path, JsonObject body) throws NiceAltsException {
        return parseObject(endpoint, request(endpoint, "POST", BASE_URL + path, body.toString()));
    }

    private static HttpURLConnection open(String method, String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/json, */*");
        return connection;
    }

    private static String request(Endpoint endpoint, String method, String url, String body) throws NiceAltsException {
        HttpURLConnection connection = null;
        try {
            connection = open(method, url);
            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = stream == null ? "" : new String(readAll(stream), StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) throw error(endpoint, status, text, connection.getHeaderField("Retry-After"));
            return text;
        } catch (IOException e) {
            throw new NiceAltsException(0, "Could not reach NiceAlts: " + e.getMessage(), 0L);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static JsonObject parseObject(Endpoint endpoint, String text) throws NiceAltsException {
        try {
            JsonElement parsed = new JsonParser().parse(text);
            if (parsed != null && parsed.isJsonObject()) {
                JsonObject json = parsed.getAsJsonObject();
                String status = optString(json, "status");
                if (!status.isEmpty() && !"success".equalsIgnoreCase(status)) {
                    String message = optString(json, "message");
                    if (message.isEmpty()) message = optString(json, "error");
                    throw new NiceAltsException(200, message.isEmpty() ? "NiceAlts answered \"" + status + "\"" : message, 0L);
                }
                return json;
            }
        } catch (RuntimeException ignored) {
        }
        String trimmed = text == null ? "" : text.trim();
        throw new NiceAltsException(200, "Unexpected NiceAlts response" + (isShortText(trimmed) ? ": " + trimmed : ""), 0L);
    }

    
    static NiceAltsException error(Endpoint endpoint, int status, String body, String retryAfterHeader) {
        long retryAfter = retryAfterMillis(retryAfterHeader);
        String server = body == null ? "" : body.trim();
        String message;
        switch (status) {
            case 400:
                message = endpoint == Endpoint.CUSTOM ? "Invalid server address or protocol" : "Invalid request parameters";
                if (isShortText(server)) message += " (" + server + ")";
                break;
            case 401:
                message = "Invalid or missing API key";
                break;
            case 402:
                message = "Not enough balance, top up at nicealts.com";
                break;
            case 403:
                if (endpoint == Endpoint.GENERATE) message = "An active subscription is required to generate";
                else if (endpoint == Endpoint.PURCHASE) message = "Product not sold via API, or account banned/restricted";
                else if (endpoint == Endpoint.CUSTOM) message = "Your NiceAlts account is banned or restricted";
                else message = "Your NiceAlts account is banned";
                break;
            case 404:
                if (endpoint == Endpoint.CUSTOM) message = "Out of stock for this server";
                else if (endpoint == Endpoint.DOWNLOAD) message = "No download for this order";
                else message = "Out of stock";
                break;
            case 429:
                if (endpoint == Endpoint.PURCHASE || endpoint == Endpoint.CUSTOM) {
                    message = "Purchase limit is 1 per minute (or one is still in progress)";
                    if (retryAfter <= 0L) retryAfter = 60000L;
                } else if (endpoint == Endpoint.GENERATE) {
                    message = "Generator cooldown (Premium 10 min, Premium+ 5 min)";
                    if (retryAfter <= 0L) retryAfter = 60000L;
                } else if (endpoint == Endpoint.STOCK) {
                    message = "Stock is limited to 20 requests per minute, wait a moment";
                    if (retryAfter <= 0L) retryAfter = 15000L;
                } else {
                    message = "Rate limited (100 requests per minute), wait a moment";
                    if (retryAfter <= 0L) retryAfter = 10000L;
                }
                break;
            default:
                if (status >= 500) message = "NiceAlts is temporarily unavailable (HTTP " + status + ")";
                else message = "NiceAlts request failed (HTTP " + status + ")" + (isShortText(server) ? ": " + server : "");
        }
        return new NiceAltsException(status, message, retryAfter);
    }

    private static boolean isShortText(String text) {
        return text != null && !text.isEmpty() && text.length() <= 90 && !text.startsWith("<") && !text.startsWith("{");
    }

    private static long retryAfterMillis(String header) {
        if (header == null || header.trim().isEmpty()) return 0L;
        try {
            return Math.max(0L, (long) (Double.parseDouble(header.trim()) * 1000.0D));
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        try (InputStream in = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
            return buffer.toByteArray();
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (IOException e) {
            return value;
        }
    }

    private static String optString(JsonObject json, String field) {
        JsonElement value = json.get(field);
        if (value == null || value.isJsonNull()) return "";
        try {
            return value.isJsonPrimitive() ? value.getAsString() : value.toString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static double optDouble(JsonObject json, String field) {
        JsonElement value = json.get(field);
        try {
            return value == null || !value.isJsonPrimitive() ? 0.0D : value.getAsDouble();
        } catch (RuntimeException e) {
            return 0.0D;
        }
    }

    private static List<String> strings(JsonElement element) {
        List<String> values = new ArrayList<>();
        if (element == null || element.isJsonNull()) return values;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement value : array) {
                if (value == null || value.isJsonNull()) continue;
                values.add(value.isJsonPrimitive() ? value.getAsString() : value.toString());
            }
        } else if (element.isJsonPrimitive()) {
            values.add(element.getAsString());
        }
        return values;
    }

    

    public static final class Stock {
        public final Map<String, Integer> stock;
        public final Map<String, Double> prices;

        Stock(Map<String, Integer> stock, Map<String, Double> prices) {
            this.stock = stock;
            this.prices = prices;
        }

        public int stockOf(String id) {
            Integer value = stock.get(id);
            return value == null ? 0 : value;
        }

        public double priceOf(String id) {
            Double value = prices.get(id);
            return value == null ? 0.0D : value;
        }

        public double customPrice() {
            Double value = prices.get("C");
            return value == null || value <= 0 ? DEFAULT_CUSTOM_PRICE : value;
        }
    }

    public static final class Account {
        public final String username, subStatus, subExpiry;
        public final double balance;

        Account(String username, double balance, String subStatus, String subExpiry) {
            this.username = username;
            this.balance = balance;
            this.subStatus = subStatus;
            this.subExpiry = subExpiry;
        }

        
        public long subExpiryMillis() {
            if (subExpiry == null || subExpiry.trim().isEmpty()) return -1L;
            try {
                return java.time.Instant.parse(subExpiry.trim()).toEpochMilli();
            } catch (RuntimeException e) {
                return -1L;
            }
        }

        public boolean hasSubscription() {
            String status = subStatus == null ? "" : subStatus.trim().toLowerCase(Locale.ROOT);
            if (status.isEmpty() || "none".equals(status) || "false".equals(status) || "0".equals(status)) return false;
            long expiry = subExpiryMillis();
            return expiry < 0L || expiry > System.currentTimeMillis();
        }
    }

    public static final class HistoryEntry {
        public final String purchaseId, productId, timestamp, refundStatus;
        public final List<String> items, usernames;

        HistoryEntry(String purchaseId, String productId, String timestamp, List<String> items, List<String> usernames, String refundStatus) {
            this.purchaseId = purchaseId;
            this.productId = productId;
            this.timestamp = timestamp;
            this.items = items;
            this.usernames = usernames;
            this.refundStatus = refundStatus;
        }
    }

    public static final class Purchase {
        public final String orderId;
        public final List<String> items;

        Purchase(String orderId, List<String> items) {
            this.orderId = orderId;
            this.items = items;
        }
    }

    public static final class Profile {
        public final String name, uuid;

        Profile(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }

    public static class NiceAltsException extends Exception {
        public final int status;
        public final long retryAfterMillis;

        public NiceAltsException(int status, String message, long retryAfterMillis) {
            super(message);
            this.status = status;
            this.retryAfterMillis = retryAfterMillis;
        }
    }
}
