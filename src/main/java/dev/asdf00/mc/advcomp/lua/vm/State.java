package dev.asdf00.mc.advcomp.lua.vm;

enum State {
    UNINITIALIZED(true, false),
    STARTING(false, true),
    RUNNING(false, true),
    SUSPENDED(false, false),
    CRASHED(true, false),
    ENDED(true, false);

    public final boolean resting;

    public final boolean killable;

    State(boolean resting, boolean killable) {
        this.resting = resting;
        this.killable = killable;
    }
}
