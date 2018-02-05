package com.github.kaeluka.cflat.collections;

import com.github.kaeluka.cflat.annotations.Cflat;
import com.github.kaeluka.cflat.storage.Storage;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

final class SequenceIdx {
    public static final String expr = "*(next)->ok";
    public static final Object[] shape = new Object[]{-2147483648, Integer.valueOf(1), Integer.valueOf(1)};
    private int idx_next;

    SequenceIdx next() {
        String var10000 = "from MutableStep(ctx={...}, stepIdx=next)";
        this.idx_next = this.idx_next + 1;
        return this;
    }

    SequenceIdx next_back() {
        String var10000 = "from MutableBackwardsStep(ctx={...}, stepIdx=next)";
        --this.idx_next;
        return this;
    }

    SequenceIdx next_nth(int var1) {
        String var10000 = "from MutableRandomAccessStep(classDesc=com/github/kaeluka/cflat/SequenceIdx, ctx={...}, stepIdx=next, info='')";
        this.idx_next += var1;
        if (this.idx_next <= 2147483646) {
            return this;
        } else {
            throw new AssertionError("SequenceIdx:  -> index next accessed out of bounds [0..2147483647)");
        }
    }

    int ok() {
        return this.idx_next;
    }

    public SequenceIdx copy() {
        SequenceIdx var10000 = new SequenceIdx();
        var10000.idx_next = this.idx_next;
        return var10000;
    }
}

@Cflat("*(next)->ok")
public class Sequence<T> extends AbstractList<T> implements List<T>, Iterable<T>, Cloneable {
    public Storage<T> storage;
    private SequenceIdx nxt = new SequenceIdx();

    public Sequence(Storage<T> storage) {
        this.storage = storage;
    }

    @Override
    public boolean add(final T x) {
        this.storage = this.storage.set(this.nxt.ok(), x);
        this.nxt.next();
        return true;
    }

    @Override
    public boolean remove(final Object o) {
        int i = this.storage.find((T) o, this.nxt.ok());
        if (i >= 0) {
            this.remove(i);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        this.storage = this.storage.clear();
        this.nxt = new SequenceIdx();
    }

    public int find(T x) {
        return this.storage.find(x, -1);
    }

    @Override
    public T get(int index) {
        return this.storage.get(index);
    }

    @Override
    public T set(final int index, final T element) {
        final T ret = (T) this.storage.get(index);
        this.storage = this.storage.set(index, element);
        return ret;
    }

    @Override
    public void add(final int index, final T element) {
//        this.storage = this.storage.moveSubtree(index, SequenceIdx.shape, index+1, this.size() - index);
//        this.storage = this.storage.moveSubtree(index, SequenceIdx.shape, index+1);
        this.storage = this.storage.copyRange(index,index+1, this.size() - index);
        this.storage = this.storage.set(index, element);
        this.nxt.next();
    }

    @Override
    public T remove(final int index) {
        T ret = this.get(index);
//        this.storage.moveSubtree(index+1, SequenceIdx.shape, index, this.size() - index - 1);
        this.storage.moveSubtree(index+1, SequenceIdx.shape, index);
        this.nxt.next_back();
        return ret;
    }

    @Override
    public int size() {
        return this.nxt.ok();
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iter();
    }

    @Override
    public Object[] toArray() {
        final Object[] ret = new Object[this.nxt.ok()];
        return this.toArray(ret);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        final Sequence<T> sequence = new Sequence<>(storage.copy());
        sequence.nxt = this.nxt.copy();
        return sequence;
    }

    @Override
    public <T1> T1[] toArray(final T1[] a) {
        T1[] ret = null;
        if (a.length > this.nxt.ok()) {
            ret = a;
        } else {
            ret = (T1[]) Array.newInstance(
                    a.getClass().getComponentType(),
                    this.nxt.ok());
        }
        for (SequenceIdx i=new SequenceIdx(); i.ok()<ret.length; i.next()) {
            ret[i.ok()] = (T1) this.storage.get(i.ok());
        }
        return ret;
    }

    private class Iter implements Iterator<T> {
        private final SequenceIdx cur = new SequenceIdx();

        private T get() {
            return Sequence.this.storage.get(this.cur.ok());
        }

        @Override
        public boolean hasNext() {
            return this.get() != null;
        }

        @Override
        public T next() {
            final T ret = this.get();
            this.cur.next();
            return ret;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"("+this.storage.getClass()+")"+super.toString();
    }
}
