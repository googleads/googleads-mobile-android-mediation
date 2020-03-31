package com.google.ads.mediation.inmobi;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
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

/**
 * An {@link AsyncTask} used to load images for InMobi native adapter.
 */
class ImageDownloaderAsyncTask extends AsyncTask<Object, Void, HashMap<String, Drawable>> {

  static final String KEY_IMAGE = "image_key";

  static final String KEY_ICON = "icon_key";

  private static final long DRAWABLE_FUTURE_TIMEOUT_SECONDS = 10;

  private final DrawableDownloadListener mListener;

  private final InMobiMemoryCache mMemoryCache = new InMobiMemoryCache();

  public ImageDownloaderAsyncTask(DrawableDownloadListener listener) {
    mListener = listener;
  }

  /**
   * Override this method to perform a computation on a background thread. The specified parameters
   * are the parameters passed to {@link #execute} by the caller of this task.
   * <p/>
   * This method can call {@link #publishProgress} to publish updates on the UI thread.
   *
   * @param params The parameters of the task.
   * @return A result, defined by the subclass of this task.
   * @see #onPreExecute()
   * @see #onPostExecute
   * @see #publishProgress
   */
  @Override
  protected HashMap<String, Drawable> doInBackground(Object... params) {
    HashMap<String, URL> urlsMap = (HashMap<String, URL>) params[0];
    ExecutorService executorService = Executors.newCachedThreadPool();
    Drawable imageDrawable;
    Drawable iconDrawable;

    try {
      if (null != mMemoryCache.get(String.valueOf(urlsMap.get(KEY_ICON)))) {
        iconDrawable = mMemoryCache.get(String.valueOf(urlsMap.get(KEY_ICON)));
      } else {
        iconDrawable = getDrawableFuture(urlsMap.get(KEY_ICON), executorService).get
            (DRAWABLE_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mMemoryCache.put(String.valueOf(urlsMap.get(KEY_ICON)), iconDrawable);
      }

      HashMap<String, Drawable> drawableHashMap = new HashMap<>();
      drawableHashMap.put(KEY_ICON, iconDrawable);

      return drawableHashMap;
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      e.printStackTrace();
      return null;
    }
  }

  private Future<Drawable> getDrawableFuture(final URL url, ExecutorService executorService) {
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

  /**
   * <p>Runs on the UI thread after {@link #doInBackground}. The
   * specified result is the value returned by {@link #doInBackground}.</p>
   * <p/>
   * <p>This method won't be invoked if the task was cancelled.</p>
   *
   * @param stringDrawableHashMap The result of the operation computed by {@link #doInBackground}.
   * @see #onPreExecute
   * @see #doInBackground
   * @see #onCancelled(Object)
   */
  @Override
  protected void onPostExecute(HashMap<String, Drawable> stringDrawableHashMap) {
    super.onPostExecute(stringDrawableHashMap);
    if (stringDrawableHashMap != null) {
      // Image download successful, send on success callback.
      mListener.onDownloadSuccess(stringDrawableHashMap);
    } else {
      mListener.onDownloadFailure();
    }
  }


  interface DrawableDownloadListener {

    // A success callback.
    void onDownloadSuccess(HashMap<String, Drawable> drawableMap);

    // A failure callback.
    void onDownloadFailure();
  }
}
