package net.minecraftforge.forgespi.language;

import org.apache.maven.artifact.versioning.VersionRange;

import java.util.List;
import java.util.Map;

public interface IModFileInfo {
    List<IModInfo> getMods();

    record LanguageSpec(String languageName, VersionRange acceptedVersions) {}

    List<LanguageSpec> requiredLanguageLoaders();

    boolean showAsResourcePack();

    Map<String,Object> getFileProperties();

    String getLicense();

    String moduleName();

    String versionString();

    List<String> usesServices();

    //IModFile getFile(); //TODO

    //IConfigurable getConfig(); //TODO
}
