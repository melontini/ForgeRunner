package me.melontini.forgerunner.mod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;

public class MixinConfig implements ByteConvertible {

    private final JsonObject backing;

    public MixinConfig(Reader reader) {
        backing = JsonParser.parseReader(reader).getAsJsonObject();
    }

    public String getRefMap() {
        return backing.has("refmap") ? backing.get("refmap").getAsString() : null;
    }

    @Override
    public byte[] toBytes() {
        return backing.toString().getBytes();
    }
}
