package com.google.ads.mediation.dap.forwarder;

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

import com.duapps.ad.DuNativeAd;
import com.google.ads.mediation.dap.DuAdAdapter;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DuNativeAdMapper extends NativeAppInstallAdMapper {
    private static final String TAG = "DuNativeAdMapper";
    private final DuNativeAd mNativeAd;
    private NativeAdMapperListener mNativeAdMapperListener;
    private static final int DRAWABLE_FUTURE_TIMEOUT_SECONDS = 10;

    public DuNativeAdMapper(DuNativeAd nativeAd) {
        mNativeAd = nativeAd;
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
        extras.putString(DuAdAdapter.KEY_SOURCE, mNativeAd.getSource());
        setExtras(extras);

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(false);

        boolean urlsOnly = false;
        if (urlsOnly) {
            mNativeAdMapperListener.onMappingSuccess();
        } else {
            new DownloadDrawablesAsync(mNativeAdMapperListener).execute(DuNativeAdMapper.this);
        }

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
        private NativeAdMapperListener mDrawableListener;

        public DownloadDrawablesAsync(NativeAdMapperListener listener) {
            this.mDrawableListener = listener;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            DuNativeAdMapper mapper = (DuNativeAdMapper) params[0];
            ExecutorService executorService = Executors.newCachedThreadPool();

            HashMap<DuNativeMappedImage, Future<Drawable>> futuresMap = new HashMap<>();

            List<NativeAd.Image> images = mapper.getImages();
            for (int i = 0; i < images.size(); i++) {
                DuNativeMappedImage image = (DuNativeMappedImage) images.get(i);
                Future<Drawable> drawableFuture = getDrawableFuture(image.getUri(), executorService);
                futuresMap.put(image, drawableFuture);
            }

            DuNativeMappedImage iconImage = (DuNativeMappedImage) mapper.getIcon();
            Future<Drawable> drawableFuture = getDrawableFuture(iconImage.getUri(), executorService);
            futuresMap.put(iconImage, drawableFuture);

            for (Map.Entry<DuNativeMappedImage, Future<Drawable>> pair : futuresMap.entrySet()) {
                Drawable drawable;
                try {
                    drawable = pair.getValue().get(DRAWABLE_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                    return false;
                }
                pair.getKey().setDrawable(drawable);
            }

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
