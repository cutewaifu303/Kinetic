package secret.kinetic.api.gui.alt.comp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;












public final class CookieLogin {

    private static final String XBOX_CLIENT_ID = "00000000402b5328";
    private static final String XBOX_REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final String XBOX_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_REDIRECTS = 12;

    private CookieLogin() {
    }

    public static final class Result {
        
        public final String accessToken;
        
        public final String refreshToken;
        public final String error;

        Result(String accessToken, String refreshToken, String error) {
            this.accessToken = accessToken == null ? "" : accessToken;
            this.refreshToken = refreshToken == null ? "" : refreshToken;
            this.error = error;
        }

        public boolean isGood() {
            return !accessToken.isEmpty() || !refreshToken.isEmpty();
        }
    }

    public static Result fromFile(File file) {
        try {
            return fromText(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new Result(null, null, "Could not read the file");
        }
    }

    public static Result fromText(String text) {
        Map<String, String> cookies = parse(text);
        if (cookies.isEmpty()) return new Result(null, null, "No login.live.com cookies found in the file");
        try {
            return redeem(cookies);
        } catch (IOException e) {
            return new Result(null, null, "Could not reach Microsoft: " + e.getMessage());
        }
    }

    
    public static boolean looksLikeCookies(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("live.com") && (lower.contains("wlssc") || lower.contains("msaauth") || lower.contains("\tmspauth\t"));
    }

    

    private static Map<String, String> parse(String text) {
        Map<String, String> cookies = new LinkedHashMap<>();
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                parseJson(new JsonParser().parse(trimmed), cookies);
                if (!cookies.isEmpty()) return cookies;
            } catch (RuntimeException ignored) {
            }
        }
        for (String line : trimmed.split("[\\r\\n]+")) {
            String row = line.trim();
            if (row.isEmpty() || row.startsWith("#")) continue;
            String[] parts = row.split("\\t");
            if (parts.length >= 7) {
                put(cookies, parts[5], parts[6]);
                continue;
            }
            
            for (String pair : row.split(";")) {
                int eq = pair.indexOf('=');
                if (eq > 0) put(cookies, pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return cookies;
    }

    private static void parseJson(JsonElement root, Map<String, String> cookies) {
        JsonArray array = null;
        if (root.isJsonArray()) array = root.getAsJsonArray();
        else if (root.isJsonObject() && root.getAsJsonObject().has("cookies") && root.getAsJsonObject().get("cookies").isJsonArray()) {
            array = root.getAsJsonObject().getAsJsonArray("cookies");
        }
        if (array == null) return;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject cookie = element.getAsJsonObject();
            String name = cookie.has("name") ? cookie.get("name").getAsString() : null;
            String value = cookie.has("value") ? cookie.get("value").getAsString() : null;
            if (name != null && value != null) put(cookies, name, value);
        }
    }

    private static void put(Map<String, String> cookies, String name, String value) {
        String key = name == null ? "" : name.trim();
        String val = value == null ? "" : value.trim();
        if (!key.isEmpty()) cookies.put(key, val);
    }

    

    private static Result redeem(Map<String, String> cookies) throws IOException {
        String url = "https://login.live.com/oauth20_authorize.srf?client_id=" + XBOX_CLIENT_ID
                + "&redirect_uri=" + enc(XBOX_REDIRECT_URI) + "&response_type=token&scope=" + enc(XBOX_SCOPE)
                + "&display=touch&locale=en";

        for (int redirect = 0; redirect < MAX_REDIRECTS; redirect++) {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setRequestProperty("User-Agent", USER_AGENT);
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
                connection.setRequestProperty("Cookie", header(cookies));

                int status = connection.getResponseCode();
                mergeSetCookies(connection, cookies);
                String location = connection.getHeaderField("Location");

                if (location != null && !location.isEmpty()) {
                    String refresh = extract(location, "refresh_token");
                    String access = extract(location, "access_token");
                    if ((access != null && !access.isEmpty()) || (refresh != null && !refresh.isEmpty())) {
                        return new Result(access, refresh, null);
                    }
                    url = absolute(location, url);
                    continue;
                }
                
                return new Result(null, null, "Cookies expired or need a fresh sign-in (HTTP " + status + ")");
            } finally {
                connection.disconnect();
            }
        }
        return new Result(null, null, "Too many redirects while signing in");
    }

    private static String header(Map<String, String> cookies) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (builder.length() > 0) builder.append("; ");
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static void mergeSetCookies(HttpURLConnection connection, Map<String, String> cookies) {
        for (Map.Entry<String, java.util.List<String>> entry : connection.getHeaderFields().entrySet()) {
            if (entry.getKey() == null || !entry.getKey().equalsIgnoreCase("Set-Cookie")) continue;
            for (String value : entry.getValue()) {
                if (value == null) continue;
                String pair = value.split(";", 2)[0];
                int eq = pair.indexOf('=');
                if (eq <= 0) continue;
                String name = pair.substring(0, eq).trim();
                String cookie = pair.substring(eq + 1).trim();
                
                if (!cookie.isEmpty() && !cookie.equalsIgnoreCase("deleted")) cookies.put(name, cookie);
            }
        }
    }

    private static String extract(String url, String key) {
        String needle = key + "=";
        int index = url.indexOf(needle);
        if (index < 0) return null;
        int start = index + needle.length();
        int end = start;
        while (end < url.length() && "&#? ".indexOf(url.charAt(end)) < 0) end++;
        try {
            return URLDecoder.decode(url.substring(start, end), "UTF-8");
        } catch (Exception e) {
            return url.substring(start, end);
        }
    }

    private static String absolute(String location, String base) {
        try {
            return new URL(new URL(base), location).toString();
        } catch (Exception e) {
            return location;
        }
    }

    private static String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
