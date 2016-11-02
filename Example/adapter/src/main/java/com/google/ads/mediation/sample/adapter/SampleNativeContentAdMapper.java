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

    // The Sample SDK doesn't do its own impression/click tracking, instead relies on its
    // publishers calling the recordImpression and handleClick methods on its native ad object. So
    // there's no need to pass it a reference to the View being used to display the native ad. If
    // your mediated network does need a reference to the view, the following method can be used
    // to provide one.

    @Override
    public void trackView(View view) {
        super.trackView(view);
        // Here you would pass the View back to the mediated network's SDK.

        /*
         * Your adapter is responsible for placing your AdChoices icon in Google's native ad view.
         * You should use the trackView and untrackView APIs provided in NativeAppInstallAdMapper
         * and NativeContentAdMapper to add and remove your AdChoices view.
         *
         * The adapter must also respect the publisher’s preference of AdChoices location by
         * checking the getAdChoicesPlacement() method in NativeAdOptions and rendering the ad
         * in the publisher’s preferred corner. If a preferred corner is not set, AdChoices
         * should be rendered in the top right corner.
         */

        if (!(view instanceof ViewGroup)) {
            Log.w(SampleAdapter.TAG, "Failed to show information icon. Ad view not a ViewGroup.");
            return;
        }

        ViewGroup adView = (ViewGroup) view;
        // Find the overlay view in the given ad view. The overlay view will always be the
        // top most view in the hierarchy.
        View overlayView = adView.getChildAt(adView.getChildCount() - 1);
        if (overlayView instanceof FrameLayout) {

            // The sample SDK returns a view for AdChoices asset. If your SDK provides image and
            // click through URLs instead of the view asset, the adapter is responsible for
            // downloading the icon image and creating the AdChoices icon view.

            // Get the information icon provided by the Sample SDK.
            mInformationIconView = mSampleAd.getInformationIcon();

            // Add the view to the overlay view.
            ((ViewGroup) overlayView).addView(mInformationIconView);

            // We know that the overlay view is a FrameLayout, so we get the FrameLayout's
            // LayoutParams from the AdChoicesView.
            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) mInformationIconView.getLayoutParams();

            // Note: The NativeAdOptions' getAdChoicesPlacement() preference requires
            // Google Play Services 9.6.0 and higher.
            if (mNativeAdOptions == null) {
                // Default to top right if native ad options are not provided.
                params.gravity = Gravity.TOP | Gravity.RIGHT;
            } else {
                switch (mNativeAdOptions.getAdChoicesPlacement()) {
                    case NativeAdOptions.ADCHOICES_TOP_LEFT:
                        params.gravity = Gravity.TOP | Gravity.LEFT;
                        break;
                    case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
                        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                        break;
                    case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
                        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                        break;
                    case NativeAdOptions.ADCHOICES_TOP_RIGHT:
                    default:
                        params.gravity = Gravity.TOP | Gravity.RIGHT;
                }
            }
            adView.requestLayout();
        } else {
            Log.w(SampleAdapter.TAG, "Failed to show information icon. Overlay view not found.");
        }
    }

    @Override
    public void untrackView(View view) {
        super.untrackView(view);
        // Here you would remove any trackers from the View added in trackView.

        // Remove the previously added AdChoices view from the ad view.
        ViewParent parent = mInformationIconView.getParent();
        if (parent != null && parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(mInformationIconView);
        }
    }
}
