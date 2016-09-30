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

import com.google.ads.mediation.sample.sdk.SampleNativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link NativeAppInstallAdMapper} extension to map {@link SampleNativeAppInstallAd} instances to
 * the Mobile Ads SDK's {@link com.google.android.gms.ads.formats.NativeAppInstallAd} interface.
 */
public class SampleNativeAppInstallAdMapper extends NativeAppInstallAdMapper {

    private final SampleNativeAppInstallAd mSampleAd;

    public SampleNativeAppInstallAdMapper(SampleNativeAppInstallAd ad) {
        mSampleAd = ad;
        setHeadline(mSampleAd.getHeadline());
        setBody(mSampleAd.getBody());
        setCallToAction(mSampleAd.getCallToAction());
        setStarRating(mSampleAd.getStarRating());
        setStore(mSampleAd.getStoreName());
        setIcon(new SampleNativeMappedImage(ad.getAppIcon(), ad.getAppIconUri(),
                SampleAdapter.SAMPLE_SDK_IMAGE_SCALE));

        List<NativeAd.Image> imagesList = new ArrayList<NativeAd.Image>();
        imagesList.add(new SampleNativeMappedImage(ad.getImage(), ad.getImageUri(),
                SampleAdapter.SAMPLE_SDK_IMAGE_SCALE));
        setImages(imagesList);

        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        String priceString = formatter.format(mSampleAd.getPrice());
        setPrice(priceString);

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
