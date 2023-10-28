package me.melontini.forgerunner.api.adapt;

import com.google.gson.Gson;
import org.objectweb.asm.commons.Remapper;

public interface IEnvironment {

    Gson gson();
    Remapper frr();

    void addClass(IModClass modClass);

    void appendModFile(IModFile modFile);
    void removeModFile(IModFile modFile);
}
