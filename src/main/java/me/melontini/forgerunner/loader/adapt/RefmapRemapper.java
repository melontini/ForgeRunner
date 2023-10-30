package me.melontini.forgerunner.loader.adapt;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.ByteConvertible;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.loader.remapping.SrgRemapper;
import me.melontini.forgerunner.mod.MixinConfig;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

@Log4j2
public class RefmapRemapper implements Adapter {

    public static void remap(JsonObject object, Remapper remapper) {
        if (!object.has("data")) return;

        JsonObject mappings = object.get("mappings").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
            if (entry.getValue() instanceof JsonObject jo) {
                HashSet<Map.Entry<String, JsonElement>> set = new HashSet<>(jo.entrySet());
                for (Map.Entry<String, JsonElement> innerEntry : set) {
                    if (innerEntry.getValue() instanceof JsonPrimitive jp && jp.isString()) {
                        try {
                            jo.remove(innerEntry.getKey());
                            jo.add(innerEntry.getKey(), new JsonPrimitive(remapRef(jp.getAsString(), remapper)));
                        } catch (Throwable t) {
                            throw  new IllegalStateException("Failed to remap refmap mappings! mixin: %s, ref: %s, mapped %s".formatted(entry.getKey(), innerEntry.getKey(), jp), t);
                        }
                    }
                }
            }
        }

        JsonObject data = object.get("data").getAsJsonObject();
        for (Map.Entry<String, JsonElement> mappingData : new HashSet<>(data.entrySet())) {
            for (Map.Entry<String, JsonElement> entry : mappingData.getValue().getAsJsonObject().entrySet()) {
                if (entry.getValue() instanceof JsonObject jo) {
                    HashSet<Map.Entry<String, JsonElement>> set = new HashSet<>(jo.entrySet());
                    for (Map.Entry<String, JsonElement> innerEntry : set) {
                        if (innerEntry.getValue() instanceof JsonPrimitive jp && jp.isString()) {
                            try {
                                jo.remove(innerEntry.getKey());
                                jo.add(innerEntry.getKey(), new JsonPrimitive(remapRef(jp.getAsString(), remapper)));
                            } catch (Throwable t) {
                                throw  new IllegalStateException("Failed to remap refmap mappings! mixin: %s, ref: %s, mapped %s".formatted(entry.getKey(), innerEntry.getKey(), jp), t);
                            }
                        }
                    }
                }
            }
            if ("searge".equals(mappingData.getKey())) {
                data.remove("searge");
                data.add("named:intermediary", mappingData.getValue());
            }
        }
    }

    private static String remapRef(String reference, Remapper remapper) {
        String owner = null;
        if (reference.startsWith("L") && reference.indexOf(';') != reference.length() - 1) {
            owner = reference.substring(0, reference.indexOf(';') + 1);
        }
        String left = owner != null ? reference.substring(reference.indexOf(';') + 1) : reference;
        if (reference.contains(":")) {
            return remapField(owner, left.substring(0, left.indexOf(':')), left.substring(left.indexOf(':') + 1), remapper);
        }
        if (reference.contains("(")) {
            return remapMethod(owner, left.substring(0, left.indexOf('(')), left.substring(left.indexOf('(')), remapper);
        }
        if ((reference.startsWith("L") || reference.startsWith("[")) && reference.endsWith(";"))
            return remapper.mapDesc(reference);
        else return remapper.mapType(reference);
    }

    private static String remapField(String owner, String name, String desc, Remapper remapper) {
        name = SrgRemapper.mapFieldName(owner, name, desc);
        desc = remapper.mapDesc(desc);

        String mappedOwner = owner != null ? remapper.mapDesc(owner) : "";
        return mappedOwner + name + ':' + desc;
    }

    private static String remapMethod(String owner, String name, String desc, Remapper remapper) {
        name = SrgRemapper.mapMethodName(owner, name, desc);
        desc = remapper.mapMethodDesc(desc);

        String mappedOwner = owner != null ? remapper.mapDesc(owner) : "";
        return mappedOwner + name + desc;
    }

    @Override
    public void adapt(IModFile mod, IEnvironment env) {
        for (String mixinConfig : mod.mixinConfigs()) {
            MixinConfig config = (MixinConfig) mod.getFile(mixinConfig);
            if (config == null) continue;

            String refmapFile = config.getRefMap();
            if (refmapFile == null) continue;
            ByteConvertible refmap = mod.getFile(refmapFile);
            if (refmap == null) continue;

            Reader bais = new InputStreamReader(new ByteArrayInputStream(refmap.toBytes()));
            JsonObject refmapObject = JsonParser.parseReader(bais).getAsJsonObject();
            RefmapRemapper.remap(refmapObject, env.frr());
            mod.putFile(refmapFile, () -> refmapObject.toString().getBytes());
        }
    }

    @Override
    public long priority() {
        return 30;
    }

    @Override
    public void onPrepare(IModFile mod, IEnvironment env, FileSystem fs) throws IOException {
        for (String mixinConfig : mod.mixinConfigs()) {
            MixinConfig config = (MixinConfig) mod.getFile(mixinConfig);
            if (config == null) continue;

            String refmapFile = config.getRefMap();
            if (refmapFile == null) continue;
            Path p = fs.getPath(refmapFile);
            if (!Files.exists(p)) continue;

            byte[] bytes = Files.readAllBytes(p);
            mod.putFile(refmapFile, () -> bytes);
        }
    }
}
