package me.melontini.forgerunner.patches;

import me.melontini.forgerunner.loader.adapt.ModAdapter;
import org.objectweb.asm.tree.ClassNode;

public interface Patch {
    boolean patch(ClassNode node, ModAdapter adapter);
}
