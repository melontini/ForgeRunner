package me.melontini.forgerunner.forge.mod;

import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class Mods {

    private static final Set<ModContainerImpl> FORGE_MODS = new LinkedHashSet<>();
    private static final Map<ModContainerImpl, ModContainer> WRAPPERS = new HashMap<>();

    private static final Map<ModContainerImpl, ModFileInfo> FILE_INFO = new HashMap<>();
    private static final Map<ModFileInfo, ModInfo> MOD_INFO = new HashMap<>();

    public static final Map<ModContainerImpl, Object> MOD_OBJECTS = new HashMap<>();

    public static void addForgeMod(ModContainerImpl mod) {
        FORGE_MODS.add(mod);
        WRAPPERS.put(mod, new FMLModContainer(mod, () -> MOD_OBJECTS.get(mod)));
    }

    public static Set<ModContainerImpl> getForgeMods() {
        return FORGE_MODS;
    }

    public static void forEachForgeMod(BiConsumer<ModContainerImpl, ModContainer> consumer) {
        getForgeMods().forEach(mod -> consumer.accept(mod, getFromDelegate(mod)));
    }

    public static void forEachMod(BiConsumer<ModContainerImpl, ModContainer> consumer) {
        Loader.getMods().forEach(mod -> consumer.accept(mod, getFromDelegate(mod)));
    }

    public static ModContainer getFromDelegate(ModContainerImpl mod) {
        return WRAPPERS.computeIfAbsent(mod, modContainer -> {
            if (modContainer.getMetadata().containsCustomValue("forgerunner:forge_mod") && modContainer.getMetadata().getCustomValue("forgerunner:forge_mod").getAsBoolean()) {
                return new FMLModContainer(modContainer, () -> MOD_OBJECTS.get(modContainer));
            }
            return new FabricModContainer(modContainer);
        });
    }

    public static ModFileInfo getFileInfo(ModContainerImpl mod) {
        return FILE_INFO.computeIfAbsent(mod, ModFileInfo::new);
    }

    public static ModInfo getModInfo(ModFileInfo fileInfo) {
        return MOD_INFO.computeIfAbsent(fileInfo, ModInfo::new);
    }
}
