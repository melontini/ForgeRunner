package me.melontini.forgerunner.loader.remapping;

import org.objectweb.asm.commons.Remapper;

public class ForgeRunnerRemapper extends Remapper {

    @Override
    public String mapAnnotationAttributeName(String descriptor, String name) {
        return SrgRemapper.mapAnnotationAttributeName(descriptor, name, null);//TODO: ???
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if (!descriptor.startsWith("(")) {
            return mapFieldName(owner, name, descriptor);
        }
        return SrgRemapper.mapMethodName(owner, name, descriptor);
    }

    @Override
    public String mapInvokeDynamicMethodName(String name, String descriptor) {
        return name;
    }

    @Override
    public String mapRecordComponentName(String owner, String name, String descriptor) {
        return mapFieldName(owner, name, descriptor);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        return SrgRemapper.mapFieldName(owner, name, descriptor);
    }

    @Override
    public String mapPackageName(String name) {
        return SrgRemapper.mapPackageName(name);
    }

    @Override
    public String map(String internalName) {
        return SrgRemapper.mapClassName(internalName);
    }
}
