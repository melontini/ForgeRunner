package me.melontini.forgerunner.loader.remapping;

import net.minecraftforge.srgutils.IMappingFile;
import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.Map;

public class ForgeRunnerRemapper extends Remapper {

    private final Map<String, String> flatMap = new HashMap<>();
    private final IMappingFile map;

    public ForgeRunnerRemapper() {
        this.map = SrgMap.getMappingFile();
        // I'm not sure what to do with non-mapped `main` methods in SRG
        // e.g. main -> method_36106 in net/minecraft/gametest/framework/StructureUtils
        for (IMappingFile.IClass aClass : map.getClasses()) {
            for (IMappingFile.IField field : aClass.getFields()) {
                String original = field.getOriginal();
                String mapped = field.getMapped();
                if (mapped.startsWith("field_") && original.startsWith("f_"))
                    flatMap.put(original, mapped);
            }
            for (IMappingFile.IMethod method : aClass.getMethods()) {
                String original = method.getOriginal();
                String mapped = method.getMapped();
                if ((mapped.startsWith("method_") || mapped.startsWith("comp_")) &&
                        (original.startsWith("m_") || original.startsWith("f_")))
                    flatMap.put(original, mapped);
            }
        }
    }

    @Override
    public String map(String internalName) {
        return map.remapClass(internalName);
    }

    @Override
    public String mapPackageName(String name) {
        return map.remapPackage(name);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String flat = flatMap.get(name);
        if (flat != null) return flat;
        if (owner == null) return name;
        IMappingFile.IClass cls = map.getClass(owner);
        if (cls == null) return name;
        return cls.remapField(name);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String flat = flatMap.get(name);
        if (flat != null) return flat;
        if (owner == null) return name;
        IMappingFile.IClass cls = map.getClass(owner);
        if (cls == null) return name;
        return cls.remapMethod(name, descriptor);
    }

    @Override
    public String mapRecordComponentName(String owner, String name, String descriptor) {
        String flat = flatMap.get(name);
        if (flat != null) return flat;
        if (owner == null) return name;
        IMappingFile.IClass cls = map.getClass(owner);
        if (cls == null) return name;
        return cls.remapField(name);
    }
}
