package net.minecraftforge.fml.loading;

import com.google.common.base.Suppliers;
import me.melontini.forgerunner.forge.mod.Mods;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.util.List;
import java.util.function.Supplier;

public class LoadingModList {

    private static LoadingModList INSTANCE = new LoadingModList();
    private static final Supplier<List<ModInfo>> modList = Suppliers.memoize(() -> Loader.getMods().stream().map(container -> Mods.getModInfo(Mods.getFileInfo(container))).toList());

    public static LoadingModList get() {
        return INSTANCE;
    }

    public ModFileInfo getModFileById(String modid) {
        ModContainerImpl container = Loader.getMod(modid);
        if (container != null) return Mods.getFileInfo(container);
        return null;
    }

    public List<ModInfo> getMods() {
        return modList.get();
    }

    public List<EarlyLoadingException> getErrors() {
        return List.of();//If we are here, no errors were caught :)
    }
}
