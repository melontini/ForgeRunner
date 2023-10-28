package me.melontini.forgerunner.forge.mod;

import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Mods {

    private static final Set<ModContainerImpl> FORGE_MODS = new LinkedHashSet<>();
    public static final Map<ModContainerImpl, ModContainer> WRAPPERS = new HashMap<>();
    public static final Map<ModContainerImpl, Object> MOD_OBJECTS = new HashMap<>();

    public static void addForgeMod(ModContainerImpl mod) {
        FORGE_MODS.add(mod);
    }

    public static Set<ModContainerImpl> getForgeMods() {
        return FORGE_MODS;
    }

    public static void consumeForgeMods(Consumer<ModContainerImpl> consumer) {
        getForgeMods().forEach(consumer);
    }

    public static ModContainer getFromDelegate(ModContainerImpl mod) {
        return WRAPPERS.computeIfAbsent(mod, modContainer -> {
            if (modContainer.getMetadata().containsCustomValue("forgerunner:forge_mod") && modContainer.getMetadata().getCustomValue("forgerunner:forge_mod").getAsBoolean()) {
                return new FMLModContainer(modContainer, () -> MOD_OBJECTS.get(modContainer));
            }
            return new FabricModContainer(modContainer);
        });
    }
}
