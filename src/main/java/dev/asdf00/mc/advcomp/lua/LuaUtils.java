package dev.asdf00.mc.advcomp.lua;

import org.jetbrains.annotations.NotNull;
import party.iroiro.luajava.AbstractLua;
import party.iroiro.luajava.Lua;

public class LuaUtils
{
    public static Object[] PopArgs(Lua L, int argCnt)
    {
        var args = new Object[argCnt];
        for (int i = L.getTop(); i > L.getTop() - argCnt; i--) // pop in reverse order as the args are pushed in order
        {
            if (L.isFunction(i) && false)
                args[i - 1] = L.toObject(i, Runnable.class);
            else
                args[i - 1] = L.toObject(i);
        }
        L.pop(argCnt);
        return args;
    }

    public static Object[] PopAllArgs(Lua L)
    {
        int argCount = L.getTop();
        return PopArgs(L, argCount);
    }

    /**
     * MAKE SURE TO PROPERLY RETURN THE CORRECT ARG COUNT WHEN USING THIS FUNCTION
     * @param L
     * @param args
     */
    public static void PushArgs(Lua L, Object @NotNull [] args)
    {
        for (Object o : args)
            L.push(o, Lua.Conversion.SEMI);
    }
}
