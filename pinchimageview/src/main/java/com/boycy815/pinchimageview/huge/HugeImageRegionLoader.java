package com.boycy815.pinchimageview.huge;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.boycy815.pinchimageview.util.BitmapUtils;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by clifford on 16/4/6.
 */
public class HugeImageRegionLoader extends ImageRegionLoader {

    private static final String FILE_PREFIX = "file://";
    private static final String ASSET_PREFIX = FILE_PREFIX + "/android_asset/";
    private static final String RESOURCE_PREFIX = ContentResolver.SCHEME_ANDROID_RESOURCE + "://";

    private int mWidth;
    private int mHeight;
    private BitmapRegionDecoder mDecoder;

    private Context mContext;
    private Uri mUri;

    private boolean mIniting;
    private boolean mRecyled;

    private DraweeHolder<GenericDraweeHierarchy> mDraweeHolder;
    private CloseableReference<CloseableImage> imageReference = null;


    public HugeImageRegionLoader(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
        GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(context.getResources())
                .setFadeDuration(300)
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                //.setProgressBarImage(new CustomProgressbarDrawable(this))
                .build();
        mDraweeHolder = DraweeHolder.create(hierarchy, context);
        mDraweeHolder.onAttach();
    }

    @Override
    public void init() {
        if (!mIniting) {
            mIniting = true;
            loadImage(mUri);
        }
    }

    public void loadImage(Uri uri) {
        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(uri).build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        final DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, this);
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setOldController(mDraweeHolder.getController())
                .setImageRequest(imageRequest)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String s, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
                        try {
                            imageReference = dataSource.getResult();
                            if (imageReference != null) {
                                CloseableImage image = imageReference.get();
                                // do something with the image
                                if (image != null && image instanceof CloseableStaticBitmap) {
                                    CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) image;
                                    Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
                                    if (bitmap != null) {
                                        byte[] bytes = BitmapUtils.getBitmap2Bytes(bitmap);
                                        new InitTaskByFresco().execute(bytes);
                                    }
                                }
                            }
                        } finally {
                            dataSource.close();
                            CloseableReference.closeSafely(imageReference);
                        }
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        super.onFailure(id, throwable);
                    }
                })
                .setTapToRetryEnabled(true)
                .build();
        mDraweeHolder.setController(controller);
    }

    private class InitTask extends AsyncTask<InputStream, Void, BitmapRegionDecoder> {

        @Override
        protected BitmapRegionDecoder doInBackground(InputStream... params) {
            try {
                return BitmapRegionDecoder.newInstance(params[0], false);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    params[0].close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(BitmapRegionDecoder result) {
            if (!mRecyled) {
                mDecoder = result;
                if (mDecoder != null) {
                    mWidth = mDecoder.getWidth();
                    mHeight = mDecoder.getHeight();
                    dispatchInited();
                }
            }
        }
    }

    private class InitTaskByFresco extends AsyncTask<byte[], Void, BitmapRegionDecoder> {

        @Override
        protected BitmapRegionDecoder doInBackground(byte[]... params) {
            try {
                return BitmapRegionDecoder.newInstance(params[0], 0, params[0].length, false);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(BitmapRegionDecoder result) {
            if (!mRecyled) {
                mDecoder = result;
                if (mDecoder != null) {
                    mWidth = mDecoder.getWidth();
                    mHeight = mDecoder.getHeight();
                    dispatchInited();
                }
            }
        }
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    private Map<String, Boolean> mRecycleCommands = new HashMap<String, Boolean>();
    private Map<String, Bitmap> mLoadedBitmaps = new HashMap<String, Bitmap>();

    private String genKey(int id, int sampleSize) {
        return sampleSize + ":" + id;
    }

    @Override
    public void loadRegion(int id, int sampleSize, Rect sampleRect) {
        if (mDecoder != null) {
            String key = genKey(id, sampleSize);
            if (mRecycleCommands.containsKey(key)) {
                mRecycleCommands.remove(key);
            }
            if (mLoadedBitmaps.containsKey(key)) {
                dispatchRegionLoad(id, sampleSize, sampleRect, mLoadedBitmaps.get(key));
            } else {
                (new LoadTask(id, sampleSize, sampleRect)).execute();
            }
        }
    }

    private class LoadTask extends AsyncTask<Void, Void, Bitmap> {

        private int mId;
        private int mSampleSize;
        private Rect mSampleRect;

        public LoadTask(int id, int sampleSize, Rect sampleRect) {
            mId = id;
            mSampleSize = sampleSize;
            mSampleRect = sampleRect;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            if (mDecoder != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = mSampleSize;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                return mDecoder.decodeRegion(mSampleRect, options);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && mDecoder != null) {
                String key = genKey(mId, mSampleSize);
                if (!mRecycleCommands.containsKey(key)) {
                    if (mLoadedBitmaps.containsKey(key)) {
                        mLoadedBitmaps.get(key).recycle();
                    }
                    mLoadedBitmaps.put(key, result);
                    dispatchRegionLoad(mId, mSampleSize, mSampleRect, result);
                }
            }
        }
    }

    @Override
    public void recycleRegion(int id, int sampleSize, Rect sampleRect) {
        if (mDecoder != null) {
            String key = genKey(id, sampleSize);
            Bitmap bitmap = mLoadedBitmaps.get(key);
            if (bitmap != null) {
                mLoadedBitmaps.remove(key);
                bitmap.recycle();
            } else {
                mRecycleCommands.put(key, true);
            }
        }
    }

    @Override
    public void recycle() {
        mRecyled = true;
        if (mDecoder != null) {
            mDecoder.recycle();
            mDecoder = null;
        }
        for (Bitmap bitmap : mLoadedBitmaps.values()) {
            bitmap.recycle();
        }
        mLoadedBitmaps.clear();
        mDraweeHolder.onDetach();
    }
}