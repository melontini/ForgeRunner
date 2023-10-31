package me.melontini.forgerunner.loader.adapt;

import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.api.adapt.IModClass;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.api.patch.ClassPatch;
import me.melontini.forgerunner.mod.ModClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
        for (IModClass aClass : mod.classes().values()) {
            aClass.accept((s, bytes) -> {
                ClassReader reader = new ClassReader(bytes);
                ClassNode node = new ClassNode();
                reader.accept(new ClassRemapper(node, env.frr()), 0);

                int flags = 0;
                for (ClassPatch patch : PATCHES) {
                    ClassPatch.Result r = patch.patch(node, mod);
                    if (r.computeFrames()) flags |= ClassWriter.COMPUTE_FRAMES;
                    if (r.computeMaxs()) flags |= ClassWriter.COMPUTE_MAXS;
                }

                MixinClassWriter writer = new MixinClassWriter(flags);
                node.accept(writer);
                return writer.toByteArray();
            });
        }
    }

    @Override
    public long priority() {
        return 60;
    }

    @Override
    public void onPrepare(IModFile mod, IEnvironment env, FileSystem fs) throws IOException {
        Files.walkFileTree(fs.getRootDirectories().iterator().next(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String s = file.toString().substring(1);
                if (s.endsWith(".class")) {
                    String name = file.toString().substring(0, s.length() - 6);
                    byte[] bytes = Files.readAllBytes(file);
                    ModClass cls = new ModClass(name, bytes);

                    env.addClass(cls);
                    mod.putFile(s, cls);
                    mod.classes().put(name, cls);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
