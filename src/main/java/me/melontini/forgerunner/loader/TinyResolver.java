package me.melontini.forgerunner.loader;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.Descriptored;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
//TODO mapping i.o?
public class TinyResolver {

    private static NamespaceData data;
    private static final Map<Tuple, String> fieldOwnersCache = new HashMap<>();
    private static final Map<Tuple, String> methodOwnersCache = new HashMap<>();

    @SneakyThrows
    public static void load() {
        TinyTree tree = TinyMappingFactory.loadWithDetection(Files.newBufferedReader(FabricLoader.getInstance().getModContainer("forgerunner").orElseThrow().findPath("data/forgerunner/mappings_1.20.1.tiny").orElseThrow()));

        NamespaceData data = new NamespaceData();
        Map<String, String> classNameMap = new HashMap<>();

        for (ClassDef classEntry : tree.getClasses()) {
            String fromClass = mapClassName(classNameMap, classEntry.getName("searge"));
            String toClass = mapClassName(classNameMap, classEntry.getName("intermediary"));

            data.classNames.put(fromClass, toClass);
            data.classNamesInverse.put(toClass, fromClass);

            String mappedClassName = mapClassName(classNameMap, fromClass);

            recordMember(classEntry.getFields(), data.fieldNames, mappedClassName);
            recordMember(classEntry.getMethods(), data.methodNames, mappedClassName);
        }
        TinyResolver.data = data;
    }

    public static String getFieldOwner(String name, String desc) {
        String owner = fieldOwnersCache.get(new Tuple(name, desc));
        if (owner == null) {
            for (Map.Entry<EntryTriple, String> entry : data.fieldNames.entrySet()) {
                if (entry.getKey().desc.equals(desc) && entry.getKey().name.equals(name)) {
                    owner = entry.getKey().owner;
                    fieldOwnersCache.put(new Tuple(name, desc), owner.replace(".", "/"));
                    return owner;
                }
            }
        }
        return owner;
    }

    public static String getMethodOwner(String name, String desc) {
        String owner = methodOwnersCache.get(new Tuple(name, desc));
        if (owner == null) {
            for (Map.Entry<EntryTriple, String> entry : data.methodNames.entrySet()) {
                if (entry.getKey().desc.equals(desc) && entry.getKey().name.equals(name)) {
                    owner = entry.getKey().owner;
                    methodOwnersCache.put(new Tuple(name, desc), owner.replace(".", "/"));
                    return owner;
                }
            }
        }
        return owner;
    }

    public static String mapClassName(String className) {
        if (className.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Class names must be provided in dot format: " + className);
        }

        return data.classNames.getOrDefault(className, className);
    }

    public static String unmapClassName(String className) {
        if (className.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Class names must be provided in dot format: " + className);
        }

        return data.classNamesInverse.getOrDefault(className, className);
    }

    public static String mapFieldName(String owner, String name, String descriptor) {
        if (owner.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Class names must be provided in dot format: " + owner);
        }

        return data.fieldNames.getOrDefault(new EntryTriple(owner, name, descriptor), name);
    }

    public static String mapMethodName(String owner, String name, String descriptor) {
        if (owner.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Class names must be provided in dot format: " + owner);
        }

        return data.methodNames.getOrDefault(new EntryTriple(owner, name, descriptor), name);
    }

    private static String mapClassName(Map<String, String> classNameMap, String s) {
        return classNameMap.computeIfAbsent(s, s1 -> s1.replace('/', '.'));
    }

    private static  <T extends Descriptored> void recordMember(Collection<T> descriptoredList, Map<EntryTriple, String> putInto, String fromClass) {
        for (T descriptored : descriptoredList) {
            EntryTriple fromEntry = new EntryTriple(fromClass, descriptored.getName("searge"), descriptored.getDescriptor("searge"));
            putInto.put(fromEntry, descriptored.getName("intermediary"));
        }
    }

    private static class NamespaceData {
        private final Map<String, String> classNames = new HashMap<>();
        private final Map<String, String> classNamesInverse = new HashMap<>();
        private final Map<EntryTriple, String> fieldNames = new HashMap<>();
        private final Map<EntryTriple, String> methodNames = new HashMap<>();
    }

    public record EntryTriple(String owner, String name, String desc) {
    }

    public record Tuple(String name, String desc) {
    }
}
