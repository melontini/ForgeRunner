package me.melontini.forgerunner.loader.adapt;

import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.mod.ModFile;
import me.melontini.forgerunner.patches.Patch;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

@Log4j2
public class ClassAdapter {

    private static final Set<Patch> PATCHES = new HashSet<>();

    public static void adapt(ClassNode node, ModFile file) {
        for (Patch patch : PATCHES) {
            patch.patch(node, file);
        }
    }

    static {
        for (Patch patch : ServiceLoader.load(Patch.class)) {
            PATCHES.add(patch);
        }
    }
}
