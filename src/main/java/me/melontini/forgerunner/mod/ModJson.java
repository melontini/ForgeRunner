package me.melontini.forgerunner.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModJson implements ByteConvertible {

    private final JsonObject backing = new JsonObject();

    public ModJson() {
        backing.addProperty("schemaVersion", 1);
        backing.add("entrypoints", new JsonObject());
    }

    public void accept(Consumer<JsonObject> consumer) {
        consumer.accept(backing);
    }

    public void id(String modId) {
        backing.addProperty("id", modId);
    }
    public void version(String version) {
        backing.addProperty("version", version);
    }

    public void addEntrypoint(String entrypoint, String notation) {
        JsonObject entrypoints = backing.get("entrypoints").getAsJsonObject();
        JsonArray ep = entrypoints.has(entrypoint) ? entrypoints.get(entrypoint).getAsJsonArray() : new JsonArray();
        ep.add(notation);
        entrypoints.add(entrypoint, ep);
    }

    public void addMixins(String config) {
        JsonArray array = backing.has("mixins") ? backing.get("mixins").getAsJsonArray() : new JsonArray();
        array.add(config);
        backing.add("mixins", array);
    }

    public List<String> getMixins() {
        if (backing.has("mixins")) {
            List<String> mixins = new ArrayList<>();
            for (JsonElement element : backing.get("mixins").getAsJsonArray()) {
                mixins.add(element.getAsString());
            }
            return mixins;
        }
        return List.of();
    }

    @Override
    public byte[] toBytes() {
        return backing.toString().getBytes();
    }
}
