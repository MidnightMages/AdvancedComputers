package dev.asdf00.mc.advcomp.exceptions;

/**
 * This exception occurs while initializing the Advanced Computers mod and
 * is not intended to be caught but rather crash initialization.
 */
public class ACInitError extends ACError {
    public ACInitError(String msg) {
        super("Initialization Error: " + msg);
    }
}
