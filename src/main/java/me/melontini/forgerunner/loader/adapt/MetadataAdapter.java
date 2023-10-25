package me.melontini.forgerunner.loader.adapt;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;

import java.util.Map;

public class MetadataAdapter {

    private static final Map<String, String> MOD_INFO = ImmutableMap.<String, String>builder()
            .put("modId", "id")
            .put("version", "version")
            .put("displayName", "name")
            .put("logoFile", "icon")
            .put("description", "description")
            .put("license", "license")
            .build();

    //TODO: dependencies.
    //TODO: environment.
    static void adapt(JsonObject fabric, JsonObject forge, ModAdapter adapter) {
        JsonArray modInfoArray = forge.get("mods").getAsJsonArray();
        if (modInfoArray.size() > 1) {
            throw new IllegalStateException("Multiple mods in a single jar file are not supported (yet?)");
        }
        JsonObject modInfo = modInfoArray.get(0).getAsJsonObject();

        fabric.addProperty("schemaVersion", 1);
        MOD_INFO.forEach((key, value) -> {
            if (modInfo.has(key)) fabric.add(value, modInfo.get(key));
        });

        JsonObject contact = new JsonObject();
        if (forge.has("issueTrackerURL"))
            contact.add("issues", forge.get("issueTrackerURL"));
        if (forge.has("displayURL"))
            contact.add("homepage", forge.get("displayURL"));
        fabric.add("contact", contact);

        adaptMixins(adapter, fabric);

        JsonObject entrypoints = new JsonObject();
        JsonArray main = new JsonArray();
        adapter.getEntrypointClasses().forEach(main::add);
        entrypoints.add("main", main);
        fabric.add("entrypoints", entrypoints);
    }

    @SneakyThrows
    private static void adaptMixins(ModAdapter adapter, JsonObject fabric) {
        if (!adapter.getMixinConfigs().isEmpty()) {
            JsonArray array = new JsonArray();
            adapter.getMixinConfigs().forEach(array::add);
            fabric.add("mixins", array);
        }
    }
}
