package net.minecraftforge.fml;

import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.forgespi.language.DelegatedInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class ModContainer {

    private final ModContainerImpl delegate;
    private final DelegatedInfo modInfo;
    protected Supplier<?> contextExtension;
    protected final Map<Class<? extends IExtensionPoint<?>>, Supplier<?>> extensionPoints = new IdentityHashMap<>();

    public ModContainer(ModContainerImpl delegate) {
        this.delegate = delegate;
        this.modInfo = new DelegatedInfo(delegate);
    }

    public final String getModId() {
        return delegate.getMetadata().getId();
    }

    public final String getNamespace() {
        return delegate.getMetadata().getId();
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

    public abstract boolean matches(Object mod);

    public abstract Object getMod();

    protected <T extends Event & IModBusEvent> void acceptEvent(T e) {
        //no-op
    }
}
