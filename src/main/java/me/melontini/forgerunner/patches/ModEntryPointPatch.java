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
    public Result patch(ClassNode node, IModFile modFile) {
        if (node.visibleAnnotations != null) {
            if (node.visibleAnnotations.stream().anyMatch(annotation -> MOD.equals(annotation.desc))) {
                modFile.modJson().addCustomEntrypoint("main", node.name.replace('/', '.'));
                node.access = (node.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;

                if (node.interfaces == null) node.interfaces = new ArrayList<>();
                node.interfaces.add(MOD_INITIALIZER.getInternalName());

                for (MethodNode methodNode : node.methods) {
                    if ("<init>".equals(methodNode.name)) {
                        methodNode.access = (methodNode.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                    }
                }

                MethodNode method = node.methods.stream().filter(m -> "onInitialize".equals(m.name) && m.desc.equals("()V")).findFirst().orElse(null);
                if (method != null) return Result.DEFAULT;

                MethodVisitor visitor = node.visitMethod(Opcodes.ACC_PUBLIC, "onInitialize", "()V", null, null);
                visitor.visitCode();
                visitor.visitInsn(Opcodes.RETURN);
                visitor.visitMaxs(1, 1);
                visitor.visitEnd();
                return Result.COMPUTE_MAXS;
            }
        }
        return Result.DEFAULT;
    }
}
