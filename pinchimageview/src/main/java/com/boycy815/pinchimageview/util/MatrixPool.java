package com.boycy815.pinchimageview.util;


import android.graphics.Matrix;

/**
 * 矩阵对象池
 */
public class MatrixPool extends ObjectsPool<Matrix> {

    public MatrixPool(int size) {
        super(size);
    }

    @Override
    protected Matrix newInstance() {
        return new Matrix();
    }

    @Override
    protected Matrix resetInstance(Matrix obj) {
        obj.reset();
        return obj;
    }
}
