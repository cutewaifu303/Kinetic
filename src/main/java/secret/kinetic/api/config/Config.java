package secret.kinetic.api.config;

import com.google.gson.JsonObject;
import secret.kinetic.Kinetic;
import secret.kinetic.managers.impl.RotationLearnerManager;
import secret.kinetic.modules.Module;
import secret.kinetic.modules.ModuleCategory;

import java.io.File;
import java.io.IOException;

public final class Config implements Serializable {

    private final String name;
    private final File file;

    public Config(String name) {
        this(name, true);
    }

    



    public Config(String name, boolean createFile) {
        this.name = name;
        this.file = new File(ConfigManager.CONFIGS_DIR, name + ConfigManager.EXTENSION);

        if (createFile && !file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    @Override
    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();
        JsonObject modulesObject = new JsonObject();

        for (Module module : Kinetic.INSTANCE.getModuleManager().getModules()) {
            if (module.getCategory() == ModuleCategory.RENDER) continue;
            modulesObject.add(module.getLabel(), module.save(false));
        }

        jsonObject.add("Modules", modulesObject);
        
        jsonObject.add("Visuals", new VisualsConfig().save());

        if (RotationLearnerManager.hasModelLoaded() && RotationLearnerManager.getLoadedModelName() != null) {
            String presetName = RotationLearnerManager.getLoadedModelName();
            String presetData = RotationLearnerManager.exportPresetRaw(presetName);
            if (presetData != null) {
                JsonObject rotationPreset = new JsonObject();
                rotationPreset.addProperty("name", presetName);
                rotationPreset.addProperty("data", presetData);
                jsonObject.add("RotationPreset", rotationPreset);
            }
        }

        return jsonObject;
    }

    @Override
    public void load(JsonObject object) {
        if (object.has("Modules")) {
            JsonObject modulesObject = object.getAsJsonObject("Modules");

            for (Module module : Kinetic.INSTANCE.getModuleManager().getModules()) {
                if (module.getCategory() == ModuleCategory.RENDER) continue;
                if (modulesObject.has(module.getLabel()))
                    module.load(modulesObject.getAsJsonObject(module.getLabel()));
            }
        }
        if (object.has("Visuals") && object.get("Visuals").isJsonObject()) {
            new VisualsConfig().load(object.getAsJsonObject("Visuals"));
        }
        if (object.has("RotationPreset")) {
            JsonObject rotationPreset = object.getAsJsonObject("RotationPreset");
            if (rotationPreset.has("name") && rotationPreset.has("data")) {
                String presetName = rotationPreset.get("name").getAsString();
                String presetData = rotationPreset.get("data").getAsString();
                if (RotationLearnerManager.importPresetRaw(presetName, presetData)) {
                    RotationLearnerManager.loadPreset(presetName);
                }
            }
        }
    }
}