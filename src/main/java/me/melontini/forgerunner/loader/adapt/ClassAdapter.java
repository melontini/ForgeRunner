package me.melontini.forgerunner.loader.adapt;

import me.melontini.forgerunner.patches.Patch;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ClassAdapter {

    private static final Set<Patch> PATCHES = new HashSet<>();

    public static void addPatch(Patch patch) {
        PATCHES.add(patch);
    }

    public static boolean adapt(ClassNode node, ModAdapter adapter) {
        boolean modified = false;
        for (Patch patch : PATCHES) {
            modified |= patch.patch(node, adapter);
        }
        return modified;
    }

    static {
        FabricLoader.getInstance().invokeEntrypoints("forgerunner:patches", Supplier.class,
                supplier -> PATCHES.add(((Supplier<Patch>) supplier).get()));
    }
}
