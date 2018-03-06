package com.github.kaeluka.cflat.traversal;

import scala.Int;

public class GenericShape {
    private final static int INFINITE_SIZE = Integer.MIN_VALUE;

    public static boolean isStar(final Object[] shape) {
        return (int) shape[0] == INFINITE_SIZE && shape.length == 3;
    }

    public static Object[] mkStar(Object inner, Object after) {
        return new Object[]{INFINITE_SIZE, inner, after};
    }

    public static boolean isRep(final Object[] shape) {
        return (int) shape[0] > 0 && shape.length == 3;
    }

    public static Object[] mkRep(int n, Object inner, Object after) {
        return new Object[]{n, inner, after};
    }

    public static int getRepN(final Object[] shape) {
        assert(isRep(shape) || isStar(shape));
        return (int) shape[0];
    }
//    public static Object repStep(final Object[] shape, int n) {
//        final Object inner = getInner(shape);
//        final Object after = getAfter(shape);
//        final int innerSz = (inner instanceof Integer) ? (int)inner : shapeSize((Object[])inner);
//        assert(n <= innerSz);
//        if (n == innerSz) {
//            return after;
//        } else {
//            if (inner instanceof Integer) {
//                return 0;
//            } else {
//
//                return mkRep(getRepN(shape)-1,shapeStep((Object[])inner, n), after);
//            }
//            return inner instanceof Integer ? 0 :
//        }
//    }

    public static Object getInner(final Object[] shape) {
        assert(isRep(shape) || isStar(shape));
        return (int) shape[1];
    }
    public static int getAfter(final Object[] shape) {
        assert(isRep(shape) || isStar(shape));
        return (int) shape[2];
    }

    public static boolean isAlt(final Object[] shape) {
        return (int) shape[0] == -1 && shape.length > 1;
    }

    public static int altSize(final Object[] shape) {
        return shape.length - 1;
    }

    public static Object altStep(final Object[] shape, int n) {
        return shape[n+1];
    }

    public static Object[] mkAlt(Object... alts) {
        Object[] ret = new Object[alts.length+1];
        ret[0] = -1;
        int i=1;
        for (Object a : alts) {
            ret[i++] = a;
        }
        return ret;
    }

    public static boolean isSimpleAlternative(final Object[] shape) {
        return shape == null;
    }

    public static int parentOf(final int idx, final Object[] shape) {
        assert(isRep(shape) || isStar(shape));
        int branchingFactor = (shape[1] instanceof Integer) ? (int)shape[1] : shapeSize((Object[])shape[1]);
        return (idx - 1) / branchingFactor;
    }

    public static boolean isParentOfTransitive(final int predecessor, final int successor, final Object[] shape) {
        int iter = successor;
        while (iter != 0) {
            if (iter == predecessor) {
                return true;
            }
            iter = parentOf(iter, shape);
        }
        return false;
    }


//    public static int nStepsShape(final Object[] shape) {
//        if (isAlt(shape)) {
//            return shape.length - 1;
//        } else if (isRep(shape)) {
//            return 0; //continuehere
//        }
//        throw new UnsupportedOperationException();
//    }
//
//    public static Object[] shapeStep(Object[] shape, int n) {
//        throw new UnsupportedOperationException();
//    }

    public static int shapeSize(final Object shape) {
        if (shape instanceof Object[]) {
            final Object[] shapeArr = (Object[]) shape;
            if (isStar(shapeArr)) {
                return INFINITE_SIZE;
            } else {
                int sz = 0;
                if (isAlt(shapeArr)) {
                    for (int i = 1; i < shapeArr.length; i++) {
                        if (shapeArr[i] instanceof Integer) {
                            sz += (int) shapeArr[i];
                        } else {
                            sz += shapeSize((Object[]) shapeArr[i]);
                        }
                    }
                } else {
                    assert (isRep(shapeArr));
                    int innerSz = (shapeArr[1] instanceof Object[]) ? shapeSize((Object[]) shapeArr[1]) : (int) shapeArr[1];
                    int afterSz = (shapeArr[2] instanceof Object[]) ? shapeSize((Object[]) shapeArr[2]) : (int) shapeArr[2];
                    return innerSz * (int) shapeArr[0] + afterSz;
                }
                return sz;
            }
        } else {
            return (int)shape;
        }
    }

    public static long cantorPairingFunction(final int k1, final int k2) {
        return ((long)k1+k2)*(k1+k2+1)/2 + k2;
    }

    public static int[] cantorPairingFunctionRev(final long z) {
        final long w = (long) Math.floor((Math.sqrt(8 * z + 1) - 1) / 2);
        final long t = (w * w + w) / 2;
        final long y = z - t;
        final long x = w - y;
        return new int[]{Math.toIntExact(x), Math.toIntExact(y)};
    }
}
