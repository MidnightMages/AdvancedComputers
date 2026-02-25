package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.runtime.utils.UDTranslators;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.blocks.screen.ScreenBlockEntity;

import java.util.*;

public final class TextBufferUD implements LuaUserData {

    @LuaExposed(LuaExposed.Policy.READ)
    public int width;
    @LuaExposed(LuaExposed.Policy.READ)
    public int height;

    private final GpuUD gpuUD;
    public boolean isFreed = false;

    /**
     * The bits in the foreground color are inverted for performance reasons.
     */
    private byte[] foregroundColor;
    private byte[] backgroundColor;
    private char[] text;
    private int lStart;

    public String getTextAsString() {
        var guiTextSb = new StringBuilder();
        for (int line = 0; line < height; line++) {
            int actualLine = (lStart + line) % height;
            String lineText = String.valueOf(text, actualLine * width, width)
                    .replaceAll("[\0-\\x19]", " ")
                    .replace((char) -1, ' ').stripTrailing() + "\n";
            guiTextSb.append(lineText);
        }
        return guiTextSb.toString();
    }

    public TextBufferUD(int width, int height, GpuUD gpuUD) {
        this.width = width;
        this.height = height;
        this.gpuUD = gpuUD;
        this.foregroundColor = new byte[width * height];
        this.backgroundColor = new byte[width * height];
        this.text = new char[width * height];
        this.lStart = 0;
    }

    @LuaCallable
    public void free() {
        gpuUD.freeBuffer(this);
        isFreed = true;
        // now we are safe to drop the memory backing this buffer.
        width = height = 0;
        backgroundColor = null;
        foregroundColor = null;
        text = null;
        // clear screens on client
        gpuUD.acVm.dirtyBuffer(this);
    }

    @LuaCallable
    public byte getFg(int x, int y) {
        return foregroundColor[calcIdx(x, y)];
    }

    @LuaCallable
    public byte getBg(int x, int y) {
        return backgroundColor[calcIdx(x, y)];
    }

    @LuaCallable
    public char getText(int x, int y) {
        return text[calcIdx(x, y)];
    }

    @LuaCallable
    public void set(int x, int y, LuaObject lval, LuaObject lfg, LuaObject lbg) {
        int idx = calcIdx(x, y);
        char val = lval.isNil() ? (char) -1 : UDTranslators.lo2c(lval);
        byte fg = lfg.isNil() ? -1 : UDTranslators.lo2b(lfg);
        byte bg = lbg.isNil() ? -1 : UDTranslators.lo2b(lbg);
        if (!lval.isNil()) {
            text[idx] = val;
        }
        if (lfg.isNil()) {
            foregroundColor[idx] = (byte) ~fg;
        }
        if (lbg.isNil()) {
            backgroundColor[idx] = bg;
        }
        gpuUD.acVm.dirtyBuffer(this);
    }

    @LuaCallable
    public void rotRows(int cnt) {
        luaGuarantee(cnt < height && cnt >= -height, "line out of bounds");
        cnt = cnt < 0 ? height - cnt : cnt;
        lStart = (lStart + cnt) % height;
        gpuUD.acVm.dirtyBuffer(this);
    }

    @LuaCallable
    public void clearRow(int line) {
        luaGuarantee(line < height && line >= -height, "line out of bounds");
        line = line < 0 ? height - line : line;
        int target = ((lStart + line) % height) * width;
        Arrays.fill(text, target, target + (width - 1), '\0');
        Arrays.fill(foregroundColor, target, target + (width - 1), (byte) 0);
        Arrays.fill(backgroundColor, target, target + (width - 1), (byte) 0);
        gpuUD.acVm.dirtyBuffer(this);
    }

    @LuaCallable
    public void newline() {
        clearRow(0);
        rotRows(1);
    }

    @LuaCallable
    public LuaObject[] pasteText(String uText) {
        return pasteText(0, 0, uText);
    }

    @LuaCallable
    public LuaObject[] pasteText(int x, int y, String uText) {
        return pasteText(x, y, PasteMode.STOP, uText);
    }

    @LuaCallable
    public LuaObject[] pasteText(int x, int y, PasteMode mode, String uText) {
        luaGuarantee(x < width && x >= -width, "x out of bounds");
        luaGuarantee(y < height && y >= -height, "y out of bounds");
        x = x < 0 ? width - x : x;
        y = y < 0 ? height - y : y;

        int printed = 0;
        while (printed < uText.length()) {

            // paste as much text into the current line as there is space in the buffer
            boolean endedOnNewLine = false;
            lineLoop:
            while (printed < uText.length() && x < width) {
                char toPrint = uText.charAt(printed++);
                switch (toPrint) {
                    case '\n' -> {
                        if (mode.clearRestOnNewLine) {
                            // we erase the rest of the line
                            while (x < width) {
                                text[rawCalcIdx(x++, y)] = '\0';
                            }
                        }
                        endedOnNewLine = true;
                        break lineLoop;
                    }
                    case '\r' -> {
                        // carriage return jumps back to the start of the line
                        x = 0;
                    }
                    case '\t' -> {
                        text[rawCalcIdx(x++, y)] = ' ';
                        while (x % 4 != 0 && x < width) {
                            text[rawCalcIdx(x++, y)] = ' ';
                        }
                    }
                    case '\b' -> {
                        if (x > 0) {
                            text[rawCalcIdx(--x, y)] = '\0';
                        }
                    }
                    default -> {
                        text[rawCalcIdx(x++, y)] = toPrint;
                    }
                }
            }

            // handle transition to next line

            if (mode.lineMode == PasteMode.LineMode.SINGLE) {
                // we do not care about other lines, we are done here
                break;
            }

            if (endedOnNewLine) {
                y = moveCursorToNewLine(mode.lineMode, y);
                x = 0;
            } else if (printed < uText.length()) {
                // not ended on \n but there is still stuff to print
                if (uText.charAt(printed) == '\n') {
                    // this is a perfect line ending
                    printed++;
                    y = moveCursorToNewLine(mode.lineMode, y);
                    x = 0;
                } else {
                    // there is still stuff left in the given line, possibly clip
                    if (!mode.spillOnBufferLineEnd) {
                        // we need to clip the rest of the line until we run into a '\n'
                        while (printed < uText.length() && uText.charAt(printed++) != '\n') ;
                        // here we either just passed a '\n' or we are at the end of the text
                        if (printed != uText.length()) {
                            // if we encountered a \n we need to update x and y
                            y = moveCursorToNewLine(mode.lineMode, y);
                            x = 0;
                        }
                    } else {
                        // stuff is spilling into the next line
                        y = moveCursorToNewLine(mode.lineMode, y);
                        x = 0;
                    }
                }
            }

            if (mode.lineMode == PasteMode.LineMode.MULTI) {
                // we might now be at the bottom end of the buffer
                if (y == height) {
                    break;
                }
            }
        }
        gpuUD.acVm.dirtyBuffer(this);
        return new LuaObject[]{LuaObject.of(x), LuaObject.of(y)};
    }

    private int moveCursorToNewLine(PasteMode.LineMode lineMode, int y) {
        if (lineMode == PasteMode.LineMode.SCROLL && y == height - 1) {
            // if we are in the bottom line of the buffer, scroll the buffer for the newline
            newline();
            return y;
        } else {
            return y + 1;
        }
    }

    private int calcIdx(int x, int y) {
        luaGuarantee(x < width && x >= -width, "x out of bounds");
        luaGuarantee(y < height && y >= -height, "y out of bounds");
        return unsafeCalcIdx(x, y);
    }

    private int unsafeCalcIdx(int x, int y) {
        x = x < 0 ? width - x : x;
        y = y < 0 ? height - y : y;
        return rawCalcIdx(x, y);
    }

    private int rawCalcIdx(int x, int y) {
        return ((lStart + y) % height) * width + x;
    }

    private static void luaGuarantee(boolean condition, String msg) {
        if (!condition) {
            throw new LuaJavaError(msg);
        }
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        throw new UnsupportedOperationException("serialization not implemented");
    }

    @LuaDeserializer
    public static TextBufferUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        // TODO actually provide serializaion
        return null;
    }

    @Override
    public boolean luaFieldGuard(LuaObject key, LuaObject value) {
        return !isFreed;
    }

    @Override
    public boolean luaCallGuard(String name, LuaObject[] arguments) {
        return !isFreed;
    }

    public Set<ScreenBlockEntity> getAssociatedScreens() {
        Set<ScreenBlockEntity> screens = gpuUD.screenBufferMap.getBack(this);
        return screens == null ? Set.of() : screens;
    }

    public enum PasteMode {
        STOP(LineMode.SINGLE, false, false),
        STOP_CLEAR(LineMode.SINGLE, true, false),
        FILL_CLIP(LineMode.MULTI, false, false),
        FILL_CLIP_CLEAR(LineMode.MULTI, true, false),
        FILL_SPILL(LineMode.MULTI, false, true),
        FILL_SPILL_CLEAR(LineMode.MULTI, true, true),
        SCROLL_CLIP(LineMode.SCROLL, false, false),
        SCROLL_CLIP_CLEAR(LineMode.SCROLL, true, false),
        SCROLL_SPILL(LineMode.SCROLL, false, true),
        SCROLL_SPILL_CLEAR(LineMode.SCROLL, true, true),

        ;

        public final LineMode lineMode;
        public final boolean clearRestOnNewLine;
        public final boolean spillOnBufferLineEnd;

        PasteMode(LineMode lineMode, boolean clearRestOnNewLine, boolean spillOnBufferLineEnd) {
            this.lineMode = lineMode;
            this.clearRestOnNewLine = clearRestOnNewLine;
            this.spillOnBufferLineEnd = spillOnBufferLineEnd;
        }

        public enum LineMode {
            SINGLE, MULTI, SCROLL
        }
    }

    private record LPrintInfo(int printed, int xAfter) {
    }
}
