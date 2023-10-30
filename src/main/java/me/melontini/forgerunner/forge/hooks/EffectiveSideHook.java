package me.melontini.forgerunner.forge.hooks;

import net.minecraft.client.MinecraftClient;
import net.minecraftforge.fml.LogicalSide;

import java.util.ServiceLoader;

public abstract class EffectiveSideHook {

    private static EffectiveSideHook INSTANCE = new EffectiveSideHook() {
        @Override
        public LogicalSide getSided() {
            return MinecraftClient.getInstance().isOnThread() ? LogicalSide.CLIENT : LogicalSide.SERVER;
        }
    };

    public static LogicalSide getSidedClient() {
        return INSTANCE.getSided();
    }

    public abstract LogicalSide getSided();

    static {
        ServiceLoader.load(EffectiveSideHook.class).findFirst()
                .ifPresent(hook -> INSTANCE = hook);
    }
}
