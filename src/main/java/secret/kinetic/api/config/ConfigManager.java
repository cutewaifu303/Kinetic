package secret.kinetic.api.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import secret.kinetic.Kinetic;
import secret.kinetic.utils.misc.Manager;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigManager extends Manager<Config> {

    @Getter
    private static ConfigManager instance;

    @Getter
    private final BindsConfig bindsConfig;

    @Getter
    private final VisualsConfig visualsConfig;

    public ConfigManager() {
        super(loadConfigs());
        instance = this;

        if (!CONFIGS_DIR.exists()) {
            boolean ignored = CONFIGS_DIR.mkdirs();
        }

        this.bindsConfig = new BindsConfig();
        this.visualsConfig = new VisualsConfig();
    }

    public static final File CONFIGS_DIR = new File(Kinetic.NAME, "configs");
    public static final String EXTENSION = ".json";

    
    private static final String BUNDLED_PATH = "/assets/minecraft/kinetic/configs/";
    private static final String BUNDLED_MANIFEST = BUNDLED_PATH + "manifest.txt";

    private List<String> bundledNames;

    
    public List<String> getBundledNames() {
        if (bundledNames != null) return bundledNames;

        List<String> names = new ArrayList<>();
        try (InputStream input = ConfigManager.class.getResourceAsStream(BUNDLED_MANIFEST)) {
            if (input != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) names.add(line);
                    }
                }
            }
        } catch (IOException ignored) {
        }
        bundledNames = Collections.unmodifiableList(names);
        return bundledNames;
    }

    public boolean isBundled(String configName) {
        if (configName == null) return false;
        for (String name : getBundledNames()) {
            if (name.equalsIgnoreCase(configName)) return true;
        }
        return false;
    }

    
    public boolean loadBundled(String name) {
        if (name == null) return false;
        String resolved = null;
        for (String bundled : getBundledNames()) {
            if (bundled.equalsIgnoreCase(name)) {
                resolved = bundled;
                break;
            }
        }
        if (resolved == null) return false;

        try (InputStream input = ConfigManager.class.getResourceAsStream(BUNDLED_PATH + resolved + EXTENSION)) {
            if (input == null) return false;
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonObject object = (JsonObject) new JsonParser().parse(reader);
                new Config(resolved, false).load(object);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadConfig(String configName) {
        if (configName == null) return false;
        Config config = findConfig(configName);

        if (config == null) return false;
        try (FileReader reader = new FileReader(config.getFile())) {
            JsonParser parser = new JsonParser();
            JsonObject object = (JsonObject) parser.parse(reader);
            config.load(object);
            return true;
        } catch (Exception e) {
            System.err.println("[Kinetic] Could not load config '" + configName + "': " + e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveConfig(String configName) {
        if (configName == null) return false;
        Config config;
        if ((config = findConfig(configName)) == null) {
            Config newConfig = (config = new Config(configName));
            getElements().add(newConfig);
        }

        String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(config.save());
        try (FileWriter writer = new FileWriter(config.getFile())) {
            writer.write(contentPrettyPrint);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Config findConfig(String configName) {
        if (configName == null) return null;
        for (Config config : getElements()) {
            if (config.getName().equalsIgnoreCase(configName))
                return config;
        }

        if (new File(CONFIGS_DIR, configName + EXTENSION).exists())
            return new Config(configName);

        return null;
    }

    public boolean deleteConfig(String configName) {
        if (configName == null) return false;
        Config config;
        if ((config = findConfig(configName)) != null) {
            final File f = config.getFile();
            getElements().remove(config);
            return f.exists() && f.delete();
        }
        return false;
    }

    private static ArrayList<Config> loadConfigs() {
        final ArrayList<Config> loadedConfigs = new ArrayList<>();
        File[] files = CONFIGS_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                if (FilenameUtils.getExtension(file.getName()).equals("json"))
                    loadedConfigs.add(new Config(FilenameUtils.removeExtension(file.getName())));
            }
        }
        return loadedConfigs;
    }
}