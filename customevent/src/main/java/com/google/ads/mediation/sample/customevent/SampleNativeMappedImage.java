/*
 * Copyright (C) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.sample.customevent;

import com.google.android.gms.ads.formats.NativeAd;

import android.graphics.drawable.Drawable;
import android.net.Uri;

/**
 * A simple class that fits the the {@link NativeAd.Image} interface and can be filled with assets
 * returned by the Sample SDK.
 */
public class SampleNativeMappedImage implements NativeAd.Image {

    private Drawable mDrawable;
    private Uri mImageUri;

    public SampleNativeMappedImage(Drawable drawable, Uri imageUri) {
        mDrawable = drawable;
        mImageUri = imageUri;
    }

    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    @Override
    public Uri getUri() {
        return mImageUri;
    }
}

