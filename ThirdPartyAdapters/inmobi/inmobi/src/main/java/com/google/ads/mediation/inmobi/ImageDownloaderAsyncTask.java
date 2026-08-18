// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.inmobi;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import androidx.annotation.VisibleForTesting;
import java.io.BufferedInputStream;
import java.io.IOException;
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

  /**
   * Maximum image width in pixels (px). 1024px is chosen as a reasonable upper bound for native ad
   * image assets to ensure high quality on high-density displays while preventing excessive memory
   * consumption (OutOfMemoryError) when decoding large images.
   */
  @VisibleForTesting static final int MAX_IMAGE_WIDTH = 1024;

  /**
   * Maximum image height in pixels (px). 1024px is chosen as a reasonable upper bound for native ad
   * image assets to ensure high quality on high-density displays while preventing excessive memory
   * consumption (OutOfMemoryError) when decoding large images.
   */
  @VisibleForTesting static final int MAX_IMAGE_HEIGHT = 1024;

  @VisibleForTesting static final int STREAM_BUFFER_SIZE = MAX_IMAGE_WIDTH * MAX_IMAGE_HEIGHT;

  private final long drawableFutureTimeoutSeconds;

  private final DrawableDownloadListener listener;

  @VisibleForTesting
  final InMobiMemoryCache memoryCache = new InMobiMemoryCache();

  public ImageDownloaderAsyncTask(DrawableDownloadListener listener) {
    this.listener = listener;
    this.drawableFutureTimeoutSeconds = 10;
  }

  @VisibleForTesting
  ImageDownloaderAsyncTask(DrawableDownloadListener listener, Long timeout) {
    this.listener = listener;
    this.drawableFutureTimeoutSeconds = timeout;
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
    Drawable iconDrawable;

    try {
      if (null != memoryCache.get(String.valueOf(urlsMap.get(KEY_ICON)))) {
        iconDrawable = memoryCache.get(String.valueOf(urlsMap.get(KEY_ICON)));
      } else {
        iconDrawable = getDrawableFuture(urlsMap.get(KEY_ICON), executorService).get
            (drawableFutureTimeoutSeconds, TimeUnit.SECONDS);
        memoryCache.put(String.valueOf(urlsMap.get(KEY_ICON)), iconDrawable);
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
    return executorService.submit(
        new Callable<Drawable>() {
          @Override
          public Drawable call() throws Exception {
            try (InputStream inputStream = url.openStream();
                var bufferedInputStream = new BufferedInputStream(inputStream)) {
              bufferedInputStream.mark(STREAM_BUFFER_SIZE);

              // 1. Decode bounds first to check dimensions without allocating memory for pixels.
              var options = new BitmapFactory.Options();
              options.inJustDecodeBounds = true;
              BitmapFactory.decodeStream(bufferedInputStream, /* outPadding= */ null, options);

              // 2. Reset stream to the beginning and calculate inSampleSize.
              bufferedInputStream.reset();
              options.inSampleSize =
                  calculateInSampleSize(options, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);

              // 3. Decode the actual bitmap with inSampleSize set.
              options.inJustDecodeBounds = false;
              Bitmap bitmap =
                  BitmapFactory.decodeStream(bufferedInputStream, /* outPadding= */ null, options);

              if (bitmap == null) {
                throw new IOException("Failed to decode bitmap from URL: " + url);
              }

              // Defaulting to a scale of 1.
              bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
              return new BitmapDrawable(Resources.getSystem(), bitmap);
            }
          }
        });
  }

  /**
   * Calculates the inSampleSize value to downsample the image if it exceeds the requested bounds.
   */
  @VisibleForTesting
  static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    final int height = options.outHeight;
    final int width = options.outWidth;

    if (reqWidth <= 0 || reqHeight <= 0 || height <= 0 || width <= 0) {
      return 1;
    }

    final int heightRatio = height / reqHeight;
    final int widthRatio = width / reqWidth;

    // Find the smallest ratio to ensure both dimensions stay larger than requested.
    final int minRatio = Math.min(heightRatio, widthRatio);

    // Round down to the nearest power of 2 (returns 1 if minRatio < 1).
    return Math.max(1, Integer.highestOneBit(minRatio));
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
   */
  @Override
  protected void onPostExecute(HashMap<String, Drawable> stringDrawableHashMap) {
    super.onPostExecute(stringDrawableHashMap);
    if (stringDrawableHashMap != null) {
      // Image download successful, send on success callback.
      listener.onDownloadSuccess(stringDrawableHashMap);
    } else {
      listener.onDownloadFailure();
    }
  }


  interface DrawableDownloadListener {

    // A success callback.
    void onDownloadSuccess(HashMap<String, Drawable> drawableMap);

    // A failure callback.
    void onDownloadFailure();
  }
}
