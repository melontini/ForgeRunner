package me.melontini.forgerunner.loader.adapt;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.ByteConvertible;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.api.adapt.IModJson;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Map;

@Log4j2
public class MetadataAdapter implements Adapter {

    private static final Map<String, String> MOD_INFO = ImmutableMap.<String, String>builder()
            .put("modId", "id")
            .put("version", "version")
            .put("displayName", "name")
            .put("logoFile", "icon")
            .put("description", "description")
            .put("license", "license")
            .build();

    static void adapt(JsonObject forge, IModFile file) {
        JsonArray modInfoArray = forge.get("mods").getAsJsonArray();
        if (modInfoArray.size() > 1) log.error("{} contains more than one mod in it's metadata, which is currently not supported. Issues may arise", file.path().getFileName());
        JsonObject modInfo = modInfoArray.get(0).getAsJsonObject();

        IModJson fabric = file.modJson();
        fabric.accept(object -> {
            String v = file.manifest().get().getMainAttributes().getValue("Implementation-Version");
            if (v != null) object.addProperty("version", v);
        });
        fabric.accept(object -> MOD_INFO.forEach((key, value) -> {
            if (modInfo.has(key) && !object.has(key)) object.add(value, modInfo.get(key));
        }));

        JsonObject contact = new JsonObject();
        if (forge.has("issueTrackerURL"))
            contact.add("issues", forge.get("issueTrackerURL"));
        if (forge.has("displayURL"))
            contact.add("homepage", forge.get("displayURL"));
        fabric.accept(object -> object.add("contact", contact));

        fabric.accept(object -> object.addProperty("environment", "*"));

        String id = file.id();
        if (!forge.has("dependencies")) return;
        JsonObject deps =  forge.get("dependencies").getAsJsonObject();
        if (!deps.has(id)) return;
        JsonArray modDeps = deps.get(id).getAsJsonArray();
        JsonObject depends = new JsonObject();
        JsonObject recommends = new JsonObject();

        for (JsonElement modDep : modDeps) {
            if (!modDep.isJsonObject()) continue;
            JsonObject dep = modDep.getAsJsonObject();
            String modId = dep.get("modId").getAsString();
            if (dep.get("mandatory") instanceof JsonPrimitive jp && jp.getAsBoolean()) {
                depends.addProperty(modId, "*"); //TODO versions
            } else {
                recommends.addProperty(modId, "*"); //TODO versions
            }
        }
        if (depends.size() > 0) fabric.accept(object -> object.add("depends", depends));
        if (recommends.size() > 0) fabric.accept(object -> object.add("recommends", recommends));
    }

    @Override
    public void adapt(IModFile mod, IEnvironment env) {
        if (mod.hasForgeMeta()) {
            ByteConvertible forge = mod.getFile("META-INF/mods.toml");
            if (forge == null) return;

            CommentedConfig cc = TomlFormat.instance().createParser().parse(new InputStreamReader(new ByteArrayInputStream(forge.toBytes())));
            JsonObject forgeMeta = env.gson().fromJson(JsonFormat.minimalInstance().createWriter().writeToString(cc), JsonObject.class);

            MetadataAdapter.adapt(forgeMeta, mod);
        }
    }

    @Override
    public long priority() {
        return 0;
    }
}
