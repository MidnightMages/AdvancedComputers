package dev.asdf00.mc.advcomp.exceptions;

public class AdvancedComputersRuntimeError extends AdvancedComputersError {
    public AdvancedComputersRuntimeError(String msg) {
        super("Runtime error: " + msg);
    }
}
