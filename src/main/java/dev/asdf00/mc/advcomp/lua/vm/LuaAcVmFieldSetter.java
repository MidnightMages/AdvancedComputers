package dev.asdf00.mc.advcomp.lua.vm;

import dev.asdf00.jluavm.api.functions.ApiFunctionRegistry;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.LuaJavaApiFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.runtime.utils.Singletons;
import dev.asdf00.mc.advcomp.lua.components.ComputerUD;

import java.util.function.BiConsumer;

final class LuaAcVmFieldSetter extends LuaJavaApiFunction {
    private final BiConsumer<LuaVirtualMachine, LuaObject> fieldSetter;

    public LuaAcVmFieldSetter(ApiFunctionRegistry registry, LuaObject _ENV, BiConsumer<LuaVirtualMachine, LuaObject> fieldSetter) {
        super(registry, _ENV, Singletons.EMPTY_LUA_OBJ_ARRAY);
        this.fieldSetter = fieldSetter;
    }

    @Override
    public void invoke(LuaVM_RT vm, LuaObject[] stackFrame, int resume, LuaObject[] expressionStack, LuaObject[] returned) {
        vm.registerLocals(1);
        LuaVirtualMachine acVm = ((ComputerUD) _ENV.refVal).getAcVm();
        fieldSetter.accept(acVm, stackFrame[0]);
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
