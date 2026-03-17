package dev.asdf00.mc.advcomp.exceptions;

public class ACRuntimeError extends ACError {
    public ACRuntimeError(String msg) {
        super("Runtime error: " + msg);
    }
}
