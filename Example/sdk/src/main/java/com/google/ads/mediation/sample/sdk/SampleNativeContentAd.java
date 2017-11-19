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
import android.widget.ImageView;

/**
 * A native ad object that holds assets for a sample content ad.
 */
public class SampleNativeContentAd {

    private String mAdvertiser;
    private String mBody;
    private String mCallToAction;
    private String mHeadline;
    private Drawable mImage;
    private Uri mImageUri;
    private Drawable mLogo;
    private Uri mLogoUri;
    private String mDegreeOfAwesomeness;
    private ImageView mInformationIcon;
    private SampleMediaView mMediaView;

    public SampleNativeContentAd() {
    }

    public String getAdvertiser() {
        return mAdvertiser;
    }

    public void setAdvertiser(String advertiser) {
        this.mAdvertiser = advertiser;
    }

    public String getBody() {
        return mBody;
    }

    public void setBody(String body) {
        this.mBody = body;
    }

    public String getCallToAction() {
        return mCallToAction;
    }

    public void setCallToAction(String callToAction) {
        this.mCallToAction = callToAction;
    }

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

    public Drawable getLogo() {
        return mLogo;
    }

    public void setLogo(Drawable logo) {
        this.mLogo = logo;
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

    public Uri getLogoUri() {
        return mLogoUri;
    }

    public void setLogoUri(Uri logoUri) {
        this.mLogoUri = logoUri;
    }

    public ImageView getInformationIcon() {
        return mInformationIcon;
    }

    public void setInformationIcon(ImageView informationIcon) {
        this.mInformationIcon = informationIcon;
    }

    public SampleMediaView getMediaView() {
        return mMediaView;
    }

    public void setMediaView(SampleMediaView mediaView) {
        this.mMediaView = mediaView;
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

    public void registerNativeAdView(View view) {
        // Starts playing video if there is any video asset. Here, passing view is not mandatory
        // since we are just calling the playback to play the video.
        if (mMediaView != null) {
            mMediaView.beginPlaying();
        }
    }
}
