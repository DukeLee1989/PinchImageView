package com.boycy815.pinchimageview.util;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

/**
 * ＊@title
 * PinchImageView
 * com.boycy815.pinchimageview.util
 * Created by lee on 16/6/18.
 * Email:lee131483@gmail.com
 */
public class BitmapUtils {

    private BitmapUtils() {

    }

    public static byte[] getBitmap2Bytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }
}
