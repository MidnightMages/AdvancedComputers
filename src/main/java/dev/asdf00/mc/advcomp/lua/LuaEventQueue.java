package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class LuaEventQueue {
    private final ConcurrentLinkedQueue<LuaObject[]> backing = new ConcurrentLinkedQueue<>();

    public LuaObject[] getQueuedEventOrNull() {
        return backing.poll();
    }

    public void addRaw(String eventName, LuaObject... args) {
        backing.add(Stream.concat(Stream.of(LuaObject.of(eventName)), Arrays.stream(args)).toArray(LuaObject[]::new));
    }

    public void addKeyPressed(KeyEvent keyEvent) {
        addRaw("keyPressed", LuaObject.of(keyEvent.getExtendedKeyCode()));
    }

    public void addKeyReleased(KeyEvent keyEvent) {
        addRaw("keyReleased", LuaObject.of(keyEvent.getExtendedKeyCode()));
    }

    public void addKeyTyped(KeyEvent keyEvent) {
        addRaw("keyTyped", LuaObject.of(Character.toString(keyEvent.getKeyChar())));
    }

    public void addRequestShutdown() {
        addRaw("shutdown");
    }

    public void addComponentAdded(LuaUserDataComponent comp) {
        addRaw("componentAdded", LuaObject.of(comp.getComponentType()), LuaObject.of(comp));
    }

    public void addComponentRemoved(LuaUserData comp) {
        addRaw("componentRemoved", LuaObject.of(comp));
    }
}
