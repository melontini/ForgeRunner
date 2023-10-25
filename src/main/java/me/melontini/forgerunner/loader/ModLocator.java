package me.melontini.forgerunner.loader;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.loader.adapt.ModAdapter;
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarFile;

@Slf4j
public class ModLocator {

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Function<Path, Boolean> IS_VALID_FILE = Exceptions.uncheck(() -> {
        MethodHandles.Lookup pl = MethodHandles.privateLookupIn(DirectoryModCandidateFinder.class, LOOKUP);
        MethodHandle mh = pl.findStatic(DirectoryModCandidateFinder.class, "isValidFile", MethodType.methodType(boolean.class, Path.class));
        return path -> (Boolean) Exceptions.uncheck(() -> mh.invoke(path));
    });

    @SneakyThrows
    public static List<JarPath> start() {
        Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
        log.info("Scanning {} for Forge mods", mods);

        Set<Path> guaranteedFabric = new HashSet<>();
        for (ModContainer allMod : FabricLoader.getInstance().getAllMods()) {
            if (allMod.getOrigin().getKind() == ModOrigin.Kind.PATH) {
                guaranteedFabric.addAll(allMod.getOrigin().getPaths());
            }
        }
        log.info("Found {} guaranteed Fabric mod" + (guaranteedFabric.size() == 1 ? "" : "s"), guaranteedFabric.size());

        List<JarPath> candidates = new ArrayList<>();
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

        Files.walkFileTree(ModAdapter.REMAPPED_MODS, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.exists(mods.resolve(file.getFileName()))) {
                    Files.deleteIfExists(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return candidates;
    }
}
