package dev.asdf00.mc.advcomp.lua.adapterapi;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.api.AcAdapter;
import dev.asdf00.mc.advcomp.api.AcAdapterContext;
import dev.asdf00.mc.advcomp.blocks.adapter.AdapterBlockUD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static dev.asdf00.mc.advcomp.utils.MiscUtil.*;

public class AdapterCompanion {
    private static final AdapterCompanion EMPTY_ADAPTER_COMPANION = new AdapterCompanion(Map.of(), Map.of(), Map.of());
    private static final Map<Class<? extends Block>, AdapterCompanion> ALL_COMPANIONS = new HashMap<>();

    private final Map<String, MethodHandle> propertyGetter;
    private final Map<String, MethodHandle> propertySetter;
    private final Map<String, MethodHandle> callables;

    public final String[] readableKeys;
    public final String[] writableKeys;

    private AdapterCompanion(Map<String, MethodHandle> propertyGetter, Map<String, MethodHandle> propertySetter, Map<String, MethodHandle> callables) {
        this.propertyGetter = propertyGetter;
        this.propertySetter = propertySetter;
        this.callables = callables;
        var readables = new ArrayList<String>(propertyGetter.size() + callables.size());
        readables.addAll(propertyGetter.keySet());
        readables.addAll(callables.keySet());
        readableKeys = readables.toArray(String[]::new);
        writableKeys = propertySetter.keySet().toArray(String[]::new);
    }

    public boolean isGetter(String key) {
        return propertyGetter.containsKey(key);
    }

    public boolean isSetter(String key) {
        return propertySetter.containsKey(key);
    }

    public boolean isCallable(String key, int argCnt) {
        return callables.containsKey(mangleFuncName(key, argCnt));
    }

    public LuaObject get(AdapterBlockUD adapter, Level lvl, BlockPos pos, String key) {
        MethodHandle getter = Objects.requireNonNull(propertyGetter.get(key), "this should only be called for existing getters");
        final Object result;
        try {
            result = getter.invoke(new AcAdapterContext(adapter, lvl, pos));
        } catch (LuaJavaError e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return convertToLuaObject(result);
    }

    public void set(AdapterBlockUD adapter, Level lvl, BlockPos pos, String key, LuaObject value) {
        MethodHandle setter = Objects.requireNonNull(propertySetter.get(key), "this should only be called for existing setters");
        Object typeTranslated = convertToJavaType(toBoxedType(setter.type().parameterType(1)), value);
        try {
            setter.invoke(new AcAdapterContext(adapter, lvl, pos), typeTranslated);
        } catch (LuaJavaError e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public LuaObject[] call(AdapterBlockUD adapter, Level lvl, BlockPos pos, LuaObject... args) {
        // TODO
        return null;
    }

    public static AdapterCompanion ofBlock(Class<? extends Block> block) {
        return ALL_COMPANIONS.getOrDefault(block, EMPTY_ADAPTER_COMPANION);
    }

    private static String mangleFuncName(String name, int argCnt) {
        return "%s#%d".formatted(name, argCnt);
    }


    // =================================================================================================================
    // setup
    // =================================================================================================================

    static {
        // collect adapter classes from the class path
        var loader = Thread.currentThread().getContextClassLoader();
        var adapterClasses = new ArrayList<Class<?>>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File file = new File(entry);
            if (file.isDirectory()) {
                scanDirectory(adapterClasses, file, file, loader);
            } else if (entry.endsWith(".jar")) {
                scanJar(adapterClasses, file, loader);
            }
        }

        // process adapter classes
        for (Class<?> adCls : adapterClasses) {
            // block specific class
            Set<Method> foundMethods = processAdapterClass(adCls);

            // check for remaining abstract methods
            var abstractMethods = foundMethods.stream()
                    .filter(m -> {
                        if (m.isAnnotationPresent(AcAdapter.PropertyGet.class)) {
                            return m.getAnnotation(AcAdapter.PropertyGet.class).isAbstract();
                        } else if (m.isAnnotationPresent(AcAdapter.PropertySet.class)) {
                            return m.getAnnotation(AcAdapter.PropertySet.class).isAbstract();
                        } else {
                            return m.getAnnotation(AcAdapter.Method.class).isAbstract();
                        }
                    })
                    .map(Method::getName)
                    .toArray(String[]::new);
            if (abstractMethods.length > 0) {
                throw new IllegalStateException("Adapter-class %s must implement the abstract methods: %s".formatted(
                        adCls.getName(),
                        Arrays.toString(abstractMethods)
                ));
            }

            // build MethodHandles
            var lookup = MethodHandles.publicLookup();
            var getters = new HashMap<String, MethodHandle>();
            var setters = new HashMap<String, MethodHandle>();
            var methods = new HashMap<String, MethodHandle>();
            for (Method m : foundMethods) {
                var propGet = m.getAnnotation(AcAdapter.PropertyGet.class);
                if (propGet != null) {
                    // this is a getter
                    getters.put(m.getName(), makeMethodHandle(lookup, m));
                }
                var propSet = m.getAnnotation(AcAdapter.PropertySet.class);
                if (propSet != null) {
                    // this is a setter
                    setters.put(m.getName(), makeMethodHandle(lookup, m));
                }
                var propMeth = m.getAnnotation(AcAdapter.Method.class);
                if (propMeth != null) {
                    // this is a method
                    methods.put(mangleFuncName(m), makeMethodHandle(lookup, m));
                }
            }

            ALL_COMPANIONS.put(adCls.getAnnotation(AcAdapter.class).block(), new AdapterCompanion(getters, setters, methods));
        }
    }

    private static String distinctFuncName(Method method) {
        return method.isAnnotationPresent(AcAdapter.Method.class)
                ? mangleFuncName(method.getName(), method.getParameterCount() - 1)
                : method.isAnnotationPresent(AcAdapter.PropertyGet.class)
                ? method.getName() + "#get"
                : method.getName() + "#set";
    }

    private static String mangleFuncName(Method method) {
        return method.isAnnotationPresent(AcAdapter.Method.class)
                ? mangleFuncName(method.getName(), method.getParameterCount() - 1)
                : method.getName();
    }

    private static void scanDirectory(List<Class<?>> collected, File root, File dir, ClassLoader loader) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(collected, root, file, loader);
            } else if (file.getName().endsWith(".class")) {
                String path = root.toURI().relativize(file.toURI()).getPath();
                String className = path
                        .substring(0, path.length() - 6)
                        .replace(File.separatorChar, '.');
                checkClass(collected, className, loader);
            }
        }
    }

    private static void scanJar(List<Class<?>> collected, File file, ClassLoader loader) {
        try (JarFile jar = new JarFile(file)) {
            jar.stream()
                    .filter(e -> !e.isDirectory())
                    .filter(e -> e.getName().endsWith(".class"))
                    .forEach(e -> {
                        String className = e.getName()
                                .substring(0, e.getName().length() - 6)
                                .replace(File.separatorChar, '.');
                        checkClass(collected, className, loader);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkClass(List<Class<?>> collected, String name, ClassLoader loader) {
        try {
            Class<?> adapterClazz = Class.forName(name, false, loader);
            AcAdapter annotation = adapterClazz.getAnnotation(AcAdapter.class);
            if (annotation != null && annotation.block() != null) {
                // basic class checks
                if (!Modifier.isFinal(adapterClazz.getModifiers()) && !Modifier.isAbstract(adapterClazz.getModifiers()) || !Modifier.isPublic(adapterClazz.getModifiers())) {
                    throw new IllegalStateException("Adapter-classes must be 'public final' or 'public abstract', %s does not comply with this".formatted(adapterClazz.getName()));
                }
                if (annotation.block() == Block.class || Modifier.isAbstract(adapterClazz.getModifiers()) || Modifier.isAbstract(annotation.block().getModifiers())) {
                    // this is an abstract adapter class, we ignore this class here
                } else {
                    // this might be a specific class, we take those
                    collected.add(adapterClazz);
                }
            }
        } catch (TypeNotPresentException | ClassNotFoundException ignored) {
            // do nothing and skip this class
        }
    }

    private static Set<Method> processAdapterClass(Class<?> clazz) {
        var annotation = clazz.getAnnotation(AcAdapter.class);

        // process super methods
        var inheritedMethods = new LinkedHashSet<Method>();
        for (var superClazz : annotation.inheritsFrom()) {
            inheritedMethods.addAll(processAdapterClass(superClazz));
        }

        // remove diamond problem methods
        var toOverride = new LinkedHashSet<String>();
        inheritedMethods.stream()
                .collect(Collectors.groupingBy(m -> distinctFuncName(m), Collectors.toList()))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(duplicates -> {
                    toOverride.add(duplicates.getKey());
                    for (var m : duplicates.getValue()) {
                        inheritedMethods.remove(m);
                    }
                });

        // process methods defined here
        var ownMethods = new LinkedHashSet<Method>();
        for (var m : clazz.getDeclaredMethods()) {
            var propGet = m.getAnnotation(AcAdapter.PropertyGet.class);
            if (propGet != null) {
                // this is a getter
                checkGetter(m);
                var mangledName = distinctFuncName(m);
                toOverride.remove(mangledName);
                ownMethods.add(m);
                continue;
            }
            var propSet = m.getAnnotation(AcAdapter.PropertySet.class);
            if (propSet != null) {
                // this is a setter
                checkSetter(m);
                toOverride.remove(distinctFuncName(m));
                ownMethods.add(m);
                continue;
            }
            var propMeth = m.getAnnotation(AcAdapter.Method.class);
            if (propMeth != null) {
                // this is a method
                checkMethod(m);
                toOverride.remove(distinctFuncName(m));
                ownMethods.add(m);
                continue;
            }
            // this not of interest
        }

        // check for diamond problem
        if (!toOverride.isEmpty()) {
            throw new IllegalStateException("Adapter-class %s must override conflicting inheritance%s %s".formatted(
                    clazz.getName(),
                    toOverride.size() > 1 ? "s" : "",
                    Arrays.toString(toOverride.toArray(String[]::new))
            ));
        }

        // add all non-overridden inherited methods to own methods
        var ownMangledNames = ownMethods.stream().map(AdapterCompanion::distinctFuncName).collect(Collectors.toSet());
        ownMethods.stream()
                .filter(m -> !ownMangledNames.contains(distinctFuncName(m)))
                .forEach(ownMethods::add);

        // check for clashes between methods and properties
        var clearPropNames = ownMethods.stream()
                .filter(m -> m.isAnnotationPresent(AcAdapter.PropertyGet.class) || m.isAnnotationPresent(AcAdapter.PropertySet.class))
                .map(m -> m.getName())
                .collect(Collectors.toSet());
        var collisions = ownMethods.stream()
                .filter(m -> m.isAnnotationPresent(AcAdapter.Method.class) && clearPropNames.contains(m.getName()))
                .map(Method::getName)
                .toArray(String[]::new);
        if (collisions.length > 0) {
            throw new IllegalStateException("Adapter-class %s has methods that collide with properties: %s".formatted(
                    clazz.getName(),
                    Arrays.toString(collisions)
            ));
        }

        return ownMethods;
    }

    /**
     * Getters must have the signature {@code public static <LuaConvertible> <name>(AcAdapterContext ctx)}.
     */
    private static void checkGetter(Method m) {
        if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
            throw new IllegalStateException("Adapter-getters must be 'public static', %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        if (!isLuaObjectConvertible(m.getReturnType())) {
            throw new IllegalStateException("Adapter-getters must return a LuaObject-convertible object, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        if (m.getParameterCount() != 1 || m.getParameterTypes()[0] != AcAdapterContext.class) {
            throw new IllegalStateException("Adapter-getters must take exactly one argument of type AcAdapterContext, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
    }

    /**
     * Setters must have the signature {@code public static void <name>(AcAdapterContext ctx, <LuaConvertible> value}.
     */
    private static void checkSetter(Method m) {
        if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
            throw new IllegalStateException("Adapter-setters must be 'public static', %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        if (m.getReturnType() != void.class) {
            throw new IllegalStateException("Adapter-setters must return void, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        var params = m.getParameterTypes();
        if (params.length != 2 || params[0] != AcAdapterContext.class || !isFromLuaObjectConvertible(toBoxedType(params[1]))) {
            throw new IllegalStateException("Adapter-getters must take a AcAdapterContext and one LuaObject-convertible as parameters, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
    }

    /**
     * Methods must have the signature {@code public static <void|LuaObject-convertible> <name>(AcAdapterContext ctx, <LuaConvertible> value}.
     */
    private static void checkMethod(Method m) {
        if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
            throw new IllegalStateException("Adapter-methods must be 'public static', %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        if (m.getReturnType() != void.class && isLuaObjectConvertible(toBoxedType(m.getReturnType()))) {
            throw new IllegalStateException("Adapter-methods must return void or a LuaObject-convertible object, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        var params = m.getParameterTypes();
        boolean foundError = false;
        for (int i = 1; i < params.length; i++) {
            if (!isFromLuaObjectConvertible(params[i])) {
                foundError = true;
                break;
            }
        }
        if (foundError || params.length < 1 || params[0] != AcAdapterContext.class) {
            throw new IllegalStateException("Adapter-methods must take a AcAdapterContext and any number of LuaObject-convertible parameters, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
    }

    private static MethodHandle makeMethodHandle(MethodHandles.Lookup lookup, Method m) {
        try {
            return lookup.findVirtual(m.getDeclaringClass(), m.getName(), MethodType.methodType(m.getReturnType(), m.getParameterTypes()));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
