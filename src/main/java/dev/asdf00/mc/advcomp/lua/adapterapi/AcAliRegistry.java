package dev.asdf00.mc.advcomp.lua.adapterapi;

import dev.asdf00.mc.advcomp.api.AcAdapterLuaImplementation;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;

import java.util.HashSet;

public class AcAliRegistry {
    private volatile boolean isClosed = false;
    private final HashSet<Class<?>> registeredTypes = new HashSet<>();
    private final Object lockObj = new Object();

    public Class<?>[] getRegisteredClasses() {
        synchronized (lockObj) {
            return registeredTypes.toArray(Class[]::new);
        }
    }


    /**
     * Registers a single class tagged with AcAdapterLuaImplementation
     */
    public void register(Class<?> clazz) {
        RuntimeAssert.RuntimeAssert(!isClosed, "Adapter registry is already closed. Please register your stuff earlier.");
        RuntimeAssert.RuntimeAssert(clazz.isAnnotationPresent(AcAdapterLuaImplementation.class),
                "The given class %s is not tagged with AcAdapterLuaImplementation!");
        synchronized (lockObj) {
            registeredTypes.add(clazz);
        }
    }

//    /**
//     * Registers all AcAdapterLuaImplementations that are located in the given package
//     */
//    public void registerAllInSamePackageAs(Class<?> clazz) {
//        var packageName = clazz.getPackageName();
//        var packages = Arrays.stream(clazz.getClassLoader().getDefinedPackages()).filter(x->x.getName().equals(packageName)).toArray(Package[]::new);
//
//        var classLoader = ClassLoader.getSystemClassLoader();
//        try (var stream = classLoader.getResourceAsStream(packageName.replace('.', '/'))) {
//            RuntimeAssert.RuntimeAssert(stream != null, "couldnt find package '%s'!".formatted(packageName));
//            var reader = new BufferedReader(new InputStreamReader(stream));
//            var foundClassNames = reader.lines().filter(l -> l.endsWith(".class")).toArray(String[]::new);
//            for (var name : foundClassNames) {
//
//                var loadedClass = Class.forName(packageName + "." + name.substring(0, name.length() - ".class".length()));
//                if (loadedClass.getAnnotation(AcAdapterLuaImplementation.class) != null)
//                    register(loadedClass);
//            }
//        } catch (IOException | ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * SHALL ONLY BE CALLED BY ADVANCED COMPUTERS
     */
    public void closeRegistration() {
        RuntimeAssert.RuntimeAssert(!isClosed, "registry already closed?");
        isClosed = true;
    }

    public boolean isClosed() {
        return isClosed;
    }
}
