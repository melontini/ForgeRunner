package me.melontini.forgerunner.loader.remapping;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.util.Loader;
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
public class SrgRemapper {

    private static NamespaceData data;
    private static final Map<Tuple, String> fieldOwnersCache = new HashMap<>();
    private static final Map<Tuple, String> methodOwnersCache = new HashMap<>();

    @SneakyThrows
    public static void load() {
        TinyTree tree = TinyMappingFactory.loadWithDetection(Files.newBufferedReader(Loader.HIDDEN_FOLDER.resolve("mappings.tiny")));

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
        SrgRemapper.data = data;

        for (Map.Entry<EntryTriple, String> entry : data.fieldNames.entrySet()) {
            fieldOwnersCache.put(new Tuple(entry.getKey().name, entry.getKey().desc), entry.getKey().owner.replace(".", "/"));
        }
        for (Map.Entry<EntryTriple, String> entry : data.methodNames.entrySet()) {
            methodOwnersCache.put(new Tuple(entry.getKey().name, entry.getKey().desc), entry.getKey().owner.replace(".", "/"));
        }
    }

    public static String getFieldOwner(String name, String desc) {
        return fieldOwnersCache.get(new Tuple(name, desc));
    }

    public static String getMethodOwner(String name, String desc) {
        return methodOwnersCache.get(new Tuple(name, desc));
    }

    public static String mapClassName(String className) {
        return data.classNames.getOrDefault(className, className);
    }

    public static String unmapClassName(String className) {
        return data.classNamesInverse.getOrDefault(className, className);
    }

    public static String mapFieldName(String owner, String name, String descriptor) {
        return data.fieldNames.getOrDefault(new EntryTriple(owner, name, descriptor), name);
    }

    public static String mapMethodName(String owner, String name, String descriptor) {
        return data.methodNames.getOrDefault(new EntryTriple(owner, name, descriptor), name);
    }

    private static String mapClassName(Map<String, String> classNameMap, String s) {
        return classNameMap.computeIfAbsent(s, s1 -> s1);
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
