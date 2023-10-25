package me.melontini.forgerunner.mod;

import lombok.Getter;
import me.melontini.forgerunner.util.Exceptions;
import me.melontini.forgerunner.util.JarPath;
import org.spongepowered.asm.util.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ModFile implements ByteConvertible {

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

        jar.jarFile().stream().filter(jarEntry -> jarEntry.getRealName().startsWith("META-INF/jarjar"))
                .forEach(jarEntry -> Exceptions.uncheck(() -> {
                    if (jarEntry.getRealName().endsWith(".jar")) {
                        File temp = File.createTempFile(jarEntry.getRealName().replace(".jar", "") + "-JarJar", ".jar");
                        temp.deleteOnExit();
                        byte[] bytes = Exceptions.uncheck(() -> jar.jarFile().getInputStream(jarEntry).readAllBytes());
                        Files.write(temp.toPath(), bytes);

                        ModFile file = new ModFile(new JarPath(new JarFile(temp), temp.toPath(), true), environment);
                        environment.appendModFile(file);
                        this.getModJson().addJar(jarEntry.getRealName());
                        this.files.put(jarEntry.getRealName(), file);
                    }
                }));

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

        String attr = getManifest().manifest().getMainAttributes().getValue(Constants.ManifestAttributes.MIXINCONFIGS);
        if (attr != null) {
            String[] configs = attr.split(",");
            for (String mixin : configs) {
                JarEntry entry = jar.jarFile().getJarEntry(mixin);
                if (entry == null) continue; //To work around some mods including configs from others.
                this.files.put(mixin, new MixinConfig(Exceptions.uncheck(() -> new InputStreamReader(jar.jarFile().getInputStream(entry)))));
                this.getModJson().addMixins(mixin);
            }
        }

        //add every other file.
        jar.jarFile().stream().filter(entry -> !this.files.containsKey(entry.getRealName()))
                .forEach(entry -> {
                    byte[] bytes = Exceptions.uncheck(() -> jar.jarFile().getInputStream(entry).readAllBytes());
                    this.files.put(entry.getRealName(), () -> bytes);
                });
    }

    public boolean hasForgeMeta() {
        return this.hasForgeMeta;
    }

    public ModJson getModJson() {
        return (ModJson) this.files.get("fabric.mod.json");
    }

    public Manifest getManifest() {
        return (Manifest) this.files.get("META-INF/MANIFEST.MF");
    }

    public List<String> getMixinConfigs() {
        return getModJson().getMixins();
    }

    public Collection<ModClass> getClasses() {
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
