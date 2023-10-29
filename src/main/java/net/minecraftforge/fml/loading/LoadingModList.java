package net.minecraftforge.fml.loading;

import com.google.common.base.Suppliers;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LoadingModList {

    private static LoadingModList INSTANCE = new LoadingModList();
    private static final Map<ModContainerImpl, ModFileInfo> fileByDelegate = new HashMap<>();
    private static final Map<ModFileInfo, ModInfo> byDelegate = new HashMap<>();
    private static final Supplier<List<ModInfo>> modList = Suppliers.memoize(() -> Loader.getMods().stream().map(container -> byDelegate.computeIfAbsent(
            fileByDelegate.computeIfAbsent(container, ModFileInfo::new), ModInfo::new)).toList());

    public static LoadingModList get() {
        return INSTANCE;
    }

    public static ModFileInfo fr$getModFileByDelegate(ModContainerImpl container) {
        return fileByDelegate.computeIfAbsent(container, ModFileInfo::new);
    }

    public static ModInfo fr$getModInfo(ModFileInfo fileInfo) {
        return byDelegate.computeIfAbsent(fileInfo, ModInfo::new);
    }

    public ModFileInfo getModFileById(String modid) {
        ModContainerImpl container = Loader.getModMap().get(modid);
        if (container != null) return fileByDelegate.computeIfAbsent(container, ModFileInfo::new);
        return null;
    }

    public List<ModInfo> getMods() {
        return modList.get();
    }

    public List<EarlyLoadingException> getErrors() {
        return List.of();//If we are here, no errors were caught :)
    }
}
