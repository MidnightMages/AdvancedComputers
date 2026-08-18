package dev.asdf00.mc.advcomp.lua.adapterapi;

import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.api.AcAdapter;
import dev.asdf00.mc.advcomp.blocks.adapter.AdapterBlockUD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.jar.JarFile;

import static dev.asdf00.mc.advcomp.utils.MiscUtil.convertToLuaObject;

public class AdapterCompanion {
    private static final AdapterCompanion EMPTY_ADAPTER_COMPANION = new AdapterCompanion(Map.of(), Map.of(), Map.of());
    private static final Map<Class<? extends Block>, AdapterCompanion> ALL_COMPANIONS = new HashMap<>();

    private final Map<String, MethodHandle> propertyGetter;
    private final Map<String, MethodHandle> propertySetter;
    private final Map<String, MethodDescription> callables;

    public final String[] readableKeys;
    public final String[] writableKeys;

    private AdapterCompanion(Map<String, MethodHandle> propertyGetter, Map<String, MethodHandle> propertySetter, Map<String, MethodDescription> callables) {
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

    public boolean isCallable(String key) {
        return callables.containsKey(key);
    }

    public LuaObject get(AdapterBlockUD adapter, Level lvl, BlockPos pos, String key) {
        MethodHandle getter = Objects.requireNonNull(propertyGetter.get(key), "this should only be called for existing getters");
        final Object result;
        try {
            result = getter.invokeExact(adapter, lvl, pos, key);
        } catch (LuaJavaError e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return convertToLuaObject(result);
    }

    public void set(AdapterBlockUD adapter, Level lvl, BlockPos pos, String key, LuaObject value) {
        // TODO
    }

    public LuaObject[] call(AdapterBlockUD adapter, Level lvl, BlockPos pos, LuaObject... args) {
        // TODO
        return null;
    }

    public static AdapterCompanion ofBlock(Class<? extends Block> block) {
        return ALL_COMPANIONS.getOrDefault(block, EMPTY_ADAPTER_COMPANION);
    }


    static {
        var loader = Thread.currentThread().getContextClassLoader();
        var myList = new ArrayList<Class<?>>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File file = new File(entry);
            if (file.isDirectory()) {
                scanDirectory(myList, file, file, loader);
            } else if (entry.endsWith(".jar")) {
                scanJar(myList, file, loader);
            }
        }
        // TODO build and verify maps
    }

    static void scanDirectory(List<Class<?>> collected, File root, File dir, ClassLoader loader) {
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

    static void scanJar(List<Class<?>> collected, File file, ClassLoader loader) {
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
                collected.add(adapterClazz);
            }
        } catch (TypeNotPresentException | ClassNotFoundException ignored) {
            // do nothing
        }
    }
}
