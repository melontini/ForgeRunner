package me.melontini.forgerunner.patches;

import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.api.patch.ClassPatch;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;

@Log4j2
public class ModEntryPointPatch implements ClassPatch {

    private static final Type MOD_INITIALIZER = Type.getObjectType("net/fabricmc/api/ModInitializer");
    private static final String MOD = "Lnet/minecraftforge/fml/common/Mod;";

    @Override
    public void patch(ClassNode node, IModFile modFile) {
        if (node.visibleAnnotations != null) {
            if (node.visibleAnnotations.stream().anyMatch(annotation -> MOD.equals(annotation.desc))) {
                modFile.modJson().entrypoint("forgerunner:main", node.name.replace("/", "."));

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
