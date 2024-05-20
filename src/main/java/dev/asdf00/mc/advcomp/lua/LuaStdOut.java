package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.utils.list.PrimitiveList;

public final class LuaStdOut {
    private static final int MAX_LINE_CNT = 128;

    private volatile PrimitiveList<Character>[] buffer;
    private volatile boolean wrapping;
    private volatile int top;
    private int caret;

    private volatile String[] lastPrinted;

    public LuaStdOut() {
        clear();
    }

    public void print(CharSequence out) {
        var curLine = buffer[top];
        for (int i = 0; i < out.length(); i++) {
            var c = out.charAt(i);
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
        lastPrinted = null;
    }

    public void clear() {
        buffer = new PrimitiveList[MAX_LINE_CNT];
        buffer[0] = PrimitiveList.create(Character.class);
        wrapping = false;
        top = 0;
        caret = 0;
        lastPrinted = null;
    }

    /**
     * DO NOT MUTATE THE ARRAY RETURNED BY THIS METHOD!
     */
    public String[] getLastLines(final int n) {
        if (lastPrinted == null || lastPrinted.length != n) {
            String[] lines = new String[n];
            int avail = wrapping ? MAX_LINE_CNT : top + 1;
            int toPrint = n;
            if (avail < n) {
                for (int i = 0; i < n - avail; i++) {
                    lines[i] = "";
                }
                toPrint = avail;
            }
            for (int i = ((top - (toPrint - 1)) + MAX_LINE_CNT) % MAX_LINE_CNT; i != top; i = (i + 1) % MAX_LINE_CNT) {
                lines[n - toPrint] = String.valueOf(buffer[i].toCharArray());
                toPrint--;
            }
            // we always have at least 1 line to print which is at top
            lines[n - 1] = String.valueOf(buffer[top].toCharArray());
            lastPrinted = lines;
        }
        return lastPrinted;
    }
}
