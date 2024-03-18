package dev.asdf00.mc.advcomp.lua;

import party.iroiro.luajava.JFunction;
import party.iroiro.luajava.Lua;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class LuaFunctionProxy implements JFunction
{
    private static final Logger log = Logger.getLogger("LuaFuncProxy");

    private Consumer<Object[]> action;
    private Function<Object[], Object[]> function;

    public LuaFunctionProxy(Consumer<Object[]> a)
    {
        this.action = a;
    }

    public LuaFunctionProxy(Function<Object[], Object[]> f)
    {
        this.function = f;
    }

//    private Object popConverted(Lua L){
//         L.getTop();
//        L.pop(1);
//    }

    @Override
    public int __call(Lua L)
    {
        try
        {
            var argCnt = L.getTop();
            var args = new Object[argCnt];
            for (int i = L.getTop(); i > L.getTop()-argCnt; i--) // pop in reverse order as the args are pushed in order
            {
                args[i-1] = L.toObject(i);
            }
            L.pop(argCnt);

            try
            {
                if (action != null) // no return value
                {
                    action.accept(args);
                    return 0; // no return values, but also no error
                }
                else // one or more return values
                {
                    var rv = function.apply(args);
                    for (Object o : rv)
                    {
                        L.push(o, Lua.Conversion.SEMI);
                    }
                    return rv.length;
                }
            }
            catch (Exception ex)
            {
                L.push("Internal error");
                log.warning(String.format("Function invocation produced unexpected error: %s", ex));
                return -1;
            }
        }
        catch (Exception ex)
        {
            System.out.printf("Caught uncaught exception: %s", ex);
        }
        return 0;
    }
}
