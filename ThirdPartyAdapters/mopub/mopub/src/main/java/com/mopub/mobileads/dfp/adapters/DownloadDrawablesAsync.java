package com.mopub.mobileads.dfp.adapters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class DownloadDrawablesAsync extends AsyncTask<Object, Void, HashMap<String, Drawable>> {

    public static final String KEY_IMAGE = "image_key";
    public static final String KEY_ICON = "icon_key";
    private static final long DRAWABLE_FUTURE_TIMEOUT_SECONDS = 10;

    private DrawableDownloadListener mListener;

    public DownloadDrawablesAsync(DrawableDownloadListener listener) {
        mListener = listener;
    }

    @Override
    protected HashMap<String, Drawable> doInBackground(Object... params) {

        HashMap<String, URL> urlsMap = (HashMap<String, URL>) params[0];
        ExecutorService executorService = Executors.newCachedThreadPool();

        // Here we are using Future to download images, you can use your download logic.
        Future<Drawable> imageDrawableFuture =
                getDrawableFuture(urlsMap.get(KEY_IMAGE), executorService);
        Future<Drawable> iconDrawableFuture =
                getDrawableFuture(urlsMap.get(KEY_ICON), executorService);

        try {
            Drawable imageDrawable =
                    imageDrawableFuture.get(DRAWABLE_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Drawable iconDrawable =
                    iconDrawableFuture.get(DRAWABLE_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            HashMap<String, Drawable> drawablesMap = new HashMap<>();
            drawablesMap.put(KEY_IMAGE, imageDrawable);
            drawablesMap.put(KEY_ICON, iconDrawable);
            return drawablesMap;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.d("MoPub", "Native ad images failed to download");
            return null;
        }
    }

    private Future<Drawable> getDrawableFuture(final URL url, ExecutorService executorService) {
        // The call() will be executed as the threads in executorService's thread pool become
        // available.
        return executorService.submit(new Callable<Drawable>() {
            @Override
            public Drawable call() throws Exception {
                InputStream in = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);

                // Defaulting to a scale of 1.
                bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                return new BitmapDrawable(Resources.getSystem(), bitmap);
            }
        });
    }

    @Override
    protected void onPostExecute(HashMap<String, Drawable> drawablesMap) {
        super.onPostExecute(drawablesMap);
        if (drawablesMap != null) {
            // Image download successful, send on success callback.
            mListener.onDownloadSuccess(drawablesMap);
        } else {
            mListener.onDownloadFailure();
        }
    }
}

