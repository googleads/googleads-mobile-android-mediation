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

import com.duapps.ad.DuMediaVIew;
import com.duapps.ad.DuNativeAd;
import com.google.ads.mediation.dap.DuAdMediation;
import com.google.ads.mediation.dap.DuNativeAdAdapter;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;

import java.io.InputStream;
import java.net.URL;
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
    private DuMediaVIew mDuMediaView;

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

        Bundle extras = new Bundle();
        extras.putString(DuNativeAdAdapter.KEY_SOURCE, mNativeAd.getSource());
        setExtras(extras);

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(false);

        boolean urlsOnly = false;
        if (mNativeAdOptions != null) {
            urlsOnly = mNativeAdOptions.shouldReturnUrlsForImageAssets();
        }

        mDuMediaView = new DuMediaVIew(mContext);
        if (!urlsOnly) {
            setMediaView(mDuMediaView);
        }

        new DownloadDrawablesAsync(mNativeAdMapperListener)
                .execute(DuNativeAdMapper.this);
    }

    @Override
    public void trackView(View view) {
        mNativeAd.registerViewForInteraction(view, mDuMediaView);
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
        private NativeAdMapperListener mDrawableListener;

        public DownloadDrawablesAsync(NativeAdMapperListener listener) {
            this.mDrawableListener = listener;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            DuNativeAdMapper mapper = (DuNativeAdMapper) params[0];
            ExecutorService executorService = Executors.newCachedThreadPool();

            // Download icon image
            DuNativeMappedImage iconImage = (DuNativeMappedImage) mapper.getIcon();
            Uri uri = iconImage.getUri();
            Future<Drawable> drawableFuture = getDrawableFuture(uri, executorService);
            Drawable drawable = null;
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
