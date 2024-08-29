package dev.asdf00.mc.advcomp.lua;

import party.iroiro.luajava.JFunction;
import party.iroiro.luajava.Lua;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class LuaFunctionProxy implements JFunction {
    private static final Logger log = Logger.getLogger("LuaFuncProxy");

    private final LuaVirtualMachine lvm;
    private Consumer<Object[]> action;
    private Function<Object[], Object[]> function;

    public LuaFunctionProxy(LuaVirtualMachine lvm, Consumer<Object[]> a) {
        this.lvm = lvm;
        this.action = a;
    }

    public LuaFunctionProxy(LuaVirtualMachine lvm, Function<Object[], Object[]> f) {
        this.lvm = lvm;
        this.function = f;
    }

//    private Object popConverted(Lua L){
//         L.getTop();
//        L.pop(1);
//    }

    @Override
    public int __call(Lua L) {
        try {
            var args = LuaUtils.popAllArgs(L);
            if (action != null) {
                // no return value
                action.accept(args);
                return 0; // no return values, but also no error
            } else {
                // one or more return values
                var rv = function.apply(args);
                LuaUtils.pushArgs(L, rv);
                return rv.length;
            }
        } catch (AcLuaException err) {
            // pass exception into lua as error
            L.push(err.getMessage());
            return -1;
        } catch (LuaVirtualMachine.LvmKillException ex) {
            throw ex;
        } catch (Exception ex) {
            L.push("Internal error");
            log.warning(String.format("Function invocation produced unexpected error: %s", ex));
            return -1;
        }
    }
}
