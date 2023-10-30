package me.melontini.forgerunner.api.patch;

import me.melontini.forgerunner.api.adapt.IModFile;
import org.objectweb.asm.tree.ClassNode;

public interface ClassPatch {
    Result patch(ClassNode node, IModFile modFile);

    record Result(boolean computeMaxs, boolean computeFrames) {
        public static final Result DEFAULT = new Result(false, false);
        public static final Result COMPUTE_MAXS = new Result(true, false);
        public static final Result COMPUTE_FRAMES = new Result(false, true);
        public static final Result COMPUTE_MAXS_AND_FRAMES = new Result(true, true);
    }
}
