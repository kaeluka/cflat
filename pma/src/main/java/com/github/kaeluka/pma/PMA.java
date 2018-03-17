package com.github.kaeluka.pma;

/**
 * An implementation of a Packed Memory Array
 * Papers: Bender, Hu: An Adaptive Packed Memory Array
 *
 * Partially based on https://github.com/dhruvbird/packed-memory-array
 *
 * @author Stephan Brandauer
 */

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;

import static java.lang.Integer.numberOfLeadingZeros;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"WeakerAccess", "unused"})
public class PMA<T> implements Iterable<T> {
    protected TIntArrayList keys;
    protected ArrayList<T> values;
    protected int segmentSize;
//    private int nSegments;
    protected int nLevels;
//    private int lgn;
//    private BitSet present;
    protected static final int EMPTY = Integer.MIN_VALUE;
    protected static double RHO_H = 0.25;
    protected static double RHO_0 = 0.0;
    protected static double TAU_H = 0.5;
    protected static double TAU_0 = 1.0;

//    protected static final boolean DEBUG = PMA.class.desiredAssertionStatus();

    static {
        //see Bender, Hu, p5, (4)
        assert 2 * RHO_H <= TAU_H;
    }

    @Override
    public Iterator<T> iterator() {
        return new PMAIterator();
    }

    protected final class PMAIterator implements Iterator<T> {
        int nxt = 0;

        PMAIterator() {
            nxt = 0;
            maintainNxt();
        }

        private void maintainNxt() {
            while (!isPresent(nxt) && nxt < keys.size()) {
                nxt++;
            }
        }

        @Override
        public boolean hasNext() {
            return nxt < keys.size();
        }

        @Override
        public T next() {
            final int oldNxt = nxt;
            nxt++;
            maintainNxt();
            return values.get(oldNxt);
        }

        @Override
        public void forEachRemaining(final Consumer<? super T> action) {
            final int size = values.size();
            for (int i = nxt; i < size; i++) {
                final T value = values.get(i);
                if (value != null) {
                    action.accept(value);
                }
            }
            nxt = size;
        }
    }

    protected final static class MutableInt {
        int x;
        MutableInt(int x) { this.x = x; }

        @Override
        public String toString() {
            return "MutableInt("+x+")";
        }
    }
    
    protected boolean isPresent(int idx) {
//        if (idx >= keys.size()) { return false; }
        return keys.get(idx) != EMPTY;
    }

//    protected static void debug(String fmt, Object... args) {
//        if (PMA.DEBUG) {
//            System.out.print(String.format(fmt + "\n", args));
//            System.out.flush();
//        }
//    }

    protected static int log2(int bits)
    {
        // source: https://stackoverflow.com/questions/3305059/how-do-you-calculate-log-base-2-in-java-for-integers#3305710
        if (bits == 0) {
            throw new IllegalArgumentException("log2(0)");
        }
        @SuppressWarnings("UnnecessaryLocalVariable")
        final int cap = 31 - numberOfLeadingZeros(bits);
        return cap;
    }

    @SuppressWarnings("unused")
    public PMA() {
        this(2);
    }

    public PMA(int capacity) {
        assert capacity > 1;
        assert 1 << log2(capacity) == capacity;
        this.initialise(capacity);
        this.keys = new TIntArrayList(capacity, EMPTY);
        this.values = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            this.keys.add(EMPTY);
            this.values.add(null);
        }
//        assertRepInvariants();
//        this.present = new BitSet(capacity);
    }

    void print() {
        for (int i = 0; i < this.values.size(); ++i) {
            String annotation = "";
            if (isPresent(i) && (keys.get(i) == EMPTY || values.get(i) == null)) {
                annotation = " BUG!";
            }
//            if (isPresent(i) || annotation.length() > 0) {
//                debug("%s -> %s%s", keys.get(i) == EMPTY ? "empty" : keys.get(i), values.get(i), annotation);
//            }
        }
//        debug("");
    }

    protected double lowerThreshold(int level) {
        assert level < this.nLevels;
        @SuppressWarnings("UnnecessaryLocalVariable")
        double threshold = RHO_H - ((RHO_H - RHO_0) * level) / (double) nLevels;
        return threshold;
    }

    protected double upperThreshold(int level) {
        assert level <= this.nLevels;
        @SuppressWarnings("UnnecessaryLocalVariable")
        double threshold = TAU_0 - ((TAU_0 - TAU_H) * level) / (double) nLevels;
        return threshold;
    }

    protected boolean intervalStats(int left, int level, MutableInt msz) {
        double t = upperThreshold(level);
        int w = getWindowWidth(level);
        int sz = 0;
        for (int i = left; i < left + w; ++i) {
            sz += this.isPresent(i) ? 1 : 0;
        }
        if (msz != null) {
            msz.x = sz;
        }
        return (double)(sz+1) / (double) w < t;
    }

//    protected void assertRepInvariants() {
//        assertLevels();
//        assertSorted();
//        assertLeftBiasedSegments();
//    }

//    private void assertLevels() {
//        if (DEBUG) {
//            assertThat(keys.size(), is(segmentSize * (1 << nLevels)));
//        }
//    }

//    private void assertSorted() {
//        if (DEBUG) {
//            int prev = EMPTY;
//            for (int i = 0; i < keys.size(); i++) {
//                int cur = keys.get(i);
//                if (cur != EMPTY) {
//                    if (!(prev == EMPTY || cur > prev)) {
//                        throw new AssertionError("not sorted at location " + i + "!\n" + neighbourhoodToString(i));
//                    }
//                    prev = cur;
//                }
//            }
//        }
//    }

    private static String leftPad(String str, int len, char c) {
        StringBuilder strBuilder = new StringBuilder(str);
        while (strBuilder.length() < len) {
            strBuilder.insert(0, c);
        }
        return strBuilder.toString();
    }

    private static String intToBits(int i) {
        return leftPad(Integer.toBinaryString(i), 31, '0') + " lz="+Integer.numberOfLeadingZeros(i);
    }

    protected static int fastDiv(int a, int b) {
//        assertPowerOfTwo(b);
//        System.out.println("a  =" + intToBits(a));
//        System.out.println("b  =" + intToBits(b));
//        System.out.println("a/b=" + intToBits(a/b));
        int ret = a >> (31 - Integer.numberOfLeadingZeros(b));
//        System.out.println("ret=" + intToBits(ret));
//        assert ret == a/b;
        return ret;
    }

    protected static int fastDivBothPow2(int a, int b) {
//        assertPowerOfTwo(a);
//        assertPowerOfTwo(b);
        int ret = 1 << (Integer.numberOfLeadingZeros(b) - Integer.numberOfLeadingZeros(a));
//        System.out.println("ret="+intToBits(ret));
//        assertThat(ret, is(a/b));
        return ret;
    }

//    private void assertLeftBiasedSegments() {
//        if (DEBUG) {
//            final int NSEGMENTS = fastDivBothPow2(keys.size(), segmentSize);
//            assert NSEGMENTS == keys.size() / segmentSize;
//            for (int seg = 0; seg < NSEGMENTS; seg++) {
//                int i = seg * segmentSize;
//
//                boolean hadEmpty = false;
//                for (int cur = seg * segmentSize; cur < (seg + 1) * segmentSize; ++cur) {
//                    if (hadEmpty && keys.get(cur) != EMPTY) {
//                        String msg = String.format("invalid segment %d: %s", i, neighbourhoodToString(i));
//                        throw new AssertionError(msg);
//
//                    } else {
//                        if (keys.get(cur) == EMPTY) {
//                            hadEmpty = true;
//                        }
//                    }
//                }
//            }
//        }
//    }

    protected void mergeInto(int segmentStart, int key, T val) {
        int insertTo = segmentStart;
        for (int i = segmentStart; i < segmentStart+segmentSize; i++) {
            final int curKey = keys.get(i);
            if (curKey >= key || curKey == EMPTY) {
                insertTo = i;
                break;
            }
        }
        assert insertTo < segmentStart + segmentSize;

        int moveTo = insertTo;
        while (moveTo < segmentStart + segmentSize - 1 && isPresent(moveTo)) {
            moveTo++;
        }

        assert moveTo < segmentStart + segmentSize;
        for (int m = moveTo; m > insertTo; --m) {
            keys  .set(m,   keys.get(m - 1));
            values.set(m, values.get(m - 1));
        }

        keys  .set(insertTo, key);
        values.set(insertTo, val);
//        assertRepInvariants();
    }

//    protected void printNeighbourhood(final int idx) {
//        if (PMA.DEBUG) {
//            String n = neighbourhoodToString(idx);
//            System.out.println(n);
//        }
//    }

    protected String neighbourhoodToString(final int idx) {
        final int start = Math.max(0, idx - segmentSize);
        final int end = Math.min(keys.size()-1, idx+segmentSize);
        StringBuilder ret = new StringBuilder();
        for (int i=start; i<=end; ++i) {
            if (i % segmentSize == 0) {
                ret.append("(");
            }

            String valStr = keys.get(i) == EMPTY ? "_" : Integer.toString(keys.get(i));

            if (i == idx) {
                ret.append("<").append(valStr).append(">");
            } else {
                ret.append(valStr);
            }
            if ((i+1) % segmentSize == 0) {
                ret.append(")");
            } else {
                ret.append(" ");
            }
        }
        return ret.toString();
    }

    protected void grow() {
//        assertRepInvariants();
        int capacity = 2 * values.size();
//        assertPowerOfTwo(capacity);
        initialise(capacity);
        final TIntArrayList newKeys = new TIntArrayList(capacity, EMPTY);
        final ArrayList<T> newValues = new ArrayList<>(capacity);

        final int NEW_NSEGMENTS = 2 * keys.size() / segmentSize;
        for (int seg = 0; seg < NEW_NSEGMENTS; seg++) {
            for (int i = 0; i < segmentSize/2; i++) {
                int idx = seg*segmentSize/2+i;
                newKeys  .add(keys.get(idx));
                newValues.add(values.get(idx));
            }
            for (int i = 0; i < segmentSize/2; i++) {
                newKeys  .add(EMPTY);
                newValues.add(null);
            }
        }

        assert newKeys.size() == capacity;

        keys = newKeys;
        values = newValues;
//        assertRepInvariants();
    }

    protected void rebalanceWindow(int windowStart, int level) {
//        debug("rebalanceWindow(%d, %d)", windowStart, level);
        int wWidth = getWindowWidth(level);

        TIntArrayList tmpKeys = new TIntArrayList(wWidth, EMPTY);
        ArrayList<T> tmpValues = new ArrayList<>(wWidth);
        for (int i = 0; i < wWidth; i++) {
            if (isPresent(windowStart+i)) {
                tmpKeys  .add(  keys.get(windowStart+i));
                tmpValues.add(values.get(windowStart+i));
                keys  .set(windowStart+i, EMPTY);
                values.set(windowStart+i, null);
            }
        }

        final int nValues = tmpKeys.size();
        assert nValues > 1; // shouldn't be called, otherwise
//        if (nValues <= 1) {
//            //evenly distributed per definition
//            return;
//        }

        final double fullness = (double) nValues / (double) wWidth;
        final int valuesPerSegment = (int) Math.ceil(segmentSize*fullness);
//        debug("rebalancing %d values in window [%d, %d), fullness: %f, values per segment: %d", nValues, windowStart, windowStart+wWidth, fullness, valuesPerSegment);
//        assert stride >= 1.0;
        int destSegStart = segmentStart(windowStart);
        int destSegIdx = 0;
        for (int i = 0; i < nValues; i++) {
            final int dest = destSegStart + destSegIdx;
//            debug("dest=%d: %d -> %s", dest, tmpKeys.get(i), tmpValues.get(i));
            keys  .set(dest, tmpKeys.get(i));
            values.set(dest, tmpValues.get(i));
            destSegIdx++;
            if (destSegIdx == valuesPerSegment) {
                destSegStart+=segmentSize;
                destSegIdx = 0;
            }
        }

//        int windows = 1;
//        int curWindowWidth = wWidth;
//        for (int l = level; l >= 0; l--) {
//            for (int window = 0; window < windows; window++) {
//                debug("checking window %d (width=%d) at level %d", window, curWindowWidth, l);
//                assert intervalStats(windowStart+window*curWindowWidth, l, null);
//            }
//
//            windows *= 2;
//            curWindowWidth /= 2;
//        }
//        assertRepInvariants();
    }

    protected int getWindowWidth(final int level) {
        return (1 << level) * segmentSize;
    }

    public void put(final int key, T x) {
//        debug("put(%d, %s)", key, x);


        int idx = find(key);
        if (idx >= 0 && keys.get(idx) == key) {
            values.set(idx, x);
            return;
        }
        if (idx >= 0 && x == null && values.get(idx) == null) { return; }

        int curWindowStart = Math.max(0, segmentStart(idx));
        if (curWindowStart == keys.size()) {
            curWindowStart = 0;
        }
        assert curWindowStart >= 0;
        assert curWindowStart <= keys.size() - segmentSize;

        // Check in a window of size 'curChunkSize'
        int curChunkSize = segmentSize;
        int level = 0;

        int sz = curChunkSize - 1;

        boolean in_limit;

        if (this.isPresent(curWindowStart + segmentSize - 1)) {
            final MutableInt rsz = new MutableInt(sz);
            intervalStats(curWindowStart, level, rsz);
            sz = rsz.x;
        }

        if (sz < curChunkSize) {
            // There is some space in this interval. We can just
            // shuffle elements and put.
            this.mergeInto(curWindowStart, key, x);
        } else {
            // No space in this interval. Find an interval above this
            // interval that is within limits, re-balance, and
            // re-start insertion.
            in_limit = false;
            while (!in_limit) {
                curChunkSize *= 2;
                level++;
                if (level > this.nLevels) {
                    grow();
                    put(key, x);
                    return;
                }
                curWindowStart = this.windowStart(curWindowStart, level);
                final MutableInt rsz = new MutableInt(sz);
                in_limit = intervalStats(curWindowStart, level, rsz);
                sz = rsz.x;
//                debug("level: %d, this.nLevels: %d, in_limit: %b, sz: %d", level, this.nLevels, in_limit, sz);
            }
            this.rebalanceWindow(curWindowStart, level);
            this.put(key, x);
        }
//        assertRepInvariants();
    }

    /** Find the index {@code key}, or the first value larger than {@code key}
     * in the {@code keys} array.
     *
     * @param key the key to look for.
     * @return the index of key in the keys array, -1 if there are no keys,
     * or {@code }keys.size()} if all keys are smaller than {@code key}.
     */
    protected int find(final int key) {
        int l = 0, r = keys.size()-1;
        int m;
        int segL = 0;
        int segR = (keys.size()/segmentSize)-1;
        while (segL < segR) {
            if (!isPresent(segR * segmentSize)) {
                segR--;
                continue;
            }
            if (!isPresent(segL * segmentSize)) {
                segL++;
                continue;
            }

            int segM = segL + (segR - segL) / 2;

            while (!isPresent(segM * segmentSize) && segL < segM) {
                segM--;
            }

            final int segStart = segM * segmentSize;
            final int keyM = keys.get(segStart);
            assert keyM != EMPTY;
            if (keyM <= key) {
                for (int i = 0; i < segmentSize; ++i) {
                    final int curIdx = segStart + i;
                    final int curKey = keys.get(curIdx);
                    if (curKey >= key) {
                        return curIdx;
                    }
                }

                segL = segM + 1;
            } else {
                segR = segM;
            }
        }

        for (int i = 0; i < segmentSize; ++i) {
            final int curIdx = segL*segmentSize + i;
            final int curKey = keys.get(curIdx);
            if (curKey >= key || curKey == EMPTY || i == segmentSize - 1) {
                return curIdx;
            }
        }
        return -1;
    }

    protected int findSegmentStart(final int key) {
        return Math.max(0, segmentStart(find(key)));
    }

    protected int segmentStart(final int idx) {
        //FIXME: avoid integer division
        return fastDiv(idx, segmentSize) * segmentSize;
    }

    protected int windowStart(final int idx, int level) {
        //FIXME: avoid integer division
        final int windowWidth = segmentSize * (1 << level);
        return fastDiv(idx, windowWidth) * windowWidth;
    }

    /**
     *
     * @param idx the index the segment starts at
     * @param key the key to look for
     * @return 0, if the key is between the segment's min and max;
     * -1 if the key must be left of this segment,
     * +1 if the key must be right of this segment,
     * EMPTY, if the sgement contains no data.
     */
    /*
    private int cmpSegment(int idx, int key) {
        boolean empty = true;
        int mi = Integer.MAX_VALUE;
        int ma = Integer.MIN_VALUE;
        for (int i=idx; i < idx+segmentSize; i++) {
            final int cur = keys.get(i);
            if (cur != EMPTY) {
                if (cur < mi) { mi = cur; }
                if (cur > ma) { ma = cur; }
                empty = false;
            }
        }

        if (empty) {
            return EMPTY;
        } else {
            if (mi <= key && key <= ma) {
                return 0;
            } else if (ma < key) {
                return 1;
            } else {
                return -1;
            }
        }
    }
    */

    private int findInSegment(int idx, int key) {
        for (int i=idx; i < idx+segmentSize; i++) {
            if (keys.get(i) >= key) {
                return i;
            }
        }
        return idx+segmentSize;
    }

    public String getDensityMap() {
        final int level = Math.max(0, this.nLevels-6);
        final int windowSize = getWindowWidth(level);
        final int nWindows = keys.size() / windowSize;
        StringBuilder ret = new StringBuilder(level+" |");
        final String charMap = " .+*%#";

        for (int i = 0; i < nWindows; i++) {
            int valsInWindow = 0;
            for (int j = 0; j < windowSize; j++) {
                if (isPresent(i*windowSize + j)) {
                    valsInWindow++;
                }
            }
            double density = valsInWindow / (double) windowSize;
            ret.append(charMap.charAt((int) (density * (charMap.length() - 1))));
        }
        return ret.append("|").toString();
    }

    private int findMinInSegment(int idx) {
        int m = EMPTY;
        for (int i=idx; i < idx+segmentSize; i++) {
            final int cur = keys.get(i);
            if (cur != EMPTY && cur > m) {
                m = cur;
            }
        }
        return m;
    }

    private int findFirstLeqInSegment(int idx, int v) {
        int i;
        for (i = idx; i < idx + segmentSize; ++i) {
            if (this.isPresent(i)) {
                if (this.keys.get(i) >= v) {
                    return i;
                }
            }
        }
        return i;
    }

    private void initialise(final int capacity) {
        segmentSize = 1 << log2(log2(capacity) * 2);
        assert capacity % segmentSize == 0;
        //FIXME: speed up integer division?
        final int nSegments = capacity / segmentSize;
        nLevels = log2(nSegments);
//        debug("initialised: segmentSize: %d, nSegments: %d, nLevels: %d",
//                                 segmentSize,     nSegments,     nLevels);
//        assertPowerOfTwo(segmentSize);
    }

//    protected static void assertPowerOfTwo(int i) {
//        if (DEBUG) {
//            assert i == 1 << log2(i);
//        }
//    }

    public T get(final int key) {
        final int i = find(key);
        if (i >= 0 && keys.get(i) == key) {
            return values.get(i);
        } else {
            return null;
        }
    }

    public void clear() {
        final int capacity = keys.size();
        keys = new TIntArrayList(capacity, EMPTY);
        values = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            keys.add(EMPTY);
            values.add(null);
        }

//        assertRepInvariants();
    }
}
