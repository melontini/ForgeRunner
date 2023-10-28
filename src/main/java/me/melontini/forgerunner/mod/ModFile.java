package me.melontini.forgerunner.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.melontini.forgerunner.api.ByteConvertible;
import me.melontini.forgerunner.api.adapt.IModClass;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
import org.spongepowered.asm.util.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Accessors(fluent = true)
public class ModFile implements IModFile {

    private final Map<String, ByteConvertible> files = new HashMap<>();
    private final Map<String, ModClass> classes = new HashMap<>();
    @Getter
    private final JarPath jar;
    @Getter
    private final Environment environment;
    private final Set<String> excludedEntries = new HashSet<>();
    private final boolean hasForgeMeta;

    public ModFile(JarPath jarPath, Environment environment) {
        this.jar = jarPath;
        this.files.put("fabric.mod.json", new ModJson());
        this.environment = environment;
        this.hasForgeMeta = jarPath.jarFile().getJarEntry("META-INF/mods.toml") != null;
        this.excludedEntries.add("META-INF/mods.toml");

        try {
            this.parseJarJar();
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare JarJar", e);
        }

        this.files.put("META-INF/MANIFEST.MF", new Manifest(Exceptions.uncheck(() -> jarPath.jarFile().getManifest())));

        jar.jarFile().stream().filter(jarEntry -> jarEntry.getRealName().endsWith(".class"))
                .forEach(jarEntry -> Exceptions.uncheck(() -> {
                    String name = jarEntry.getRealName().replace(".class", "");
                    byte[] bytes = jar.jarFile().getInputStream(jarEntry).readAllBytes();
                    ModClass cls = new ModClass(name, bytes, environment);

                    environment.addClass(cls);
                    this.files.put(name + ".class", cls);
                    this.classes.put(name, cls);
                }));

        String attr = manifest().manifest().getMainAttributes().getValue(Constants.ManifestAttributes.MIXINCONFIGS);
        if (attr != null) {
            String[] configs = attr.split(",");
            for (String mixin : configs) {
                JarEntry entry = jar.jarFile().getJarEntry(mixin);
                if (entry == null) continue; //To work around some mods including configs from others.
                this.files.put(mixin, new MixinConfig(Exceptions.uncheck(() -> new InputStreamReader(jar.jarFile().getInputStream(entry)))));
                this.modJson().mixinConfig(mixin);
            }
        }

        //add every other file.
        jar.jarFile().stream().filter(entry -> !this.files.containsKey(entry.getRealName()))
                .forEach(entry -> {
                    byte[] bytes = Exceptions.uncheck(() -> jar.jarFile().getInputStream(entry).readAllBytes());
                    this.files.put(entry.getRealName(), () -> bytes);
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

            ModFile file = new ModFile(new JarPath(new JarFile(temp), temp.toPath(), true), environment);
            if (!file.hasForgeMeta()) {
                JsonObject id = jarObj.get("identifier").getAsJsonObject();
                String modId = (id.get("group").getAsString().replace(".", "_") + "_" + id.get("artifact").getAsString().replace(".", "-")).toLowerCase();
                String name = id.get("artifact").getAsString();

                JsonObject version = jarObj.get("version").getAsJsonObject();
                String modVersion = version.get("artifactVersion").getAsString();

                file.modJson().id(modId);
                file.modJson().version(modVersion);
                file.modJson().accept(object1 -> object1.addProperty("name", name));
            }

            this.environment().appendModFile(file);
            this.modJson().jar(path);
            this.files.put(path, file);
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

    public Manifest manifest() {
        return (Manifest) this.files.get("META-INF/MANIFEST.MF");
    }

    public List<String> mixinConfigs() {
        return modJson().mixinConfigs();
    }

    public Collection<IModClass> classes() {
        return Collections.unmodifiableCollection(this.classes.values());
    }

    public void writeToJar(ZipOutputStream zos) throws IOException {
        for (Map.Entry<String, ByteConvertible> entry : this.files.entrySet()) {
            if (this.excludedEntries.contains(entry.getKey())) continue;

            zos.putNextEntry(new ZipEntry(entry.getKey()));
            zos.write(entry.getValue().toBytes());
            zos.closeEntry();
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
            file.deleteOnExit();
            ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file.toPath()));
            writeToJar(zos);
            zos.close();
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
