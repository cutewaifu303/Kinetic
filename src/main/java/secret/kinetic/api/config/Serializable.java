package secret.kinetic.api.config;

import com.google.gson.JsonObject;

public interface Serializable {

    JsonObject save();

    void load(JsonObject object);

}
