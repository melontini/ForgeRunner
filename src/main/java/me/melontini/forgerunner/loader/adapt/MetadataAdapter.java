package me.melontini.forgerunner.loader.adapt;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.melontini.forgerunner.mod.ModFile;
import me.melontini.forgerunner.mod.ModJson;

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
    static void adapt(JsonObject forge, ModFile file) {
        JsonArray modInfoArray = forge.get("mods").getAsJsonArray();
        if (modInfoArray.size() > 1) {
            throw new IllegalStateException("Multiple mods in a single jar file are not supported (yet?)");
        }
        JsonObject modInfo = modInfoArray.get(0).getAsJsonObject();

        ModJson fabric = file.getModJson();
        fabric.accept(object -> MOD_INFO.forEach((key, value) -> {
            if (modInfo.has(key)) object.add(value, modInfo.get(key));
        }));

        JsonObject contact = new JsonObject();
        if (forge.has("issueTrackerURL"))
            contact.add("issues", forge.get("issueTrackerURL"));
        if (forge.has("displayURL"))
            contact.add("homepage", forge.get("displayURL"));
        fabric.accept(object -> object.add("contact", contact));
    }
}
