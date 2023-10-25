package me.melontini.forgerunner.patches;

import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.mod.ModFile;
import net.fabricmc.api.ModInitializer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;

@Slf4j
public class ModEntryPointPatch implements Patch {

    private static final Type MOD_INITIALIZER = Type.getType(ModInitializer.class);

    @Override
    public void patch(ClassNode node, ModFile modFile) {
        if (node.visibleAnnotations != null) {
            if (node.visibleAnnotations.stream().anyMatch(annotation -> annotation.desc.equals("Lnet/minecraftforge/fml/common/Mod;"))) {
                modFile.getModJson().addEntrypoint("main", node.name.replace("/", "."));

                if (node.interfaces == null) node.interfaces = new ArrayList<>();
                node.interfaces.add(MOD_INITIALIZER.getInternalName());

                MethodNode method = node.methods.stream().filter(m -> "onInitialize".equals(m.name) && m.desc.equals("()V")).findFirst().orElse(null);
                if (method != null) return;

                MethodVisitor visitor = node.visitMethod(Opcodes.ACC_PUBLIC, "onInitialize", "()V", null, null);
                visitor.visitCode();
                visitor.visitInsn(Opcodes.RETURN);
                visitor.visitMaxs(1, 1);
                visitor.visitEnd();
            }
        }
    }
}
