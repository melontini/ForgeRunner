package net.minecraftforge.fml;

import me.melontini.forgerunner.forge.mod.Mods;
import me.melontini.forgerunner.util.Loader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.ModContainerImpl;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ModList {

    private static final ModList INSTANCE = new ModList();

    public static ModList get() {
        return INSTANCE;
    }

    public Optional<? extends ModContainer> getModContainerById(String modId) {
        ModContainerImpl container = Loader.getMod(modId);
        if (container != null) {
            return Optional.ofNullable(Mods.getFromDelegate(container));
        }
        return Optional.empty();
    }

    public boolean isLoaded(String modTarget) {
        return FabricLoader.getInstance().isModLoaded(modTarget);
    }

    public int size() {
        return FabricLoader.getInstance().getAllMods().size();
    }

    public void forEachModContainer(BiConsumer<String, ModContainer> modContainerConsumer) {
        Mods.forEachMod((mod, container) -> modContainerConsumer.accept(container.getModId(), container));
    }

    public void forEachModInOrder(Consumer<ModContainer> containerConsumer) {
        Mods.forEachMod((mod, container) -> containerConsumer.accept(container));
    }
}
