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
 * Created by vineet.srivastava
 */
class InMobiMemoryCache {
    private static final String TAG = "MemoryCache";
    private final Map<String, Drawable> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, Drawable>(10, 1.5f, true));//Last argument true for LRU
    // ordering
    private long size = 0;//current allocated size
    private long limit = 1000000;//max memory in bytes

    InMobiMemoryCache() {
        //use 25% of available heap size
        setLimit(Runtime.getRuntime().maxMemory() / 4);
    }

    private void setLimit(long new_limit) {
        limit = new_limit;
        Log.i(TAG, "MemoryCache will use up to " + limit / 1024. / 1024. + "MB");
    }

    public Drawable get(String id) {
        try {
            if (!cache.containsKey(id))
                return null;
            //NullPointerException sometimes happen here http://code.google
            // .com/p/osmdroid/issues/detail?id=78
            return cache.get(id);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    void put(String id, Drawable drawable) {
        try {
            if (cache.containsKey(id))
                size -= getSizeInBytes(((BitmapDrawable) cache.get(id)).getBitmap());
            cache.put(id, drawable);
            size += getSizeInBytes(((BitmapDrawable) drawable).getBitmap());
            checkSize();
            Log.d(TAG, "Drawable used from cache");
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private void checkSize() {
        Log.i(TAG, "cache size=" + size + " length=" + cache.size());
        if (size > limit) {
            Iterator<Map.Entry<String, Drawable>> iter = cache.entrySet().iterator();//least
            // recently accessed item will be the first one iterated
            while (iter.hasNext()) {
                Map.Entry<String, Drawable> entry = iter.next();
                size -= getSizeInBytes(((BitmapDrawable) entry.getValue()).getBitmap());
                iter.remove();
                if (size <= limit)
                    break;
            }
            Log.i(TAG, "Clean cache. New size " + cache.size());
        }
    }

    public void clear() {
        try {
            //NullPointerException sometimes happen here http://code.google
            // .com/p/osmdroid/issues/detail?id=78
            cache.clear();
            size = 0;
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    private long getSizeInBytes(Bitmap bitmap) {
        if (bitmap == null)
            return 0;
        return bitmap.getRowBytes() * bitmap.getHeight();
    }
}
