package me.melontini.forgerunner.forge.entrypoints;

import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.forge.mod.Mods;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Log4j2
public class ContextLoader {

    private static final ThreadLocal<ModContainer> LOADING_MOD = ThreadLocal.withInitial(() -> null);

    public static void onPreLaunch() {
        Mods.consumeForgeMods(modContainer -> Mods.WRAPPERS.computeIfAbsent(modContainer, id -> new FMLModContainer(modContainer, () -> Mods.MOD_OBJECTS.get(modContainer))));
        Mods.consumeForgeMods(mod -> loadingMod(() -> FMLJavaModLoadingContext.get().getModEventBus(), mod));
    }

    //Does main really work as @Mod init? Some mods do load Minecraft's classes by that point, soooo...
    public static void onMain() {
        runWithContext("forgerunner:bus", BusSubscriber.class, (busSubscriber, mod) -> busSubscriber.onEventBus());
        runWithContext("forgerunner:main", ModInitializer.class, (modInitializer, mod) -> {
            Mods.MOD_OBJECTS.put((ModContainerImpl) mod, modInitializer);//Cheeky, but should work IG
            modInitializer.onInitialize();
        });

        Mods.consumeForgeMods(mod -> loadingMod(() -> FMLJavaModLoadingContext.get().getModEventBus().post(new FMLCommonSetupEvent(Mods.getFromDelegate(mod))), mod));
    }

    //This is not how Forge handles this, but I'll see if I have to reimplement the dumb ParallelDispatchEvent and ModStateProvider
    public static void onClient() {
        Mods.consumeForgeMods(mod -> loadingMod(() -> FMLJavaModLoadingContext.get().getModEventBus().post(new FMLClientSetupEvent(Mods.getFromDelegate(mod))), mod));
        fireComms();
    }

    public static void onServer() {
        Mods.consumeForgeMods(mod -> loadingMod(() -> FMLJavaModLoadingContext.get().getModEventBus().post(new FMLDedicatedServerSetupEvent(Mods.getFromDelegate(mod))), mod));
        fireComms();
    }

    private static void fireComms() {
        Mods.consumeForgeMods(mod -> loadingMod(() -> FMLJavaModLoadingContext.get().getModEventBus().post(new InterModEnqueueEvent(Mods.getFromDelegate(mod))), mod));
        Mods.consumeForgeMods(mod -> loadingMod(() -> FMLJavaModLoadingContext.get().getModEventBus().post(new InterModProcessEvent(Mods.getFromDelegate(mod))), mod));
    }

    public static <T> void runWithContext(String name, Class<T> type, BiConsumer<T, ModContainer> consumer) {
        Collection<EntrypointContainer<T>> entrypoints = FabricLoader.getInstance().getEntrypointContainers(name, type);

        for (EntrypointContainer<T> container : entrypoints) {
            try {
                loadingMod(() -> consumer.accept(container.getEntrypoint(), container.getProvider()), container.getProvider());
            } catch (Throwable t) {
                throw new RuntimeException(String.format("Could not execute entrypoint stage '%s' due to errors, provided by '%s'!",
                        name, container.getProvider().getMetadata().getId()), t);
            }
        }
    }

    private static <T> T loadingMod(Supplier<T> supplier, ModContainer mod) {
        try {
            LOADING_MOD.set(mod);
            ModLoadingContext.get().setActiveContainer(Mods.getFromDelegate((ModContainerImpl) mod));
            return supplier.get();
        } finally {
            LOADING_MOD.remove();
        }
    }

    private static void loadingMod(Runnable runnable, ModContainer mod) {
        try {
            LOADING_MOD.set(mod);
            runnable.run();
        } finally {
            LOADING_MOD.remove();
        }
    }
}
