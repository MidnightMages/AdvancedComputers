package dev.asdf00.mc.advcomp.lua.vm;

import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlockEntity;

class VmRunStateHandler {
    private final ComputerBlockEntity computer;
    private volatile State state = State.UNINITIALIZED;

    VmRunStateHandler(ComputerBlockEntity computer) {
        this.computer = computer;
    }

    State getState() {
        return state;
    }

    synchronized void initializing() {
        state = State.STARTING;
        computer.setRunState(ComputerBlock.ComputerRunState.RUNNING);
        notifyAll();
    }

    synchronized void startRun() {
        state = State.RUNNING;
        computer.setRunState(ComputerBlock.ComputerRunState.RUNNING);
        notifyAll();
    }

    synchronized void stop() {
        state = State.ENDED;
        computer.setRunState(ComputerBlock.ComputerRunState.STOPPED);
        notifyAll();
    }

    synchronized void suspend() {
        state = State.SUSPENDED;
        computer.setRunState(ComputerBlock.ComputerRunState.RUNNING);
        notifyAll();
    }

    synchronized void crash() {
        state = State.CRASHED;
        computer.setRunState(ComputerBlock.ComputerRunState.CRASHED);
        notifyAll();
    }

    synchronized boolean suspendAndWait(Runnable suspendingAction) throws InterruptedException {
        if (state == State.RUNNING) {
            suspendingAction.run();
            wait();
        }
        return state == State.SUSPENDED;
    }

    @Override
    public String toString() {
        return "VmRunStateHandler{" +
                "computer=" + computer +
                ", state=" + state +
                '}';
    }
}
