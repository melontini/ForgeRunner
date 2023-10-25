package me.melontini.forgerunner.loader.adapt;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.loader.TinyResolver;
import net.fabricmc.tinyremapper.api.TrRemapper;

import java.util.HashSet;
import java.util.Map;

@Slf4j
public class RefmapRemapper {

    public static void remap(JsonObject object, TrRemapper remapper) {
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

        JsonObject data = object.get("data").getAsJsonObject();
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

    private static String remapRef(String reference, TrRemapper remapper) {
        String owner = null;
        if (reference.startsWith("L")) {
            owner = reference.substring(0, reference.indexOf(";") + 1);
        }
        String left = owner!=null ? reference.replaceFirst(owner, "") : reference;
        if (reference.contains(":")) {
            return remapField(owner, left.substring(0, left.indexOf(":")), left.substring(left.indexOf(":")), remapper);
        }
        if (reference.contains("(")) {
            return remapMethod(owner, left.substring(0, left.indexOf("(")), left.substring(left.indexOf("(")), remapper);
        }
        return remapper.mapDesc(reference);
    }

    private static String remapField(String owner, String name, String desc, TrRemapper remapper) {
        String top = TinyResolver.getMethodOwner(name, desc);

        String s = top != null ? top : owner.substring(1, owner.length() - 1);
        name = TinyResolver.mapFieldName(s.replace("/", "."), name, desc);
        desc = remapper.mapDesc(desc);

        String mappedOwner = owner != null ? remapper.mapDesc(owner) : "";
        return mappedOwner + name + ":" + desc;
    }

    private static String remapMethod(String owner, String name, String desc, TrRemapper remapper) {
        String top = TinyResolver.getMethodOwner(name, desc);

        String s = top != null ? top : owner.substring(1, owner.length() - 1);
        name = TinyResolver.mapMethodName(s.replace("/", "."), name, desc);
        desc = remapper.mapMethodDesc(desc);

        String mappedOwner = owner != null ? remapper.mapDesc(owner) : "";
        return mappedOwner + name + desc;
    }
}
