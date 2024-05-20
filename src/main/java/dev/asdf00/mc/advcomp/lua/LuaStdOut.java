package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.utils.list.PrimitiveList;

public final class LuaStdOut {
    private static final int MAX_LINE_CNT = 128;

    private PrimitiveList<Character>[] buffer;
    private boolean wrapping;
    private int top;
    private int caret;

    private String[] lastPrinted;

    public LuaStdOut() {
        clear();
    }

    public void print(CharSequence out) {
        lastPrinted = null;
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
            int avail = wrapping ? MAX_LINE_CNT : top;
            int toPrint = n;
            if (avail < n) {
                for (int i = 0; i < n - avail; i++) {
                    lines[i] = "";
                }
                toPrint = avail;
            }
            for (int i = ((top - toPrint) + MAX_LINE_CNT) % MAX_LINE_CNT; i != top; i = (i + 1) % MAX_LINE_CNT) {
                lines[n - toPrint] = String.valueOf(buffer[i].toCharArray());
                toPrint--;
            }
            lastPrinted = lines;
        }
        return lastPrinted;
    }
}
