package me.melontini.forgerunner.loader.adapt;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.loader.MixinHacks;
import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;
import me.melontini.forgerunner.mod.Environment;
import me.melontini.forgerunner.mod.ModFile;
import me.melontini.forgerunner.util.JarPath;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.gui.FabricGuiEntry;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipOutputStream;

@Log4j2
public class ModAdapter {

    @SneakyThrows
    public static void start(Set<JarPath> jars) {
        jars.removeIf(jar -> Files.exists(Loader.REMAPPED_MODS.resolve(jar.path().getFileName())));
        if (jars.isEmpty()) return;

        ForgeRunnerRemapper frr = new ForgeRunnerRemapper();

        Environment env = new Environment(new HashMap<>(), new Gson(), new LinkedHashSet<>(), frr);
        log.info("Preparing modfiles... 0%");
        int processed = 0;
        int lastPercent;
        int percent = 0;
        for (JarPath jar : new LinkedHashSet<>(jars)) {
            try {
                env.appendModFile((new ModFile(jar, env)));
            } catch (Throwable t) {
                log.error("Failed to prepare modfile %s".formatted(jar.path().getFileName()), t);
                FabricGuiEntry.displayError("Failed to prepare modfile %s".formatted(jar.path().getFileName()), t, true);
            }
            lastPercent = percent;
            percent = Math.min(++processed * 100 / jars.size(), 100);
            if (percent != lastPercent && percent % 10 == 0)
                log.info("Preparing modfiles... %d%%".formatted(percent));
        }

        MixinHacks.bootstrap();
        IClassBytecodeProvider current = MixinService.getService().getBytecodeProvider();
        IMixinService currentService = MixinService.getService();
        MixinHacks.crackMixinBytecodeProvider(current, currentService, env);

        List<Adapter> adapters = ServiceLoader.load(Adapter.class).stream().map(ServiceLoader.Provider::get).sorted((o1, o2) -> Comparator.<Long>naturalOrder().compare(o1.priority(), o2.priority())).toList();

        log.info("Adapting modfiles... 0%");
        processed = 0;
        for (ModFile mod : env.modFiles()) {
            log.debug("Adapting {}", mod.jar().path().getFileName());

            try {
                for (Adapter adapter : adapters) {
                    adapter.adapt(mod, env);
                }
            } catch (Throwable t) {
                log.error("Failed to adapt mod %s (%s)".formatted(mod.id(), mod.jar().path().getFileName()), t);
                FabricGuiEntry.displayError("Failed to adapt mod %s (%s)".formatted(mod.id(), mod.jar().path().getFileName()), t, true);
            }
            lastPercent = percent;
            percent = Math.min(++processed * 100 / jars.size(), 100);
            if (percent != lastPercent && percent % 10 == 0)
                log.info("Adapting modfiles... %d%%".formatted(percent));
        }

        log.info("Writing modfiles... 0%");
        processed = 0;
        for (ModFile mod : env.modFiles()) {
            Path file = mod.jar().temp() ? null : Loader.REMAPPED_MODS.resolve(mod.jar().path().getFileName());

            try {
                if (file != null) {
                    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
                        mod.writeToJar(zos);
                    }
                }
            } catch (Throwable t) {
                Files.deleteIfExists(file);
                log.error("Failed to write mod " + mod.jar().path().getFileName(), t);
                FabricGuiEntry.displayError("Failed to write mod " + mod.jar().path().getFileName(), t, true);
            } finally {
                mod.jar().jarFile().close();
            }
            lastPercent = percent;
            percent = Math.min(++processed * 100 / jars.size(), 100);
            if (percent != lastPercent && percent % 10 == 0)
                log.info("Writing modfiles... %d%%".formatted(percent));
        }
        log.info("Done!");

        Adapter.SERVICE.shutdown();
        MixinHacks.uncrackMixinService(currentService, env);
    }
}
