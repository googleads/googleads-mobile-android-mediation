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
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link NativeAppInstallAdMapper} extension to map {@link SampleNativeAd} instances to
 * the Mobile Ads SDK's {@link com.google.android.gms.ads.formats.NativeAppInstallAd} interface.
 */
public class SampleNativeAppInstallAdMapper extends NativeAppInstallAdMapper {

    private final SampleNativeAd sampleAd;
    // For the sake of simplicity, NativeAdOptions are not used by the Sample Custom
    // Event. They're included to demonstrate how the custom event can map options and views between
    // the Google Mobile Ads SDK and the Sample SDK.
    public SampleNativeAppInstallAdMapper(SampleNativeAd ad, NativeAdOptions unusedAdOptions) {
        sampleAd = ad;
        setHeadline(sampleAd.getHeadline());
        setBody(sampleAd.getBody());
        setCallToAction(sampleAd.getCallToAction());
        setStarRating(sampleAd.getStarRating());
        setStore(sampleAd.getStoreName());
        setIcon(new SampleNativeMappedImage(ad.getIcon(), ad.getIconUri(),
                SampleCustomEvent.SAMPLE_SDK_IMAGE_SCALE));

        List<NativeAd.Image> imagesList = new ArrayList<NativeAd.Image>();
        imagesList.add(new SampleNativeMappedImage(ad.getImage(), ad.getImageUri(),
                SampleCustomEvent.SAMPLE_SDK_IMAGE_SCALE));
        setImages(imagesList);

        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        String priceString = formatter.format(sampleAd.getPrice());
        setPrice(priceString);

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
    public void trackView(View view) {
        super.trackView(view);
        // Here you would pass the View back to the mediated network's SDK.
    }

    @Override
    public void untrackView(View view) {
        super.untrackView(view);
        // Here you would remove any trackers from the View added in trackView.
    }
}
