package dev.asdf00.mc.advcomp.exceptions;

import java.util.function.Supplier;

public abstract class AdvancedComputersError extends RuntimeException {
    protected AdvancedComputersError(String msg) {
        super("[AdvancedComputers] " + msg);
    }

    public static void Assert(boolean condition, String msg) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg);
        }
    }

    public static void Assert(boolean condition, String msg, Object o1) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.formatted(o1));
        }
    }

    public static void Assert(boolean condition, String msg, Object o1, Object o2) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.formatted(o1, o2));
        }
    }

    public static void Assert(boolean condition, String msg, Object o1, Object o2, Object o3) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.formatted(o1, o2, o3));
        }
    }

    public static void Assert(boolean condition, String msg, Object... args) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.formatted(args));
        }
    }

    public static void Assert(boolean condition, Supplier<String> msg) {
        if (!condition) {
            throw new AdvancedComputersRuntimeError(msg.get());
        }
    }

    public static AdvancedComputersError shouldNotReach(String msg, Object... args) {
        throw new AdvancedComputersRuntimeError(msg.formatted(args));
    }

    public static AdvancedComputersError shouldNotReach() {
        throw new AdvancedComputersRuntimeError("This statement should not be reached");
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
