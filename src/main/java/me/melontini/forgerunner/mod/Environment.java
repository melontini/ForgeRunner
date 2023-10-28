package me.melontini.forgerunner.mod;

import com.google.gson.Gson;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;

import java.util.Map;
import java.util.Set;

public record Environment(Map<String, ModClass> classPool, Gson gson,
                          Set<ModFile> modFiles, ForgeRunnerRemapper frr) implements IEnvironment {

    public void addClass(ModClass modClass) {
        classPool.put(modClass.name(), modClass);
    }

    public void appendModFile(ModFile modFile) {
        modFiles.add(modFile);
    }
    public void removeModFile(ModFile modFile) {
        modFiles.remove(modFile);
    }
}
