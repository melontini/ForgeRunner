package me.melontini.forgerunner.mod;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public record ModClass(String name, AtomicReference<byte[]> bytes, Environment environment) implements ByteConvertible {

    public ModClass(String name, byte[] bytes, Environment environment) {
        this(name, new AtomicReference<>(bytes), environment);
    }

    public void accept(BiFunction<String, byte[], byte[]> transformer) {
        bytes.set(transformer.apply(name, bytes.get()));
    }

    @Override
    public byte[] toBytes() {
        return bytes.get();
    }
}
