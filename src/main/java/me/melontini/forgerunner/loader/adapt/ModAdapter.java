package me.melontini.forgerunner.loader.adapt;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.loader.MixinHacks;
import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;
import me.melontini.forgerunner.mod.*;
import me.melontini.forgerunner.util.JarPath;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.gui.FabricGuiEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipOutputStream;

@Log4j2
public class ModAdapter {

    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 2), 8));

    @SneakyThrows
    public static void start(Set<JarPath> jars) {
        jars.removeIf(jar -> Files.exists(Loader.REMAPPED_MODS.resolve(jar.path().getFileName())));
        if (jars.isEmpty()) return;

        ForgeRunnerRemapper frr = new ForgeRunnerRemapper();

        Environment env = new Environment(new HashMap<>(), new LinkedHashSet<>(), frr);
        log.info("Preparing modfiles...");
        try {
            for (JarPath jar : new LinkedHashSet<>(jars)) {
                env.appendModFile((new ModFile(jar, env)));
            }
        } catch (Throwable t) {
            log.error("Failed to prepare modfiles", t);
            FabricGuiEntry.displayError("Failed to prepare modfiles", t, true);
        }

        MixinHacks.bootstrap();
        IClassBytecodeProvider current = MixinService.getService().getBytecodeProvider();
        IMixinService currentService = MixinService.getService();
        MixinHacks.crackMixinBytecodeProvider(current, currentService, env);

        Gson gson = new Gson();
        log.info("Adapting modfiles...");
        for (ModFile mod : env.modFiles()) {
            log.debug("Adapting {}", mod.jar().path().getFileName());

            try {
                remapMixinConfigs(mod);
                adaptModMetadata(gson, mod);
                transformClasses(mod, frr);
            } catch (Throwable t) {
                log.error("Failed to adapt mod " + mod.id(), t);
                FabricGuiEntry.displayError("Failed to adapt mod " + mod.id(), t, true);
            }
        }

        log.info("Writing modfiles...");
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
        }
        log.info("Done!");

        SERVICE.shutdown();
        MixinHacks.uncrackMixinService(currentService, env);
    }

    private static void transformClasses(ModFile modFile, ForgeRunnerRemapper frr) {
        Set<CompletableFuture<?>> futures = new HashSet<>();
        for (ModClass aClass : modFile.classes()) {
            futures.add(CompletableFuture.runAsync(() -> aClass.accept((s, bytes) -> {
                ClassReader reader = new ClassReader(bytes);
                ClassNode node = new ClassNode();
                reader.accept(new ClassRemapper(node, frr), 0);

                ClassAdapter.adapt(node, modFile);

                MixinClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                node.accept(writer);
                return writer.toByteArray();
            }), SERVICE));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private static void remapMixinConfigs(ModFile file) {
        for (String mixinConfig : file.mixinConfigs()) {
            MixinConfig config = (MixinConfig) file.getFile(mixinConfig);
            if (config == null) continue;

            String refmapFile = config.getRefMap();
            if (refmapFile == null) continue;
            ByteConvertible refmap = file.getFile(refmapFile);
            if (refmap == null) continue;

            Reader bais = new InputStreamReader(new ByteArrayInputStream(refmap.toBytes()));
            JsonObject refmapObject = JsonParser.parseReader(bais).getAsJsonObject();
            RefmapRemapper.remap(refmapObject, file.environment().frr());
            file.putFile(refmapFile, () -> refmapObject.toString().getBytes());
        }
    }

    private static void adaptModMetadata(Gson gson, ModFile file) {
        if (file.hasForgeMeta()) {
            ByteConvertible forge = file.getFile("META-INF/mods.toml");
            if (forge == null) return;

            CommentedConfig cc = TomlFormat.instance().createParser().parse(new InputStreamReader(new ByteArrayInputStream(forge.toBytes())));
            JsonObject forgeMeta = gson.fromJson(JsonFormat.minimalInstance().createWriter().writeToString(cc), JsonObject.class);

            MetadataAdapter.adapt(forgeMeta, file);
        }
    }
}
