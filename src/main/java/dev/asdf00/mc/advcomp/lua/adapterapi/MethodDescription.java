package dev.asdf00.mc.advcomp.lua.adapterapi;

import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.lang.invoke.MethodHandle;
import java.util.function.Function;

record MethodDescription(MethodHandle handle, Function<LuaObject, Object>[] argTranslators,
                         Function<Object, LuaObject> resultTranslator) {
}
