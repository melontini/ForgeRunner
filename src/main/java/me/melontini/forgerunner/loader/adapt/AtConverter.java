package me.melontini.forgerunner.loader.adapt;

import me.melontini.forgerunner.api.ByteConvertible;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.loader.remapping.SrgMap;
import net.minecraftforge.srgutils.IMappingFile;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class AtConverter implements Adapter {
    @Override
    public void adapt(IModFile mod, IEnvironment env) {
        ByteConvertible file = mod.getFile("META-INF/accesstransformer.cfg");
        if (file == null) return;

        String accessTransformer = new String(file.toBytes());
        StringBuilder accessWidener = new StringBuilder();
        accessWidener.append("accessWidener  ").append("v2").append("  intermediary")
                .append("\n").append("\n");

        accessTransformer.lines().filter(s -> !s.startsWith("#") || s.isBlank()).forEach(s -> {
            int hash = s.indexOf('#');
            String[] at = hash == -1 ? s.split(" ") : s.substring(0, hash).split(" ");
            if (at.length < 2) return;
            for (int i = 0; i < at.length; i++) {at[i] = at[i].trim();}

            //Other types are not support and are pointless
            boolean mutable = at[0].endsWith("-f") || at[0].startsWith("protected");
            boolean widen = at[0].startsWith("public");

            String cls = at[1].replace('.', '/');
            if (at.length == 2) {
                String mapped = env.frr().map(cls);
                if (widen) accessWidener.append("accessible ").append("class ")
                        .append(mapped).append("\n");
                if (mutable) accessWidener.append("extendable ").append("class ")
                        .append(mapped).append("\n");
                return;
            }

            String member = at[2];
            String mapped = env.frr().map(cls);
            if (member.startsWith("*")) {// Does not seem to be part of the spec https://github.com/MinecraftForge/AccessTransformers/blob/master/FMLAT.md
                if (member.contains("(") && member.contains(")")) {
                    IMappingFile.IClass iClass = SrgMap.getMappingFile().getClass(cls);
                    if (iClass == null) return;
                    for (IMappingFile.IMethod iMethod : iClass.getMethods()) {
                        method(accessWidener, mapped, iMethod.getMapped(), iMethod.getMappedDescriptor(), mutable, widen);
                    }
                } else {
                    IMappingFile.IClass iClass = SrgMap.getMappingFile().getClass(cls);
                    if (iClass == null) return;
                    for (IMappingFile.IField iField : iClass.getFields()) {
                        field(accessWidener, mapped, iField.getMapped(), iField.getMappedDescriptor(), mutable, widen);
                    }
                }
                return;
            }
            if (member.contains("(") && member.contains(")")) {
                String name = member.substring(0, member.indexOf('('));
                String desc = member.substring(member.indexOf('('));
                member = env.frr().mapMethodName(cls, name, desc);
                String mappedDesc = env.frr().mapDesc(desc);

                method(accessWidener, mapped, member, mappedDesc, mutable, widen);
            } else {
                //having to scrape by, since ATs don't provide field descriptors.
                //This breaks mods transforming other mods, but why would you do that?
                IMappingFile.IClass iClass = SrgMap.getMappingFile().getClass(cls);
                if (iClass == null) return;
                IMappingFile.IField iField = iClass.getField(member);
                if (iField == null) return;

                field(accessWidener, mapped, iField.getMapped(), iField.getMappedDescriptor(), mutable, widen);
            }
        });

        byte[] aw = accessWidener.toString().getBytes();
        mod.putFile(mod.id() + ".accesswidener", () -> aw);
        mod.modJson().accept(object -> object.addProperty("accessWidener", mod.id() + ".accesswidener"));
        mod.removeFile("META-INF/accesstransformer.cfg");
    }

    private static void method(StringBuilder aw, String cls, String method, String mappedDesc, boolean mutable, boolean widen) {
        if (widen) aw.append("accessible ").append("method ")
                .append(cls).append(" ").append(method).append(" ").append(mappedDesc).append("\n");
        if (mutable) aw.append("extendable ").append("method ")
                .append(cls).append(" ").append(method).append(" ").append(mappedDesc).append("\n");
    }
    private static void field(StringBuilder aw, String cls, String field, String mappedDesc, boolean mutable, boolean widen) {
        if (widen) aw.append("accessible ").append("field ")
                .append(cls).append(" ").append(field).append(" ").append(mappedDesc).append("\n");
        if (mutable) aw.append("mutable ").append("field ")
                .append(cls).append(" ").append(field).append(" ").append(mappedDesc).append("\n");
    }

    @Override
    public long priority() {
        return 62;
    }

    @Override
    public void onPrepare(IModFile mod, IEnvironment env, FileSystem fs) throws IOException {
        Path p = fs.getPath("META-INF/accesstransformer.cfg");
        if (Files.exists(p)) {
            byte[] bytes = Files.readAllBytes(p);
            mod.putFile("META-INF/accesstransformer.cfg", () -> bytes);
        }
    }
}
