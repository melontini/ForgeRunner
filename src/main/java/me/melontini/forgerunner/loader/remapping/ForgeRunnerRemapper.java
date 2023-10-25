package me.melontini.forgerunner.loader.remapping;

import net.fabricmc.tinyremapper.api.TrRemapper;
import org.objectweb.asm.commons.Remapper;

public class ForgeRunnerRemapper extends Remapper {

    private final TrRemapper remapper;

    public ForgeRunnerRemapper(TrRemapper remapper) {
        this.remapper = remapper;
    }

    @Override
    public String mapDesc(String descriptor) {
        return remapper.mapDesc(descriptor);
    }

    @Override
    public String mapType(String internalName) {
        return remapper.mapType(internalName);
    }

    @Override
    public String[] mapTypes(String[] internalNames) {
        return remapper.mapTypes(internalNames);
    }

    @Override
    public String mapMethodDesc(String methodDescriptor) {
        return remapper.mapMethodDesc(methodDescriptor);
    }

    @Override
    public Object mapValue(Object value) {
        return remapper.mapValue(value);
    }

    @Override
    public String mapSignature(String signature, boolean typeSignature) {
        return remapper.mapSignature(signature, typeSignature);
    }

    @Override
    public String mapAnnotationAttributeName(String descriptor, String name) {
        return remapper.mapAnnotationAttributeName(descriptor, name, null);//TODO: ???
    }

    @Override
    public String mapInnerClassName(String name, String ownerName, String innerName) {
        return remapper.mapInnerClassName(name, ownerName, innerName);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String og = SrgRemapper.getMethodOwner(name, descriptor);//This is a bit naive, but alright.
        if (og != null) return SrgRemapper.mapMethodName(og, name, descriptor);
        return remapper.mapMethodName(owner, name, descriptor);
    }

    @Override
    public String mapInvokeDynamicMethodName(String name, String descriptor) {
        return remapper.mapInvokeDynamicMethodName(name, descriptor);
    }

    @Override
    public String mapRecordComponentName(String owner, String name, String descriptor) {
        return remapper.mapRecordComponentName(owner, name, descriptor);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String og = SrgRemapper.getFieldOwner(name, descriptor);
        if (og != null) return SrgRemapper.mapFieldName(og, name, descriptor);
        return remapper.mapFieldName(owner, name, descriptor);
    }

    @Override
    public String mapPackageName(String name) {
        return remapper.mapPackageName(name);
    }

    @Override
    public String mapModuleName(String name) {
        return remapper.mapModuleName(name);
    }

    @Override
    public String map(String internalName) {
        return remapper.map(internalName);
    }
}
