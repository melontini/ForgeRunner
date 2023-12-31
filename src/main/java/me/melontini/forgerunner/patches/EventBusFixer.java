package me.melontini.forgerunner.patches;

import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.api.patch.ClassPatch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;

@Log4j2
public class EventBusFixer implements ClassPatch {

    private static final Type BUS_SUBSCRIBER = Type.getObjectType("me/melontini/forgerunner/forge/entrypoints/BusSubscriber");
    private static final Type SUBSCRIBE_EVENT = Type.getObjectType("net/minecraftforge/eventbus/api/SubscribeEvent");
    private static final String EVENT_SUBSCRIBER = "Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;";

    @Override
    public Result patch(ClassNode node, IModFile modFile) {
        if (node.visibleAnnotations != null) {
            if (node.visibleAnnotations.stream().anyMatch(annotation -> EVENT_SUBSCRIBER.equals(annotation.desc))) {
                modFile.modJson().addCustomEntrypoint("bus", node.name.replace('/', '.'));
                node.access = (node.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;

                if (node.methods != null) for (MethodNode methodNode : node.methods) {
                    if (methodNode.visibleAnnotations != null && methodNode.visibleAnnotations.stream().anyMatch(annotation -> SUBSCRIBE_EVENT.getDescriptor().equals(annotation.desc))) {
                        methodNode.access = (methodNode.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                    }

                    if ("<init>".equals(methodNode.name)) {
                        methodNode.access = (methodNode.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                    }
                }

                if (node.interfaces == null) node.interfaces = new ArrayList<>();
                node.interfaces.add(BUS_SUBSCRIBER.getInternalName());
            }
        }
        return Result.DEFAULT;
    }
}
