package me.melontini.forgerunner.loader;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.forge.mod.Mods;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.*;
import net.fabricmc.loader.impl.gui.FabricGuiEntry;
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
            "com_github_llamalad7_mixinextras", List.of("mixinextras"),
            "fabric-language-kotlin", List.of("kotlinforforge")
    );//TODO: use a service
    private static final String CACHE_DIR_NAME = ".fabric";
    private static final String PROCESSED_MODS = "processedForgeMods";

    @SneakyThrows
    public static void inject() {
        Map<String, ModContainerImpl> modMap = Loader.getModMap();
        for (ModContainerImpl mod : Loader.getInstance().getModsInternal()) {
            modMap.putIfAbsent(mod.getMetadata().getId().replace('-', '_'), mod);
            //Some mods use hyphens in their ID on Fabric and underscores on Forge, so we link hyphens with underscores.
        }
        ALIASES.forEach((s, strings) -> {
            ModContainerImpl impl = modMap.get(s);
            if (impl != null) strings.forEach(alias -> modMap.put(alias, impl));
        });

        Map<String, Set<ModCandidate>> envDisabledMods = new HashMap<>();
        ModDiscoverer discoverer = new ModDiscoverer(new VersionOverrides(), new DependencyOverrides(Loader.getInstance().getConfigDir()));
        discoverer.addCandidateFinder(new DirectoryModCandidateFinder(Loader.REMAPPED_MODS, Loader.getInstance().isDevelopmentEnvironment()));
        List<ModCandidate> candidates = discoverer.discoverMods(Loader.getInstance(), envDisabledMods);

        candidates = resolve(candidates);

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

    private static void remove(Collection<ModCandidate> candidates, ModCandidate candidate) {
        candidates.removeIf(candidate1 -> candidate1.getId().equals(candidate.getId()) &&
                candidate1.getVersion().equals(candidate.getVersion()));
        for (ModCandidate modCandidate : candidates) {
            remove(modCandidate.getNestedMods(), candidate);
        }
    }

    private static List<ModCandidate> resolve(List<ModCandidate> candidates) {
        Map<String, Set<ModCandidate>> idMap = new LinkedHashMap<>();
        candidates.forEach(candidate -> idMap.computeIfAbsent(candidate.getId(), k -> new LinkedHashSet<>()).add(candidate));

        for (Map.Entry<String, Set<ModCandidate>> e : idMap.entrySet()) {
            ModCandidate top = e.getValue().stream().max(Comparator.comparing(ModCandidate::getVersion)).orElseThrow();
            if (Loader.getInstance().isModLoaded(top.getId())) {
                for (ModCandidate candidate : e.getValue()) {
                    remove(candidates, candidate);
                }
                continue;
            }

            e.getValue().remove(top);
            for (ModCandidate candidate : e.getValue()) {
                remove(candidates, candidate);
            }
        }

        Map<ModCandidate, Set<ModDependency>> unmet = new HashMap<>();
        Map<ModCandidate, Set<ModDependency>> recommends = new HashMap<>();
        for (ModCandidate candidate : candidates) {
            for (ModDependency dependency : candidate.getDependencies()) {
                switch (dependency.getKind()) {
                    case DEPENDS -> resolve(dependency, candidate, idMap.keySet(), unmet);
                    case RECOMMENDS -> resolve(dependency, candidate, idMap.keySet(), recommends);
                    default -> throw new IllegalStateException("Unexpected value: " + dependency.getKind());
                }
            }
        }
        if (!unmet.isEmpty()) {
            StringBuilder solution = new StringBuilder();
            solution.append("Some of your mods are incompatible with the game or each other!").append('\n');
            solution.append("A potential solution has been determined, this may resolve your problem:").append('\n');

            Set<String> dIds = new HashSet<>();
            unmet.forEach((candidate, modDependencies) -> modDependencies.forEach(modDependency -> dIds.add(modDependency.getModId())));
            dIds.forEach(s -> solution.append("\t").append("- Install ").append(s).append(", any version").append('\n'));
            solution.append("More details:").append('\n');
            unmet.forEach((candidate, modDependencies) -> modDependencies.forEach(modDependency -> solution
                    .append('\t').append("- Mod '").append(candidate.getMetadata().getName())
                    .append(" (").append(candidate.getId()).append(") ").append(candidate.getMetadata().getVersion())
                    .append(" requires any version of ").append(modDependency.getModId())
                    .append(", which is missing!").append('\n')));
            log.info(solution);
            FabricGuiEntry.displayError("Mod resolution exception!", new ModResolutionException(solution.toString()), true);
        }

        if (!recommends.isEmpty()) {
            recommends.forEach((candidate, modDependencies) -> modDependencies.forEach(dependency -> {
                StringBuilder solution = new StringBuilder();
                solution.append("- Mod '").append(candidate.getMetadata().getName())
                        .append(" (").append(candidate.getId()).append(") ").append(candidate.getMetadata().getVersion())
                        .append(" recommends any version of ").append(dependency.getModId())
                        .append(", which is missing!").append('\n');
                solution.append('\t').append("- You should install any version of ").append(dependency.getModId()).append(" for the optimal experience.").append('\n');
                log.info(solution);
            }));
        }
        return candidates;
    }

    private static void resolve(ModDependency dependency, ModCandidate candidate, Set<String> resolvedIds, Map<ModCandidate, Set<ModDependency>> deps){
        if (dependency.getKind().isPositive()) {
            if (!Loader.getInstance().isModLoaded(dependency.getModId()) && !resolvedIds.contains(dependency.getModId())) {
                deps.computeIfAbsent(candidate, candidate1 -> new HashSet<>()).add(dependency);
            }
        } else {
            if (Loader.getInstance().isModLoaded(dependency.getModId()) || resolvedIds.contains(dependency.getModId())) {
                deps.computeIfAbsent(candidate, candidate1 -> new HashSet<>()).add(dependency);
            }
        }
    }
}
