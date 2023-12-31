package net.minecraftforge.fml;

import me.melontini.forgerunner.forge.mod.Mods;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ModContainer {

    private final ModFileInfo delegate;
    private final ModInfo modInfo;
    protected Supplier<?> contextExtension;
    protected final Map<Class<? extends IExtensionPoint<?>>, Supplier<?>> extensionPoints = new IdentityHashMap<>();
    protected final EnumMap<ModConfig.Type, ModConfig> configs = new EnumMap<>(ModConfig.Type.class);
    protected Optional<Consumer<IConfigEvent>> configHandler = Optional.empty();

    public ModContainer(ModFileInfo delegate) {
        this.delegate = delegate;
        this.modInfo = Mods.getModInfo(delegate);
    }

    public final String getModId() {
        return modInfo.getModId();
    }

    public final String getNamespace() {
        return modInfo.getNamespace();
    }

    public IModInfo getModInfo() {
        return modInfo;
    }

    @SuppressWarnings("unchecked")
    public <T extends Record> Optional<T> getCustomExtension(Class<? extends IExtensionPoint<T>> point) {
        return Optional.ofNullable((T) extensionPoints.getOrDefault(point, () -> null).get());
    }

    public <T extends Record & IExtensionPoint<T>> void registerExtensionPoint(Class<? extends IExtensionPoint<T>> point, Supplier<T> extension) {
        extensionPoints.put(point, extension);
    }

    public void addConfig(final ModConfig modConfig) {
        configs.put(modConfig.getType(), modConfig);
    }

    public void dispatchConfigEvent(IConfigEvent event) {
        configHandler.ifPresent(configHandler->configHandler.accept(event));
    }

    public abstract boolean matches(Object mod);

    public abstract Object getMod();

    protected <T extends Event & IModBusEvent> void acceptEvent(T e) {
        //no-op
    }
}
