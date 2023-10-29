package me.melontini.forgerunner.loader.adapt;

import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.api.adapt.IModClass;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.api.patch.ClassPatch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

@Log4j2
public class ClassAdapter implements Adapter {

    private static final Set<ClassPatch> PATCHES = new HashSet<>();

    static {
        for (ClassPatch patch : ServiceLoader.load(ClassPatch.class)) {
            PATCHES.add(patch);
        }
    }

    @Override
    public void adapt(IModFile mod, IEnvironment env) {
        for (IModClass aClass : mod.classes()) {
            aClass.accept((s, bytes) -> {
                ClassReader reader = new ClassReader(bytes);
                ClassNode node = new ClassNode();
                reader.accept(new ClassRemapper(node, env.frr()), 0);

                for (ClassPatch patch : PATCHES) {
                    patch.patch(node, mod);
                }

                MixinClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                node.accept(writer);
                return writer.toByteArray();
            });
        }
    }

    @Override
    public long priority() {
        return 60;
    }
}
