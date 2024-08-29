package dev.asdf00.mc.advcomp.exceptions;

import java.util.function.Supplier;

public abstract class AdvancedComputersError extends RuntimeException {
    public AdvancedComputersError(String msg) {
        super("[AdvancedComputers] " + msg);
    }

    public static void AssertRuntime(boolean condition, String msg) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg);
        }
    }

    public static void AssertRuntime(boolean condition, String msg, Object o1) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.formatted(o1));
        }
    }

    public static void AssertRuntime(boolean condition, String msg, Object o1, Object o2) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.formatted(o1, o2));
        }
    }

    public static void AssertRuntime(boolean condition, String msg, Object o1, Object o2, Object o3) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.formatted(o1, o2, o3));
        }
    }

    public static void AssertRuntime(boolean condition, String msg, Object... args) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.formatted(args));
        }
    }

    public static void AssertRuntime(boolean condition, Supplier<String> msg) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.get());
        }
    }

    public static void AssertInit(boolean condition, String msg) {
        if (!condition) {
            throw new AdvancedComputersInitError(msg);
        }
    }

    public static void AssertInit(boolean condition, Supplier<String> msg) {
        if (!condition) {
            throw new AdvancedComputersInitError(msg.get());
        }
    }
}
