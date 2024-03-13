// Copyright 2018 Google LLC
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

package com.google.ads.mediation.sample.adapter;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.ads.mediation.sample.sdk.SampleMediaView;
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
public class SampleNativeAdMapper extends UnifiedNativeAdMapper {

  private final SampleNativeAd sampleAd;

  public SampleNativeAdMapper(@NonNull SampleNativeAd ad) {
    sampleAd = ad;
    setHeadline(sampleAd.getHeadline());
    setBody(sampleAd.getBody());
    setCallToAction(sampleAd.getCallToAction());
    setStarRating(sampleAd.getStarRating());
    setStore(sampleAd.getStoreName());
    setIcon(new SampleNativeMappedImage(ad.getIcon(), ad.getIconUri(),
        SampleAdapter.SAMPLE_SDK_IMAGE_SCALE));
    setAdvertiser(ad.getAdvertiser());

    List<NativeAd.Image> imagesList = new ArrayList<>();
    imagesList.add(new SampleNativeMappedImage(ad.getImage(), ad.getImageUri(),
        SampleAdapter.SAMPLE_SDK_IMAGE_SCALE));
    setImages(imagesList);

    if (sampleAd.getPrice() != null) {
      NumberFormat formatter = NumberFormat.getCurrencyInstance();
      String priceString = formatter.format(sampleAd.getPrice());
      setPrice(priceString);
    }

    Bundle extras = new Bundle();
    extras.putString(SampleAdapter.DEGREE_OF_AWESOMENESS, ad.getDegreeOfAwesomeness());
    this.setExtras(extras);

    SampleMediaView mediaView = sampleAd.getMediaView();

    // Some ads from Sample SDK have video assets and some do not.
    if (mediaView != null) {
      setMediaView(mediaView);
      setHasVideoContent(true);
    } else {
      setHasVideoContent(false);
    }

    setOverrideClickHandling(false);
    setOverrideImpressionRecording(false);

    setAdChoicesContent(sampleAd.getInformationIcon());
  }

  @Override
  public void recordImpression() {
    sampleAd.recordImpression();
  }

  @Override
  public void handleClick(@NonNull View view) {
    sampleAd.handleClick(view);
  }

  // The Sample SDK doesn't do its own impression/click tracking, and instead relies on its
  // publishers calling the recordImpression and handleClick methods on its native ad object. So
  // there's no need to pass it a reference to the View being used to display the native ad. If
  // your mediated network does need a reference to the view, the following method can be used
  // to provide one.

  @Override
  public void trackViews(@NonNull View containerView,
      @NonNull Map<String, View> clickableAssetViews,
      @NonNull Map<String, View> nonClickableAssetViews) {
    super.trackViews(containerView, clickableAssetViews, nonClickableAssetViews);
    sampleAd.registerNativeAdView(containerView);
    // If your ad network SDK does its own impression tracking, here is where you can track the
    // top level native ad view and its individual asset views.
  }

  @Override
  public void untrackView(@NonNull View view) {
    super.untrackView(view);
    // Here you would remove any trackers from the View added in trackView.
  }
}
