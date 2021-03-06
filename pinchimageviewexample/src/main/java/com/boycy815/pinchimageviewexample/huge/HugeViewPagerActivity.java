package com.boycy815.pinchimageviewexample.huge;

import android.app.Activity;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;

import com.boycy815.pinchimageview.PinchImageView;
import com.boycy815.pinchimageview.huge.HugeImageRegionLoader;
import com.boycy815.pinchimageview.huge.TileDrawable;
import com.boycy815.pinchimageviewexample.R;


public class HugeViewPagerActivity extends Activity {

    private TileDrawable mTileDrawable;

    private String mPicUrl="http://ww4.sinaimg.cn/bmiddle/005SiNxygw1f4lpwlbo8uj30bi7dvb29.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_huge);
        final PinchImageView pinchImageView = (PinchImageView) findViewById(R.id.pic);
        pinchImageView.post(new Runnable() {
            @Override
            public void run() {
                mTileDrawable = new TileDrawable();
                mTileDrawable.setInitCallback(new TileDrawable.InitCallback() {
                    @Override
                    public void onInit() {
                        pinchImageView.setImageDrawable(mTileDrawable);
                    }
                });
                mTileDrawable.init(new HugeImageRegionLoader(HugeViewPagerActivity.this, Uri.parse("file:///android_asset/card.png")), new Point(pinchImageView.getWidth(), pinchImageView.getHeight()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mTileDrawable != null) {
            mTileDrawable.recycle();
        }
        super.onDestroy();
    }
}