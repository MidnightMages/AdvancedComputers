package dev.asdf00.mc.advcomp.exceptions;

import java.util.function.Supplier;

public abstract class ACError extends RuntimeException {
    protected ACError(String msg) {
        super("[AdvancedComputers] " + msg);
    }

    public static void Assert(boolean condition, String msg) {
        if (!condition) {
            throw new ACsRuntimeError(msg);
        }
    }

    public static void Assert(boolean condition, String msg, Object o1) {
        if (!condition) {
            throw new ACsRuntimeError(msg.formatted(o1));
        }
    }

    public static void Assert(boolean condition, String msg, Object o1, Object o2) {
        if (!condition) {
            throw new ACsRuntimeError(msg.formatted(o1, o2));
        }
    }

    public static void Assert(boolean condition, String msg, Object o1, Object o2, Object o3) {
        if (!condition) {
            throw new ACsRuntimeError(msg.formatted(o1, o2, o3));
        }
    }

    public static void Assert(boolean condition, String msg, Object... args) {
        if (!condition) {
            throw new ACsRuntimeError(msg.formatted(args));
        }
    }

    public static void Assert(boolean condition, Supplier<String> msg) {
        if (!condition) {
            throw new ACsRuntimeError(msg.get());
        }
    }

    public static ACError shouldNotReach(String msg, Object... args) {
        throw new ACsRuntimeError(msg.formatted(args));
    }

    public static ACError shouldNotReach() {
        throw new ACsRuntimeError("This statement should not be reached");
    }


    public static void AssertInit(boolean condition, String msg) {
        if (!condition) {
            throw new ACInitError(msg);
        }
    }

    public static void AssertInit(boolean condition, Supplier<String> msg) {
        if (!condition) {
            throw new ACInitError(msg.get());
        }
    }
}
