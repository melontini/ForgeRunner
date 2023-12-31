package me.melontini.forgerunner.util;

import java.nio.file.Path;
import java.util.jar.JarFile;

public record JarPath(JarFile jarFile, Path path, boolean temp) {

    @Override
    public String toString() {
        return path.toString();
    }
}
