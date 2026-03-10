package dev.asdf00.mc.advcomp.lua.vm;

import dev.asdf00.jluavm.api.functions.ApiFunctionRegistry;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.LuaJavaApiFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.runtime.utils.Singletons;

final class LuaSleepFunction extends LuaJavaApiFunction {
    public LuaSleepFunction(ApiFunctionRegistry registry, LuaObject timeTracker) {
        super(registry, timeTracker, Singletons.EMPTY_LUA_OBJ_ARRAY);
    }

    @Override
    public void invoke(LuaVM_RT vm, LuaObject[] stackFrame, int resume, LuaObject[] expressionStack, LuaObject[] returned) {
        vm.registerLocals(1);
        try {
            var argument = stackFrame[0];
            if (!argument.isNumberCoercible()) {
                throw new LuaJavaError("expected number as first and only argument");
            }
            LuaSafepointHandler safepointHandler = ((LuaSafepointHandler) _ENV.refVal);
            long sleepBegunAt = System.nanoTime();
            safepointHandler.beforeLongLuaOperation();
            Thread.sleep((int) (argument.asDouble() * 1000));
            long sleptForNs = Math.max(0, System.nanoTime() - sleepBegunAt);
            safepointHandler.refundNanos(sleptForNs);
        } catch (InterruptedException e) {
            // premature exit, preserve interrupted state
            Thread.currentThread().interrupt();
        }
        vm.returnValue();
    }

    @Override
    public int getMaxLocalsSize() {
        return 1;
    }

    @Override
    public int getArgCount() {
        return 1;
    }

    @Override
    public boolean hasParamsArg() {
        return false;
    }
}
