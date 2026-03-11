package dev.asdf00.mc.advcomp.lua.vm;

enum State {
    UNINITIALIZED(true, false, false),
    STARTING(false, true, true),
    RUNNING(false, true, false),
    SUSPENDED(false, false, true),
    CRASHED(true, false, false),
    ENDED(true, false, false);

    public final boolean resting;

    public final boolean killable;

    public final boolean startable;

    State(boolean resting, boolean killable, boolean startable) {
        this.resting = resting;
        this.killable = killable;
        this.startable = startable;
    }
}
