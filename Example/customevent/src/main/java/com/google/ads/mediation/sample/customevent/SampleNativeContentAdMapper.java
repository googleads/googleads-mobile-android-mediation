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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.ads.mediation.sample.sdk.SampleNativeContentAd;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.NativeContentAdMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link NativeContentAdMapper} extension to map {@link SampleNativeContentAd} instances to
 * the Mobile Ads SDK's {@link com.google.android.gms.ads.formats.NativeContentAd} interface.
 */
public class SampleNativeContentAdMapper extends NativeContentAdMapper {

    private SampleNativeContentAd mSampleAd;
    private NativeAdOptions mNativeAdOptions;
    private ImageView mInformationIconView;

    public SampleNativeContentAdMapper(SampleNativeContentAd ad, NativeAdOptions adOptions) {
        mSampleAd = ad;
        mNativeAdOptions = adOptions;

        setAdvertiser(mSampleAd.getAdvertiser());
        setHeadline(mSampleAd.getHeadline());
        setBody(mSampleAd.getBody());
        setCallToAction(mSampleAd.getCallToAction());

        setLogo(new SampleNativeMappedImage(ad.getLogo(), ad.getLogoUri(),
                SampleCustomEvent.SAMPLE_SDK_IMAGE_SCALE));

        List<NativeAd.Image> imagesList = new ArrayList<NativeAd.Image>();
        imagesList.add(new SampleNativeMappedImage(ad.getImage(), ad.getImageUri(),
                SampleCustomEvent.SAMPLE_SDK_IMAGE_SCALE));
        setImages(imagesList);

        Bundle extras = new Bundle();
        extras.putString(SampleCustomEvent.DEGREE_OF_AWESOMENESS, ad.getDegreeOfAwesomeness());
        this.setExtras(extras);

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(false);

        setAdChoicesContent(mSampleAd.getInformationIcon());
    }

    @Override
    public void recordImpression() {
        mSampleAd.recordImpression();
    }

    @Override
    public void handleClick(View view) {
        mSampleAd.handleClick(view);
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
