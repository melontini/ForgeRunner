package me.melontini.forgerunner.loader.adapt;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.loader.MixinHacks;
import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;
import me.melontini.forgerunner.mod.*;
import me.melontini.forgerunner.util.JarPath;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.impl.gui.FabricGuiEntry;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ModAdapter {

    @SneakyThrows
    public static void start(List<JarPath> jars) {
        jars = jars.stream().filter(jar -> !Files.exists(Loader.REMAPPED_MODS.resolve(jar.path().getFileName()))).toList();
        if (jars.isEmpty()) return;

        IMappingProvider provider = TinyUtils.createTinyMappingProvider(Files.newBufferedReader(Loader.HIDDEN_FOLDER.resolve("mappings.tiny")), "searge", "intermediary");
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(provider).renameInvalidLocals(false).build();

        ForgeRunnerRemapper frr = new ForgeRunnerRemapper(remapper.getEnvironment().getRemapper());

        List<ModFile> modFiles = new ArrayList<>();
        Environment environment = new Environment(new HashMap<>(), modFiles, frr);
        log.info("Preparing modfiles...");
        try {
            for (JarPath jar : new ArrayList<>(jars)) {
                modFiles.add(new ModFile(jar, environment));
            }
        } catch (Throwable t) {
            log.error("Failed to prepare modfiles", t);
            FabricGuiEntry.displayError("Failed to prepare modfiles", t, true);
        }

        MixinHacks.bootstrap();
        IClassBytecodeProvider current = MixinService.getService().getBytecodeProvider();
        IMixinService currentService = MixinService.getService();
        MixinHacks.crackMixinBytecodeProvider(current, currentService, environment);

        Gson gson = new Gson();
        for (ModFile modFile : modFiles) {
            log.debug("Adapting {}", modFile.getJar().path().getFileName());
            Path file = modFile.getJar().temp() ? null : Loader.REMAPPED_MODS.resolve(modFile.getJar().path().getFileName());

            try {
                remapMixinConfigs(modFile);
                adaptModMetadata(gson, modFile);
                transformClasses(modFile, frr);
                if (file != null) {
                    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
                        modFile.writeToJar(zos);
                    }
                }
            } catch (Throwable t) {
                log.error("Failed to adapt mod " + modFile.getJar().path().getFileName(), t);
                if (file != null) Files.deleteIfExists(file);
                FabricGuiEntry.displayError("Failed to adapt mod " + modFile.getJar().path().getFileName(), t, true);
            } finally {
                modFile.getJar().jarFile().close();
            }
        }
        remapper.finish();

        MixinHacks.uncrackMixinService(currentService, environment);
    }

    private static void transformClasses(ModFile modFile, ForgeRunnerRemapper frr) {
        for (ModClass aClass : modFile.getClasses()) {
            aClass.accept((s, bytes) -> {
                ClassReader reader = new ClassReader(bytes);
                ClassNode node = new ClassNode();
                reader.accept(new ClassRemapper(node, frr), 0);

                ClassAdapter.adapt(node, modFile);

                MixinClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                node.accept(writer);
                return writer.toByteArray();
            });
        }
    }

    private static void remapMixinConfigs(ModFile file) {
        for (String mixinConfig : file.getMixinConfigs()) {
            MixinConfig config = (MixinConfig) file.getFile(mixinConfig);
            if (config == null) continue;

            String refmapFile = config.getRefMap();
            if (refmapFile == null) continue;
            ByteConvertible refmap = file.getFile(refmapFile);
            if (refmap == null) continue;

            Reader bais = new InputStreamReader(new ByteArrayInputStream(refmap.toBytes()));
            JsonObject refmapObject = JsonParser.parseReader(bais).getAsJsonObject();
            RefmapRemapper.remap(refmapObject, file.getEnvironment().frr());
            file.putFile(refmapFile, () -> refmapObject.toString().getBytes());
        }
    }

    private static void adaptModMetadata(Gson gson, ModFile file) {
        if (file.hasForgeMeta()) {
            ByteConvertible forge = file.getFile("META-INF/mods.toml");
            if (forge != null) {
                CommentedConfig cc = TomlFormat.instance().createParser().parse(new InputStreamReader(new ByteArrayInputStream(forge.toBytes())));
                JsonObject forgeMeta = gson.fromJson(JsonFormat.minimalInstance().createWriter().writeToString(cc), JsonObject.class);

                MetadataAdapter.adapt(forgeMeta, file);
            }
        }
    }
}
