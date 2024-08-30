package dev.asdf00.mc.advcomp.exceptions;

public class ACsRuntimeError extends ACError {
    public ACsRuntimeError(String msg) {
        super("Runtime error: " + msg);
    }
}
