package net.minecraftforge.fml;

import java.util.function.Supplier;

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
        return activeContainer == null ? ModList.get().getModContainerById("minecraft").orElseThrow(()->new RuntimeException("Where is minecraft???!")) : activeContainer;
    }

    public String getActiveNamespace() {
        return activeContainer == null ? "minecraft" : activeContainer.getNamespace();
    }

    public <T extends Record & IExtensionPoint<T>> void registerExtensionPoint(Class<? extends IExtensionPoint<T>> point, Supplier<T> extension) {
        getActiveContainer().registerExtensionPoint(point, extension);
    }

    public <T> T extension() {
        return (T)languageExtension;
    }
}
