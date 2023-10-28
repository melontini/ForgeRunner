package me.melontini.forgerunner.mod;

import com.google.gson.Gson;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.api.adapt.IModClass;
import me.melontini.forgerunner.api.adapt.IModFile;
import org.objectweb.asm.commons.Remapper;

import java.util.Map;
import java.util.Set;

public record Environment(Map<String, ModClass> classPool, Gson gson,
                          Set<ModFile> modFiles, Remapper frr) implements IEnvironment {

    public void addClass(IModClass modClass) {
        classPool.put(modClass.name(), (ModClass) modClass);
    }

    public void appendModFile(IModFile modFile) {
        modFiles.add((ModFile) modFile);
    }
    public void removeModFile(IModFile modFile) {
        modFiles.remove((ModFile) modFile);
    }
}
