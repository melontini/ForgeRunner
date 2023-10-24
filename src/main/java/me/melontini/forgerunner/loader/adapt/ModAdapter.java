package me.melontini.forgerunner.loader.adapt;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.knot.MixinServiceKnot;
import net.fabricmc.loader.impl.launch.knot.MixinServiceKnotBootstrap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.transformers.MixinClassWriter;
import org.spongepowered.asm.util.Constants;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
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
        System.setProperty("mixin.bootstrapService", MixinServiceKnotBootstrap.class.getName());
        System.setProperty("mixin.service", MixinServiceKnot.class.getName());

        try {
            MixinBootstrap.init();//Let's pray this doesn't break something.
        } catch (Throwable t) {
            log.error("FAILED TO BOOTSTRAP MIXIN SERVICE EARLY!!!!", t);
        }

        Gson gson = new Gson();
        for (JarPath jar : jars) {
            Path file = REMAPPED_MODS.resolve(jar.path().getFileName());
            if (Files.exists(file)) return;
            log.debug("Adapting {}", jar.path().getFileName());
            ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file));

            ModAdapter remapper = new ModAdapter(jar, zos); //TODO: Adapt ATs
            remapper.excludeJarJar();
            remapper.adaptModMetadata(gson);
            remapper.copyManifest();
            remapper.remapMixinConfigs();
            remapper.copyNonClasses();
            remapper.transformClasses();

            zos.close();
            jar.jarFile().close();
        }
    }

    private void transformClasses() {
        jar.jarFile().stream().filter(jarEntry -> jarEntry.getRealName().endsWith(".class") && !excludedEntries.contains(jarEntry.getRealName()))
                .forEach(jarEntry -> Exceptions.uncheck(() -> {
                    byte[] bytes = jar.jarFile().getInputStream(jarEntry).readAllBytes();
                    ClassNode node = new ClassNode();
                    ClassReader reader = new ClassReader(bytes);
                    reader.accept(node, 0);

                    ClassAdapter.adapt(node);

                    MixinClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    node.accept(writer);
                    zos.putNextEntry(new ZipEntry(jarEntry.getRealName()));
                    zos.write(writer.toByteArray());
                    zos.closeEntry();
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
        JsonObject forgeMeta = gson.fromJson(JsonFormat.fancyInstance().createWriter().writeToString(cc), JsonObject.class);
        JsonObject fabricMeta = new JsonObject();

        MetadataAdapter.adapt(fabricMeta, forgeMeta);
        String attr = jar.jarFile().getManifest().getMainAttributes().getValue(Constants.ManifestAttributes.MIXINCONFIGS);
        if (attr != null) {
            String[] config = attr.split(",");
            JsonArray mixins = new JsonArray();
            for (String mixin : config) {
                if (jar.jarFile().getJarEntry(mixin) == null)
                    continue; //To work around some mods including configs from others.
                mixins.add(mixin);
                this.mixinConfigs.add(mixin);
            }
            fabricMeta.add("mixins", mixins);
        }

        log.info(fabricMeta.toString());
        zos.putNextEntry(new JarEntry("fabric.mod.json"));
        zos.write(fabricMeta.toString().getBytes());
        zos.closeEntry();
    }

    private void excludeJarJar() {
        jar.jarFile().stream().filter(jarEntry -> jarEntry.getRealName().startsWith("META-INF/jarjar"))
                .forEach(jarEntry -> this.excludedEntries.add(jarEntry.getRealName()));
    }
}
