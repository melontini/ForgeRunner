package net.minecraftforge.fml.util.thread;

import me.melontini.forgerunner.forge.hooks.EffectiveSideHook;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.fml.LogicalSide;

public class EffectiveSide {

    public static LogicalSide get() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return EffectiveSideHook.getSidedClient();
        }
        return LogicalSide.SERVER; //Is it a fine assumption that the server is always logical server?
    }
}
