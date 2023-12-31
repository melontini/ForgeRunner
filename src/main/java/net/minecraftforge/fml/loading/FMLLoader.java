package net.minecraftforge.fml.loading;

import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.api.distmarker.Dist;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FMLLoader {

    private static VersionInfo versionInfo;

    public static Dist getDist() {
        return FMLEnvironment.dist;
    }

    public static LoadingModList getLoadingModList() {
        return LoadingModList.get();
    }

    public static Path getGamePath() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static boolean isProduction() {
        return FMLEnvironment.production;
    }

    public static VersionInfo versionInfo() {
        return versionInfo;
    }

    static {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("forgeVersion", "47.1.3");//TODO
        arguments.put("forgeGroup", "net.minecraftforge");
        arguments.put("mcVersion", Loader.getInstance().getGameProvider().getNormalizedGameVersion());
        arguments.put("mcpVersion", "20230612.114412");//TODO
        versionInfo = new VersionInfo(arguments);
    }
}
