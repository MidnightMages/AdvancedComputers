package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaUserData;

public interface LuaUserDataComponent extends LuaUserData {
    String getComponentType();
}
