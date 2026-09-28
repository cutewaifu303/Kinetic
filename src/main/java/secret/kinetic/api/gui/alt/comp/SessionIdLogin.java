package secret.kinetic.api.gui.alt.comp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import secret.kinetic.utils.client.NetworkUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class SessionIdLogin {

    private static final String MICROSOFT_CLIENT_ID = "00000000402B5328";
    private static final String MICROSOFT_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public static class LoginResult {
        public final String name;
        public final String uuid;
        public final String mcAccessToken;

        public LoginResult(String name, String uuid, String mcAccessToken) {
            this.name = name;
            this.uuid = uuid;
            this.mcAccessToken = mcAccessToken;
        }
    }

    public static String extractMinecraftToken(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (!trimmed.contains(":")) return trimmed;

        String best = null;
        for (String part : trimmed.split(":")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (part.startsWith("eyJ")) return part;
            if (!UUID_PATTERN.matcher(part).matches() && (best == null || part.length() > best.length())) best = part;
        }
        return best != null ? best : trimmed;
    }

    public static LoginResult login(String input) throws IOException {
        String minecraftToken = extractMinecraftToken(input);

        JsonObject profile = fetchProfile(minecraftToken);
        if (isValidProfile(profile)) {
            return new LoginResult(profile.get("name").getAsString(), profile.get("id").getAsString(), minecraftToken);
        }

        String accessToken = refreshMicrosoftToken(input.trim());
        String xblToken = authenticateXboxLive(accessToken);
        String[] xsts = authorizeXsts(xblToken);
        String mcToken = loginWithXbox(xsts[0], xsts[1]);

        profile = fetchProfile(mcToken);
        if (!isValidProfile(profile)) throw new IOException("No Minecraft profile on this account");
        return new LoginResult(profile.get("name").getAsString(), profile.get("id").getAsString(), mcToken);
    }

    private static JsonObject fetchProfile(String bearer) {
        return parseJson(NetworkUtils.getBearerResponse("https://api.minecraftservices.com/minecraft/profile", bearer));
    }

    private static boolean isValidProfile(JsonObject profile) {
        return profile != null && profile.has("name") && profile.has("id");
    }

    private static String refreshMicrosoftToken(String refreshToken) throws IOException {
        String response = NetworkUtils.postExternal(
                "https://login.live.com/oauth20_token.srf",
                "client_id=" + MICROSOFT_CLIENT_ID
                        + "&grant_type=refresh_token"
                        + "&refresh_token=" + urlEncode(refreshToken)
                        + "&scope=" + urlEncode(MICROSOFT_SCOPE),
                false);

        JsonObject json = parseJson(response);
        if (json == null || !json.has("access_token")) throw new IOException("Invalid Microsoft refresh token");
        return json.get("access_token").getAsString();
    }

    private static String authenticateXboxLive(String accessToken) throws IOException {
        IOException last = null;
        for (String ticket : new String[]{"d=" + accessToken, accessToken}) {
            JsonObject json = parseJson(NetworkUtils.postExternal(
                    "https://user.auth.xboxlive.com/user/authenticate",
                    "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\""
                            + ticket + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}",
                    true));

            if (json != null && json.has("Token")) return json.get("Token").getAsString();
            last = new IOException("Xbox Live rejected the token");
        }
        throw last;
    }

    private static String[] authorizeXsts(String xblToken) throws IOException {
        JsonObject json = parseJson(NetworkUtils.postExternal(
                "https://xsts.auth.xboxlive.com/xsts/authorize",
                "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken
                        + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}",
                true));

        if (json == null || !json.has("Token") || !json.has("DisplayClaims")) throw new IOException("XSTS authorization failed");

        String uhs = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
        return new String[]{uhs, json.get("Token").getAsString()};
    }

    private static String loginWithXbox(String uhs, String xstsToken) throws IOException {
        JsonObject json = parseJson(NetworkUtils.postExternal(
                "https://api.minecraftservices.com/authentication/login_with_xbox",
                "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}",
                true));

        if (json == null || !json.has("access_token")) throw new IOException("Minecraft authentication failed");
        return json.get("access_token").getAsString();
    }

    private static JsonObject parseJson(String response) {
        if (response == null || response.isEmpty()) return null;
        try {
            return new JsonParser().parse(response).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static String urlEncode(String value) throws IOException {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
