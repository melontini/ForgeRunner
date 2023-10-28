package me.melontini.forgerunner.loader.adapt;

import me.melontini.forgerunner.api.ByteConvertible;
import me.melontini.forgerunner.api.adapt.Adapter;
import me.melontini.forgerunner.api.adapt.IEnvironment;
import me.melontini.forgerunner.api.adapt.IModFile;
import me.melontini.forgerunner.loader.remapping.SrgRemapper;
import net.minecraftforge.srgutils.IMappingFile;

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
            boolean mutable = at[0].endsWith("-f");
            boolean widen = at[0].startsWith("public");

            String cls = at[1].replace(".", "/");
            if (at.length == 2) {
                String mapped = SrgRemapper.mapClassName(cls);
                if (widen) accessWidener.append("accessible ").append("class ")
                        .append(mapped).append("\n");
                if (mutable) accessWidener.append("extendable ").append("class ")
                        .append(mapped).append("\n");
                return;
            }

            String member = at[2];
            String mapped = SrgRemapper.mapClassName(cls);
            if (member.contains("(")) {
                String name = member.substring(0, member.indexOf('('));
                String desc = member.substring(member.indexOf('('));
                member = SrgRemapper.mapMethodName(cls, name, desc);
                String mappedDesc = env.frr().mapDesc(desc);

                if (widen) accessWidener.append("accessible ").append("method ")
                        .append(mapped).append(" ").append(member).append(" ").append(mappedDesc).append("\n");
                if (mutable) accessWidener.append("extendable ").append("method ")
                        .append(mapped).append(" ").append(member).append(" ").append(mappedDesc).append("\n");
            } else {
                //having to scrape by, since ATs don't provide field descriptors.
                //This breaks mods transforming other mods, but why would you do that?
                IMappingFile.IClass iClass = SrgRemapper.getMappingFile().getClass(cls);
                if (iClass == null) return;
                IMappingFile.IField iField = iClass.getField(member);
                if (iField == null) return;

                if (widen) accessWidener.append("accessible ").append("field ")
                        .append(mapped).append(" ").append(iField.getMapped()).append(" ").append(iField.getMappedDescriptor()).append("\n");
                if (mutable) accessWidener.append("mutable ").append("field ")
                        .append(mapped).append(" ").append(iField.getMapped()).append(" ").append(iField.getMappedDescriptor()).append("\n");
            }
        });

        byte[] aw = accessWidener.toString().getBytes();
        mod.putFile(mod.id() + ".accesswidener", () -> aw);
        mod.modJson().accept(object -> object.addProperty("accessWidener", mod.id() + ".accesswidener"));
        mod.removeFile("META-INF/accesstransformer.cfg");
    }

    @Override
    public long priority() {
        return 62;
    }
}
