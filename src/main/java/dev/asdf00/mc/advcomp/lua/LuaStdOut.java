package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.utils.list.PrimitiveList;

public final class LuaStdOut {
    private static final int MAX_LINE_CNT = 128;

    private PrimitiveList<Character>[] buffer;
    private boolean wrapping;
    private int top;
    private int caret;

    public LuaStdOut() {
        clear();
    }

    public void print(char[] out) {
        var curLine = buffer[top];
        for (var c : out) {
            switch (c) {
                case '\n' -> {
                    // newline
                    top++;
                    if (top >= MAX_LINE_CNT) {
                        top %= MAX_LINE_CNT;
                        wrapping = true;
                    }
                    buffer[top] = PrimitiveList.create(Character.class);
                    caret = 0;
                }
                case '\b' -> {
                    // backspace
                    if (caret > 0) {
                        curLine.remove(curLine.size() - 1);
                        caret--;
                    }
                }
                case '\r' -> caret = 0;
                default -> {
                    curLine.addChar(c);
                    caret++;
                }
            }
        }
    }

    public void clear() {
        buffer = new PrimitiveList[128];
        buffer[0] = PrimitiveList.create(Character.class);
        wrapping = false;
        top = 0;
        caret = 0;
    }
}
