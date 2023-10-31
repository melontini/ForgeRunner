package me.melontini.forgerunner.forge.entrypoints;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.melontini.forgerunner.forge.mod.Mods;
import me.melontini.forgerunner.forge.util.BusHelper;
import me.melontini.forgerunner.util.Exceptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Log4j2
public class ContextLoader {

    private static final ThreadLocal<ModContainer> LOADING_MOD = ThreadLocal.withInitial(() -> null);
    private static final Map<String, EntryPointData> ENTRYPOINTS = new HashMap<>();

    public static void addEntrypoint(ModContainerImpl mod, String entrypoint, String cls) {
        ENTRYPOINTS.computeIfAbsent(entrypoint, s -> new EntryPointData()).add(cls, mod);
    }

    public static void onPreLaunch() {
        Mods.consumeForgeMods(mod -> Mods.WRAPPERS.computeIfAbsent(mod, id -> new FMLModContainer(mod, () -> Mods.MOD_OBJECTS.get(mod))));
        Mods.consumeForgeMods(mod -> {
            loadingMod(() -> FMLJavaModLoadingContext.get().getModEventBus(), mod);

            if (mod.getMetadata().containsCustomValue("forgerunner:entrypoints")) {
                CustomValue.CvObject cvObject = mod.getMetadata().getCustomValue("forgerunner:entrypoints").getAsObject();
                for (Map.Entry<String, CustomValue> entry : cvObject) {
                    if (entry.getValue() instanceof CustomValue.CvArray) {
                        CustomValue.CvArray cvArray = entry.getValue().getAsArray();
                        for (CustomValue cv : cvArray) {
                            addEntrypoint(mod, entry.getKey(), cv.getAsString());
                        }
                    }
                }
            }
        });
    }

    //Does main really work as @Mod init? Some mods do load Minecraft's classes by that point, soooo...
    public static void onMain() {
        runWithContext("bus", BusSubscriber.class, (cls, mod) -> BusHelper.onEventBus(cls));
        runWithContext("main", ModInitializer.class, (cls, mod) -> {
            FMLJavaModLoadingContext.get().getModEventBus().post(new FMLConstructModEvent(Mods.getFromDelegate(mod)));
            ModInitializer instance = Exceptions.uncheck(() -> cls.getConstructor().newInstance());
            instance.onInitialize();
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, FabricLoader.getInstance().getConfigDir());
        }
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, FabricLoader.getInstance().getConfigDir());

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

    @SneakyThrows
    public static <T> void runWithContext(String name, Class<T> type, BiConsumer<Class<T>, ModContainerImpl> consumer) {
        EntryPointData epd = ENTRYPOINTS.get(name);
        if (epd == null) return;

        Map<Class<?>, ModContainerImpl> mods = new LinkedHashMap<>();

        for (Map.Entry<String, ModContainerImpl> entrypoint : epd.getEntrypoints().entrySet()) {
            Class<?> cls = Class.forName(entrypoint.getKey());
            if (!type.isAssignableFrom(cls)) throw new IllegalStateException("Entrypoint " + entrypoint + " is not a " + type.getName());
            mods.put(cls, entrypoint.getValue());
        }

        mods.forEach((aClass, mod) -> loadingMod(() -> consumer.accept((Class<T>) aClass, mod), mod));
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
