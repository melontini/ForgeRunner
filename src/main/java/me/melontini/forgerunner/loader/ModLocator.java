package me.melontini.forgerunner.loader;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.impl.discovery.DirectoryModCandidateFinder;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarFile;

@Log4j2
public class ModLocator {

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Function<Path, Boolean> IS_VALID_FILE = Exceptions.uncheck(() -> {
        MethodHandles.Lookup pl = MethodHandles.privateLookupIn(DirectoryModCandidateFinder.class, LOOKUP);
        MethodHandle mh = pl.findStatic(DirectoryModCandidateFinder.class, "isValidFile", MethodType.methodType(boolean.class, Path.class));
        return path -> (Boolean) Exceptions.uncheck(() -> mh.invoke(path));
    });
    private static final long CACHE_VERSION = 18;

    @SneakyThrows
    public static Set<JarPath> start() {
        Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
        log.info("Scanning {} for Forge mods", mods);

        Set<Path> guaranteedFabric = new HashSet<>();
        for (ModContainer allMod : FabricLoader.getInstance().getAllMods()) {
            if (allMod.getOrigin().getKind() == ModOrigin.Kind.PATH) {
                guaranteedFabric.addAll(allMod.getOrigin().getPaths());
            }
        }
        log.info("Found {} guaranteed Fabric mod" + (guaranteedFabric.size() == 1 ? "" : "s"), guaranteedFabric.size());

        Set<JarPath> candidates = new LinkedHashSet<>();
        Files.walkFileTree(mods, new SimpleFileVisitor<>() {
            @SneakyThrows
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!guaranteedFabric.contains(file) && IS_VALID_FILE.apply(file))
                    candidates.add(new JarPath(new JarFile(file.toFile()), file, false));
                return FileVisitResult.CONTINUE;
            }
        });
        candidates.removeIf(path -> path.jarFile().getEntry("META-INF/mods.toml") == null);
        log.info("Found {} Forge mod candidate" + (candidates.size() == 1 ? "" : "s"), candidates.size());

        Path p = Loader.HIDDEN_FOLDER.resolve("cache_version");
        boolean resetCache = !Files.exists(p) || Long.parseLong(Files.readString(p).trim()) != CACHE_VERSION;

        Files.walkFileTree(Loader.REMAPPED_MODS, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (resetCache || !Files.exists(mods.resolve(file.getFileName()))) {
                    Files.deleteIfExists(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if (resetCache) Files.writeString(p, String.valueOf(CACHE_VERSION));

        return candidates;
    }
}
