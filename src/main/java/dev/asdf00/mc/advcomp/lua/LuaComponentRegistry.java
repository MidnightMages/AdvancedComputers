package dev.asdf00.mc.advcomp.lua;

import java.lang.reflect.Method;
import java.util.HashMap;

public class LuaComponentRegistry {
    public LuaComponentRegistry() {
    }

    private final HashMap<String, LuaFunctionContainer> registeredFuncs = new HashMap<>();

    public void RegisterMethods(Object luaFunctionContainer) {
        var methods = luaFunctionContainer.getClass().getDeclaredMethods();
        for (var m : methods) {
            var a = m.getAnnotation(AcLuaFunction.class);
            if (a != null) {
                var funcName = a.functionName();
                var existing = registeredFuncs.getOrDefault(funcName, null);
                if (existing != null)
                    throw new RuntimeException("Lua function with name %s is already registered. There were at least two definitions: '%s' and '%s'"
                            .formatted(funcName, existing.m().getName(), m.getName()));
                registeredFuncs.put(funcName, new LuaFunctionContainer(a, m));
            }
        }
    }

    public record LuaFunctionContainer(AcLuaFunction annotation, Method m) {
    }
}
