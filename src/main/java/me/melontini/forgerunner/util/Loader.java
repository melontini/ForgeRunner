package me.melontini.forgerunner.util;

import lombok.SneakyThrows;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.ModCandidate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Loader {

    private static final Supplier<List<ModContainerImpl>> MODS = Exceptions.uncheck(() -> {
        Field modsField = FabricLoaderImpl.class.getDeclaredField("mods");
        modsField.setAccessible(true);
        return () -> (List<ModContainerImpl>) Exceptions.uncheck(() -> modsField.get(Loader.getInstance()));
    });
    private static final Supplier<Map<String, ModContainerImpl>> MOD_MAP = Exceptions.uncheck(() -> {
        Field modMapField = FabricLoaderImpl.class.getDeclaredField("modMap");
        modMapField.setAccessible(true);
        return () -> (Map<String, ModContainerImpl>) Exceptions.uncheck(() -> modMapField.get(Loader.getInstance()));
    });
    private static final Consumer<List<ModContainerImpl>> MODS_SETTER = Exceptions.uncheck(() -> {
        Field modsField = FabricLoaderImpl.class.getDeclaredField("mods");
        modsField.setAccessible(true);
        return mods -> Exceptions.uncheck(() -> modsField.set(Loader.getInstance(), mods));
    });
    private static final Consumer<List<ModCandidate>> MOD_DUMPER = Exceptions.uncheck(() -> {
        Method m = FabricLoaderImpl.class.getDeclaredMethod("dumpModList", List.class);
        m.setAccessible(true);
        return mods -> Exceptions.uncheck(() -> m.invoke(Loader.getInstance(), mods));
    });

    public static void appendMods(List<ModContainerImpl> mods) {
        List<ModContainerImpl> mergedMods = new ArrayList<>(getMods());
        mergedMods.addAll(mods);
        MODS_SETTER.accept(mergedMods);
    }

    public static FabricLoaderImpl getInstance() {
        return FabricLoaderImpl.INSTANCE;
    }

    public static List<ModContainerImpl> getMods() {
        return MODS.get();
    }

    public static Map<String, ModContainerImpl> getModMap() {
        return MOD_MAP.get();
    }

    @SneakyThrows
    public static void dumpModsList(List<ModCandidate> candidates) {
        MOD_DUMPER.accept(candidates);
    }
}
