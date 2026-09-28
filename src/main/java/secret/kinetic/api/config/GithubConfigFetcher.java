package secret.kinetic.api.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import secret.kinetic.Kinetic;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GithubConfigFetcher {

    private static final String REPO_API_URL = "https://api.github.com/repos/unleg1t/yuri-configs/contents";
    private static final String RAW_URL = "https://raw.githubusercontent.com/unleg1t/yuri-configs/main/";
    private static final String USER_AGENT = "Kinetic/1.8.9";

    private GithubConfigFetcher() {
    }

    public static List<String> fetchConfigList() {
        List<String> configs = new ArrayList<>();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(REPO_API_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            JsonArray files = new JsonParser().parse(response.toString()).getAsJsonArray();
            for (JsonElement element : files) {
                if (!element.isJsonObject()) continue;
                JsonElement nameElement = element.getAsJsonObject().get("name");
                if (nameElement == null) continue;
                String name = nameElement.getAsString();
                if (name.endsWith(".json")) configs.add(name.substring(0, name.length() - ".json".length()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configs;
    }

    public static boolean downloadAndLoadConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) return false;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(RAW_URL + configName + ".json").openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) content.append(line).append("\n");
            }

            JsonElement parsed = new JsonParser().parse(content.toString());
            if (parsed == null || !parsed.isJsonObject()) return false;
            JsonObject root = parsed.getAsJsonObject();

            JsonObject converted = YuriConfigConverter.isKineticConfig(root) ? root : YuriConfigConverter.convert(root);

            if (!ConfigManager.CONFIGS_DIR.exists()) {
                boolean ignored = ConfigManager.CONFIGS_DIR.mkdirs();
            }
            File configFile = new File(ConfigManager.CONFIGS_DIR, configName + ConfigManager.EXTENSION);
            String pretty = new GsonBuilder().setPrettyPrinting().create().toJson(converted);
            Files.write(configFile.toPath(), pretty.getBytes(StandardCharsets.UTF_8));

            ConfigManager manager = Kinetic.INSTANCE.getConfigManager();
            if (manager.findConfig(configName) == null) {
                manager.getElements().add(new Config(configName));
            }

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.isCallingFromMinecraftThread()) {
                return manager.loadConfig(configName);
            }
            return mc.addScheduledTask(() -> manager.loadConfig(configName)).get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
