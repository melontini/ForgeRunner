package net.minecraftforge.fml.loading.moddiscovery;

import me.melontini.forgerunner.forge.mod.Mods;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.List;
import java.util.Map;

public class ModFileInfo implements IModFileInfo {

    private final ModContainerImpl delegate;

    public ModFileInfo(ModContainerImpl container) {
        this.delegate = container;
    }

    @Override
    public List<IModInfo> getMods() {
        return List.of(Mods.getModInfo(this));
    }

    @Override
    public List<LanguageSpec> requiredLanguageLoaders() {
        return List.of();//TODO
    }

    @Override
    public boolean showAsResourcePack() {
        return false;
    }

    @Override
    public Map<String, Object> getFileProperties() {
        return Map.of();
    }

    @Override
    public String getLicense() {
        return String.valueOf(delegate.getMetadata().getLicense());//TODO
    }

    @Override
    public String moduleName() {
        return delegate.getMetadata().getId();
    }

    @Override
    public String versionString() {
        return delegate.getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public List<String> usesServices() {
        return List.of();
    }

    public ModContainerImpl fr$getModContainer() {
        return delegate;
    }
}
