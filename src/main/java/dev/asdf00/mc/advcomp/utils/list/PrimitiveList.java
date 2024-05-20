package dev.asdf00.mc.advcomp.utils.list;

import dev.asdf00.mc.advcomp.utils.list.internal.*;

public abstract class PrimitiveList<T> extends AbstractBaseList<T> {

    public static <T> PrimitiveList<T> create(Class<T> boxedType) {
        if (Byte.class == boxedType) {
            return (PrimitiveList<T>) new ByteList();
        } else if (Short.class == boxedType) {
            return (PrimitiveList<T>) new ShortList();
        } else if (Integer.class == boxedType) {
            return (PrimitiveList<T>) new IntegerList();
        } else if (Long.class == boxedType) {
            return (PrimitiveList<T>) new LongList();
        } else if (Float.class == boxedType) {
            return (PrimitiveList<T>) new FloatList();
        } else if (Double.class == boxedType) {
            return (PrimitiveList<T>) new DoubleList();
        } else if (Character.class == boxedType) {
            return (PrimitiveList<T>) new CharacterList();
        } else if (Boolean.class == boxedType) {
            return (PrimitiveList<T>) new BooleanList();
        } else {
            throw new IllegalArgumentException("%s is not a boxed type!".formatted(boxedType));
        }
    }

    public void addByte(byte value) {
        throw new UnsupportedOperationException("this list is not of type Byte!");
    }

    public void addShort(short value) {
        throw new UnsupportedOperationException("this list is not of type Short!");
    }

    public void addInt(int value) {
        throw new UnsupportedOperationException("this list is not of type Integer!");
    }

    public void addLong(long value) {
        throw new UnsupportedOperationException("this list is not of type Long!");
    }

    public void addFloat(float value) {
        throw new UnsupportedOperationException("this list is not of type Float!");
    }

    public void addDouble(double value) {
        throw new UnsupportedOperationException("this list is not of type Double!");
    }

    public void addChar(char value) {
        throw new UnsupportedOperationException("this list is not of type Character!");
    }

    public void addBoolean(boolean value) {
        throw new UnsupportedOperationException("this list is not of type Boolean!");
    }

    public void addAllBytes(byte[] values) {
        throw new UnsupportedOperationException("this list is not of type Byte!");
    }

    public void addAllShorts(short[] values) {
        throw new UnsupportedOperationException("this list is not of type Short!");
    }

    public void addAllInts(int[] values) {
        throw new UnsupportedOperationException("this list is not of type Integer!");
    }

    public void addAllLongs(long[] values) {
        throw new UnsupportedOperationException("this list is not of type Long!");
    }

    public void addAllFloats(float[] values) {
        throw new UnsupportedOperationException("this list is not of type Float!");
    }

    public void addAllDoubles(double[] values) {
        throw new UnsupportedOperationException("this list is not of type Double!");
    }

    public void addAllChars(char[] values) {
        throw new UnsupportedOperationException("this list is not of type Character!");
    }

    public void addAllBooleans(boolean[] values) {
        throw new UnsupportedOperationException("this list is not of type Boolean!");
    }

    public byte getByte(int index) {
        throw new UnsupportedOperationException("this list is not of type Byte!");
    }

    public short getShort(int index) {
        throw new UnsupportedOperationException("this list is not of type Short!");
    }

    public int getInt(int index) {
        throw new UnsupportedOperationException("this list is not of type Integer!");
    }

    public long getLong(int index) {
        throw new UnsupportedOperationException("this list is not of type Long!");
    }

    public float getFloat(int index) {
        throw new UnsupportedOperationException("this list is not of type Float!");
    }

    public double getDouble(int index) {
        throw new UnsupportedOperationException("this list is not of type Double!");
    }

    public char getChar(int index) {
        throw new UnsupportedOperationException("this list is not of type Character!");
    }

    public boolean getBoolean(int index) {
        throw new UnsupportedOperationException("this list is not of type Boolean!");
    }

    public byte[] toByteArray() {
        throw new UnsupportedOperationException("this list is not of type Byte!");
    }

    public short[] toShortArray() {
        throw new UnsupportedOperationException("this list is not of type Short!");
    }

    public int[] toIntArray() {
        throw new UnsupportedOperationException("this list is not of type Integer!");
    }

    public long[] toLongArray() {
        throw new UnsupportedOperationException("this list is not of type Long!");
    }

    public float[] toFloatArray() {
        throw new UnsupportedOperationException("this list is not of type Float!");
    }

    public double[] toDoubleArray() {
        throw new UnsupportedOperationException("this list is not of type Double!");
    }

    public char[] toCharArray() {
        throw new UnsupportedOperationException("this list is not of type Character!");
    }

    public boolean[] toBooleanArray() {
        throw new UnsupportedOperationException("this list is not of type Boolean!");
    }
}
