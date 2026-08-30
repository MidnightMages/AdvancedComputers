package dev.asdf00.mc.advcomp.lua.adapterapi;

import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.functions.StatelessFunctionRegistry;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.runtime.utils.Singletons;
import dev.asdf00.jluavm.runtime.utils.UDTranslators;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.api.AcALIContext;
import dev.asdf00.mc.advcomp.api.AcAdapterLuaImplementation;
import dev.asdf00.mc.advcomp.blocks.adapter.AdapterBlockUD;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static dev.asdf00.mc.advcomp.utils.MiscUtil.*;

public class AdapterCompanion {
    public static final AdapterCompanion EMPTY_COMPANION = new AdapterCompanion(Map.of(), Map.of(), Map.of(), Set.of(), null);
    private static final Map<Class<? extends Block>, AdapterCompanion> ALL_COMPANIONS = new HashMap<>();

    private final Map<String, MethodHandle> propertyGetter;
    private final Map<String, MethodHandle> propertySetter;
    private final Map<String, MethodHandle> callables;
    private final Set<String> pureCallableNames;

    private final Class<? extends Block> blockClazz;

    public final String[] readableKeys;
    public final String[] writableKeys;

    private AdapterCompanion(Map<String, MethodHandle> propertyGetter, Map<String, MethodHandle> propertySetter,
                             Map<String, MethodHandle> callables, Set<String> pureCallableNames, Class<? extends Block> blockClazz) {
        this.propertyGetter = propertyGetter;
        this.propertySetter = propertySetter;
        this.callables = callables;
        this.pureCallableNames = pureCallableNames;
        this.blockClazz = blockClazz;
        var readables = new ArrayList<String>(propertyGetter.size() + callables.size());
        readables.addAll(propertyGetter.keySet());
        readables.addAll(pureCallableNames);
        readableKeys = readables.toArray(String[]::new);
        writableKeys = propertySetter.keySet().toArray(String[]::new);
    }

    public boolean isGetter(String key) {
        return propertyGetter.containsKey(key);
    }

    public boolean isSetter(String key) {
        return propertySetter.containsKey(key);
    }

    public boolean isCallable(String key) {
        return pureCallableNames.contains(key);
    }

    public LuaObject get(AdapterBlockUD adapter, Level lvl, BlockPos pos, String key) {
        MethodHandle getter = Objects.requireNonNull(propertyGetter.get(key), "this should only be called for existing getters");
        final Object result;
        try {
            result = getter.invoke(new AcALIContext(adapter, lvl, pos));
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
            setter.invoke(new AcALIContext(adapter, lvl, pos), typeTranslated);
        } catch (LuaJavaError e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public LuaObject getFunction(String name) {
        return pureCallableNames.contains(name)
                ? LuaObject.of(ADAPTER_FUNCTION_REGISTRY.getFunction(blockClazz.getName() + "#" + name))
                : LuaObject.NIL;
    }

    private LuaObject[] call(String funcName, LuaObject... args) {
        if (args.length < 1) {
            throw new LuaJavaError("missing AdapterBlockUD as #1, use the Lua OOP call syntax to avoid this");
        }
        var possibleUd = args[0];
        if (!possibleUd.isUserData()) {
            throw new LuaJavaError("missing AdapterBlockUD as #1, use the Lua OOP call syntax to avoid this");
        }
        var adapterUd = UDTranslators.lo2ud(AdapterBlockUD.class, possibleUd);
        var context = adapterUd.validateCall(this);
        var mangled = mangleFuncName(funcName, args.length - 1);
        if (!callables.containsKey(mangled)) {
            throw new LuaJavaError("no overload found for %d arguments".formatted(args.length - 1));
        }
        var handle = callables.get(mangled);
        Object[] transformedArgs = prepareArgs(context, handle.type().parameterArray(), args);
        var rType = handle.type().returnType();
        try {
            if (rType == void.class) {
                handle.invokeWithArguments(transformedArgs);
                return Singletons.EMPTY_LUA_OBJ_ARRAY;
            } else if (rType == LuaObject[].class) {
                return (LuaObject[]) handle.invokeWithArguments(transformedArgs);
            } else {
                return new LuaObject[]{convertToLuaObject(handle.invokeWithArguments(transformedArgs))};
            }
        } catch (LuaJavaError e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] prepareArgs(AcALIContext ctx, Class<?>[] targetTypes, LuaObject[] objects) {
        var r = new Object[targetTypes.length];
        r[0] = ctx;
        assert targetTypes.length - 1 <= objects.length - 1 : "insufficient objects";
        // ignore objects[0] since that is the userdata
        // ignore r[0] and targetTypes[0] since that is the context object
        for (int i = 1; i < r.length; i++) {
            assert isFromLuaObjectConvertible(targetTypes[i]);
            r[i] = convertToJavaType(targetTypes[i], objects[i]);
        }
        return r;
    }

    public static AdapterCompanion ofBlock(Class<? extends Block> block) {
        var c = ALL_COMPANIONS.get(block);
        RuntimeAssert.RuntimeAssert(initFinished, "not initialized yet!!!");
        return c != null ? c : EMPTY_COMPANION;
    }

    private static String mangleFuncName(String name, int argCnt) {
        return "%s#%d".formatted(name, argCnt);
    }


    // =================================================================================================================
    // setup
    // =================================================================================================================

    public static final StatelessFunctionRegistry ADAPTER_FUNCTION_REGISTRY = new StatelessFunctionRegistry("advanced_computers.adapter");
    private static volatile boolean initFinished = false;
    public static void init() {
        RuntimeAssert.RuntimeAssert(AdvancedComputers.AC_ADAPTER_LUA_IMPLEMENTATION_REGISTRY.isClosed(),
                "why are we initting adapter lua implemenations while mods are still allowed to register new ones?");
        // collect adapter classes from the class path
        var adapterClasses = AdvancedComputers.AC_ADAPTER_LUA_IMPLEMENTATION_REGISTRY.getRegisteredClasses();

        // process adapter classes
        for (Class<?> adCls : adapterClasses) {
            // block specific class
            Set<Method> foundMethods = processAdapterClass(adCls);

            // check for remaining abstract methods
            var abstractMethods = foundMethods.stream()
                    .filter(m -> {
                        if (m.isAnnotationPresent(AcAdapterLuaImplementation.PropertyGet.class)) {
                            return m.getAnnotation(AcAdapterLuaImplementation.PropertyGet.class).isAbstract();
                        } else if (m.isAnnotationPresent(AcAdapterLuaImplementation.PropertySet.class)) {
                            return m.getAnnotation(AcAdapterLuaImplementation.PropertySet.class).isAbstract();
                        } else {
                            return m.getAnnotation(AcAdapterLuaImplementation.Method.class).isAbstract();
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
            var clearMethodNames = new HashSet<String>();
            for (Method m : foundMethods) {
                var propGet = m.getAnnotation(AcAdapterLuaImplementation.PropertyGet.class);
                if (propGet != null) {
                    // this is a getter
                    getters.put(m.getName(), makeMethodHandle(lookup, m));
                }
                var propSet = m.getAnnotation(AcAdapterLuaImplementation.PropertySet.class);
                if (propSet != null) {
                    // this is a setter
                    setters.put(m.getName(), makeMethodHandle(lookup, m));
                }
                var propMeth = m.getAnnotation(AcAdapterLuaImplementation.Method.class);
                if (propMeth != null) {
                    // this is a method
                    methods.put(mangleFuncName(m), makeMethodHandle(lookup, m));
                    clearMethodNames.add(m.getName());
                }
            }

            var blkClazz = adCls.getAnnotation(AcAdapterLuaImplementation.class).block();
            var companion = new AdapterCompanion(getters, setters, methods, clearMethodNames, blkClazz);
            if (ALL_COMPANIONS.put(blkClazz, companion) != null)
                throw new IllegalStateException("Adapter integration %s was defined at least twice!".formatted(blkClazz.getName()));

            // register Lua functions
            for (var name : clearMethodNames) {
                registerAdapterFunction(companion, name);
            }
        }

        AdvancedComputers.LOGGER.info("Loaded %s AdapterLuaImplementations for [%s].".formatted(
                ALL_COMPANIONS.size(),
                ALL_COMPANIONS.keySet()
                        .stream()
                        .map(Class::getName)
                        .collect(Collectors.joining(", "))
        ));
        initFinished = true;
    }

    private static String distinctFuncName(Method method) {
        return method.isAnnotationPresent(AcAdapterLuaImplementation.Method.class)
                ? mangleFuncName(method.getName(), method.getParameterCount() - 1)
                : method.isAnnotationPresent(AcAdapterLuaImplementation.PropertyGet.class)
                ? method.getName() + "#get"
                : method.getName() + "#set";
    }

    private static String mangleFuncName(Method method) {
        return method.isAnnotationPresent(AcAdapterLuaImplementation.Method.class)
                ? mangleFuncName(method.getName(), method.getParameterCount() - 1)
                : method.getName();
    }

    private static Set<Method> processAdapterClass(Class<?> clazz) {
        var annotation = clazz.getAnnotation(AcAdapterLuaImplementation.class);

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
            var propGet = m.getAnnotation(AcAdapterLuaImplementation.PropertyGet.class);
            if (propGet != null) {
                // this is a getter
                checkGetter(m);
                var mangledName = distinctFuncName(m);
                toOverride.remove(mangledName);
                ownMethods.add(m);
                continue;
            }
            var propSet = m.getAnnotation(AcAdapterLuaImplementation.PropertySet.class);
            if (propSet != null) {
                // this is a setter
                checkSetter(m);
                toOverride.remove(distinctFuncName(m));
                ownMethods.add(m);
                continue;
            }
            var propMeth = m.getAnnotation(AcAdapterLuaImplementation.Method.class);
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
                .filter(m -> m.isAnnotationPresent(AcAdapterLuaImplementation.PropertyGet.class) || m.isAnnotationPresent(AcAdapterLuaImplementation.PropertySet.class))
                .map(m -> m.getName())
                .collect(Collectors.toSet());
        var collisions = ownMethods.stream()
                .filter(m -> m.isAnnotationPresent(AcAdapterLuaImplementation.Method.class) && clearPropNames.contains(m.getName()))
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
     * Getters must have the signature {@code public static <LuaConvertible> <name>(AcALIContext ctx)}.
     */
    private static void checkGetter(Method m) {
        if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
            throw new IllegalStateException("Adapter-getters must be 'public static', %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        if (!isLuaObjectConvertible(toBoxedType(m.getReturnType()))) {
            throw new IllegalStateException("Adapter-getters must return a LuaObject-convertible object, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        if (m.getParameterCount() != 1 || m.getParameterTypes()[0] != AcALIContext.class) {
            throw new IllegalStateException("Adapter-getters must take exactly one argument of type AcALIContext, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
    }

    /**
     * Setters must have the signature {@code public static void <name>(AcALIContext ctx, <LuaConvertible> value}.
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
        if (params.length != 2 || params[0] != AcALIContext.class || !isFromLuaObjectConvertible(toBoxedType(params[1]))) {
            throw new IllegalStateException("Adapter-getters must take a AcALIContext and one LuaObject-convertible as parameters, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
    }

    /**
     * Methods must have the signature {@code public static <void|LuaObject-convertible> <name>(AcALIContext ctx, <LuaConvertible> value}.
     */
    private static void checkMethod(Method m) {
        if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
            throw new IllegalStateException("Adapter-methods must be 'public static', %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        if (m.getReturnType() != void.class && !isLuaObjectConvertible(toBoxedType(m.getReturnType()))) {
            throw new IllegalStateException("Adapter-methods must return void or a LuaObject-convertible object, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
        var params = m.getParameterTypes();
        boolean foundError = false;
        for (int i = 1; i < params.length; i++) {
            if (!isFromLuaObjectConvertible(toBoxedType(params[i]))) {
                foundError = true;
                break;
            }
        }
        if (foundError || params.length < 1 || params[0] != AcALIContext.class) {
            throw new IllegalStateException("Adapter-methods must take a AcALIContext and any number of LuaObject-convertible parameters, %s#%s does not comply with this".formatted(
                    m.getDeclaringClass().getName(),
                    m.getName()
            ));
        }
    }

    private static MethodHandle makeMethodHandle(MethodHandles.Lookup lookup, Method m) {
        try {
            return lookup.findStatic(m.getDeclaringClass(), m.getName(), MethodType.methodType(m.getReturnType(), m.getParameterTypes()));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    private static void registerAdapterFunction(AdapterCompanion companion, String name) {
        ADAPTER_FUNCTION_REGISTRY.register(companion.blockClazz.getName() + "#" + name,
                AtomicLuaFunction.vaForManyResults(ADAPTER_FUNCTION_REGISTRY, (vm, va) -> companion.call(name, va)));
    }
}
