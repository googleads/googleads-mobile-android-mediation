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

package com.google.ads.mediation.sample.adapter;

import android.os.Bundle;
import android.view.View;

import com.google.ads.mediation.sample.sdk.SampleNativeContentAd;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.NativeContentAdMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link NativeContentAdMapper} extension to map {@link SampleNativeContentAd} instances to
 * the Mobile Ads SDK's {@link com.google.android.gms.ads.formats.NativeContentAd} interface.
 */
public class SampleNativeContentAdMapper extends NativeContentAdMapper {

    private SampleNativeContentAd mSampleAd;

    public SampleNativeContentAdMapper(SampleNativeContentAd ad) {
        mSampleAd = ad;

        setAdvertiser(mSampleAd.getAdvertiser());
        setHeadline(mSampleAd.getHeadline());
        setBody(mSampleAd.getBody());
        setCallToAction(mSampleAd.getCallToAction());

        setLogo(new SampleNativeMappedImage(ad.getLogo(), ad.getLogoUri(),
                SampleAdapter.SAMPLE_SDK_IMAGE_SCALE));

        List<NativeAd.Image> imagesList = new ArrayList<NativeAd.Image>();
        imagesList.add(new SampleNativeMappedImage(ad.getImage(), ad.getImageUri(),
                SampleAdapter.SAMPLE_SDK_IMAGE_SCALE));
        setImages(imagesList);

        Bundle extras = new Bundle();
        extras.putString(SampleAdapter.DEGREE_OF_AWESOMENESS, ad.getDegreeOfAwesomeness());
        this.setExtras(extras);

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(false);
    }

    @Override
    public void recordImpression() {
        mSampleAd.recordImpression();
    }

    @Override
    public void handleClick(View view) {
        mSampleAd.handleClick(view);
    }
}
