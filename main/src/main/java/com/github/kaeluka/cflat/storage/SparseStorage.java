package com.github.kaeluka.cflat.storage;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

@SuppressWarnings("WeakerAccess")
public class SparseStorage {
    public enum USAGE {
        READ_PERFORMANCE,
        INSERT_PERFORMANCE,
        SIZE,
    }
    public static <T> NestedStorage<T> getFor(USAGE u) {
        return getFor(u,5000, 0.05);
    }

    public static <T> NestedStorage<T> getFor(USAGE u, int rowsSizeHint, double sparsityHint) {
        switch (u) {
            case SIZE:
                return new CSRStorage<>();
            case READ_PERFORMANCE:
            case INSERT_PERFORMANCE:
                return new Storage2D<>(new ChunkedStorage<>(), new ChunkedStorage<>());
        }
        throw new UnsupportedOperationException("can't construct a sparse storage for "+u);
    }

    public static <T> Storage<Storage<T>> reshape(final Storage<Storage<T>> oldSt, USAGE convertTo) {
        final int colsEstimate = oldSt.maxIdxOverapproximation();
        return reshape(oldSt, convertTo, colsEstimate, colsEstimate > 0 ? oldSt.get(0).maxIdxOverapproximation() : 0, 0.05);
    }

    public static <T> Storage<Storage<T>> reshape(final Storage<Storage<T>> oldSt, USAGE convertTo, int rowsSizeHint, int colsSizeHint, double sparsityHint) {
        switch (convertTo) {
            case SIZE:
                if (oldSt instanceof CSRStorage) {
                    return oldSt;
                } else {
                    final Storage<Storage<T>> newSt = getFor(convertTo, rowsSizeHint, sparsityHint);
                    oldSt.foreachNonNull(row -> newSt.get(row).addAll(oldSt.get(row)));
                    return newSt;
                }
            case READ_PERFORMANCE:
            case INSERT_PERFORMANCE:
                throw new UnsupportedOperationException("not implemented yet");
//                return oldSt;
        }
        throw new UnsupportedOperationException("can't construct a sparse storage for "+convertTo);
    }
}

final class CSRStorage<T> implements NestedStorage<T> {
    private static final class IntList extends TIntArrayList {
        int[] borrowArray() {
            return this._data;
        }
    }
    private static final class ObjList {
        private Object[] data;
        int size;

        private ObjList(Object[] data) {
            this.data = data;
            this.size = 0;
        }

        ObjList(int sizeHint) {
            data = new Object[Math.max(sizeHint,10)];
            size = 0;
        }

        Object get(int i) {
            if (i >= size) {
                throw new ArrayIndexOutOfBoundsException("idx="+i+", stepSize="+size);
            }
            return data[i];
        }

        void add(Object v) {
            ensureCapacity(size+1);
            data[size++] = v;
        }

        void set(int i, Object v) {
            assert(i < data.length);

            data[i] = v;
        }

        void add(int i, Object v) {
            ensureCapacity(size+1);
            System.arraycopy(data, i, data, i+1, size() - i);
            data[i] = v;
            size++;
        }

        int size() {
            return size;
        }

        int capacity() {
            return data.length;
        }

        void ensureCapacity(int cap) {
            if (data.length < cap) {
                int newCapacity = data.length + (data.length >> 1);
                data = Arrays.copyOf(data, newCapacity);
            }
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder("[");
            for (int i=0; i<size-1; ++i) {
                ret.append(get(i)).append(", ");
            }

            if (size > 0) {
                ret.append(data[size - 1]);
            }
            ret.append("] ");

            return ret.toString();
        }

        ObjList copy() {
            return new ObjList(Arrays.copyOf(this.data, this.data.length));
        }
    }

    private ObjList data;
    private IntList rowstart;
    private IntList columnIndex;
    int maxIdx;
    int maxCol = 0;

    private void init() {
        data = new ObjList(10);
        rowstart = new IntList();
        rowstart.add(0);
        columnIndex = new IntList();
        this.maxIdx = -1;
    }

    private CSRStorage(final IntList rowstart, final IntList columnIndex, final ObjList data) {
        this.rowstart = rowstart;
        rowstart.add(0);
        this.columnIndex = columnIndex;
        this.data = data;
        this.maxIdx = -1;
    }

    public CSRStorage() {
        init();
    }

    @SuppressWarnings("unchecked")
    private T matrixGet(int row, int col) {
        if (row+1 >= rowstart.size()) {
            return null;
        }
        int r = getRowOrderIndex(row, col);
        if (r >= 0 && r < this.data.size) {
            return (T) data.get(r);
        } else {
            return null;
        }
//        int rowStart = rowstart.get(row);
//        int rowEnd = rowstart.get(row+1);
//        for (int i=rowStart; i<rowEnd; ++i) {
//            if (columnIndex.get(i) == col) {
//                return (T) data.get(i);
//            }
//        }
    }

    private int matrixSize() {
        return data.size();
    }

    private void ensureSize(int row, int col) {
        if (rowstart.size() < row+2) {
            rowstart.ensureCapacity((row+2));
            final Integer sz = rowstart.get(rowstart.size() - 1);
            while (rowstart.size() < row+1) {
                rowstart.add(sz);
            }
            rowstart.add(this.matrixSize());
        }
    }

    @Override
    public int maxIdxOverapproximation() {
        return maxIdx();
    }

    @Override
    public int maxIdx() {
        return this.maxIdx+1;
    }

    @SuppressWarnings("unchecked")
    public Storage<Storage<T>> copy() {
        final IntList newrowstart = new IntList();
        newrowstart.addAll(rowstart);

        final IntList newcolumnIndex = new IntList();
        newrowstart.addAll(columnIndex);

        return new CSRStorage<>(
                newrowstart,
                newcolumnIndex,
                data.copy());
    }

    @Override
    public Storage<Storage<T>> emptyCopy() {
        return new CSRStorage<>();
    }


    // get the index in row order at which a new element should be stored,
    // or inserted. Whether to store or to insert can be decided by comparing
    // the column index at the returned location.
    private int getRowOrderIndexForInsert(final int row, final int col) {
        final int r = getRowOrderIndex(row, col);
        if (r < 0) {
            // binary search documentation:
            // "otherwise, returns (-(insertion point) - 1)."
            return -(r+1);
        } else {
            return r;
        }
    }

    private int getRowOrderIndex(final int row, final int col) {
        int start = rowstart.get(row);
        int end =   rowstart.get(row + 1);

//        if (row == 4056 && col == 3201) {
//            for (int i = start; i < end; ++i) {
//                System.out.print(String.format("%10d  ", columnIndex.get(i)));
//            }
//            System.out.println("");
//            for (int i = start; i < end; ++i) {
//                System.out.print(String.format("%10s  ", data.get(i)));
//            }
//            System.out.println("");
//        }

        int idx = Arrays.binarySearch(columnIndex.borrowArray(), start, end, col);
        return idx;
    }

    private boolean rowOrderIndexIsAt(final int r, final int row, final int col) {
        return r < columnIndex.size()
                && columnIndex.get(r) == col
                && rowstart.get(row) <= r
                && rowstart.get(row+1) > r;
    }

    private void matrixSet(final int row, final int col, final T val) {
        if (col > maxCol) { maxCol = col; }
        ensureSize(row, col);
        final int r = getRowOrderIndexForInsert(row, col);

        if (!rowOrderIndexIsAt(r, row, col)) {
            columnIndex.insert(r, col);
            data.add(r, val);
            for (int i=row+1; i<rowstart.size(); ++i) {
                rowstart.set(i, rowstart.get(i)+1);
            }
            rowstart.set(rowstart.size()-1, matrixSize());
        } else {
            data.set(r, val);
        }
        assert data.get(r) == val && columnIndex.get(r) == col;
    }

    @Override
    public Storage<T> get(final int row) {
        maxIdx = row > maxIdx ? row : maxIdx;
        return new SparseRowStorage(row);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean has(final int row) {
        if (row+1 >= rowstart.size()) {
            return false;
        } else {
            return rowstart.get(row) < rowstart.get(row + 1);
        }
    }

    @Override
    public int maxColIdxOverapproximation() {
        return this.maxCol;
    }

    @Override
    public Storage<Storage<T>> set(final int i, final Storage<T> x) {
        throw new UnsupportedOperationException("set not supported");
    }

    @Override
    public Storage<Storage<T>> clearAll() {
        init();

        return null;
    }

    @Override
    public long bytesUsed() {
        // slight under approximation: we're using the stepSize, not the capacity
        return this.data.size() * 64L + (this.columnIndex.size() + this.rowstart.size())*32L;
    }

    @Override
    public String toString() {
        return "CSRStorage:\n"+
                "\tdata        "+(this.data)+"\n"+
                "\trowstart    "+(rowstart)+"\n"+
                "\tcolumnindex "+(columnIndex);
    }

    private class SparseRowStorage implements Storage<T> {
        private final int row;

        SparseRowStorage(final int row) {
            this.row = row;
        }

        @Override
        public T get(final int col) {
            return CSRStorage.this.matrixGet(row, col);
        }

        @Override
        public Storage<T> set(final int col, final T x) {
            CSRStorage.this.matrixSet(row, col, x);
            return this;
        }

        @Override
        public Storage<T> clearAll() {
            final int start = CSRStorage.this.rowstart.get(row);
            final int end = CSRStorage.this.rowstart.get(row+1);
            Arrays.fill(CSRStorage.this.data.data, start, end, null);
            return this;
        }

        @Override
        public int maxIdxOverapproximation() {
            return maxIdx();
        }

        @Override
        public int maxIdx() {
            if (this.row+1 >= rowstart.size()) {
                return 0;
            } else {
                final int end = rowstart.get(this.row + 1);
                final int start = rowstart.get(this.row);
                if (end > start) {
                    return columnIndex.get(end-1)+1;
                } else {
                    return 0;
                }
            }
        }

        @Override
        public Storage<T> copy() {
            return this;
        }

        @Override
        public Storage<T> emptyCopy() {
            // this is inefficient, but these shouldn't be called often in
            // practise, I suppose..
            return CSRStorage.this.emptyCopy().get(this.row);
        }

        @Override
        public long bytesUsed() {
            throw new UnsupportedOperationException(
                    this.getClass().getSimpleName()+
                            " does not manage its own memory; ask the "+CSRStorage.class.getSimpleName()+" instead!");
        }
    }
}

