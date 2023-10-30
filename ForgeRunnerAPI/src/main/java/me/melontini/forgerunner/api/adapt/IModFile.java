package me.melontini.forgerunner.api.adapt;

import me.melontini.forgerunner.api.ByteConvertible;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface IModFile extends ByteConvertible {

    boolean hasForgeMeta();
    IModJson modJson();
    String id();
    String version();
    Path path();

    IManifest manifest();

    List<String> mixinConfigs();
    Map<String, IModClass> classes();

    ByteConvertible getFile(String name);
    void putFile(String name, ByteConvertible file);
    void removeFile(String name);
    boolean hasFile(String name);
}
