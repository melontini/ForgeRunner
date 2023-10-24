package me.melontini.forgerunner.loader;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.loader.adapt.ModAdapter;
import me.melontini.forgerunner.util.Exceptions;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.DirectoryModCandidateFinder;
import net.fabricmc.loader.impl.discovery.ModCandidate;
import net.fabricmc.loader.impl.discovery.ModDiscoverer;
import net.fabricmc.loader.impl.discovery.RuntimeModRemapper;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.VersionOverrides;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

//Close your eyes
@Slf4j
public class ModInjector {

    private static final Supplier<List<ModContainerImpl>> MODS = Exceptions.uncheck(() -> {
        Field modsField = FabricLoaderImpl.class.getDeclaredField("mods");
        modsField.setAccessible(true);
        return () -> (List<ModContainerImpl>) Exceptions.uncheck(() -> modsField.get(FabricLoaderImpl.INSTANCE));
    });
    private static final Supplier<Map<String, ModContainerImpl>> MOD_MAP = Exceptions.uncheck(() -> {
        Field modMapField = FabricLoaderImpl.class.getDeclaredField("modMap");
        modMapField.setAccessible(true);
        return () -> (Map<String, ModContainerImpl>) Exceptions.uncheck(() -> modMapField.get(FabricLoaderImpl.INSTANCE));
    });
    private static final List<ModContainerImpl> FORGE_MODS = new ArrayList<>();

    @SneakyThrows
    public static void inject() {
        List<ModContainerImpl> mods = MODS.get();//Can't be modified, due to ConcurrentModificationException
        Map<String, ModContainerImpl> modMap = MOD_MAP.get();
        for (ModContainerImpl mod : FabricLoaderImpl.INSTANCE.getModsInternal()) {
            modMap.putIfAbsent(mod.getMetadata().getId().replace("-", "_"), mod);
            //Some mods use hyphens in their ID on Fabric and underscores on Forge, so we link hyphens with underscores.
        }

        ModDiscoverer discoverer = new ModDiscoverer(new VersionOverrides(), new DependencyOverrides(FabricLoader.getInstance().getGameDir()));
        discoverer.addCandidateFinder(new DirectoryModCandidateFinder(ModAdapter.REMAPPED_MODS, FabricLoader.getInstance().isDevelopmentEnvironment()));
        List<ModCandidate> candidates = discoverer.discoverMods(FabricLoaderImpl.INSTANCE, Map.of());
        candidates.removeIf(candidate -> candidate.isBuiltin() || FabricLoader.getInstance().isModLoaded(candidate.getId()));
        if (candidates.isEmpty()) {
            log.info("No Forge mods found");
            return;
        }

        Path cacheDir = FabricLoader.getInstance().getGameDir().resolve(FabricLoaderImpl.CACHE_DIR_NAME);
        Path outputdir = cacheDir.resolve("processedForgeMods");
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            if (System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE) == null) {
                Log.warn(LogCategory.MOD_REMAP, "Runtime mod remapping disabled due to no fabric.remapClasspathFile being specified. You may need to update loom.");
            } else {
                RuntimeModRemapper.remap(candidates, cacheDir.resolve(FabricLoaderImpl.CACHE_DIR_NAME), outputdir);
            }
        }

        Method m = FabricLoaderImpl.class.getDeclaredMethod("dumpModList", List.class);
        m.setAccessible(true);
        m.invoke(FabricLoaderImpl.INSTANCE, candidates);

        for (ModCandidate mod : candidates) {
            if (!mod.hasPath() && !mod.isBuiltin()) {
                try {
                    mod.setPaths(Collections.singletonList(mod.copyToDir(outputdir, false)));
                } catch (IOException e) {
                    throw new RuntimeException("Error extracting mod " + mod, e);
                }
            }

            ModContainerImpl container = new ModContainerImpl(mod);
            FORGE_MODS.add(container);
            modMap.put(mod.getId(), container);

            for (String provides : mod.getProvides()) {
                modMap.put(provides, container);
            }
        }

        for (ModContainerImpl forgeMod : FORGE_MODS) {
            for (Path path : forgeMod.getCodeSourcePaths()) {
                FabricLauncherBase.getLauncher().addToClassPath(path);
            }
        }

        List<ModContainerImpl> mergedMods = new ArrayList<>(mods);
        mergedMods.addAll(FORGE_MODS);

        Field modsField = FabricLoaderImpl.class.getDeclaredField("mods");
        modsField.setAccessible(true);
        modsField.set(FabricLoaderImpl.INSTANCE, mergedMods);
    }
}
