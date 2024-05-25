package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.mc.advcomp.lua.LuaMain;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class GraphicsBuffer {
    private final int width; // x axis (left)
    private final int height; // y axis (top)
    private final byte[] data;

    private static GraphicsBuffer bakedCharacterAtlas;
    private static Map<Character, Integer> bakedCharacterAtlasIndexLut;

    private void BakeCharacterAtlas()
    {
        Font f;
        try (var stream = LuaMain.class.getClassLoader().getResourceAsStream("fonts/PixeloidMono.otf")) {
            Objects.requireNonNull(stream, "Error loading font!");
            f =  Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Font not found or failed to load!");
        }

        var letterCount = f.getNumGlyphs();
        var frc = new FontRenderContext(new AffineTransform(), false, false);
        var glyphSize = f.getStringBounds(" ", frc);

        var width = (int)Math.ceil(Math.sqrt(letterCount));
        var height = (int)Math.ceil((double)letterCount/width);
        var bi = new BufferedImage((int)Math.ceil(width*glyphSize.getX()),(int)Math.ceil(height*glyphSize.getY()), BufferedImage.TYPE_BYTE_GRAY);

        for (int i = 0; i < letterCount; i++) {
            if (f.canDisplay(i))
        }
        bi.createGraphics().drawGlyphVector(f.createGlyphVector(frc, ));
    }

    private int getIndex(int x, int y) {
        return y * width + x;
    }

    public GraphicsBuffer(int width, int height) {
        RuntimeAssert(width > 0 && height > 0, "invalid width or height %s %s".formatted(width, height));

        this.width = width;
        this.height = height;
        data = new byte[width * height];
    }


    private void RuntimeAssert(boolean ok, String message) {
        if (!ok)
            throw new IllegalStateException("Assertion failed: " + message);
    }

    public void constFill(int xStart, int yStart, int xEnd, int yEnd, byte color) {
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
            Arrays.fill(data, fillStart, fillStart + fillLen, color);
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
            System.arraycopy(this.data, srcCopyIndex, dst.data, dstCopyIndex, width);
            srcCopyIndex += width;
            dstCopyIndex += width;
        }
    }

    public void writeText(int x, int y, String text, byte color) {
        var g = new Graphics2D();
        g.setFont(new Font("Serif", Font.PLAIN, 24));

        Font.createFont().createGlyphVector()
    }

    public byte getColor(int x, int y) {
        return data[getIndex(x, y)];
    }

    public void setColor(int x, int y, byte color) {
        RuntimeAssert(x >= 0, "x must be non-negative");
        RuntimeAssert(y >= 0, "y must be non-negative");
        RuntimeAssert(x < width, "x must be < buffer_width");
        RuntimeAssert(y < height, "y must be < buffer_height");
        data[getIndex(x, y)] = color;
    }
}
