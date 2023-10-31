package me.melontini.forgerunner.api.adapt;

import com.google.gson.JsonObject;
import me.melontini.forgerunner.api.ByteConvertible;

import java.util.List;
import java.util.function.Consumer;

public interface IModJson extends ByteConvertible {

    void accept(Consumer<JsonObject> consumer);

    String id();
    String version();
    //void entrypoint(String entrypoint, String notation);
    void addCustomEntrypoint(String entrypoint, String notation);
    void mixinConfig(String config);
    List<String> mixinConfigs();
    void jar(String jar);
}
