package dev.asdf00.mc.advcomp.utils.list.internal;

import java.util.*;

public abstract class AbstractBaseList<T> implements List<T> {
    protected int size;
    protected int modCnt;

    @Override
    public Object[] toArray() {
        return toArray(Object[]::new);
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return toArray(a, 0, size);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size < 1;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index >= 0) {
            remove(index);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        ListIterator<T> itr = listIterator();
        while (itr.hasNext()) {
            if (c.contains(itr.next())) {
                itr.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        ListIterator<T> itr = listIterator();
        while (itr.hasNext()) {
            if (!c.contains(itr.next())) {
                itr.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public int indexOf(Object o) {
        return indexOf(o, 0, size);
    }

    @Override
    public int lastIndexOf(Object o) {
        return lastIndexOf(o, 0, size);
    }

    @Override
    public T set(int index, T element) {
        Objects.checkIndex(index, size);
        return directSet(index, element);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ListItr<>(this, 0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        Objects.checkIndex(index, size);
        return new ListItr<>(this, index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        Objects.checkIndex(fromIndex, size);
        if (toIndex < 0 || toIndex > size) {
            throw new IndexOutOfBoundsException();
        }
        int len = toIndex - fromIndex;
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        return new SubList<>(this, fromIndex, len);
    }

    protected abstract <T1> T1[] toArray(T1[] a, int start, int len);

    protected abstract int indexOf(Object o, int start, int len);

    protected abstract int lastIndexOf(Object o, int start, int len);

    protected abstract T directSet(int index, T element);

    protected abstract void createGap(int start, int len);

    protected abstract void closeGap(int start, int len);


    private static class ListItr<E> implements ListIterator<E> {

        private final AbstractBaseList<E> base;
        private int modCnt;
        private int cur;
        private int last;

        public ListItr(AbstractBaseList base, int start) {
            this.base = base;
            modCnt = base.modCnt;
            cur = start;
            last = -1;
        }

        @Override
        public boolean hasNext() {
            return cur < base.size;
        }

        @Override
        public E next() {
            checkConcurrentModification();
            if (!hasNext()) {
                throw new IndexOutOfBoundsException();
            }
            last = cur;
            return base.get(cur++);
        }

        @Override
        public boolean hasPrevious() {
            return cur > 1;
        }

        @Override
        public E previous() {
            checkConcurrentModification();
            if (!hasPrevious()) {
                throw new IndexOutOfBoundsException();
            }
            last = --cur;
            return base.get(cur);
        }

        @Override
        public int nextIndex() {
            return cur;
        }

        @Override
        public int previousIndex() {
            return cur - 1;
        }

        @Override
        public void remove() {
            checkConcurrentModification();
            if (last == -1) {
                throw new IllegalStateException();
            }
            modCnt++;
            base.remove(last);
            cur--;
        }

        @Override
        public void set(E e) {
            checkConcurrentModification();
            Objects.checkIndex(cur, base.size);
            if (last == -1) {
                throw new IllegalStateException();
            }
            modCnt++;
            base.set(cur, e);
            last = -1;
        }

        @Override
        public void add(E e) {
            checkConcurrentModification();
            if (last == -1) {
                throw new IllegalStateException();
            }
            modCnt++;
            last++;
            if (last == base.size) {
                base.add(e);
            } else {
                base.add(last, e);
            }
            last = -1;
        }

        private void checkConcurrentModification() {
            if (base.modCnt != modCnt) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private static class SubList<E> extends AbstractBaseList<E> {

        private final AbstractBaseList<E> base;
        private final int start;

        public SubList(AbstractBaseList<E> base, int start, int size) {
            this.base = base;
            this.start = start;
            this.size = size;
            modCnt = base.modCnt;
        }

        @Override
        public boolean add(E e) {
            checkConcurrentModification();
            modCnt++;
            if (start + size == base.size) {
                base.add(e);
            } else {
                base.add(start + size, e);
            }
            size++;
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            for (E e : c) {
                add(e);
            }
            size += c.size();
            return c.size() > 0;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            Objects.checkIndex(index, size);
            int i = 0;
            for (E e : c) {
                add(i, e);
            }
            size += c.size();
            return c.size() > 0;
        }

        @Override
        public void clear() {
            checkConcurrentModification();
            modCnt++;
            base.modCnt++;
            base.closeGap(start, size);
            size = 0;
        }

        @Override
        public E get(int index) {
            checkConcurrentModification();
            Objects.checkIndex(index, size);
            return base.get(start + index);
        }

        @Override
        public void add(int index, E element) {
            checkConcurrentModification();
            Objects.checkIndex(index, size);
            modCnt++;
            base.add(start + index, element);
        }

        @Override
        public E remove(int index) {
            checkConcurrentModification();
            Objects.checkIndex(index, size);
            modCnt++;
            size--;
            return base.remove(start + index);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            checkConcurrentModification();
            Objects.checkIndex(fromIndex, size);
            if (toIndex < 0 || toIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            int len = (toIndex - fromIndex) + 1;
            if (len < 0) {
                throw new IllegalArgumentException();
            }
            return new SubList<>(base, start + fromIndex, len);
        }

        @Override
        protected <T1> T1[] toArray(T1[] a, int start, int len) {
            checkConcurrentModification();
            modCnt++;
            return base.toArray(a, this.start + start, len);
        }

        @Override
        protected int indexOf(Object o, int start, int len) {
            checkConcurrentModification();
            return base.indexOf(o, this.start + start, len) - start;
        }

        @Override
        protected int lastIndexOf(Object o, int start, int len) {
            checkConcurrentModification();
            return base.lastIndexOf(o, this.start + start, len) - start;
        }

        @Override
        protected E directSet(int index, E element) {
            checkConcurrentModification();
            modCnt++;
            return base.directSet(start + index, element);
        }

        @Override
        protected void createGap(int start, int len) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void closeGap(int start, int len) {
            throw new UnsupportedOperationException();
        }

        private void checkConcurrentModification() {
            if (base.modCnt != modCnt) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
