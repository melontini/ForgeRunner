package me.melontini.forgerunner.patches;

import me.melontini.forgerunner.mod.ModFile;
import org.objectweb.asm.tree.ClassNode;

public interface ClassPatch {
    void patch(ClassNode node, ModFile modFile);
}
