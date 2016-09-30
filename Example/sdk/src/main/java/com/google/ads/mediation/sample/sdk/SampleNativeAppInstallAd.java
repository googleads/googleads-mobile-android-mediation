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

package com.google.ads.mediation.sample.sdk;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;

/**
 * A native ad object that holds assets for a sample app install ad.
 */
public class SampleNativeAppInstallAd {

    private String mHeadline;
    private Drawable mImage;
    private Uri mImageUri;
    private String mBody;
    private Drawable mAppIcon;
    private Uri mAppIconUri;
    private String mCallToAction;
    private double mStarRating;
    private String mStoreName;
    private Double mPrice;
    private String mDegreeOfAwesomeness;

    public String getHeadline() {
        return mHeadline;
    }

    public void setHeadline(String headline) {
        this.mHeadline = headline;
    }

    public Drawable getImage() {
        return mImage;
    }

    public void setImage(Drawable image) {
        this.mImage = image;
    }

    public String getBody() {
        return mBody;
    }

    public void setBody(String body) {
        this.mBody = body;
    }

    public Drawable getAppIcon() {
        return mAppIcon;
    }

    public void setAppIcon(Drawable mAppIcon) {
        this.mAppIcon = mAppIcon;
    }

    public String getCallToAction() {
        return mCallToAction;
    }

    public void setCallToAction(String mCallToAction) {
        this.mCallToAction = mCallToAction;
    }

    public double getStarRating() {
        return mStarRating;
    }

    public void setStarRating(double starRating) {
        this.mStarRating = starRating;
    }

    public String getStoreName() {
        return mStoreName;
    }

    public void setStoreName(String storeName) {
        this.mStoreName = storeName;
    }

    public Double getPrice() {
        return mPrice;
    }

    public void setPrice(Double mPrice) {
        this.mPrice = mPrice;
    }

    public String getDegreeOfAwesomeness() {
        return mDegreeOfAwesomeness;
    }

    public void setDegreeOfAwesomeness(String degreeOfAwesomeness) {
        this.mDegreeOfAwesomeness = degreeOfAwesomeness;
    }

    public Uri getImageUri() {
        return mImageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.mImageUri = imageUri;
    }

    public Uri getAppIconUri() {
        return mAppIconUri;
    }

    public void setAppIconUri(Uri mAppIconUri) {
        this.mAppIconUri = mAppIconUri;
    }

    public void handleClick(View view) {
        // Normally this would result in some type of click response, like a browser opening
        // or a ping to the servers. This isn't a real sdk, though, so we'll just log it.
        Log.i("SampleAdSdk", "A click has been reported for View #" + view.getId());
    }

    public void recordImpression() {
        // Here again, we'll just log that an impression took place.
        Log.i("SampleAdSdk", "An impression has been reported.");
    }
}
