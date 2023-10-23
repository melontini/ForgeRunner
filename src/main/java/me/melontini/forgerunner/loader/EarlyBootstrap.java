package me.melontini.forgerunner.loader;

import me.melontini.forgerunner.loader.adapt.ModAdapter;
import me.melontini.forgerunner.util.JarPath;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

import java.util.List;

public class EarlyBootstrap implements LanguageAdapter {

    static {
        //Dirty hack to not crash devenv with all the dependencies we use. (not sorry)
        FabricLoaderImpl.INSTANCE.getGameProvider().unlockClassPath(FabricLauncherBase.getLauncher());

        List<JarPath> forgeMods = ModLocator.start();
        ModAdapter.start(forgeMods);
        ModInjector.inject();
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        return null; //Whatever
    }
}
