package me.melontini.forgerunner.loader;

import lombok.extern.slf4j.Slf4j;
import me.melontini.forgerunner.loader.adapt.ModAdapter;
import me.melontini.forgerunner.util.Exceptions;
import net.fabricmc.loader.impl.launch.knot.MixinServiceKnot;
import net.fabricmc.loader.impl.launch.knot.MixinServiceKnotBootstrap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

@Slf4j
public class MixinHacks {

    public static void bootstrap() {
        System.setProperty("mixin.bootstrapService", MixinServiceKnotBootstrap.class.getName());
        System.setProperty("mixin.service", MixinServiceKnot.class.getName());

        try {
            MixinBootstrap.init();//Let's pray this doesn't break something.
        } catch (Throwable t) {
            log.error("FAILED TO BOOTSTRAP MIXIN SERVICE EARLY!!!!", t);
        }
    }

    public static void crackMixinBytecodeProvider(IClassBytecodeProvider current, IMixinService currentService, Map<String, byte[]> classMap) {
        IClassBytecodeProvider newProvider = new IClassBytecodeProvider() {
            @Override public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
                try {return current.getClassNode(name);} catch (Throwable t) {return getNode(name);}
            }

            @Override public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
                try {return current.getClassNode(name, runTransformers);} catch (Throwable t) {return getNode(name);}
            }

            private ClassNode getNode(String cls) throws ClassNotFoundException {
                byte[] bytes = classMap.get(cls.replace(".", "/"));
                if (bytes == null) throw new ClassNotFoundException();
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(bytes);
                reader.accept(node, 0);
                return node;
            }
        };
        //Knot returns itself, so there's no magical field we can set.
        IMixinService service = (IMixinService) Proxy.newProxyInstance(ModAdapter.class.getClassLoader(), new Class[]{IMixinService.class}, (proxy, method, args) -> {
            if (method.getName().equals("getBytecodeProvider")) return newProvider;
            return method.invoke(currentService, args);
        });

        Exceptions.uncheck(() -> {
            Method m = MixinService.class.getDeclaredMethod("getInstance");
            m.setAccessible(true);
            MixinService serviceProxy = (MixinService) m.invoke(null);

            //Use our cursed service.
            Field f = MixinService.class.getDeclaredField("service");
            f.setAccessible(true);
            f.set(serviceProxy, service);
        });
        log.info("Sin against the mixin framework was committed");
    }

    public static void uncrackMixinService(IMixinService realService, Map<String, byte[]> classMap) {
        Exceptions.uncheck(() -> {
            Method m = MixinService.class.getDeclaredMethod("getInstance");
            m.setAccessible(true);
            MixinService serviceProxy = (MixinService) m.invoke(null);

            //Revert to original MixinService
            Field f = MixinService.class.getDeclaredField("service");
            f.setAccessible(true);
            f.set(serviceProxy, realService);

            //Purge ClassInfo cache.
            Field cache = ClassInfo.class.getDeclaredField("cache");
            cache.setAccessible(true);
            Map<String, ClassInfo> cacheMap = (Map<String, ClassInfo>) cache.get(null);
            classMap.keySet().forEach(cacheMap::remove);
        });
        log.info("Sin against the mixin framework was reverted");
    }
}
