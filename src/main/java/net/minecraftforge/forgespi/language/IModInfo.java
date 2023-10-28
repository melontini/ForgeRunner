package net.minecraftforge.forgespi.language;

import me.melontini.forgerunner.util.Exceptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IModInfo {
    VersionRange UNBOUNDED = Exceptions.uncheck(() -> VersionRange.createFromVersionSpec(""));

    IModFileInfo getOwningFile();

    String getModId();

    String getDisplayName();

    String getDescription();

    ArtifactVersion getVersion();

    List<? extends ModVersion> getDependencies();

    //List<? extends ForgeFeature.Bound> getForgeFeatures();

    String getNamespace();

    Map<String, Object> getModProperties();

    Optional<URL> getUpdateURL();

    Optional<URL> getModURL();

    Optional<String> getLogoFile();

    boolean getLogoBlur();

    //IConfigurable getConfig();

    enum Ordering {
        BEFORE, AFTER, NONE
    }

    enum DependencySide {
        CLIENT(Dist.CLIENT), SERVER(Dist.DEDICATED_SERVER), BOTH(Dist.values());

        private final Dist[] dist;

        DependencySide(final Dist... dist) {
            this.dist = dist;
        }

        public boolean isContained(Dist side) {
            return this == BOTH || dist[0] == side;
        }

        public boolean isCorrectSide() {
            return this == BOTH || FMLEnvironment.dist.equals(this.dist[0]);
        }
    }

    interface ModVersion {
        String getModId();

        VersionRange getVersionRange();

        boolean isMandatory();

        Ordering getOrdering();

        DependencySide getSide();

        void setOwner(IModInfo owner);

        IModInfo getOwner();

        Optional<URL> getReferralURL();
    }
}
