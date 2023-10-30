package net.minecraftforge.fml.util.thread;

import me.melontini.forgerunner.forge.hooks.EffectiveSideHook;
import net.minecraftforge.fml.LogicalSide;

public class EffectiveSide {

    public static LogicalSide get() {
        return EffectiveSideHook.getSidedClient();
    }
}
