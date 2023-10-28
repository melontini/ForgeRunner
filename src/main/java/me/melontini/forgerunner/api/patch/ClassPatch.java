package me.melontini.forgerunner.api.patch;

import me.melontini.forgerunner.api.adapt.IModFile;
import org.objectweb.asm.tree.ClassNode;

public interface ClassPatch {
    void patch(ClassNode node, IModFile modFile);
}
