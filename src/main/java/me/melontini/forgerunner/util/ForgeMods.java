package me.melontini.forgerunner.util;

import me.melontini.forgerunner.loader.ModInjector;
import net.fabricmc.loader.impl.ModContainerImpl;

import java.util.List;
import java.util.function.Consumer;

public class ForgeMods {

    public static List<ModContainerImpl> getMods() {
        return ModInjector.FORGE_MODS;
    }

    public static void consumeMods(Consumer<ModContainerImpl> consumer) {
        getMods().forEach(consumer);
    }
}
