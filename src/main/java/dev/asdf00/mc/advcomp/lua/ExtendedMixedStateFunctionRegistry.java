package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.jluavm.api.functions.MixedStateFunctionRegistry;
import dev.asdf00.jluavm.runtime.types.LuaObject;

public class ExtendedMixedStateFunctionRegistry extends MixedStateFunctionRegistry {
    public ExtendedMixedStateFunctionRegistry(String id) {
        super(id);
    }

    public void addFunctionsToTable(LuaObject env) {
        // add all noninternal functions
        // all these functions are assumed to be stateless
        for (String fName : this.getAllNames()) {
            if (fName.charAt(0) == '$') {
                // internal function, do not add to _G
                continue;
            }
            if (fName.indexOf('.') < 0) {
                // top level
                env.set(fName, LuaObject.of(this.getFunction(fName)));
            } else {
                var path = fName.split("\\.");
                assert path.length == 2 : "only ever expected tbl.funcname, got: " + fName;
                var tbl = env.get(path[0]);
                if (tbl.isNil()) {
                    tbl = LuaObject.table();
                    env.set(path[0], tbl);
                }
                tbl.set(path[1], LuaObject.of(this.getFunction(fName)));
            }
        }
    }
}