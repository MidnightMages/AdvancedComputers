package dev.asdf00.mc.advcomp.lua.vm;

import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;

import java.util.HashSet;
import java.util.function.Consumer;

public class VmRunStateHandler {
    private final ComputerBlockEntity computer;
    private volatile State state = State.UNINITIALIZED;
    private final HashSet<Consumer<State>> onStateChangedCallbacks = new HashSet<>();

    VmRunStateHandler(ComputerBlockEntity computer) {
        this.computer = computer;
    }

    State getState() {
        return state;
    }

    synchronized void initialize() {
        state = State.STARTING;
        computer.setRunState(ComputerBlock.ComputerRunState.RUNNING);
        notifyAll();
        runStateChangedCallbacks();
    }

    synchronized void startRun() {
        state = State.RUNNING;
        computer.setRunState(ComputerBlock.ComputerRunState.RUNNING);
        notifyAll();
        runStateChangedCallbacks();
    }

    synchronized void stop() {
        state = State.ENDED;
        computer.setRunState(ComputerBlock.ComputerRunState.STOPPED);
        notifyAll();
        runStateChangedCallbacks();
    }

    synchronized void suspend() {
        state = State.SUSPENDED;
        computer.setRunState(ComputerBlock.ComputerRunState.RUNNING);
        notifyAll();
        runStateChangedCallbacks();
    }

    synchronized void crash() {
        state = State.CRASHED;
        computer.setRunState(ComputerBlock.ComputerRunState.CRASHED);
        notifyAll();
        runStateChangedCallbacks();
    }

    synchronized boolean suspendAndWait(Runnable suspendingAction) throws InterruptedException {
        if (state == State.RUNNING) {
            suspendingAction.run();
            wait();
        }
        return state == State.SUSPENDED;
    }

    private synchronized void runStateChangedCallbacks() {
        final var currState = state;
        for (var cb : onStateChangedCallbacks)
            cb.accept(currState);
    }

    public synchronized void subscribeToStateChange(Consumer<State> onStateChanged) {
        onStateChangedCallbacks.add(onStateChanged);
    }


    @Override
    public String toString() {
        return "VmRunStateHandler{" +
               "computer=" + computer +
               ", state=" + state +
               '}';
    }
}
