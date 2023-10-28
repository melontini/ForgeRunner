package me.melontini.forgerunner.api.adapt;

import com.google.gson.Gson;
import me.melontini.forgerunner.mod.ModClass;
import me.melontini.forgerunner.mod.ModFile;
import org.objectweb.asm.commons.Remapper;

public interface IEnvironment {

    Gson gson();
    Remapper frr();

    void addClass(ModClass modClass);

    void appendModFile(ModFile modFile);
    void removeModFile(ModFile modFile);
}
