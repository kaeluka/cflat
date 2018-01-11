package com.github.kaeluka.cflat.storage;

import java.util.Spliterator;

public interface Storage<T> {
   public T get(long i);
   public Storage<T> set(long i, T x);
   public Storage<T> clear();

   public default Spliterator<T> reachable(long start, Object[] shape) {
       final IndexSpliterator children = StorageUtil.childIndices(start, shape);

       return null;
   }


//        def mapT(f : t => t) : ScalaStorage[t]
//        def map[u](f : t => u) : ScalaStorage[u]
//        def foreach(f : t => Unit) : Unit
//    }
}

