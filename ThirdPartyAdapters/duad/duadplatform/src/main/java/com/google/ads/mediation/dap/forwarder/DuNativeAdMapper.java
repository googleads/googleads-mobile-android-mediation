package com.google.ads.mediation.dap.forwarder;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

import com.duapps.ad.DuNativeAd;
import com.google.ads.mediation.dap.DuAdMediation;
import com.google.ads.mediation.dap.DuNativeAdAdapter;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DuNativeAdMapper extends NativeAppInstallAdMapper {
    private static final String TAG = DuNativeAdAdapter.class.getSimpleName();

    private final DuNativeAd mNativeAd;
    private final NativeAdOptions mNativeAdOptions;
    private final Context mContext;
    private NativeAdMapperListener mNativeAdMapperListener;

    private static final int DRAWABLE_FUTURE_TIMEOUT_SECONDS = 10;

    public DuNativeAdMapper(Context context, DuNativeAd nativeAd, NativeAdOptions nativeAdOptions) {
        mContext = context;
        mNativeAd = nativeAd;
        mNativeAdOptions = nativeAdOptions;
    }

    public void mapNativeAd(NativeAdMapperListener listener) {
        mNativeAdMapperListener = listener;
        setHeadline(mNativeAd.getTitle());
        setBody(mNativeAd.getShortDesc());
        setCallToAction(mNativeAd.getCallToAction());
        setStarRating(mNativeAd.getRatings());
        setIcon(new DuNativeMappedImage(Uri.parse(mNativeAd.getIconUrl())));
        List<NativeAd.Image> imageList = new ArrayList<>();

        imageList.add(new DuNativeMappedImage(Uri.parse(mNativeAd.getImageUrl())));
        setImages(imageList);

        Bundle extras = new Bundle();
        extras.putString(DuNativeAdAdapter.KEY_SOURCE, mNativeAd.getSource());
        setExtras(extras);

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(false);

        boolean urlsOnly = false;
        if (mNativeAdOptions != null) {
            urlsOnly = mNativeAdOptions.shouldReturnUrlsForImageAssets();
        }

        new DownloadDrawablesAsync(mContext, mNativeAdMapperListener, urlsOnly)
                .execute(DuNativeAdMapper.this);
    }

    @Override
    public void trackView(View view) {
        mNativeAd.registerViewForInteraction(view);
    }

    @Override
    public void untrackView(View view) {
        mNativeAd.unregisterView();
    }

    public interface NativeAdMapperListener {
        void onMappingSuccess();
        void onMappingFailed();
    }

    private static class DownloadDrawablesAsync extends AsyncTask<Object, Void, Boolean> {
        private final Context mContext;
        private NativeAdMapperListener mDrawableListener;
        private boolean mUrlsOnly;

        public DownloadDrawablesAsync(Context context,
                                      NativeAdMapperListener listener,
                                      boolean urlsOnly) {
            this.mContext = context;
            this.mDrawableListener = listener;
            this.mUrlsOnly = urlsOnly;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            DuNativeAdMapper mapper = (DuNativeAdMapper) params[0];
            ExecutorService executorService = Executors.newCachedThreadPool();

            // Download ad image
            DuNativeMappedImage image = (DuNativeMappedImage) mapper.getImages().get(0);
            Uri uri = image.getUri();
            Future<Drawable> drawableFuture = getDrawableFuture(uri, executorService);
            Drawable drawable = null;
            DuAdMediation.debugLog(TAG, "start to download ad image: " + uri);
            try {
                drawable = drawableFuture.get(DRAWABLE_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                return false;
            }

            image.setDrawable(drawable);
            if (!mUrlsOnly) {
                ImageView imageView = new ImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageDrawable(drawable);
                mapper.setMediaView(imageView);
            }

            // Download icon image
            DuNativeMappedImage iconImage = (DuNativeMappedImage) mapper.getIcon();
            uri = iconImage.getUri();
            drawableFuture = getDrawableFuture(uri, executorService);
            DuAdMediation.debugLog(TAG, "start to download icon image: " + uri);
            try {
                drawable = drawableFuture.get(DRAWABLE_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                return false;
            }
            iconImage.setDrawable(drawable);

            return true;
        }

        private Future<Drawable> getDrawableFuture(final Uri uri, ExecutorService executorService) {
            return executorService.submit(new Callable<Drawable>() {
                @Override
                public Drawable call() throws Exception {
                    InputStream in = new URL(uri.toString()).openStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(in);

                    bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                    return new BitmapDrawable(Resources.getSystem(), bitmap);
                }
            });
        }

        @Override
        protected void onPostExecute(Boolean isDownloadSuccessful) {
            super.onPostExecute(isDownloadSuccessful);
            if (isDownloadSuccessful) {
                mDrawableListener.onMappingSuccess();
            } else {
                mDrawableListener.onMappingFailed();
            }
        }
    }
}
