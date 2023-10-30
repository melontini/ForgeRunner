package me.melontini.forgerunner.api.adapt;

import java.io.IOException;
import java.nio.file.FileSystem;

public interface Adapter {

    void adapt(IModFile mod, IEnvironment env);

    long priority();

    default void onPrepare(IModFile mod, IEnvironment env, FileSystem fs) throws IOException {}
}
