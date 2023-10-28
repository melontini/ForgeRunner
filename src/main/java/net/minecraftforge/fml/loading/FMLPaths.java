package net.minecraftforge.fml.loading;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public enum FMLPaths {
    GAMEDIR(),
    MODSDIR("mods"),
    CONFIGDIR("config"),
    FMLCONFIG(false, CONFIGDIR, "fml.toml");

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path relativePath;
    private final boolean isDirectory;
    private Path absolutePath;

    FMLPaths() {
        this("");
    }

    FMLPaths(String... path) {
        relativePath = computePath(path);
        this.isDirectory = true;
    }

    FMLPaths(boolean isDir, FMLPaths parent, String... path) {
        this.relativePath = parent.relativePath.resolve(computePath(path));
        this.isDirectory = isDir;
    }

    private Path computePath(String... path) {
        return Paths.get(path[0], Arrays.copyOfRange(path, 1, path.length));
    }

    public static void loadAbsolutePaths() {
        for (FMLPaths path : FMLPaths.values()) {
            path.absolutePath = FabricLoader.getInstance().getGameDir().resolve(path.relativePath).toAbsolutePath().normalize();
            if (path.isDirectory && !Files.isDirectory(path.absolutePath)) {
                try {
                    Files.createDirectories(path.absolutePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static Path getOrCreateGameRelativePath(Path path) {
        Path gameFolderPath = FMLPaths.GAMEDIR.get().resolve(path);

        if (!Files.isDirectory(gameFolderPath)) {
            try {
                Files.createDirectories(gameFolderPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return gameFolderPath;
    }

    public Path relative() {
        return relativePath;
    }

    public Path get() {
        return absolutePath;
    }

    static {
        loadAbsolutePaths();
    }
}
