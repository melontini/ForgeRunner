package me.melontini.forgerunner.mod;

import me.melontini.forgerunner.api.adapt.IManifest;

import java.io.ByteArrayOutputStream;

public record Manifest(java.util.jar.Manifest manifest) implements IManifest {

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

    @Override
    public java.util.jar.Manifest get() {
        return manifest();
    }
}
