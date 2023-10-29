package me.melontini.forgerunner.loader.adapt;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.loader.MixinHacks;
import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;
import me.melontini.forgerunner.mod.Environment;
import me.melontini.forgerunner.mod.ModFile;
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.gui.FabricGuiEntry;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

@Log4j2
public class ModAdapter {

    public static final ExecutorService SERVICE = Executors.newFixedThreadPool(Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 2), 8));

    @SneakyThrows
    public static void start(Set<JarPath> jars) {
        jars.removeIf(jar -> Files.exists(Loader.REMAPPED_MODS.resolve(jar.path().getFileName())));
        if (jars.isEmpty()) return;

        final Environment env = new Environment(new ConcurrentHashMap<>(), new Gson(), Collections.synchronizedSet(new LinkedHashSet<>()), new ForgeRunnerRemapper());

        final AtomicInteger processed = new AtomicInteger();
        final AtomicInteger lastPercent = new AtomicInteger();
        final AtomicInteger percent = new AtomicInteger();
        final Consumer<String> stage = s -> {
            lastPercent.set(percent.get());
            percent.set(Math.min(processed.incrementAndGet() * 100 / jars.size(), 100));
            if (percent.get() != lastPercent.get() && percent.get() % 5 == 0)
                log.info(s + " modfiles... %d%%".formatted(percent.get()));
        };

        log.info("Preparing modfiles... 0%");
        async(() -> stage.accept("Preparing"), jars, jar -> env.appendModFile((new ModFile(jar, env))));

        MixinHacks.bootstrap();
        IClassBytecodeProvider current = MixinService.getService().getBytecodeProvider();
        IMixinService currentService = MixinService.getService();
        MixinHacks.crackMixinBytecodeProvider(current, currentService, env);

        final List<Adapter> adapters = ServiceLoader.load(Adapter.class).stream().map(ServiceLoader.Provider::get).sorted((o1, o2) -> Comparator.<Long>naturalOrder().compare(o1.priority(), o2.priority())).toList();

        log.info("Adapting modfiles... 0%");
        processed.set(0);
        async(() -> stage.accept("Adapting"), env.modFiles(), mod -> {
            for (Adapter adapter : adapters) {
                adapter.adapt(mod, env);
            }
        });

        log.info("Writing modfiles... 0%");
        processed.set(0);
        async(() -> stage.accept("Writing"), env.modFiles(), mod -> {
            Path file = mod.jar().temp() ? null : Loader.REMAPPED_MODS.resolve(mod.jar().path().getFileName());

            if (file != null) {
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
                    mod.writeToJar(zos);
                } finally {
                    mod.jar().jarFile().close();
                }
            }
        });
        log.info("Done!");

        SERVICE.shutdown();
        MixinHacks.uncrackMixinService(currentService, env);
    }

    private static <T> void async(Runnable progress, Collection<T> collection, Exceptions.ThrowingConsumer<T> consumer) {
        record TaskFuture<T>(T object, CompletableFuture<?> future) { }

        List<TaskFuture<T>> futures = new ArrayList<>();
        AtomicBoolean caught = new AtomicBoolean(false);
        for (T t : collection) {
            futures.add(new TaskFuture<>(t, CompletableFuture.runAsync(() -> {
                try {
                    if (caught.get()) return;
                    consumer.accept(t);
                } catch (Throwable t1) {
                    caught.set(true);
                    Exceptions.uncheck(() -> {
                        throw t1;
                    });
                }
            }, SERVICE).thenRun(() -> {
                if (caught.get()) return;
                progress.run();
            })));
        }
        futures.forEach(future -> {
            try {
                future.future().get();
            } catch (Throwable t) {
                log.error("Failed to process " + future.object, t);
                FabricGuiEntry.displayError("Failed to process " + future.object, t, true);
            }
        });
    }
}
