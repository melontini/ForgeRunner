package me.melontini.forgerunner.patches;

import net.fabricmc.api.ModInitializer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ModEntryPointPatch implements Supplier<Patch> {

    private static final Type MOD_INITIALIZER = Type.getType(ModInitializer.class);

    public Patch get() {
        return (node, adapter) -> {
            if (node.visibleAnnotations != null) {
                if (node.visibleAnnotations.stream().anyMatch(annotation -> annotation.desc.equals("Lnet/minecraftforge/fml/common/Mod;"))) {
                    adapter.addEntrypointClass(node.name);

                    if (node.interfaces == null) node.interfaces = new ArrayList<>();
                    node.interfaces.add(MOD_INITIALIZER.getInternalName());

                    MethodNode method = node.methods.stream().filter(m -> "onInitialize".equals(m.name) && m.desc.equals("()V")).findFirst().orElse(null);
                    if (method != null) return true;

                    MethodVisitor visitor = node.visitMethod(Opcodes.ACC_PUBLIC, "onInitialize", "()V", null, null);
                    visitor.visitCode();
                    visitor.visitInsn(Opcodes.RETURN);
                    visitor.visitMaxs(1, 1);
                    visitor.visitEnd();
                    return true;
                }
            }
            return false;
        };
    }
}
