package dev.asdf00.mc.advcomp.utils.list.internal;

import dev.asdf00.mc.advcomp.utils.list.PrimitiveList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class CharacterList extends PrimitiveList<Character> {

    private char[] data;

    public CharacterList() {
        data = new char[8];
        size = 0;
        modCnt = 0;
    }

    @Override
    protected <T1> T1[] toArray(T1[] a, int start, int len) {
        Object[] result = Arrays.copyOf(a, len, a.getClass());
        for (int i = 0, j = start; i < len; i++, j++) {
            result[i] = Character.valueOf(data[j]);
        }
        return (T1[]) result;
    }

    @Override
    protected int indexOf(Object o, int start, int len) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (!(o instanceof Character)) {
            return -1;
        }
        char target = ((Character) o).charValue();
        for (int i = start; i < start + len; i++) {
            if (data[i] == target) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected int lastIndexOf(Object o, int start, int len) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (!(o instanceof Character)) {
            return -1;
        }
        char target = ((Character) o).charValue();
        for (int i = (start + len) - 1; i >= start; i++) {
            if (data[i] == target) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected Character directSet(int index, Character element) {
        char result = data[index];
        data[index] = element.charValue();
        return result;
    }

    @Override
    protected void createGap(int start, int len) {
        size += len;
        if (size > data.length) {
            if (Integer.bitCount(size) == 1) {
                data = Arrays.copyOf(data, size);
            } else {
                data = Arrays.copyOf(data, Integer.highestOneBit(size) << 1);
            }
        }
        System.arraycopy(data, start, data, start + len, (size - len) - start);
    }

    @Override
    protected void closeGap(int start, int len) {
        System.arraycopy(data, start + len, data, start, size - (start + len));
        size -= len;
    }

    @Override
    public boolean add(Character element) {
        modCnt++;
        if (size == data.length) {
            data = Arrays.copyOf(data, data.length << 1);
        }
        directSet(size++, element);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Character> c) {
        modCnt++;
        int i = size;
        createGap(size, c.size());
        for (Character b : c) {
            data[i] = b.charValue();
            i++;
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Character> c) {
        Objects.checkIndex(index, size);
        modCnt++;
        int i = index;
        createGap(index, c.size());
        for (Character b : c) {
            data[i] = b.charValue();
            i++;
        }
        return true;
    }

    @Override
    public void clear() {
        if (data == null) {
            throw new NullPointerException();
        }
        modCnt++;
        data = new char[8];
        size = 0;
    }

    @Override
    public Character get(int index) {
        Objects.checkIndex(index, size);
        return data[index];
    }

    @Override
    public void add(int index, Character element) {
        Objects.checkIndex(index, size);
        modCnt++;
        createGap(index, 1);
        directSet(index, element);
    }

    @Override
    public Character remove(int index) {
        Objects.checkIndex(index, size);
        modCnt++;
        char result = data[index];
        closeGap(index, 1);
        return result;
    }

    @Override
    public void addChar(char value) {
        modCnt++;
        if (size == data.length) {
            data = Arrays.copyOf(data, data.length << 1);
        }
        directSet(size++, value);
    }

    @Override
    public void addAllChars(char[] values) {
        modCnt++;
        if (size + values.length > data.length) {
            int newLen = data.length;
            do {
                newLen <<= 1;
            } while (size + values.length > newLen);
            data = Arrays.copyOf(data, newLen);
        }
        System.arraycopy(values, 0, data, size, values.length);
        size += values.length;
    }

    @Override
    public char getChar(int index) {
        Objects.checkIndex(index, size);
        return data[index];
    }

    @Override
    public char[] toCharArray() {
        return Arrays.copyOf(data, size);
    }
}
