package me.melontini.forgerunner.loader.remapping;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.srgutils.IMappingFile;

import java.nio.file.Files;

@Log4j2
public class SrgMap {

    private static IMappingFile MAPPING_FILE;

    @SneakyThrows
    public static void load() {
        FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", ""); //init mappings resolver for intermediary

        MAPPING_FILE = IMappingFile.load(Files.newInputStream(Loader.HIDDEN_FOLDER.resolve("mappings.tiny")));
    }

    public static IMappingFile getMappingFile() {
        return MAPPING_FILE;
    }
}
