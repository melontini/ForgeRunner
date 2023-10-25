package me.melontini.forgerunner.mod;

import java.io.ByteArrayOutputStream;

public record Manifest(java.util.jar.Manifest manifest) implements ByteConvertible {

    @Override
    public byte[] toBytes() {
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            manifest.write(o);
            return o.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
