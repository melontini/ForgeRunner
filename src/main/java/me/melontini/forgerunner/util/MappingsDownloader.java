package me.melontini.forgerunner.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class MappingsDownloader {

    public static void downloadMappings() throws IOException {
        Path mappingsPath = Loader.HIDDEN_FOLDER.resolve("mappings.tiny");
        if (Files.exists(mappingsPath)) return;

        String gameVersion = Loader.getInstance().getGameProvider().getRawGameVersion();
        log.info("Downloading mappings for version {}", gameVersion);
        URL url = new URL("https://raw.githubusercontent.com/melontini/srg-intermediary/main/mappings/" + gameVersion + "/mappings.tiny");

        try (InputStream is = url.openStream()) {
            Files.write(mappingsPath, is.readAllBytes());
        }
    }
}
