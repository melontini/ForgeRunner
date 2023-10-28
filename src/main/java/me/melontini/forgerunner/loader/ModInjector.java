package me.melontini.forgerunner.loader;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.forge.mod.Mods;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.*;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.VersionOverrides;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

//Close your eyes
@Log4j2
public class ModInjector {

    private static final Map<String, List<String>> ALIASES = Map.of(
            "com_github_llamalad7_mixinextras", List.of("mixinextras")
    );//TODO: use a service
    private static final String CACHE_DIR_NAME = ".fabric";
    private static final String PROCESSED_MODS = "processedForgeMods";

    @SneakyThrows
    public static void inject() {
        Map<String, ModContainerImpl> modMap = Loader.getModMap();
        for (ModContainerImpl mod : Loader.getInstance().getModsInternal()) {
            modMap.putIfAbsent(mod.getMetadata().getId().replace("-", "_"), mod);
            //Some mods use hyphens in their ID on Fabric and underscores on Forge, so we link hyphens with underscores.
        }
        ALIASES.forEach((s, strings) -> {
            ModContainerImpl impl = modMap.get(s);
            if (impl != null) strings.forEach(alias -> modMap.put(alias, impl));
        });

        Map<String, Set<ModCandidate>> envDisabledMods = new HashMap<>();
        ModDiscoverer discoverer = new ModDiscoverer(new VersionOverrides(), new DependencyOverrides(Loader.getInstance().getGameDir()));
        discoverer.addCandidateFinder(new DirectoryModCandidateFinder(Loader.REMAPPED_MODS, Loader.getInstance().isDevelopmentEnvironment()));
        List<ModCandidate> candidates = discoverer.discoverMods(Loader.getInstance(), envDisabledMods);

        candidates.removeIf(candidate -> candidate.isBuiltin() || Loader.getInstance().isModLoaded(candidate.getId()));
        candidates.forEach(modCandidate -> modCandidate.getNestedMods().removeIf(candidate -> candidate.isBuiltin() || Loader.getInstance().isModLoaded(candidate.getId())));

        candidates = ModResolver.resolve(candidates, Loader.getInstance().getEnvironmentType(), envDisabledMods);

        if (candidates.isEmpty()) {
            log.info("No Forge mod candidates will be loaded");
            return;
        }

        Path cacheDir = Loader.getInstance().getGameDir().resolve(CACHE_DIR_NAME);
        Path outputdir = cacheDir.resolve(PROCESSED_MODS);
        if (Loader.getInstance().isDevelopmentEnvironment()) {
            if (System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE) == null) {
                Log.warn(LogCategory.MOD_REMAP, "Runtime mod remapping disabled due to no fabric.remapClasspathFile being specified. You may need to update loom.");
            } else {
                RuntimeModRemapper.remap(candidates, cacheDir.resolve(CACHE_DIR_NAME), outputdir);
            }
        }

        Loader.dumpModsList(candidates);

        for (ModCandidate mod : candidates) {
            if (!mod.hasPath() && !mod.isBuiltin()) {
                try {
                    mod.setPaths(Collections.singletonList(mod.copyToDir(outputdir, false)));
                } catch (IOException e) {
                    throw new RuntimeException("Error extracting mod " + mod, e);
                }
            }

            ModContainerImpl container = new ModContainerImpl(mod);
            Mods.addForgeMod(container);
            modMap.put(mod.getId(), container);

            for (String provides : mod.getProvides()) {
                modMap.put(provides, container);
            }
        }

        for (ModContainerImpl forgeMod : Mods.getForgeMods()) {
            for (Path path : forgeMod.getCodeSourcePaths()) {
                FabricLauncherBase.getLauncher().addToClassPath(path);
            }
        }

        Loader.appendMods(Mods.getForgeMods());
    }
}
