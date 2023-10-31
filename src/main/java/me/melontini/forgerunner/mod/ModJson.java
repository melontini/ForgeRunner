package me.melontini.forgerunner.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.melontini.forgerunner.api.adapt.IModJson;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModJson implements IModJson {

    private final JsonObject backing = new JsonObject();

    public ModJson() {
        backing.addProperty("schemaVersion", 1);
        backing.add("entrypoints", new JsonObject());

        JsonObject custom = new JsonObject();
        custom.addProperty("forgerunner:forge_mod", true);
        backing.add("custom", custom);
    }

    public void accept(Consumer<JsonObject> consumer) {
        consumer.accept(backing);
    }

    public void id(String modId) {
        backing.addProperty("id", modId);
    }
    public String id() {
        return backing.get("id").getAsString();
    }

    public void version(String version) {
        backing.addProperty("version", version);
    }
    public String version() {
        return backing.get("version").getAsString();
    }

    public void entrypoint(String entrypoint, String notation) {
        JsonObject entrypoints = backing.get("entrypoints").getAsJsonObject();
        JsonArray ep = entrypoints.has(entrypoint) ? entrypoints.get(entrypoint).getAsJsonArray() : new JsonArray();
        JsonPrimitive notationPrimitive = new JsonPrimitive(notation);
        if (!ep.contains(notationPrimitive)) ep.add(notationPrimitive);
        entrypoints.add(entrypoint, ep);
    }

    public void mixinConfig(String config) {
        JsonArray array = backing.has("mixins") ? backing.get("mixins").getAsJsonArray() : new JsonArray();
        array.add(config);
        backing.add("mixins", array);
    }

    public List<String> mixinConfigs() {
        if (backing.has("mixins")) {
            List<String> mixins = new ArrayList<>();
            for (JsonElement element : backing.get("mixins").getAsJsonArray()) {
                mixins.add(element.getAsString());
            }
            return mixins;
        }
        return List.of();
    }

    public void jar(String jar) {
        JsonArray array = backing.has("jars") ? backing.get("jars").getAsJsonArray() : new JsonArray();

        JsonObject file = new JsonObject();
        file.addProperty("file", jar);
        array.add(file);

        backing.add("jars", array);
    }

    public boolean has(String s) {
        return backing.has(s);
    }

    @Override
    public byte[] toBytes() {
        return backing.toString().getBytes();
    }
}
