package me.melontini.forgerunner.loader.remapping;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.srgutils.IMappingFile;
import org.objectweb.asm.Type;

import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class SrgRemapper {

    private static NamespaceData data;
    private static IMappingFile MAPPING_FILE;

    @SneakyThrows
    public static void load() {
        FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", ""); //init mappings resolver for intermediary
        IMappingFile mappingFile = IMappingFile.load(Files.newInputStream(Loader.HIDDEN_FOLDER.resolve("mappings.tiny")));

        NamespaceData data = new NamespaceData();

        for (IMappingFile.IPackage aPackage : mappingFile.getPackages()) {
            String original = aPackage.getOriginal();
            if (original.startsWith("net/minecraft") || original.startsWith("com/mojang")) {
                data.packages.put(original, aPackage.getMapped());
            }
        }

        for (IMappingFile.IClass cls : mappingFile.getClasses()) {
            String original = cls.getOriginal();
            String mapped = cls.getMapped();

            if (original.startsWith("net/minecraft") || original.startsWith("com/mojang")) {
                data.classNames.put(original, mapped);
                data.classNamesInverse.put(mapped, original);
            }

            recordFields(cls.getFields(), data, original);
            recordMethods(cls.getMethods(), data, original);
        }
        SrgRemapper.data = data;

        MAPPING_FILE = mappingFile;
    }

    public static IMappingFile getMappingFile() {
        return MAPPING_FILE;
    }

    public static String getFieldOwner(String name, String desc) {
        return data.ownerlessFields.get(new Tuple(name, desc));
    }

    public static String getMethodOwner(String name, String desc) {
        return data.ownerlessMethods.get(new Tuple(name, desc));
    }

    public static String mapClassName(String className) {
        return data.classNames.getOrDefault(className, className);
    }

    public static String unmapClassName(String className) {
        return data.classNamesInverse.getOrDefault(className, className);
    }

    public static String mapPackageName(String packageName) {
        return data.packages.getOrDefault(packageName, packageName);
    }

    public static String mapFieldName(String owner, String name, String descriptor) {
        String mapped = data.fieldNames.get(new Triple(owner, name, descriptor));
        if (mapped != null) return mapped;
        mapped = data.ownerlessFields.get(new Tuple(name, descriptor));
        if (mapped != null) return mapped;
        IMappingFile.IClass iClass = getMappingFile().getClass(owner);
        return iClass != null ? iClass.remapField(name) : name;
    }

    public static String mapMethodName(String owner, String name, String descriptor) {
        String mapped = data.methodNames.get(new Triple(owner, name, descriptor));
        if (mapped != null) return mapped;
        return data.ownerlessMethods.getOrDefault(new Tuple(name, descriptor), name);
    }

    public static String mapAnnotationAttributeName(String annotationDesc, String name, String attributeDesc) {
        String annotationClass = Type.getType(annotationDesc).getInternalName();
        if (attributeDesc == null) {
            return data.methodsNoReturn.getOrDefault(new Triple(annotationClass, name, "()"), name);
        }
        return data.methodNames.getOrDefault(new Triple(annotationClass, name, "()" + attributeDesc), name);
    }

    private static <T extends IMappingFile.IField> void recordFields(Collection<T> fields, NamespaceData data, String fromClass) {
        for (T field : fields) {
            data.fieldNames.put(new Triple(fromClass, field.getOriginal(), field.getDescriptor()), field.getMapped());
            data.ownerlessFields.put(new Tuple(field.getOriginal(), field.getDescriptor()), field.getMapped());
        }
    }

    private static <T extends IMappingFile.IMethod> void recordMethods(Collection<T> methods, NamespaceData data, String fromClass) {
        for (T method : methods) {
            if (method.getOriginal().startsWith("m_") && method.getOriginal().endsWith("_")) {
                data.methodNames.put(new Triple(fromClass, method.getOriginal(), method.getDescriptor()), method.getMapped());
                data.ownerlessMethods.put(new Tuple(method.getOriginal(), method.getDescriptor()), method.getMapped());
                data.methodsNoReturn.put(new Triple(fromClass, method.getOriginal(), method.getDescriptor().substring(0, method.getDescriptor().indexOf(")"))), method.getMapped());
            }
        }
    }

    private static class NamespaceData {
        private final Map<String, String> classNames = new HashMap<>();
        private final Map<String, String> classNamesInverse = new HashMap<>();

        private final Map<String, String> packages = new HashMap<>();

        private final Map<Triple, String> fieldNames = new HashMap<>();
        private final Map<Triple, String> methodNames = new HashMap<>();

        private final Map<Tuple, String> ownerlessFields = new HashMap<>();
        private final Map<Tuple, String> ownerlessMethods = new HashMap<>();

        private final Map<Triple, String> methodsNoReturn = new HashMap<>();
    }

    public record Triple(String owner, String name, String desc) {
    }

    public record Tuple(String name, String desc) {
    }
}
