package me.melontini.forgerunner.mod;

import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;

import java.util.Map;
import java.util.Set;

public record Environment(Map<String, ModClass> classPool, Set<ModFile> modFiles, ForgeRunnerRemapper frr) {

    public void addClass(ModClass modClass) {
        classPool.put(modClass.name(), modClass);
    }

    public void appendModFile(ModFile modFile) {
        modFiles.add(modFile);
    }
}
