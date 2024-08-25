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
