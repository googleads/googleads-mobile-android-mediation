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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Random;

/**
 * An example AdLoader that pretends to load native ads. It has methods that will be used by the
 * {@code SampleCustomEvent} and {@code SampleAdapter} to request native ads.
 */
public class SampleNativeAdLoader {
    private Context mContext;
    private String mAdUnit;
    private SampleNativeAdListener mListener;

    /**
     * Create a new {@link SampleInterstitial}.
     *
     * @param context An Android {@link Context}.
     */
    public SampleNativeAdLoader(Context context) {
        this.mContext = context;
    }

    /**
     * Sets the sample ad unit.
     *
     * @param sampleAdUnit The sample ad unit.
     */
    public void setAdUnit(String sampleAdUnit) {
        this.mAdUnit = sampleAdUnit;
    }

    /**
     * Sets a {@link SampleAdListener} to listen for ad events.
     *
     * @param listener The native ad listener.
     */
    public void setNativeAdListener(SampleNativeAdListener listener) {
        this.mListener = listener;
    }

    /**
     * Fetch an ad. Instead of doing an actual ad fetch, we will randomly decide to succeed, or
     * fail with different error codes.
     *
     * @param request The ad request with targeting information.
     */
    public void fetchAd(SampleNativeAdRequest request) {
        // Check for conditions that constitute a bad request.
        if ((mListener == null) || (mAdUnit == null)
                || (!request.areContentAdsRequested() && !request.areAppInstallAdsRequested())) {
            mListener.onAdFetchFailed(SampleErrorCode.BAD_REQUEST);
            return;
        }

        Random random = new Random();
        int nextInt = random.nextInt(100);
        if (mListener != null) {
            if (nextInt < 80) {
                // Act as if the request was successful and create a sample native ad
                // of the request type filled with dummy data.
                if (request.areAppInstallAdsRequested()
                        && (!request.areContentAdsRequested()
                        || random.nextBoolean())) {
                    mListener.onNativeAppInstallAdFetched(createFakeAppInstallAd(request));
                } else {
                    mListener.onNativeContentAdFetched(createFakeContentAd(request));
                }
            } else if (nextInt < 85) {
                mListener.onAdFetchFailed(SampleErrorCode.UNKNOWN);
            } else if (nextInt < 90) {
                mListener.onAdFetchFailed(SampleErrorCode.BAD_REQUEST);
            } else if (nextInt < 95) {
                mListener.onAdFetchFailed(SampleErrorCode.NETWORK_ERROR);
            } else {
                mListener.onAdFetchFailed(SampleErrorCode.NO_INVENTORY);
            }
        }
    }

    private SampleNativeAppInstallAd createFakeAppInstallAd(SampleNativeAdRequest request) {
        SampleNativeAppInstallAd fakeAd = new SampleNativeAppInstallAd();

        fakeAd.setHeadline("Sample App!");
        fakeAd.setBody("This app doesn't actually exist.");
        fakeAd.setCallToAction("Take Action!");
        fakeAd.setDegreeOfAwesomeness("Quite Awesome");
        fakeAd.setPrice(1.99);
        fakeAd.setStarRating(4.5);
        fakeAd.setStoreName("Sample Store");
        fakeAd.setImageUri(Uri.parse("http://www.example.com/"));
        fakeAd.setAppIconUri(Uri.parse("http://www.example.com/"));

        // We pretend 80% of network's inventory has video assets and 20% doesn't.
        if ((new Random()).nextInt(100) < 80) {
            fakeAd.setMediaView(new SampleMediaView(mContext));
        } else {
            fakeAd.setMediaView(null);
        }

        // There are other options offered in the SampleNativeAdRequest,
        // but for simplicity's sake, this is the only one we'll put to use.
        if (request.getShouldDownloadImages()) {
            fakeAd.setAppIcon(mContext.getResources()
                    .getDrawable(R.drawable.sample_app_icon));
            fakeAd.setImage(mContext.getResources()
                    .getDrawable(R.drawable.sample_app_image));
        }

        fakeAd.setInformationIcon(createInformationIconImageView());

        return fakeAd;
    }

    private SampleNativeContentAd createFakeContentAd(SampleNativeAdRequest request) {
        SampleNativeContentAd fakeAd = new SampleNativeContentAd();

        fakeAd.setHeadline("Sample Content!");
        fakeAd.setBody("This is a sample ad, so there's no real content. In the event of a real "
                + "ad, though, some persuasive text would appear here.");
        fakeAd.setAdvertiser("The very best advertiser!");
        fakeAd.setCallToAction("Take Action!");
        fakeAd.setDegreeOfAwesomeness("Fairly Awesome");

        // We pretend 80% of network's inventory has video assets and 20% doesn't.
        if ((new Random()).nextInt(100) < 80) {
            fakeAd.setMediaView(new SampleMediaView(mContext));
        } else {
            fakeAd.setMediaView(null);
        }

        // There are other options offered in the SampleNativeAdRequest,
        // but for simplicity's sake, this is the only one we'll put to use.
        if (request.getShouldDownloadImages()) {
            fakeAd.setLogo(mContext.getResources()
                    .getDrawable(R.drawable.sample_content_logo));
            fakeAd.setImage(mContext.getResources()
                    .getDrawable(R.drawable.sample_content_ad_image));
        }

        fakeAd.setInformationIcon(createInformationIconImageView());

        return fakeAd;
    }

    /**
     * Creates and returns an information icon image view.
     *
     * @return information icon image view.
     */
    private ImageView createInformationIconImageView() {
        ImageView informationIconImageView = new ImageView(mContext);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        informationIconImageView.setLayoutParams(params);
        informationIconImageView.setImageResource(R.drawable.ic_info_outline_black_24px);

        informationIconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new AlertDialog.Builder(mContext)
                        .setTitle("Sample SDK")
                        .setMessage("This is a sample ad from the Sample SDK.")
                        .setNeutralButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .show();
            }
        });

        return informationIconImageView;
    }
}
