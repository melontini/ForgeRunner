package me.melontini.forgerunner.mod;

import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;

import java.util.List;
import java.util.Map;

public record Environment(Map<String, ModClass> classPool, List<ModFile> modFiles, ForgeRunnerRemapper frr) {

    public void addClass(ModClass modClass) {
        classPool.put(modClass.name(), modClass);
    }

    public void appendModFile(ModFile modFile) {
        modFiles.add(modFile);
    }
}
