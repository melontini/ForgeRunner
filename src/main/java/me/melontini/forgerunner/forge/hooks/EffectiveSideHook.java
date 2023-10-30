package me.melontini.forgerunner.forge.hooks;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraftforge.fml.LogicalSide;

import java.util.ServiceLoader;

public abstract class EffectiveSideHook {

    private static EffectiveSideHook INSTANCE = new EffectiveSideHook() {
        @Override
        public LogicalSide getSided() {
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                return MinecraftClient.getInstance().isOnThread() ? LogicalSide.CLIENT : LogicalSide.SERVER;
            }
            return LogicalSide.SERVER; //Is it a fine assumption that the server is always logical server?
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
