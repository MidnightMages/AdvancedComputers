package dev.asdf00.mc.advcomp.utils.list.internal;

import dev.asdf00.mc.advcomp.utils.list.PrimitiveList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class BooleanList extends PrimitiveList<Boolean> {

    private boolean[] data;

    public BooleanList() {
        data = new boolean[8];
        size = 0;
        modCnt = 0;
    }

    @Override
    protected <T1> T1[] toArray(T1[] a, int start, int len) {
        Object[] result = Arrays.copyOf(a, len, a.getClass());
        for (int i = 0, j = start; i < len; i++, j++) {
            result[i] = Boolean.valueOf(data[j]);
        }
        return (T1[]) result;
    }

    @Override
    protected int indexOf(Object o, int start, int len) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (!(o instanceof Boolean)) {
            return -1;
        }
        boolean target = ((Boolean) o).booleanValue();
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
        if (!(o instanceof Boolean)) {
            return -1;
        }
        boolean target = ((Boolean) o).booleanValue();
        for (int i = (start + len) - 1; i >= start; i++) {
            if (data[i] == target) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected Boolean directSet(int index, Boolean element) {
        boolean result = data[index];
        data[index] = element.booleanValue();
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
    public boolean add(Boolean element) {
        modCnt++;
        if (size == data.length) {
            data = Arrays.copyOf(data, data.length << 1);
        }
        directSet(size++, element);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Boolean> c) {
        modCnt++;
        int i = size;
        createGap(size, c.size());
        for (Boolean b : c) {
            data[i] = b.booleanValue();
            i++;
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Boolean> c) {
        Objects.checkIndex(index, size);
        modCnt++;
        int i = index;
        createGap(index, c.size());
        for (Boolean b : c) {
            data[i] = b.booleanValue();
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
        data = new boolean[8];
        size = 0;
    }

    @Override
    public Boolean get(int index) {
        Objects.checkIndex(index, size);
        return data[index];
    }

    @Override
    public void add(int index, Boolean element) {
        Objects.checkIndex(index, size);
        modCnt++;
        createGap(index, 1);
        directSet(index, element);
    }

    @Override
    public Boolean remove(int index) {
        Objects.checkIndex(index, size);
        modCnt++;
        boolean result = data[index];
        closeGap(index, 1);
        return result;
    }

    @Override
    public void addBoolean(boolean value) {
        modCnt++;
        if (size == data.length) {
            data = Arrays.copyOf(data, data.length << 1);
        }
        directSet(size++, value);
    }

    @Override
    public void addAllBooleans(boolean[] values) {
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
    public boolean getBoolean(int index) {
        Objects.checkIndex(index, size);
        return data[index];
    }

    @Override
    public boolean[] toBooleanArray() {
        return Arrays.copyOf(data, size);
    }
}
