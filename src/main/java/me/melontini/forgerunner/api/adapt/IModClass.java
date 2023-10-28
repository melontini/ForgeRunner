package me.melontini.forgerunner.api.adapt;

import me.melontini.forgerunner.api.ByteConvertible;

import java.util.function.BiFunction;

public interface IModClass extends ByteConvertible {

    void accept(BiFunction<String, byte[], byte[]> transformer);
}
