package dev.asdf00.mc.advcomp.utils;

import dev.asdf00.mc.advcomp.lua.LuaMain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ResourceUtil {
    public static String loadLuaScript(String name) {
        try (var stream = LuaMain.class.getClassLoader().getResourceAsStream("assets/advancedcomputers/lua/" + name)) {
            Objects.requireNonNull(stream, "Error reading resource '%s'".formatted(name));
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Resource '%s' not found!".formatted(name));
        }
    }
}
