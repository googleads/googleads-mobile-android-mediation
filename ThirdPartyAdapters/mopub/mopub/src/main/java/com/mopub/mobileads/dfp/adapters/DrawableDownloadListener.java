package com.mopub.mobileads.dfp.adapters;

import android.graphics.drawable.Drawable;

import java.util.HashMap;

// Create an interface to send callbacks when image download finishes.
public interface DrawableDownloadListener {

    // A success callback.
    void onDownloadSuccess(HashMap<String, Drawable> drawableMap);

    // A failure callback.
    void onDownloadFailure();
}
