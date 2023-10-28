package me.melontini.forgerunner.api.adapt;

import me.melontini.forgerunner.api.ByteConvertible;

import java.util.function.Supplier;
import java.util.jar.Manifest;

public interface IManifest extends ByteConvertible, Supplier<Manifest> {

    Manifest get();
}
