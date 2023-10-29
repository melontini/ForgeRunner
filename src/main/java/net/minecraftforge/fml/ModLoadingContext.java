package net.minecraftforge.fml;

import lombok.extern.log4j.Log4j2;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.function.Supplier;

@Log4j2
public class ModLoadingContext {

    private static final ThreadLocal<ModLoadingContext> context = ThreadLocal.withInitial(ModLoadingContext::new);
    private Object languageExtension;
    private ModContainer activeContainer;

    public static ModLoadingContext get() {
        return context.get();
    }

    public void setActiveContainer(final ModContainer container) {
        this.activeContainer = container;
        this.languageExtension = container == null ? null : container.contextExtension.get();
    }

    public ModContainer getActiveContainer() {
        return activeContainer == null ? ModList.get().getModContainerById("minecraft").orElseThrow(() -> new RuntimeException("Where is minecraft???!")) : activeContainer;
    }

    public String getActiveNamespace() {
        return activeContainer == null ? "minecraft" : activeContainer.getNamespace();
    }

    public <T extends Record & IExtensionPoint<T>> void registerExtensionPoint(Class<? extends IExtensionPoint<T>> point, Supplier<T> extension) {
        getActiveContainer().registerExtensionPoint(point, extension);
    }

    public void registerConfig(ModConfig.Type type, IConfigSpec<?> spec) {
        if (spec.isEmpty()) {
            // This handles the case where a mod tries to register a config, without any options configured inside it.
            log.debug("Attempted to register an empty config for type {} on mod {}", type, getActiveContainer().getModId());
            return;
        }

        getActiveContainer().addConfig(new ModConfig(type, spec, getActiveContainer()));
    }

    public void registerConfig(ModConfig.Type type, IConfigSpec<?> spec, String fileName) {
        if (spec.isEmpty()) {
            // This handles the case where a mod tries to register a config, without any options configured inside it.
            log.debug("Attempted to register an empty config for type {} on mod {} using file name {}", type, getActiveContainer().getModId(), fileName);
            return;
        }

        getActiveContainer().addConfig(new ModConfig(type, spec, getActiveContainer(), fileName));
    }

    public <T> T extension() {
        return (T) languageExtension;
    }
}
