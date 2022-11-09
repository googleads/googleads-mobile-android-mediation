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

import android.os.Bundle;
import android.view.View;
import com.google.ads.mediation.sample.sdk.SampleNativeAd;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link UnifiedNativeAdMapper} extension to map {@link SampleNativeAd} instances to the Mobile
 * Ads SDK's {@link com.google.android.gms.ads.nativead.NativeAd} interface.
 */
public class SampleUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  private final SampleNativeAd sampleAd;

  public SampleUnifiedNativeAdMapper(SampleNativeAd ad) {
    sampleAd = ad;
    setHeadline(sampleAd.getHeadline());
    setBody(sampleAd.getBody());
    setCallToAction(sampleAd.getCallToAction());
    setStarRating(sampleAd.getStarRating());
    setStore(sampleAd.getStoreName());
    setIcon(
        new SampleNativeMappedImage(
            ad.getIcon(), ad.getIconUri(), SampleCustomEvent.SAMPLE_SDK_IMAGE_SCALE));
    setAdvertiser(ad.getAdvertiser());

    List<NativeAd.Image> imagesList = new ArrayList<NativeAd.Image>();
    imagesList.add(new SampleNativeMappedImage(ad.getImage(), ad.getImageUri(),
        SampleCustomEvent.SAMPLE_SDK_IMAGE_SCALE));
    setImages(imagesList);

    if (sampleAd.getPrice() != null) {
      NumberFormat formatter = NumberFormat.getCurrencyInstance();
      String priceString = formatter.format(sampleAd.getPrice());
      setPrice(priceString);
    }

    Bundle extras = new Bundle();
    extras.putString(SampleCustomEvent.DEGREE_OF_AWESOMENESS, ad.getDegreeOfAwesomeness());
    this.setExtras(extras);

    setOverrideClickHandling(false);
    setOverrideImpressionRecording(false);

    setAdChoicesContent(sampleAd.getInformationIcon());
  }

  @Override
  public void recordImpression() {
    sampleAd.recordImpression();
  }

  @Override
  public void handleClick(View view) {
    sampleAd.handleClick(view);
  }

  // The Sample SDK doesn't do its own impression/click tracking, instead relies on its
  // publishers calling the recordImpression and handleClick methods on its native ad object. So
  // there's no need to pass it a reference to the View being used to display the native ad. If
  // your mediated network does need a reference to the view, the following method can be used
  // to provide one.


  @Override
  public void trackViews(View containerView, Map<String, View> clickableAssetViews,
      Map<String, View> nonClickableAssetViews) {
    super.trackViews(containerView, clickableAssetViews, nonClickableAssetViews);
    // If your ad network SDK does its own impression tracking, here is where you can track the
    // top level native ad view and its individual asset views.
  }

  @Override
  public void untrackView(View view) {
    super.untrackView(view);
    // Here you would remove any trackers from the View added in trackView.
  }
}
