package me.melontini.forgerunner.loader;

import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.loader.adapt.ModAdapter;
import me.melontini.forgerunner.loader.remapping.SrgRemapper;
import me.melontini.forgerunner.util.JarPath;
import me.melontini.forgerunner.util.Loader;
import me.melontini.forgerunner.util.MappingsDownloader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.gui.FabricGuiEntry;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.knot.Knot;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

@Slf4j
public class EarlyBootstrap implements LanguageAdapter {

    static {
        //Dirty hack to not crash devenv with all the dependencies we use. (not sorry)
        Loader.getInstance().getGameProvider().unlockClassPath(FabricLauncherBase.getLauncher());
        try {
            if (FabricLauncherBase.getLauncher() instanceof Knot knot) {
                Field unlocked = Knot.class.getDeclaredField("unlocked");
                unlocked.setAccessible(true);
                unlocked.setBoolean(knot, true);
            }
        } catch (Throwable t) {
            log.error("FAILED TO UNLOCK KNOT!!!", t);
        }

        try {
            MappingsDownloader.downloadMappings();
        } catch (IOException e) {
            log.error("Failed to download mappings", e);
            FabricGuiEntry.displayError("Failed to download mappings", e, true);
        }
        SrgRemapper.load();
        List<JarPath> forgeMods = ModLocator.start();
        ModAdapter.start(forgeMods);
        ModInjector.inject();
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        return null; //Whatever
    }
}
