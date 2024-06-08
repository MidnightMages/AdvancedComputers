package dev.asdf00.mc.advcomp.lua.components;

import java.util.Arrays;

import static dev.asdf00.mc.advcomp.types.RuntimeAssert.RuntimeAssert;

public class GraphicsBuffer {
    private final int width; // x axis (left)
    private final int height; // y axis (top)
    private final char[] charData;
    private final byte[] colorData;
    private byte currentColor = 0;

    private int getIndex(int x, int y) {
        return y * width + x;
    }

    public GraphicsBuffer(int width, int height) {
        RuntimeAssert(width > 0 && height > 0, "invalid width or height %s %s", width, height);

        this.width = width;
        this.height = height;
        charData = new char[width * height];
        colorData = new byte[charData.length];
    }

    public void constFill(int xStart, int yStart, int xEnd, int yEnd, char chr) {
        RuntimeAssert(xStart >= 0, "xStart must be non-negative");
        RuntimeAssert(yStart >= 0, "yStart must be non-negative");
        RuntimeAssert(xEnd < width, "xEnd must be < width");
        RuntimeAssert(yEnd < height, "yEnd must be < height");
        RuntimeAssert(xStart < xEnd, "xStart must be < xEnd");
        RuntimeAssert(yStart < yEnd, "yStart must be < yEnd");

        int fillStart = getIndex(xStart, yStart);
        final int fillLen = xEnd - xStart;
        final int rowCnt = yEnd - yStart;
        for (int i = 0; i <= rowCnt; i++) {
            Arrays.fill(charData, fillStart, fillStart + fillLen, chr);
            Arrays.fill(colorData, fillStart, fillStart + fillLen, currentColor);
            fillStart += width;
        }
    }

    public void copyTo(GraphicsBuffer dst, int xSrcStart, int ySrcStart, int xDstStart, int yDstStart, int width, int height) {
        RuntimeAssert(xSrcStart >= 0, "xSrcStart must be non-negative");
        RuntimeAssert(ySrcStart >= 0, "ySrcStart must be non-negative");
        RuntimeAssert(xDstStart >= 0, "xDstStart must be non-negative");
        RuntimeAssert(yDstStart >= 0, "yDstStart must be non-negative");
        RuntimeAssert(width > 0, "width must be > 0");
        RuntimeAssert(height > 0, "height must be > 0");

        final int xSrcEnd = xSrcStart + width;
        final int ySrcEnd = ySrcStart + height;
        final int xDstEnd = xDstStart + width;
        final int yDstEnd = yDstStart + height;
        RuntimeAssert(xSrcEnd < this.width, "xSrcEnd must be < buffer_width");
        RuntimeAssert(ySrcEnd < this.height, "ySrcEnd must be < buffer_height");
        RuntimeAssert(xDstEnd < dst.width, "xDstEnd must be < buffer_width");
        RuntimeAssert(yDstEnd < dst.height, "yDstEnd must be < buffer_height");

        int srcCopyIndex = getIndex(xSrcStart, ySrcStart);
        int dstCopyIndex = dst.getIndex(xDstStart, yDstStart);
        for (int i = 0; i < height; i++) {
            System.arraycopy(this.charData, srcCopyIndex, dst.charData, dstCopyIndex, width);
            System.arraycopy(this.colorData, srcCopyIndex, dst.colorData, dstCopyIndex, width);
            srcCopyIndex += width;
            dstCopyIndex += width;
        }
    }

    public void writeText(int x, int y, String text, byte color) {

    }

    public byte getColor(int x, int y) {
        return colorData[getIndex(x, y)];
    }

    public char getChar(int x, int y) {
        return charData[getIndex(x, y)];
    }

    public void recolor(int x, int y, byte color) {
        RuntimeAssert(x >= 0, "x must be non-negative");
        RuntimeAssert(y >= 0, "y must be non-negative");
        RuntimeAssert(x < width, "x must be < buffer_width");
        RuntimeAssert(y < height, "y must be < buffer_height");
        colorData[getIndex(x, y)] = color;
    }

    public void setDrawColor(byte color) {
        currentColor = color;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getRow(int row) {
        RuntimeAssert(row >= 0 && row < height, "row index out of range");
        return new String(charData, getIndex(0, row), width);
    }
}
