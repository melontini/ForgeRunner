package me.melontini.forgerunner.mod;

import me.melontini.forgerunner.api.adapt.IModClass;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public record ModClass(String name, AtomicReference<byte[]> bytes) implements IModClass {

    public ModClass(String name, byte[] bytes) {
        this(name, new AtomicReference<>(bytes));
    }

    public void accept(BiFunction<String, byte[], byte[]> transformer) {
        bytes.set(transformer.apply(name, bytes.get()));
    }

    @Override
    public byte[] toBytes() {
        return bytes.get();
    }
}
