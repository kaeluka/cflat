package com.github.kaeluka.cflat.storage;

import com.github.kaeluka.cflat.storage.size.ObjectSizes;
import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.HashMap;

@SuppressWarnings({"WeakerAccess", "unchecked", "unused"})
public class HashMapStorage<T> extends Storage<T> {
    private final TIntObjectHashMap<T> data;
    private int maxIdx = -1;

    @Override
    public T get(final int i) {
        assert i >= 0;
        return data.get(i);
    }

    public HashMapStorage() {
        this(new TIntObjectHashMap<>(10, Constants.DEFAULT_LOAD_FACTOR));
//        this(new IntHashMap<>());
    }

    private HashMapStorage(final TIntObjectHashMap<T> data) {
        this.data = data;
    }

    @Override
    public Storage<T> set(final int i, final T x) {
        assert i >= 0;
        if (x == null) {
            if (i <= maxIdx) {
                data.remove(i);
            }
        } else {
//            System.out.println("setting " + i + "->" + x + " (hash: " + x.hashCode() + ")");
            if (i > maxIdx) {
                maxIdx = i;
            }
            data.put(i, x);
        }
        return this;
    }

    @Override
    public Storage<T> clear() {
        data.clear();
        return this;
    }

    @Override
    public int sizeOverApproximation() {
        return maxIdx+1;
    }

    @Override
    public Storage<T> emptyCopy() {
        return new HashMapStorage<>(new TIntObjectHashMap<>(10, Constants.DEFAULT_LOAD_FACTOR));
//        return new HashMapStorage<>(new IntHashMap<>());
    }

    @Override
    public long bytesUsed() {
        return ObjectSizes.TINTOBJECTHASHMAP_SIZE(this.data);
    }
}

final class IntHashMap<V> extends TIntObjectHashMap<V> {
//    public int collisions = 0;
//    public int noncollisions = 0;

    IntHashMap() {
        super(2, 0.33f);
        rehash(16);
    }

    @Override
    public V put( int key, V value ) {
        int index = insertKey(key);
        return _doPut( value, index );
    }

    private V _doPut( V value, int index ) {
        V previous = null;
        boolean isNewMapping = true;
        if ( index < 0 ) {
            index = -index -1;
            previous = _values[index];
            isNewMapping = false;
        }

        _values[index] = value;

        if (isNewMapping) {
            _postInsertHook( consumeFreeSlot );
        }

        return previous;
    }


    private void _postInsertHook(boolean usedFreeSlot) {
        if ( usedFreeSlot ) {
            _free--;
        }

        // rehash whenever we exhaust the available space in the table
        if ( ++_size > _maxSize || _free == 0 ) {
            // choose a new capacity suited to the new state of the table
            // if we've grown beyond our maximum size, double capacity;
            // if we've exhausted the free spots, rehash to the same capacity,
            // which will free up any stale removed slots for reuse.
            int newCapacity = capacity() << 1;
            rehash( newCapacity );
            computeMaxSize( capacity() );
        }
    }

    @Override
    protected int insertKey( int val ) {
        int hash, index;

        hash = val;
        index = hash & (_states.length - 1); //hash % _states.length;
        byte state = _states[index];

        consumeFreeSlot = false;

        if (state == FREE) {
            consumeFreeSlot = true;
            _insertKeyAt(index, val);
//            this.noncollisions++;
            return index;       // empty, all done
        }

        if (state == FULL && _set[index] == val) {
//            this.noncollisions++;
            return -index - 1;   // already stored
        }

//        this.collisions++;
//        System.out.println("collisions: "+(collisions*100.0/(collisions+noncollisions))+"%");
//        if (collisions+noncollisions >= 1000) {
//            collisions = 0;
//            noncollisions = 0;
//        }

        // already FULL or REMOVED, must probe
        if (state == FULL) {
//            System.out.println("already have " + _set[index] + " at this loc");
        }
        return _insertKeyRehash(val, index, state, hash);
    }

    private int _insertKeyRehash(int val, int index, byte state, int hash) {
        // compute the double hash
//        int probe = 1 + (hash % (_set.length - 2));
        final int loopIndex = index;
        int firstRemoved = -1;

        /*
         * Look until FREE slot or we start to loop
         */
        do {
            // Identify first removed slot
            if (state == REMOVED && firstRemoved == -1)
                firstRemoved = index;

            index--;
            if (index < 0) {
                index += _set.length;
            }
            state = _states[index];

            // A FREE slot stops the search
            if (state == FREE) {
                if (firstRemoved != -1) {
                    _insertKeyAt(firstRemoved, val);
                    return firstRemoved;
                } else {
                    consumeFreeSlot = true;
                    _insertKeyAt(index, val);
                    return index;
                }
            }

            if (state == FULL && _set[index] == val) {
                return -index - 1;
            }

            // Detect loop
        } while (index != loopIndex);

        // We inspected all reachable slots and did not find a FREE one
        // If we found a REMOVED slot we return the first one found
        if (firstRemoved != -1) {
            _insertKeyAt(firstRemoved, val);
            return firstRemoved;
        }

        // Can a resizing strategy be found that resizes the set?
        throw new IllegalStateException("No free or removed slots available. Key set full?!!");
    }

    private void _insertKeyAt(int index, int val) {
        _set[index] = val;  // insert value
        _states[index] = FULL;
    }

    @Override
    protected int index(final int val) {
        int hash, index, length;

        final byte[] states = _states;
        final int[] set = _set;
        length = states.length;
//        hash = HashFunctions.hash( val ) & 0x7fffffff;
        hash = val;
//        hash = val ^ (val >>> 16) & 0x7fffffff;
//        index = hash % length;
        index = hash & (length - 1);
//        if (index != (hash % length)) {
//            throw new AssertionError("length = "+length+", index "+index+", expr "+(hash & (length-1)));
//        }
//        index = hash & (length-1);
        byte state = states[index];

        if (state == FREE)
            return -1;

        if (state == FULL && set[index] == val)
            return index;

        return _indexRehashed(val, index, hash);
    }

    private int _indexRehashed(int key, int index, int hash) {
        // see Knuth, p. 529
        final int length = _set.length;
        final int probe = 1 + (hash % (length - 2));
        final int loopIndex = index;

        do {
            index--;
            if (index < 0) {
                index += length;
            }
            final byte state = _states[index];
            //
            if (state == FREE)
                return -1;

            //
            if (key == _set[index] && state != REMOVED)
                return index;
        } while (index != loopIndex);

        return -1;
    }

    @Override
    protected void rehash(int newCapacity) {
        // we'll keep the capacities powers of two
        int oldCapacity = _set.length;
        newCapacity = 1 << (1 + 32 - Integer.numberOfLeadingZeros(oldCapacity - 1));
//        System.out.println("capacity " + oldCapacity + " -> " + newCapacity);

        super.rehash(newCapacity);

    }
}
