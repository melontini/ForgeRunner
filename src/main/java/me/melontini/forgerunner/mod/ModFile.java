package me.melontini.forgerunner.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.api.ByteConvertible;
import me.melontini.forgerunner.api.adapt.IModClass;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
import net.fabricmc.loader.impl.util.FileSystemUtil;
import org.spongepowered.asm.util.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Accessors(fluent = true)
@Log4j2
public class ModFile implements IModFile {

    private final Map<String, ByteConvertible> files = new HashMap<>();
    private final Map<String, IModClass> classes = new HashMap<>();
    @Getter
    private final JarPath jar;
    @Getter
    private final Environment environment;
    private final boolean hasForgeMeta;

    @SneakyThrows
    public ModFile(JarPath jarPath, FileSystem fs, Environment environment) {
        this.jar = jarPath;
        this.files.put("fabric.mod.json", new ModJson());
        this.environment = environment;
        this.hasForgeMeta = Files.exists(fs.getPath("META-INF/mods.toml"));

        try {
            this.parseJarJar();
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare JarJar", e);
        }

        this.files.put("META-INF/MANIFEST.MF", new Manifest(Exceptions.uncheck(() -> jarPath.jarFile().getManifest())));

        String attr = manifest().manifest().getMainAttributes().getValue(Constants.ManifestAttributes.MIXINCONFIGS);
        if (attr != null) {
            String[] configs = attr.split(",");
            for (String mixin : configs) {
                Path path = fs.getPath(mixin);
                if (mixin.isBlank() || !Files.exists(path)) continue;
                this.files.put(mixin, new MixinConfig(Exceptions.uncheck(() -> Files.newBufferedReader(path))));
                this.modJson().mixinConfig(mixin);
            }
        }

        Files.walkFileTree(fs.getRootDirectories().iterator().next(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String s = file.toString().substring(1);
                if (s.endsWith(".RSA") || s.endsWith(".SF")) {
                    log.debug("Removing signature file {}!", file.toString());
                    Files.deleteIfExists(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void parseJarJar() throws IOException {
        JarEntry jarJarMeta = jar.jarFile().getJarEntry("META-INF/jarjar/metadata.json");
        if (jarJarMeta == null) return;

        Reader reader = new InputStreamReader(jar.jarFile().getInputStream(jarJarMeta));
        JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
        if (!object.has("jars")) return;
        JsonArray array = object.get("jars").getAsJsonArray();

        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject jarObj = element.getAsJsonObject();
            String path = jarObj.get("path").getAsString();

            File temp = File.createTempFile(path.replace(".jar", "") + "-JarJar", ".jar");
            temp.deleteOnExit();
            byte[] bytes = Exceptions.uncheck(() -> jar.jarFile().getInputStream(jar.jarFile().getJarEntry(path)).readAllBytes());
            Files.write(temp.toPath(), bytes);

            try (FileSystem fs = FileSystemUtil.getJarFileSystem(temp.toPath(), true).get()) {
                ModFile file = new ModFile(new JarPath(new JarFile(temp), temp.toPath(), true), fs, environment);
                if (!file.hasForgeMeta()) {
                    JsonObject id = jarObj.get("identifier").getAsJsonObject();
                    String modId = (id.get("group").getAsString().replace('.', '_') + '_' + id.get("artifact").getAsString().replace('.', '-')).toLowerCase();
                    String name = id.get("artifact").getAsString();

                    JsonObject version = jarObj.get("version").getAsJsonObject();
                    String modVersion = version.get("artifactVersion").getAsString();

                    file.modJson().id(modId);
                    file.modJson().version(modVersion);
                    file.modJson().accept(object1 -> object1.addProperty("name", name));
                }

                this.environment().appendModFile(file, fs);
                this.modJson().jar(path);
                this.files.put(path, file);
            }
        }
    }

    public boolean hasForgeMeta() {
        return this.hasForgeMeta;
    }

    public ModJson modJson() {
        return (ModJson) this.files.get("fabric.mod.json");
    }

    public String id() {
        return modJson().id();
    }

    public String version() {
        return modJson().version();
    }

    public Path path() {
        return jar().path();
    }

    public Manifest manifest() {
        return (Manifest) this.files.get("META-INF/MANIFEST.MF");
    }

    public List<String> mixinConfigs() {
        return modJson().mixinConfigs();
    }

    @Override
    public Map<String, IModClass> classes() {
        return classes;
    }

    public void writeToJar(FileSystem fs) throws IOException {
        for (Map.Entry<String, ByteConvertible> entry : this.files.entrySet()) {
            Path path = fs.getPath(entry.getKey());
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            Files.write(path, entry.getValue().toBytes());
        }
    }

    public ByteConvertible getFile(String name) {
        return this.files.get(name);
    }

    public void putFile(String name, ByteConvertible file) {
        this.files.put(name, file);
    }

    public void removeFile(String name) {
        this.files.remove(name);
    }

    public boolean hasFile(String name) {
        return this.files.containsKey(name);
    }

    @Override
    public byte[] toBytes() {
        try {
            File file = File.createTempFile(jar.path().getFileName().toString().replace(".jar", ""), ".jar");
            Files.deleteIfExists(file.toPath());
            try (FileSystem fs = FileSystemUtil.getJarFileSystem(file.toPath(), true).get()) {
                writeToJar(fs);
            }
            file.deleteOnExit();
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return jar.path().getFileName().toString() + (modJson().has("id") ? " (" + modJson().id() + ")" : "");
    }
}
