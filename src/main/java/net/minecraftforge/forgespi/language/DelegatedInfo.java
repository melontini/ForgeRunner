package net.minecraftforge.forgespi.language;

import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DelegatedInfo implements IModInfo {

    private final ModContainerImpl delegate;

    public DelegatedInfo(ModContainerImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public IModFileInfo getOwningFile() {
        return LoadingModList.get().getModFileById(delegate.getMetadata().getId());
    }

    @Override
    public String getModId() {
        return delegate.getMetadata().getId();
    }

    @Override
    public String getDisplayName() {
        return delegate.getMetadata().getName();
    }

    @Override
    public String getDescription() {
        return delegate.getMetadata().getDescription();
    }

    @Override
    public ArtifactVersion getVersion() {
        return new DefaultArtifactVersion(delegate.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public List<? extends ModVersion> getDependencies() {
        return List.of(); //TODO
    }

    @Override
    public String getNamespace() {
        return this.delegate.getMetadata().getId();
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
        return delegate.getMetadata().getIconPath(256);
    }

    @Override
    public boolean getLogoBlur() {
        return false;
    }
}
