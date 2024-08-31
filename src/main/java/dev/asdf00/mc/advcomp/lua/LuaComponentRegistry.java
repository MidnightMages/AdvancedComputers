package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.api.AcLuaFunction;

import java.lang.reflect.Method;
import java.util.HashMap;

public class LuaComponentRegistry {
    public LuaComponentRegistry() {
    }

    public static final LuaComponentRegistry INSTANCE = new LuaComponentRegistry(); // TODO maybe move this to a per-save sortof thing in case we load a diff map
    private final HashMap<IAcComponent, LuaFunctionGroup> registeredComponents = new HashMap<>();

    public void RegisterAcComponent(IAcComponent comp){
        var group = new LuaFunctionGroup(comp);
        group.RegisterMethods(comp);
        registeredComponents.put(comp, group);
        comp.onRegister(group);
    }

    public void UnregisterAcComponent(IAcComponent comp){
        var group = registeredComponents.get(comp);
        if(group != null) {
            comp.onDeregister(group);
            registeredComponents.remove(comp);
        }
    }

    public static final class LuaFunctionGroup {
        public IAcComponent owner;
        public HashMap<String, LuaFunctionContainer> registeredFunctions;

        public LuaFunctionGroup(IAcComponent owner) {
            this.owner = owner;
        }

//        public void AddFunction(LuaFunctionContainer c){
//            registeredFunctions.put(c.annotation.functionName(), c);
//        }

        public void RegisterMethods(Object luaFunctionContainer) {
            var methods = luaFunctionContainer.getClass().getDeclaredMethods();
            for (var m : methods) {
                var a = m.getAnnotation(AcLuaFunction.class);
                if (a != null) {
                    var funcName = a.functionName();
                    var existing = registeredFunctions.getOrDefault(funcName, null);
                    if (existing != null)
                        throw new RuntimeException("Lua function with name %s is already registered. There were at least two definitions: '%s' and '%s'"
                                .formatted(funcName, existing.m().getName(), m.getName()));
                    registeredFunctions.put(funcName, new LuaFunctionContainer(a, m));
                }
            }
        }

        public record LuaFunctionContainer(AcLuaFunction annotation, Method m){

        }

    }
}
