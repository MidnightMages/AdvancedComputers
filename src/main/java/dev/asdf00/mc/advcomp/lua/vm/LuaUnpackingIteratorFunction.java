package dev.asdf00.mc.advcomp.lua.vm;

import dev.asdf00.jluavm.api.functions.ApiFunctionRegistry;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.LuaJavaApiFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;

final class LuaUnpackingIteratorFunction extends LuaJavaApiFunction {
    public LuaUnpackingIteratorFunction(ApiFunctionRegistry registry, LuaObject tableToIterateOver, LuaObject[] closures) {
        super(registry, tableToIterateOver, closures);
    }

    @Override
    @SuppressWarnings("UnnecessaryLocalVariable")
    public void invoke(LuaVM_RT vm, LuaObject[] stackFrame, int resume, LuaObject[] expressionStack, LuaObject[] returned) {
        LuaObject tableToIterateOver = _ENV;
        LuaObject nextIndex = closures[0];
        LuaObject nextArrayToReturn = tableToIterateOver.get(nextIndex);
        if (nextArrayToReturn.isNil()) {
            vm.returnValue(LuaObject.NIL);
            return;
        }
        if (!nextArrayToReturn.isArray()) {
            throw new LuaJavaError("Internal iterator value was not unpackable!");
        }
        closures[0] = nextIndex.add(LuaObject.of(1));
        vm.returnValue(nextArrayToReturn.asArray());
    }

    @Override
    public int getMaxLocalsSize() {
        return 0;
    }

    @Override
    public int getArgCount() {
        return 0;
    }

    @Override
    public boolean hasParamsArg() {
        return false;
    }
}
