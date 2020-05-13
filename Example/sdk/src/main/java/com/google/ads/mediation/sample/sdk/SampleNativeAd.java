/*
 * Copyright (C) 2018 Google, Inc.
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
 * A native ad object that holds assets for a sample native ad.
 */
public class SampleNativeAd {

  private String headline;
  private Drawable image;
  private Uri imageUri;
  private String body;
  private Drawable icon;
  private Uri iconUri;
  private String callToAction;
  private String advertiser;
  private double starRating;
  private String storeName;
  private Double price;
  private String degreeOfAwesomeness;
  private ImageView informationIcon;
  private SampleMediaView mediaView;

  public String getHeadline() {
    return headline;
  }

  public void setHeadline(String headline) {
    this.headline = headline;
  }

  public Drawable getImage() {
    return image;
  }

  public void setImage(Drawable image) {
    this.image = image;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Drawable getIcon() {
    return icon;
  }

  public void setIcon(Drawable mAppIcon) {
    this.icon = mAppIcon;
  }

  public String getCallToAction() {
    return callToAction;
  }

  public void setCallToAction(String mCallToAction) {
    this.callToAction = mCallToAction;
  }

  public String getAdvertiser() {
    return advertiser;
  }

  public void setAdvertiser(String advertiser) {
    this.advertiser = advertiser;
  }

  public double getStarRating() {
    return starRating;
  }

  public void setStarRating(double starRating) {
    this.starRating = starRating;
  }

  public String getStoreName() {
    return storeName;
  }

  public void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public Double getPrice() {
    return price;
  }

  public void setPrice(Double mPrice) {
    this.price = mPrice;
  }

  public String getDegreeOfAwesomeness() {
    return degreeOfAwesomeness;
  }

  public void setDegreeOfAwesomeness(String degreeOfAwesomeness) {
    this.degreeOfAwesomeness = degreeOfAwesomeness;
  }

  public Uri getImageUri() {
    return imageUri;
  }

  public void setImageUri(Uri imageUri) {
    this.imageUri = imageUri;
  }

  public Uri getIconUri() {
    return iconUri;
  }

  public void setIconUri(Uri mAppIconUri) {
    this.iconUri = mAppIconUri;
  }

  public ImageView getInformationIcon() {
    return informationIcon;
  }

  public void setInformationIcon(ImageView informationIcon) {
    this.informationIcon = informationIcon;
  }

  public SampleMediaView getMediaView() {
    return mediaView;
  }

  public void setMediaView(SampleMediaView mediaView) {
    this.mediaView = mediaView;
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
    if (mediaView != null) {
      mediaView.beginPlaying();
    }
  }
}
