package com.boycy815.pinchimageview.util;

import android.graphics.RectF;

/**
 * 矩形对象池
 */
public class RectFPool extends ObjectsPool<RectF> {

    public RectFPool(int size) {
        super(size);
    }

    @Override
    protected RectF newInstance() {
        return new RectF();
    }

    @Override
    protected RectF resetInstance(RectF obj) {
        obj.setEmpty();
        return obj;
    }
}