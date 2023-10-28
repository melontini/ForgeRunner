package net.minecraftforge.fml.loading.moddiscovery;

import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModInfo implements IModInfo, IConfigurable {

    private final ModFileInfo delegate;
    private final ModContainerImpl modContainer;

    public ModInfo(ModFileInfo delegate) {
        this.delegate = delegate;
        this.modContainer = delegate.fr$getModContainer();
    }

    @Override
    public IModFileInfo getOwningFile() {
        return delegate;
    }

    @Override
    public String getModId() {
        return modContainer.getMetadata().getId();
    }

    @Override
    public String getDisplayName() {
        return modContainer.getMetadata().getName();
    }

    @Override
    public String getDescription() {
        return modContainer.getMetadata().getDescription();
    }

    @Override
    public ArtifactVersion getVersion() {
        return new DefaultArtifactVersion(modContainer.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public List<? extends ModVersion> getDependencies() {
        return List.of(); //TODO
    }

    @Override
    public String getNamespace() {
        return modContainer.getMetadata().getId();
    }

    @Override
    public Map<String, Object> getModProperties() {
        return Map.of(); //TODO
    }

    @Override
    public Optional<URL> getUpdateURL() {
        return Optional.empty();
    }

    @Override
    public Optional<URL> getModURL() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getLogoFile() {
        return modContainer.getMetadata().getIconPath(256);
    }

    @Override
    public boolean getLogoBlur() {
        return false;
    }

    @Override
    public <T> Optional<T> getConfigElement(String... key) {
        return Optional.empty(); //TODO
    }

    @Override
    public List<? extends IConfigurable> getConfigList(String... key) {
        return List.of(); //TODO
    }
}
