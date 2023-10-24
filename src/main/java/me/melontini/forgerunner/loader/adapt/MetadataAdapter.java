package me.melontini.forgerunner.loader.adapt;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
    static void adapt(JsonObject fabric, JsonObject forge) {
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
        fabric.add("contact", contact);
    }
}
