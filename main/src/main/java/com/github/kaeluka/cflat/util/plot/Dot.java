package com.github.kaeluka.cflat.util.plot;

import com.github.kaeluka.cflat.storage.Storage;
import com.github.kaeluka.cflat.traversal.GenericShape;

import java.util.function.Function;

public class Dot {
    public static <T> String from(final Storage<T> st,
                                  final Object[] shape,
                                  final Function<Integer, String> attributes) {
        final StringBuilder ret = new StringBuilder();
        ret.append("digraph {\n");
        st.foreachNonNull(i -> {
            if (i > 0) {
                int parent = GenericShape.parentOf(i, shape);
                ret.append(String.format("    %d -> %d;\n", parent, i));
            }
        });
        ret.append("\n");
        st.foreachNonNull(i -> {
            ret.append(String.format("    %d [%s];\n", i, attributes.apply(i)));
        });
        ret.append("}");
        return ret.toString();
    }
    public static <T> String from(final Storage<T> st, final Object[] shape) {
        return from(st, shape, i -> String.format("label=\"%d: %s\"", i, st.get(i).toString()));
//        final StringBuilder ret = new StringBuilder();
//        ret.append("digraph {\n");
//        st.foreachNonNull(i -> {
//            if (i > 0) {
//                int parent = GenericShape.parentOf(i, shape);
//                ret.append(String.format("    %d -> %d;\n", parent, i));
//            }
//        });
//        ret.append("\n");
//        st.foreachNonNull(i -> {
//            ret.append(String.format("    %d [label=\"%d: %s\"];\n", i, i, st.get(i).toString()));
//        });
//        ret.append("}");
//        return ret.toString();
    }
}
