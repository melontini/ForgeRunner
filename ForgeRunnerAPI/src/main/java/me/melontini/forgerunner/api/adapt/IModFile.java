package me.melontini.forgerunner.api.adapt;

import me.melontini.forgerunner.api.ByteConvertible;

import java.util.Collection;
import java.util.List;

public interface IModFile extends ByteConvertible {

    boolean hasForgeMeta();
    IModJson modJson();
    String id();
    String version();

    IManifest manifest();

    List<String> mixinConfigs();
    Collection<IModClass> classes();

    ByteConvertible getFile(String name);
    void putFile(String name, ByteConvertible file);
    void removeFile(String name);
    boolean hasFile(String name);
}
