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
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
import net.fabricmc.loader.api.FabricLoader;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ModAdapter {

    public static final Path REMAPPED_MODS = FabricLoader.getInstance().getGameDir().resolve(".remapped_forge_mods");

    static {
        Exceptions.uncheck(() -> Files.createDirectories(REMAPPED_MODS));
    }

    @SneakyThrows
    public static void start(List<JarPath> jars) {
        jars = jars.stream().filter(jar -> !Files.exists(REMAPPED_MODS.resolve(jar.path().getFileName()))).toList();
        if (jars.isEmpty()) return;

        IMappingProvider provider = TinyUtils.createTinyMappingProvider(Files.newBufferedReader(FabricLoader.getInstance().getModContainer("forgerunner").orElseThrow().findPath("data/forgerunner/mappings_1.20.1.tiny").orElseThrow()), "searge", "intermediary");
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(provider).renameInvalidLocals(false).build();

        Map<String, byte[]> classMap = new HashMap<>();
        for (JarPath jar : jars) {
            jar.jarFile().stream().filter(jarEntry -> jarEntry.getRealName().endsWith(".class"))
                    .forEach(jarEntry -> Exceptions.uncheck(() -> classMap.put(jarEntry.getRealName().replace(".class", ""), jar.jarFile().getInputStream(jarEntry).readAllBytes())));
        }

        MixinHacks.bootstrap();
        IClassBytecodeProvider current = MixinService.getService().getBytecodeProvider();
        IMixinService currentService = MixinService.getService();
        MixinHacks.crackMixinBytecodeProvider(current, currentService, classMap);

        ForgeRunnerRemapper frr = new ForgeRunnerRemapper(remapper.getEnvironment().getRemapper());
        Environment environment = new Environment(new HashMap<>(), frr);
        Gson gson = new Gson();
        for (JarPath jar : jars) {
            log.debug("Adapting {}", jar.path().getFileName());
            Path file = REMAPPED_MODS.resolve(jar.path().getFileName());

            try {
                ModFile modFile = new ModFile(jar, environment);
                remapMixinConfigs(modFile);
                adaptModMetadata(gson, modFile);
                transformClasses(modFile, frr);
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
                    modFile.writeToJar(zos);
                }
            } catch (Throwable t) {
                log.error("Failed to adapt mod " + jar.path().getFileName(), t);
                Files.deleteIfExists(file);
                FabricGuiEntry.displayError("Failed to adapt mod " + jar.path().getFileName(), t, true);
            } finally {
                jar.jarFile().close();
            }
        }
        remapper.finish();

        MixinHacks.uncrackMixinService(currentService, classMap);
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

            String refmapFile = config.getRefMap();
            ByteConvertible refmap = file.getFile(refmapFile);
            if (refmap == null) continue;

            Reader bais = new InputStreamReader(new ByteArrayInputStream(refmap.toBytes()));
            JsonObject refmapObject = JsonParser.parseReader(bais).getAsJsonObject();
            RefmapRemapper.remap(refmapObject, file.getEnvironment().frr());
            file.putFile(refmapFile, () -> refmapObject.toString().getBytes());
        }
    }

    private static void adaptModMetadata(Gson gson, ModFile file) {
        ByteConvertible forge = file.getFile("META-INF/mods.toml");

        CommentedConfig cc = TomlFormat.instance().createParser().parse(new InputStreamReader(new ByteArrayInputStream(forge.toBytes())));
        JsonObject forgeMeta = gson.fromJson(JsonFormat.minimalInstance().createWriter().writeToString(cc), JsonObject.class);

        MetadataAdapter.adapt(forgeMeta, file);
    }
}
