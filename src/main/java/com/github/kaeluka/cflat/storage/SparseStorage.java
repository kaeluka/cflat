package com.github.kaeluka.cflat.storage;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

@SuppressWarnings("WeakerAccess")
public class SparseStorage {
    public enum USAGE {
        READ_PERFORMANCE,
        INSERT_PERFORMANCE,
        SIZE,
        CHANGEABLE
    }
    public static <T> Storage<Storage<T>> getFor(USAGE u) {
        return getFor(u,5000, 5000, 0.05);
    }

    public static <T> Storage<Storage<T>> getFor(USAGE u, int rowsSizeHint, int colsSizeHint, double sparsityHint) {
        switch (u) {
            case SIZE:
                return new CSRStorage<>(rowsSizeHint, rowsSizeHint, sparsityHint);
            case READ_PERFORMANCE:
            case INSERT_PERFORMANCE:
                return new Storage2D<>(new ChunkedStorage<>(), new ChunkedStorage<>());
            case CHANGEABLE:
                return new Changable2DStorage<T>(USAGE.INSERT_PERFORMANCE, rowsSizeHint, colsSizeHint, sparsityHint);
        }
        throw new UnsupportedOperationException("can't construct a sparse storage for "+u);
    }

    public static <T> Storage<Storage<T>> reshape(final Storage<Storage<T>> oldSt, USAGE convertTo) {
        final int colsEstimate = oldSt.sizeOverApproximation();
        return reshape(oldSt, convertTo, colsEstimate, colsEstimate > 0 ? oldSt.get(0).sizeOverApproximation() : 0, 0.05);
    }

    public static <T> Storage<Storage<T>> reshape(final Storage<Storage<T>> oldSt, USAGE convertTo, int rowsSizeHint, int colsSizeHint, double sparsityHint) {
        switch (convertTo) {
            case SIZE:
                if (oldSt instanceof CSRStorage) {
                    System.out.println("already a CSRStorage");
                    return oldSt;
                } else {
                    final Storage<Storage<T>> newSt = getFor(convertTo, rowsSizeHint, colsSizeHint, sparsityHint);
                    oldSt.foreachNonNull(row -> newSt.get(row).addAll(oldSt.get(row)));
                    return newSt;
                }
            case READ_PERFORMANCE:
            case INSERT_PERFORMANCE:
                throw new UnsupportedOperationException("not implemented yet");
//                return oldSt;
            case CHANGEABLE:
                throw new IllegalArgumentException("Can't change to changable");
        }
        throw new UnsupportedOperationException("can't construct a sparse storage for "+convertTo);
    }
}

final class CSRStorage<T> extends Storage<Storage<T>> {
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
                throw new ArrayIndexOutOfBoundsException("idx="+i+", size="+size);
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
    private TIntArrayList rowstart;
    private TIntArrayList columnIndex;
    int maxIdx;
    int maxCol = 0;

    private void init(final int rowsSizeHint, final int colsSizeHint, final double sparsityHint) {
        data = new ObjList((int)(rowsSizeHint*colsSizeHint*sparsityHint));
        rowstart = new TIntArrayList();
        rowstart.add(0);
        columnIndex = new TIntArrayList();
        this.maxIdx = -1;
    }

    public CSRStorage(final int rowsSizeHint, final int colsSizeHint, final double sparsityHint) {
        init(rowsSizeHint, colsSizeHint, sparsityHint);
    }

    private CSRStorage(final TIntArrayList rowstart, final TIntArrayList columnIndex, final ObjList data) {
        this.rowstart = rowstart;
        rowstart.add(0);
        this.columnIndex = columnIndex;
        this.data = data;
        this.maxIdx = -1;
    }

    public CSRStorage() {
        this(0,0, 0.05);
    }

    public T matrixGet(int row, int col) {
        if (row+1 >= rowstart.size()) {
            return null;
        }
        int rowStart = rowstart.get(row);
        int rowEnd = rowstart.get(row+1);
        for (int i=rowStart; i<rowEnd; ++i) {
            if (columnIndex.get(i) == col) {
                return (T) data.get(i);
            }
        }

        return null;
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
    public int sizeOverApproximation() {
        return sizePrecise();
    }

    @Override
    public int sizePrecise() {
        return this.maxIdx+1;
    }

    @SuppressWarnings("unchecked")
    public Storage<Storage<T>> copy() {
        final TIntArrayList newrowstart = new TIntArrayList();
        newrowstart.addAll(rowstart);

        final TIntArrayList newcolumnIndex = new TIntArrayList();
        newrowstart.addAll(columnIndex);

        return new CSRStorage<>(
                newrowstart,
                newcolumnIndex,
                data.copy());
    }

    @Override
    public Storage<Storage<T>> emptyCopy() {
        final int rowsHint = this.rowstart.size();
        final int colsHint = this.maxCol;
        return new CSRStorage<>(
                rowsHint,
                colsHint, /* assuming square */
                this.data.size() * 1.0 / (rowsHint * colsHint)
        );
    }


//    private void ensureSize(int row, int size) {
//        if (rowstart.length < row+2) {
//            final int oldLength = rowstart.length;
//            rowstart = Arrays.copyOf(rowstart, row+2);
//            Arrays.fill(rowstart, oldLength, rowstart.length, rowstart[oldLength-1]);
//            rowstart[row+1] = size+1;
//        }
//        if (data.length < size+1) {
//            data = Arrays.copyOf(data, size+1);
//        }
//        if (columnIndex.size() < size+1) {
//            columnIndex = Arrays.copyOf(columnIndex, size+1);
//        }
//    }



    private int getRowOrderIndex(final int row, final int col) {
        int start = rowstart.get(row);
        int end = rowstart.get(row + 1);
        while (start<end-1) {
            final int mid = (start+end)/2;
            final int colAtMid = columnIndex.get(mid);
            if (colAtMid == col) {
                return mid;
            } else if (colAtMid > col) {
                end = mid;
            } else {
                start = mid;
            }
        }
        return rowstart.get(row+1);
    }
//    private int getRowOrderIndex(final int row, final int col) {
//        final int start = rowstart.get(row);
//        final int end = rowstart.get(row + 1);
//        for (int r = start; r< end; r++) {
//            if (this.columnIndex.get(r) > col) {
//                return r;
//            }
//        }
//        return end;
//    }

//    private int getRowOrderIndex(final int row, final int col) {
//        int start = rowstart.get(row);
//        int end = rowstart.get(row + 1);
//        while (start<end-1) {
//            if (columnIndex.get(start) == col) {
//                return start;
//            }
//            final int mid = (start+end) >> 1;
//            if (columnIndex.get(mid) < col) {
//                start = mid;
//            } else {
//                end = mid;
//            }
//        }
//        return columnIndex.get(row);
//    }

    private void matrixSet(int row, int col, T val) {
        if (col > maxCol) { maxCol = col; }
        ensureSize(row, col);
        int r = getRowOrderIndex(row, col);

        columnIndex.insert(r, col);
        data.add(r, val);
//        final Mutable<Storage<Integer>> newRowStartMut = new Mutable<>(rowstart);
//        rowstart.foreachSuccessor(row+1, GenericShape.mkStar(1, 1),
//                i -> newRowStartMut.x = newRowStartMut.x.set(i, newRowStartMut.x.get(i)+1));
//        rowstart = newRowStartMut.x;

//        final int[] rowstart_data = rowstart.data;
        for (int i=row+1; i<rowstart.size(); ++i) {
            rowstart.set(i, rowstart.get(i)+1);
        }
        rowstart.set(rowstart.size()-1, matrixSize());
    }

    @Override
    public Storage<T> get(final int row) {
        maxIdx = row > maxIdx ? row : maxIdx;
        return new SparseRowStorage(row);
    }

    @Override
    public boolean has(final int row) {
        if (row+1 >= rowstart.size()) {
            return false;
        } else {
            return rowstart.get(row) < rowstart.get(row + 1);
        }
    }

    @Override
    public Storage<Storage<T>> set(final int i, final Storage<T> x) {
        return null;
    }

    @Override
    public Storage<Storage<T>> clear() {
        init(0,0, 0.05);

        return null;
    }

    @Override
    public long bytesUsed() {
        // slight underapproximation: we're using the size, not the capacity
        return this.data.size() * 64L + (this.columnIndex.size() + this.rowstart.size())*32L;
    }

    @Override
    public String toString() {
        return "CSRStorage:\n"+
                "\tdata        "+(this.data)+"\n"+
                "\trowstart    "+(rowstart)+"\n"+
                "\tcolumnindex "+(columnIndex);
    }

    private class SparseRowStorage extends Storage<T> {
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
        public Storage<T> clear() {
            final int start = CSRStorage.this.rowstart.get(row);
            final int end = CSRStorage.this.rowstart.get(row+1);
            Arrays.fill(CSRStorage.this.data.data, start, end, null);
            return this;
        }

        @Override
        public int sizeOverApproximation() {
            return sizePrecise();
        }

        @Override
        public int sizePrecise() {
            return CSRStorage.this.rowstart.get(this.row+1) - CSRStorage.this.rowstart.get(this.row);
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

//class CSRStorage<T> extends Storage<Storage<T>> {
//    private static class ObjList {
//        private Object[] data;
//        int size;
//
//        private ObjList(Object[] data) {
//            this.data = data;
//            this.size = 0;
//        }
//
//        ObjList(int sizeHint) {
//            data = new Object[Math.max(sizeHint,10)];
//            size = 0;
//        }
//
//        Object get(int i) {
//            if (i >= size) {
//                throw new ArrayIndexOutOfBoundsException("idx="+i+", size="+size);
//            }
//            return data[i];
//        }
//
//        void add(Object v) {
//            ensureCapacity(size+1);
//            data[size++] = v;
//        }
//
//        void set(int i, Object v) {
//            assert(i < data.length);
//
//            data[i] = v;
//        }
//
//        void add(int i, Object v) {
//            ensureCapacity(size+1);
//            System.arraycopy(data, i, data, i+1, size() - i);
//            data[i] = v;
//            size++;
//        }
//
//        int size() {
//            return size;
//        }
//
//        int capacity() {
//            return data.length;
//        }
//
//        void ensureCapacity(int cap) {
//            if (data.length < cap) {
//                int newCapacity = data.length + (data.length >> 1);
//                data = Arrays.copyOf(data, newCapacity);
//            }
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder ret = new StringBuilder("[");
//            for (int i=0; i<size-1; ++i) {
//                ret.append(get(i)).append(", ");
//            }
//
//            if (size > 0) {
//                ret.append(data[size - 1]);
//            }
//            ret.append("] ");
//
//            return ret.toString();
//        }
//
//        ObjList copy() {
//            return new ObjList(Arrays.copyOf(this.data, this.data.length));
//        }
//    }
//    private static class IntList {
//        private int[] data;
//        int size;
//
//        private IntList(int[] data) {
//            this.data = data;
//            this.size = 0;
//        }
//
//        IntList(int sizeHint) {
//            data = new int[Math.max(sizeHint,10)];
//            size = 0;
//        }
//
//        int get(int i) {
//            if (i >= size) {
//                throw new ArrayIndexOutOfBoundsException("idx="+i+", size="+size);
//            }
//            return data[i];
//        }
//
//        void add(int v) {
//            ensureCapacity(size+1);
//            data[size++] = v;
//        }
//
//        void set(int i, int v) {
//            assert(i < data.length);
//
//            data[i] = v;
//        }
//
//        void add(int i, int v) {
//            ensureCapacity(size+1);
//            System.arraycopy(data, i, data, i+1, size() - i);
//            data[i] = v;
//            size++;
//        }
//
//        int size() {
//            return size;
//        }
//
//        int capacity() {
//            return data.length;
//        }
//
//        void ensureCapacity(int cap) {
//            if (data.length < cap) {
//                int newCapacity = data.length + (data.length >> 1);
//                data = Arrays.copyOf(data, newCapacity);
//            }
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder ret = new StringBuilder("[");
//            for (int i=0; i<size-1; ++i) {
//                ret.append(get(i)).append(", ");
//            }
//
//            if (size > 0) {
//                ret.append(data[size - 1]);
//            }
//            ret.append("] ");
//
//            return ret.toString();
//        }
//
//        IntList copy() {
//            return new IntList(Arrays.copyOf(this.data, this.data.length));
//        }
//    }
//
//    private ObjList data;
//    private gnu.trove.list.array.TIntArrayList rowstart;
//    private gnu.trove.list.array.TIntArrayList columnIndex;
//    int maxIdx;
//    int maxCol = 0;
//
//    private void init(final int rowsSizeHint, final int colsSizeHint, final double sparsityHint) {
//        data = new ObjList((int)(rowsSizeHint*colsSizeHint*sparsityHint));
//        rowstart = new TIntArrayList();
//        rowstart.add(0);
//        columnIndex = new TIntArrayList();
//        this.maxIdx = -1;
//    }
//
//    public CSRStorage(final int rowsSizeHint, final int colsSizeHint, final double sparsityHint) {
//        init(rowsSizeHint, colsSizeHint, sparsityHint);
//    }
//
//    private CSRStorage(final TIntArrayList rowstart, final TIntArrayList columnIndex, final ObjList data) {
//        this.rowstart = rowstart;
//        rowstart.add(0);
//        this.columnIndex = columnIndex;
//        this.data = data;
//        this.maxIdx = -1;
//    }
//
//    public CSRStorage() {
//        this(0,0, 0.05);
//    }
//
//    public T matrixGet(int row, int col) {
//        if (row+1 >= rowstart.size()) {
//            return null;
//        }
//        int rowStart = rowstart.get(row);
//        int rowEnd = rowstart.get(row+1);
//        for (int i=rowStart; i<rowEnd; ++i) {
//            if (columnIndex.get(i) == col) {
//                return (T) data.get(i);
//            }
//        }
//
//        return null;
//    }
//
//    private int matrixSize() {
//        return data.size();
//    }
//
//    private void ensureSize(int row, int col) {
//        if (rowstart.size() < row+2) {
//            rowstart.ensureCapacity((row+2));
//            final Integer sz = rowstart.get(rowstart.size() - 1);
//            while (rowstart.size() < row+1) {
//                rowstart.add(sz);
//            }
//            rowstart.add(this.matrixSize());
//        }
//    }
//
//    @Override
//    public int sizeOverApproximation() {
//        return sizePrecise();
//    }
//
//    @Override
//    public int sizePrecise() {
//        return this.maxIdx+1;
//    }
//
//    @SuppressWarnings("unchecked")
//    public Storage<Storage<T>> copy() {
//        final TIntArrayList newrowstart = new TIntArrayList();
//        newrowstart.addAll(rowstart);
//
//        final TIntArrayList newcolumnIndex = new TIntArrayList();
//        newrowstart.addAll(columnIndex);
//
//        return new CSRStorage<>(
//                newrowstart,
//                newcolumnIndex,
//                data.copy());
//    }
//
//    @Override
//    public Storage<Storage<T>> emptyCopy() {
//        final int rowsHint = this.rowstart.size();
//        final int colsHint = this.maxCol;
//        return new CSRStorage<>(
//                rowsHint,
//                colsHint, /* assuming square */
//                this.data.size() * 1.0 / (rowsHint * colsHint)
//        );
//    }
//
//
////    private void ensureSize(int row, int size) {
////        if (rowstart.length < row+2) {
////            final int oldLength = rowstart.length;
////            rowstart = Arrays.copyOf(rowstart, row+2);
////            Arrays.fill(rowstart, oldLength, rowstart.length, rowstart[oldLength-1]);
////            rowstart[row+1] = size+1;
////        }
////        if (data.length < size+1) {
////            data = Arrays.copyOf(data, size+1);
////        }
////        if (columnIndex.size() < size+1) {
////            columnIndex = Arrays.copyOf(columnIndex, size+1);
////        }
////    }
//
//
//
//    private int getRowOrderIndex(final int row, final int col) {
//        int start = rowstart.get(row);
//        int end = rowstart.get(row + 1);
//        while (start<end-1) {
//            final int mid = (start+end)/2;
//            final int colAtMid = columnIndex.get(mid);
//            if (colAtMid == col) {
//                return mid;
//            } else if (colAtMid > col) {
//                end = mid;
//            } else {
//                start = mid;
//            }
//        }
//        return rowstart.get(row+1);
//    }
////    private int getRowOrderIndex(final int row, final int col) {
////        final int start = rowstart.get(row);
////        final int end = rowstart.get(row + 1);
////        for (int r = start; r< end; r++) {
////            if (this.columnIndex.get(r) > col) {
////                return r;
////            }
////        }
////        return end;
////    }
//
////    private int getRowOrderIndex(final int row, final int col) {
////        int start = rowstart.get(row);
////        int end = rowstart.get(row + 1);
////        while (start<end-1) {
////            if (columnIndex.get(start) == col) {
////                return start;
////            }
////            final int mid = (start+end) >> 1;
////            if (columnIndex.get(mid) < col) {
////                start = mid;
////            } else {
////                end = mid;
////            }
////        }
////        return columnIndex.get(row);
////    }
//
//    private void matrixSet(int row, int col, T val) {
//        if (col > maxCol) { maxCol = col; }
//        ensureSize(row, col);
//        int r = getRowOrderIndex(row, col);
//
//        columnIndex.insert(r, col);
//        data.add(r, val);
////        final Mutable<Storage<Integer>> newRowStartMut = new Mutable<>(rowstart);
////        rowstart.foreachSuccessor(row+1, GenericShape.mkStar(1, 1),
////                i -> newRowStartMut.x = newRowStartMut.x.set(i, newRowStartMut.x.get(i)+1));
////        rowstart = newRowStartMut.x;
//
////        final int[] rowstart_data = rowstart.data;
//        for (int i=row+1; i<rowstart.size(); ++i) {
//            rowstart.set(i, rowstart.get(i)+1);
//        }
//        rowstart.set(rowstart.size()-1, matrixSize());
//    }
//
//    @Override
//    public Storage<T> get(final int row) {
//        maxIdx = row > maxIdx ? row : maxIdx;
//        return new SparseRowStorage(row);
//    }
//
//    @Override
//    public boolean has(final int row) {
//        if (row+1 >= rowstart.size()) {
//            return false;
//        } else {
//            return rowstart.get(row) < rowstart.get(row + 1);
//        }
//    }
//
//    @Override
//    public Storage<Storage<T>> set(final int i, final Storage<T> x) {
//        return null;
//    }
//
//    @Override
//    public Storage<Storage<T>> clear() {
//        init(0,0, 0.05);
//
//        return null;
//    }
//
//    @Override
//    public long bytesUsed() {
//        // slight underapproximation: we're using the size, not the capacity
//        return this.data.size() * 64L + (this.columnIndex.size() + this.rowstart.size())*32L;
//    }
//
//    @Override
//    public String toString() {
//        return "CSRStorage:\n"+
//                "\tdata        "+(this.data)+"\n"+
//                "\trowstart    "+(rowstart)+"\n"+
//                "\tcolumnindex "+(columnIndex);
//    }
//
//    private class SparseRowStorage extends Storage<T> {
//        private final int row;
//
//        SparseRowStorage(final int row) {
//            this.row = row;
//        }
//
//        @Override
//        public T get(final int col) {
//            return CSRStorage.this.matrixGet(row, col);
//        }
//
//        @Override
//        public Storage<T> set(final int col, final T x) {
//            CSRStorage.this.matrixSet(row, col, x);
//            return this;
//        }
//
//        @Override
//        public Storage<T> clear() {
//            final int start = CSRStorage.this.rowstart.get(row);
//            final int end = CSRStorage.this.rowstart.get(row+1);
//            Arrays.fill(CSRStorage.this.data.data, start, end, null);
//            return this;
//        }
//
//        @Override
//        public int sizeOverApproximation() {
//            return sizePrecise();
//        }
//
//        @Override
//        public int sizePrecise() {
//            return CSRStorage.this.rowstart.get(this.row+1) - CSRStorage.this.rowstart.get(this.row);
//        }
//
//        @Override
//        public Storage<T> copy() {
//            return this;
//        }
//
//        @Override
//        public Storage<T> emptyCopy() {
//            // this is inefficient, but these shouldn't be called often in
//            // practise, I suppose..
//            return CSRStorage.this.emptyCopy().get(this.row);
//        }
//
//        @Override
//        public long bytesUsed() {
//            throw new UnsupportedOperationException(
//                    this.getClass().getSimpleName()+
//                            " does not manage its own memory; ask the "+CSRStorage.class.getSimpleName()+" instead!");
//        }
//    }
//}
