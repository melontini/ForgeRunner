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
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ModAdapter {

    public static final Path REMAPPED_MODS = FabricLoader.getInstance().getGameDir().resolve(".remapped_forge_mods");

    static {
        Exceptions.uncheck(() -> Files.createDirectories(REMAPPED_MODS));
    }

    private final JarPath jar;
    private final ZipOutputStream zos;
    private final Set<String> excludedEntries = new HashSet<>();
    private final Set<String> mixinConfigs = new HashSet<>();
    private final Set<String> mainEntrypoint = new HashSet<>();

    private ModAdapter(JarPath jar, ZipOutputStream zos) {
        this.jar = jar;
        this.zos = zos;

        this.excludedEntries.add("fabric.mod.json");
        this.excludedEntries.add("META-INF/mods.toml");
        this.excludedEntries.add("META-INF/MANIFEST.MF");
        this.excludedEntries.add("META-INF/accesstransformer.cfg");
    }

    @SneakyThrows
    public static void start(List<JarPath> jars) {
        jars = jars.stream().filter(jar -> !Files.exists(REMAPPED_MODS.resolve(jar.path().getFileName()))).toList();
        if (jars.isEmpty()) return;

        MixinHacks.bootstrap();

        Gson gson = new Gson();
        Map<String, byte[]> classMap = new HashMap<>();
        for (JarPath jar : jars) {
            jar.jarFile().stream().filter(jarEntry -> jarEntry.getRealName().endsWith(".class"))
                    .forEach(jarEntry -> Exceptions.uncheck(() -> classMap.put(jarEntry.getRealName().replace(".class", ""), jar.jarFile().getInputStream(jarEntry).readAllBytes())));
        }

        IClassBytecodeProvider current = MixinService.getService().getBytecodeProvider();
        IMixinService currentService = MixinService.getService();
        MixinHacks.crackMixinBytecodeProvider(current, currentService, classMap);

        IMappingProvider provider = TinyUtils.createTinyMappingProvider(Files.newBufferedReader(FabricLoader.getInstance().getModContainer("forgerunner").orElseThrow().findPath("data/forgerunner/mappings_1.20.1.tiny").orElseThrow()), "searge", "intermediary");
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(provider).renameInvalidLocals(false).build();

        Map<JarPath, Map<String, byte[]>> localClasses = new HashMap<>();
        Map<InputTag, JarPath> tags = new HashMap<>();
        for (JarPath jar : jars) {
            InputTag tag = remapper.createInputTag();
            tags.put(tag, jar);
            remapper.readInputsAsync(tag, jar.path());
        }
        tags.forEach((tag, jarPath) -> remapper.apply((s, bytes) -> localClasses.computeIfAbsent(tags.get(tag), jarPath1 -> new HashMap<>()).put(s, bytes), tag));
        remapper.finish();

        for (JarPath jar : jars) {
            log.debug("Adapting {}", jar.path().getFileName());
            Path file = REMAPPED_MODS.resolve(jar.path().getFileName());
            try {
                ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file));

                ModAdapter adapter = new ModAdapter(jar, zos); //TODO: Adapt ATs
                adapter.excludeJarJar();
                adapter.copyManifest();
                adapter.remapMixinConfigs();
                adapter.transformClasses(localClasses.get(jar));
                adapter.adaptModMetadata(gson);
                adapter.copyNonClasses();

                zos.close();
                jar.jarFile().close();
            } catch (Throwable t) {
                log.error("Failed to adapt mod " + jar.path().getFileName(), t);
                Files.deleteIfExists(file);
            }
        }

        MixinHacks.uncrackMixinService(currentService, classMap);
    }

    private void transformClasses(Map<String, byte[]> localClasses) {
        localClasses.forEach((s, bytes) -> Exceptions.uncheck(() -> {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(bytes);
            reader.accept(node, 0);

            boolean modified = ClassAdapter.adapt(node, this);

            if (modified) {
                MixinClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                node.accept(writer);
                zos.putNextEntry(new ZipEntry(node.name + ".class"));
                zos.write(writer.toByteArray());
                zos.closeEntry();
            } else {//No point in writing the class if it wasn't modified
                zos.putNextEntry(new ZipEntry(node.name + ".class"));
                zos.write(bytes);
                zos.closeEntry();
            }
        }));
    }

    private void copyNonClasses() {
        jar.jarFile().stream().filter(jarEntry -> !jarEntry.getRealName().endsWith(".class") && !excludedEntries.contains(jarEntry.getRealName()))
                .forEach(jarEntry -> Exceptions.uncheck(() -> {
                    zos.putNextEntry(new ZipEntry(jarEntry.getRealName()));
                    zos.write(jar.jarFile().getInputStream(jarEntry).readAllBytes());
                    zos.closeEntry();
                }));
    }

    @SneakyThrows
    private void remapMixinConfigs() {
        for (String mixinConfig : mixinConfigs) {
            JarEntry entry = jar.jarFile().getJarEntry(mixinConfig);
            if (entry == null) continue;
            this.excludedEntries.add(mixinConfig);

            try (InputStream is = jar.jarFile().getInputStream(entry)) {
                JsonObject object = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();

                String refmap = object.get("refmap").getAsString();
                JarEntry refmapEntry = jar.jarFile().getJarEntry(refmap);
                if (refmapEntry == null) continue;
                this.excludedEntries.add(refmap);

                try (InputStream is2 = jar.jarFile().getInputStream(refmapEntry)) {
                    JsonObject refmapObject = JsonParser.parseReader(new InputStreamReader(is2)).getAsJsonObject();
                    RefmapRemapper.remap(refmapObject);
                    zos.putNextEntry(new ZipEntry(refmap));
                    zos.write(refmapObject.toString().getBytes());
                    zos.closeEntry();
                    log.debug("Remapped refmap {}", refmap);
                }

                zos.putNextEntry(new ZipEntry(mixinConfig));
                zos.write(object.toString().getBytes());
                zos.closeEntry();
            }
        }
    }

    @SneakyThrows
    private void copyManifest() {
        zos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
        jar.jarFile().getManifest().write(zos);
        zos.closeEntry();
    }

    @SneakyThrows
    private void adaptModMetadata(Gson gson) {
        JarEntry entry = jar.jarFile().getJarEntry("META-INF/mods.toml");
        if (entry == null) return;

        CommentedConfig cc = TomlFormat.instance().createParser().parse(jar.jarFile().getInputStream(entry));
        JsonObject forgeMeta = gson.fromJson(JsonFormat.minimalInstance().createWriter().writeToString(cc), JsonObject.class);
        JsonObject fabricMeta = new JsonObject();

        MetadataAdapter.adapt(fabricMeta, forgeMeta, this);

        zos.putNextEntry(new JarEntry("fabric.mod.json"));
        zos.write(fabricMeta.toString().getBytes());
        zos.closeEntry();
    }

    private void excludeJarJar() {
        jar.jarFile().stream().filter(jarEntry -> jarEntry.getRealName().startsWith("META-INF/jarjar"))
                .forEach(jarEntry -> this.excludedEntries.add(jarEntry.getRealName()));
    }

    void addMixinConfig(String mixinConfig) {
        this.mixinConfigs.add(mixinConfig);
    }

    void addEntrypointClass(String cls) {
        this.mainEntrypoint.add(cls.replace("/", "."));
    }

    Set<String> getEntrypointClasses() {
        return Collections.unmodifiableSet(this.mainEntrypoint);
    }

    JarFile getJarFile() {
        return jar.jarFile();
    }
}
