package me.melontini.forgerunner.util;

import java.nio.file.Path;
import java.util.jar.JarFile;

public record JarPath(JarFile jarFile, Path path) {

}
