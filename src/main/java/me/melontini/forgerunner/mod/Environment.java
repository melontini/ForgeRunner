package me.melontini.forgerunner.mod;

import com.google.gson.Gson;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.api.adapt.IModClass;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.util.Exceptions;
import org.objectweb.asm.commons.Remapper;

import java.nio.file.FileSystem;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record Environment(Map<String, ModClass> classPool, Gson gson,
                          Set<ModFile> modFiles, Remapper frr, List<Adapter> adapters) implements IEnvironment {

    public void addClass(IModClass modClass) {
        classPool.put(modClass.name(), (ModClass) modClass);
    }

    public void appendModFile(IModFile mod, FileSystem fs) {
        modFiles.add((ModFile) mod);
        adapters.forEach(adapter -> Exceptions.uncheck(() -> adapter.onPrepare(mod, this, fs)));
    }
    public void removeModFile(IModFile modFile) {
        modFiles.remove((ModFile) modFile);
    }
}
