package dev.asdf00.mc.advcomp.types;

public class RuntimeAssert {
    public static void RuntimeAssert(boolean ok, String message) {
        if (!ok)
            throw new IllegalStateException("Assertion failed: " + message);
    }

    public static void RuntimeAssert(boolean ok, String message, Object arg1) {
        if (!ok)
            throw new IllegalStateException("Assertion failed: " + message.formatted(arg1));
    }

    public static void RuntimeAssert(boolean ok, String message, Object arg1, Object arg2) {
        if (!ok)
            throw new IllegalStateException("Assertion failed: " + message.formatted(arg1, arg2));
    }
}
