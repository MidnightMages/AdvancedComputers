package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.utils.list.PrimitiveList;

public final class LuaStdOut {
    private static final int MAX_LINE_CNT = 128;

    private PrimitiveList<Character>[] buffer;
    private boolean wrapping;
    private int top;
    private int caret;

    private String lastPrinted;
    private int linesPrinted;

    public LuaStdOut() {
        clear();
    }

    public void print(char[] out) {
        linesPrinted = -1;
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
        buffer = new PrimitiveList[MAX_LINE_CNT];
        buffer[0] = PrimitiveList.create(Character.class);
        wrapping = false;
        top = 0;
        caret = 0;
        linesPrinted = -1;
    }

    public String getLastLines(int n) {
        if (linesPrinted != n) {
            var sb = new StringBuilder();
            int avail = wrapping ? MAX_LINE_CNT : top;
            if (avail < n) {
                sb.append("\n".repeat(n - avail));
                n = avail;
            }
            boolean first = true;
            for (int i = ((top - n) + MAX_LINE_CNT) % MAX_LINE_CNT; i != top; i = (i + 1) % MAX_LINE_CNT) {
                if (first) {
                    first = false;
                } else {
                    sb.append('\n');
                }
                sb.append(buffer[i].toCharArray());
            }
            lastPrinted = sb.toString();
            linesPrinted = n;
        }
        return lastPrinted;
    }
}
