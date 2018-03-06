package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.util.Mutable;

import java.util.concurrent.atomic.AtomicLong;

public class Storage2D<T> implements Storage<Storage<T>> {
    private final Storage<T> colsProto;
    public Storage data;
    private int maxIdx = -1;

    @SuppressWarnings("unchecked")
    public Storage2D(final Storage<Storage<T>> rowsProto, final Storage<T> colsProto) {
        this.colsProto = colsProto;
        this.data = rowsProto.emptyCopy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Storage<T> get(final int i) {
        final Mutable<Storage<T>> mgotten = new Mutable<>(null);
        this.data = this.data.computeIfAbsent(i, idx -> colsProto.emptyCopy(), mgotten);
        if (i > maxIdx) { maxIdx = i; }
        return mgotten.x;
//        final Storage<T> gotten = (Storage<T>) data.get(i);
//        if (gotten == null) {
//            final Storage<T> newInstance = colsProto.emptyCopy();
//            this.data = this.data.set(i, newInstance);
//            maxIdx = i > maxIdx ? i : maxIdx;
//            return newInstance;
//        } else {
            // FIXME: return a wrapper here that makes sure that any identity-changing
            // operation on gotten will change the identity inside this container, too!
//            return gotten;
//        }
//        return data.getOrElse(i, () -> {
//            try {
//                return (Storage<T>) colsKlass.newInstance();
//            } catch (InstantiationException | IllegalAccessException e) {
//                e.printStackTrace();
//                return null;
//            }
//        });
    }

    @Override
    public boolean has(final int i) {
        return this.data.has(i);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Storage<Storage<T>> set(final int l, final Storage<T> x) {
        maxIdx = l > maxIdx ? l : maxIdx;
        data.set(l, x);
        return this;
    }

    @Override
    public Storage<Storage<T>> clearAll() {
        data.clearAll();
        this.maxIdx = -1;
        return this;
    }

    @Override
    public int sizePrecise() {
        return maxIdx+1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Storage<Storage<T>> emptyCopy() {
        return new Storage2D<>(data.emptyCopy(), colsProto.emptyCopy());
    }

    @Override
    public Storage<Storage<T>> copy() {
        final Storage<Storage<T>> cp = this.emptyCopy();
        this.foreachNonNull(i -> cp.set(i, this.get(i).copy()));
        return cp;
    }

    @Override
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    @Override
    public long bytesUsed() {
        final AtomicLong ret = new AtomicLong(this.data.bytesUsed());
        data.foreachSuccessor(0, new Object[]{0, 1, 1}, i -> ret.addAndGet(get(i).bytesUsed()));
        return ret.get();
    }
}
