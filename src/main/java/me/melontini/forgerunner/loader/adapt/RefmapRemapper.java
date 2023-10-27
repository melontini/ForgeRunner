package me.melontini.forgerunner.loader.adapt;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;
import me.melontini.forgerunner.loader.remapping.SrgRemapper;

import java.util.HashSet;
import java.util.Map;

@Log4j2
public class RefmapRemapper {

    public static void remap(JsonObject object, ForgeRunnerRemapper remapper) {
        if (!object.has("data")) return;
        JsonObject data = object.get("data").getAsJsonObject();
        if (!data.has("searge")) return;

        JsonObject mappings = object.get("mappings").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
            if (entry.getValue() instanceof JsonObject jo) {
                HashSet<Map.Entry<String, JsonElement>> set = new HashSet<>(jo.entrySet());
                for (Map.Entry<String, JsonElement> innerEntry : set) {
                    if (innerEntry.getValue() instanceof JsonPrimitive jp && jp.isString()) {
                        jo.remove(innerEntry.getKey());
                        jo.add(innerEntry.getKey(), new JsonPrimitive(remapRef(jp.getAsString(), remapper)));
                    }
                }
            }
        }

        JsonObject searge = data.get("searge").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : searge.entrySet()) {
            if (entry.getValue() instanceof JsonObject jo) {
                HashSet<Map.Entry<String, JsonElement>> set = new HashSet<>(jo.entrySet());
                for (Map.Entry<String, JsonElement> innerEntry : set) {
                    if (innerEntry.getValue() instanceof JsonPrimitive jp && jp.isString()) {
                        jo.remove(innerEntry.getKey());
                        jo.add(innerEntry.getKey(), new JsonPrimitive(remapRef(jp.getAsString(), remapper)));
                    }
                }
            }
        }
        data.remove("searge");
        data.add("named:intermediary", searge);
    }

    private static String remapRef(String reference, ForgeRunnerRemapper remapper) {
        String owner = null;
        if (reference.startsWith("L")) {
            owner = reference.substring(0, reference.indexOf(";") + 1);
        }
        String left = owner != null ? reference.substring(reference.indexOf(";") + 1) : reference;
        if (reference.contains(":")) {
            return remapField(owner, left.substring(0, left.indexOf(":")), left.substring(left.indexOf(":") + 1), remapper);
        }
        if (reference.contains("(")) {
            return remapMethod(owner, left.substring(0, left.indexOf("(")), left.substring(left.indexOf("(")), remapper);
        }
        if ((reference.startsWith("L") || reference.startsWith("[")) && reference.endsWith(";"))
            return remapper.mapDesc(reference);
        else return remapper.mapType(reference);
    }

    private static String remapField(String owner, String name, String desc, ForgeRunnerRemapper remapper) {
        String top = SrgRemapper.getFieldOwner(name, desc);

        String s = top != null ? top : owner != null ? owner.substring(1, owner.length() - 1) : "";
        name = SrgRemapper.mapFieldName(s, name, desc);
        desc = remapper.mapDesc(desc);

        String mappedOwner = owner != null ? remapper.mapDesc(owner) : "";
        return mappedOwner + name + ":" + desc;
    }

    private static String remapMethod(String owner, String name, String desc, ForgeRunnerRemapper remapper) {
        String top = SrgRemapper.getMethodOwner(name, desc);

        String s = top != null ? top : owner != null ? owner.substring(1, owner.length() - 1) : "";
        name = SrgRemapper.mapMethodName(s, name, desc);
        desc = remapper.mapMethodDesc(desc);

        String mappedOwner = owner != null ? remapper.mapDesc(owner) : "";
        return mappedOwner + name + desc;
    }
}
