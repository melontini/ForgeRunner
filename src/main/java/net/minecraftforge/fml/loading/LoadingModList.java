package net.minecraftforge.fml.loading;

import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;

import java.util.HashMap;
import java.util.Map;

public class LoadingModList {

    private static LoadingModList INSTANCE = new LoadingModList();
    private static final Map<ModContainerImpl, ModFileInfo> byDelegate = new HashMap<>();

    public static LoadingModList get() {
        return INSTANCE;
    }

    public ModFileInfo getModFileById(String modid) {
        ModContainerImpl container = Loader.getModMap().get(modid);
        if (container != null) return byDelegate.computeIfAbsent(container, ModFileInfo::new);
        return null;
    }
}
