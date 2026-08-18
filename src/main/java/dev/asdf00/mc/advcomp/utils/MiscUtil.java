package dev.asdf00.mc.advcomp.utils;

import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class MiscUtil {
    // src: https://stackoverflow.com/questions/29076439/java-8-copy-directory-recursively/60621544#60621544
    public static void copyFolderRecursively(Path source, Path target, CopyOption... options) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()), options);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static String AcIpToString(int acIpAddress) {
        var rv = new StringBuilder();
        var hexRepr = String.format("%08X", acIpAddress);
        for (int i = 0; i < 4; i++) {
            if (i > 0)
                rv.append('.');
            rv.append(hexRepr, i * 2, i * 2 + 2);
        }
        return rv.toString();
    }

    public static Class<?> boxThatType(Class<?> type) {
        if (Object.class.isAssignableFrom(type)) {
            return type;
        } else if (byte.class.equals(type)) {
            return Byte.class;
        } else if (short.class.equals(type)) {
            return Short.class;
        } else if (int.class.equals(type)) {
            return Integer.class;
        } else if (long.class.equals(type)) {
            return Long.class;
        } else if (float.class.equals(type)) {
            return Float.class;
        } else if (double.class.equals(type)) {
            return Double.class;
        } else if (boolean.class.equals(type)) {
            return Boolean.class;
        } else if (char.class.equals(type)) {
            return Character.class;
        } else if (void.class.equals(type)) {
            return type;
        }
        throw new IllegalArgumentException("Failed to box possibly primitive " + type.getName());
    }

    public static LuaObject convertToLuaObject(Object value) {
        if (value == null) {
            return LuaObject.nil();
        } else if (value instanceof LuaObject lo) {
            return lo;
        } else if (value instanceof Number num) {
            if (value instanceof Float || value instanceof Double) {
                return LuaObject.of(num.doubleValue());
            } else {
                return LuaObject.of(num.longValue());
            }
        } else if (value instanceof Boolean b) {
            return LuaObject.of(b);
        } else if (value instanceof Character c) {
            return LuaObject.of(c);
        } else if (value instanceof String s) {
            return LuaObject.of(s);
        } else if (LuaUserData.class.isAssignableFrom(value.getClass())) {
            return LuaObject.of((LuaUserData) value);
        } else {
            throw new IllegalArgumentException("cannot convert to LuaObject: " + value);
        }
    }
}
