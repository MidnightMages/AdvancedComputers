package dev.asdf00.mc.advcomp.lua;

import dev.asdf00.mc.advcomp.utils.list.PrimitiveList;

public final class LuaStdOut {
    private static final int MAX_LINE_CNT = 128;

    private PrimitiveList<Character>[] buffer;
    private boolean wrapping;
    private int top;
    private int caret;

    private volatile PrintedCache cache = null;

    public LuaStdOut() {
        clear();
    }

    public void print(CharSequence out) {
        synchronized (buffer) {
            cache = null;
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
                        curLine = PrimitiveList.create(Character.class);
                        buffer[top] = curLine;
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
    }

    public void clear() {
        buffer = new PrimitiveList[MAX_LINE_CNT];
        buffer[0] = PrimitiveList.create(Character.class);
        wrapping = false;
        top = 0;
        caret = 0;
        cache = null;
    }

    /**
     * DO NOT MUTATE THE ARRAY RETURNED BY THIS METHOD! No defensive copy is made.
     */
    public String[] getLastLines(final int n) {
        if (n < 1) {
            return new String[0];
        }
        synchronized (buffer) {
            var c = cache;
            if (c == null || c.requestedLines != n) {
                // cache was invalidated -> rebuild output
                int avail = wrapping ? MAX_LINE_CNT : (top + 1);
                int toPrint = Math.min(n, avail);
                String[] lines = new String[toPrint];
                for (int i = ((top - (toPrint - 1)) + MAX_LINE_CNT) % MAX_LINE_CNT; i != top; i = (i + 1) % MAX_LINE_CNT) {
                    // TODO: properly support \t
                    lines[lines.length - toPrint] = String.valueOf(buffer[i].toCharArray()).replace("\t", "  ");
                    toPrint--;
                }
                // we always have at least 1 line to print which is at top
                lines[lines.length - 1] = String.valueOf(buffer[top].toCharArray());
                c = new PrintedCache(lines, n);
                cache = c;
            }
            return c.lines;
        }
    }

    private record PrintedCache(String[] lines, int requestedLines) {
    }
}
