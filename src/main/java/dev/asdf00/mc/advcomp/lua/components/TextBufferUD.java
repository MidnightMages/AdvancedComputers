package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.runtime.utils.UDTranslators;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class TextBufferUD implements LuaUserData {

    @LuaExposed(LuaExposed.Policy.READ)
    public final int width;
    @LuaExposed(LuaExposed.Policy.READ)
    public final int height;

    private final GpuUD gpuUD;
    volatile boolean wasFreedAlready = false;

    /** The bits in the foreground color are inverted for performance reasons. */
    private final byte[] foregroundColor;
    private final byte[] backgroundColor;
    private final char[] text;
    private int lStart;

    // TODO not threadsafe and not protected
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
        this.gpuUD.freeBuffer(this);
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
        gpuUD.dirty(this);
    }

    @LuaCallable
    public void rotRows(int cnt) {
        luaGuarantee(cnt < height && cnt >= -height, "line out of bounds");
        cnt = cnt < 0 ? height - cnt : cnt;
        lStart = (lStart + cnt) % height;
        gpuUD.dirty(this);
    }

    @LuaCallable
    public void clearRow(int line) {
        luaGuarantee(line < height && line >= -height, "line out of bounds");
        line = line < 0 ? height - line : line;
        int target = ((lStart + line) % height) * width;
        Arrays.fill(text, target, target + (width - 1), '\0');
        Arrays.fill(foregroundColor, target, target + (width - 1), (byte) 0);
        Arrays.fill(backgroundColor, target, target + (width - 1), (byte) 0);
        gpuUD.dirty(this);
    }

    @LuaCallable
    public void newline() {
        clearRow(0);
        rotRows(1);
    }
    }

    private int calcIdx(int x, int y) {
        luaGuarantee(x < width && x >= -width, "x out of bounds");
        luaGuarantee(y < height && y >= -height, "y out of bounds");
        x = x < 0 ? width - x : x;
        y = y < 0 ? height - y : y;
        return ((lStart + y) % height) * width + x;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        throw new UnsupportedOperationException("serialization not implemented");
    }

    private static void luaGuarantee(boolean condition, String msg) {
        if (!condition) {
            throw new LuaJavaError(msg);
        }
    }

    @LuaDeserializer
    public static TextBufferUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }

    public void markAsFreed() {
        wasFreedAlready = true;
    }

    @Override
    public boolean luaFieldGuard(LuaObject key, LuaObject value) {
        return !wasFreedAlready;
    }

    @Override
    public boolean luaCallGuard(String name, LuaObject[] arguments) {
        return !wasFreedAlready;
    }
}
