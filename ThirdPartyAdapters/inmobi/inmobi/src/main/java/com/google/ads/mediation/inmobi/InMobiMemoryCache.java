package com.google.ads.mediation.inmobi;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is used to cache images loaded/shown by InMobi adapter.
 */
class InMobiMemoryCache {

  private static final String TAG = "MemoryCache";
  // Last argument true for LRU
  private final Map<String, Drawable> mCache = Collections.synchronizedMap(
      new LinkedHashMap<String, Drawable>(10, 1.5f, true));
  // ordering.
  private long mSize = 0; // Current allocated size.
  private long mLimit = 1000000; // Max memory in bytes.

  InMobiMemoryCache() {
    // Use 25% of available heap size.
    setLimit(Runtime.getRuntime().maxMemory() / 4);
  }

  private void setLimit(long new_limit) {
    mLimit = new_limit;
    Log.i(TAG, "MemoryCache will use up to " + mLimit / 1024. / 1024. + "MB");
  }

  public Drawable get(String id) {
    try {
      if (!mCache.containsKey(id)) {
        return null;
      }
      // NullPointerException sometimes happen here
      // http://code.google.com/p/osmdroid/issues/detail?id=78
      return mCache.get(id);
    } catch (NullPointerException ex) {
      ex.printStackTrace();
      return null;
    }
  }

  void put(String id, Drawable drawable) {
    try {
      if (mCache.containsKey(id)) {
        mSize -= getSizeInBytes(((BitmapDrawable) mCache.get(id)).getBitmap());
      }
      mCache.put(id, drawable);
      mSize += getSizeInBytes(((BitmapDrawable) drawable).getBitmap());
      checkSize();
      Log.d(TAG, "Drawable used from cache");
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  private void checkSize() {
    Log.i(TAG, "cache size=" + mSize + " length=" + mCache.size());
    if (mSize > mLimit) {
      Iterator<Map.Entry<String, Drawable>> iter = mCache.entrySet().iterator();//least
      // recently accessed item will be the first one iterated
      while (iter.hasNext()) {
        Map.Entry<String, Drawable> entry = iter.next();
        mSize -= getSizeInBytes(((BitmapDrawable) entry.getValue()).getBitmap());
        iter.remove();
        if (mSize <= mLimit) {
          break;
        }
      }
      Log.i(TAG, "Clean cache. New size " + mCache.size());
    }
  }

  public void clear() {
    try {
      // NullPointerException sometimes happen here
      // http://code.google.com/p/osmdroid/issues/detail?id=78
      mCache.clear();
      mSize = 0;
    } catch (NullPointerException ex) {
      ex.printStackTrace();
    }
  }

  private long getSizeInBytes(Bitmap bitmap) {
    if (bitmap == null) {
      return 0;
    }
    return bitmap.getRowBytes() * bitmap.getHeight();
  }
}
