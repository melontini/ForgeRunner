package net.minecraftforge.fml.loading;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.api.distmarker.Dist;

public class FMLEnvironment {
    public static final Dist dist = getDist();
    public static final String naming = "srg";
    public static final boolean production = !FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final boolean secureJarsEnabled = false;

    private static Dist getDist() {
        EnvType envType = FabricLoader.getInstance().getEnvironmentType();
        if (envType == EnvType.SERVER) return Dist.DEDICATED_SERVER;
        return Dist.CLIENT;
    }
}
