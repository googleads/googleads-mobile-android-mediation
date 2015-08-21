/*
 * Copyright (C) 2014 Google, Inc.
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

package com.google.ads.mediation.sample.mediationsample;

import com.google.ads.mediation.sample.customevent.SampleCustomEvent;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.google.android.gms.ads.formats.NativeContentAdView;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * A simple {@link android.app.Activity} that displays adds using the sample adapter and sample
 * custom event.
 */
public class MainActivity extends ActionBarActivity {

    private static final String LOG_TAG = "SampleApp";

    private InterstitialAd mCustomEventInterstitial;
    private InterstitialAd mAdapterInterstitial;
    private Button mCustomEventButton;
    private Button mAdapterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCustomEventButton = (Button) findViewById(R.id.customevent_button);
        mAdapterButton = (Button) findViewById(R.id.adapter_button);

        mCustomEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCustomEventInterstitial.isLoaded()) {
                    mCustomEventInterstitial.show();
                }
            }
        });

        mAdapterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdapterInterstitial.isLoaded()) {
                    mAdapterInterstitial.show();
                }
            }
        });

        AdView mCustomEventAdView = (AdView) findViewById(R.id.customevent_adview);
        mCustomEventAdView.loadAd(new AdRequest.Builder().build());

        AdView mAdapterAdView = (AdView) findViewById(R.id.adapter_adview);
        mAdapterAdView.loadAd(new AdRequest.Builder().build());

        mCustomEventInterstitial = new InterstitialAd(this);
        mCustomEventInterstitial.setAdUnitId(
                getResources().getString(R.string.customevent_interstitial_ad_unit_id));
        mCustomEventInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                Toast.makeText(MainActivity.this,
                        "Error loading custom event interstitial, code " + errorCode,
                        Toast.LENGTH_SHORT).show();
                mCustomEventButton.setEnabled(true);
            }

            @Override
            public void onAdLoaded() {
                mCustomEventButton.setEnabled(true);
            }

            @Override
            public void onAdOpened() {
                mCustomEventButton.setEnabled(false);
            }

            @Override
            public void onAdClosed() {
                mCustomEventInterstitial.loadAd(new AdRequest.Builder().build());
            }
        });

        mAdapterInterstitial = new InterstitialAd(this);
        mAdapterInterstitial.setAdUnitId(
                getResources().getString(R.string.adapter_interstitial_ad_unit_id));
        mAdapterInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                Toast.makeText(MainActivity.this,
                        "Error loading adapter interstitial, code " + errorCode,
                        Toast.LENGTH_SHORT).show();
                mAdapterButton.setEnabled(true);
            }

            @Override
            public void onAdLoaded() {
                mAdapterButton.setEnabled(true);
            }

            @Override
            public void onAdOpened() {
                mAdapterButton.setEnabled(false);
            }

            @Override
            public void onAdClosed() {
                mAdapterInterstitial.loadAd(new AdRequest.Builder().build());
            }
        });

        mCustomEventInterstitial.loadAd(new AdRequest.Builder().build());
        mAdapterInterstitial.loadAd(new AdRequest.Builder().build());

        AdLoader loader = new AdLoader.Builder(this,
                getResources().getString(R.string.customevent_native_ad_unit_id))
                .forAppInstallAd(new NativeAppInstallAd.OnAppInstallAdLoadedListener() {
                    @Override
                    public void onAppInstallAdLoaded(NativeAppInstallAd ad) {
                        FrameLayout frameLayout =
                                (FrameLayout) findViewById(R.id.customeventnative_framelayout);
                        NativeAppInstallAdView adView = (NativeAppInstallAdView) getLayoutInflater()
                                .inflate(R.layout.ad_app_install, null);
                        populateAppInstallAdView(ad, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }
                })
                .forContentAd(new NativeContentAd.OnContentAdLoadedListener() {
                    @Override
                    public void onContentAdLoaded(NativeContentAd ad) {
                        FrameLayout frameLayout =
                                (FrameLayout) findViewById(R.id.customeventnative_framelayout);
                        NativeContentAdView adView = (NativeContentAdView) getLayoutInflater()
                                .inflate(R.layout.ad_content, null);
                        populateContentAdView(ad, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        Toast.makeText(MainActivity.this,
                                "Custom event native ad failed with code: " + errorCode,
                                Toast.LENGTH_SHORT).show();
                    }
                }).build();

        loader.loadAd(new AdRequest.Builder().build());
    }

    /**
     * Populates a {@link NativeAppInstallAdView} object with data from a given
     * {@link NativeAppInstallAd}.
     *
     * @param nativeAppInstallAd the object containing the ad's assets
     * @param adView             the view to be populated
     */
    private void populateAppInstallAdView(NativeAppInstallAd nativeAppInstallAd,
                                          NativeAppInstallAdView adView) {
        // Assign native ad object to the native view.
        adView.setNativeAd(nativeAppInstallAd);

        adView.setHeadlineView(adView.findViewById(R.id.appinstall_headline));
        adView.setImageView(adView.findViewById(R.id.appinstall_image));
        adView.setBodyView(adView.findViewById(R.id.appinstall_body));
        adView.setCallToActionView(adView.findViewById(R.id.appinstall_call_to_action));
        adView.setIconView(adView.findViewById(R.id.appinstall_app_icon));
        adView.setPriceView(adView.findViewById(R.id.appinstall_price));
        adView.setStarRatingView(adView.findViewById(R.id.appinstall_stars));
        adView.setStoreView(adView.findViewById(R.id.appinstall_store));

        ((TextView) adView.getHeadlineView()).setText(nativeAppInstallAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeAppInstallAd.getBody());
        ((TextView) adView.getPriceView()).setText(nativeAppInstallAd.getPrice());
        ((TextView) adView.getStoreView()).setText(nativeAppInstallAd.getStore());
        ((Button) adView.getCallToActionView()).setText(nativeAppInstallAd.getCallToAction());
        ((ImageView) adView.getIconView()).setImageDrawable(nativeAppInstallAd.getIcon()
                .getDrawable());
        ((RatingBar) adView.getStarRatingView())
                .setRating(nativeAppInstallAd.getStarRating().floatValue());

        List<NativeAd.Image> images = nativeAppInstallAd.getImages();

        if (images.size() > 0) {
            ((ImageView) adView.getImageView())
                    .setImageDrawable(images.get(0).getDrawable());
        }

        // Handle the fact that this could be a Sample SDK native ad, which includes a
        // "degree of awesomeness" field.

        Bundle extras = nativeAppInstallAd.getExtras();
        if (extras.containsKey(SampleCustomEvent.DEGREE_OF_AWESOMENESS)) {
            TextView degree = (TextView) adView.findViewById(R.id.appinstall_degreeofawesomeness);
            degree.setVisibility(View.VISIBLE);
            degree.setText(extras.getString(SampleCustomEvent.DEGREE_OF_AWESOMENESS));
        }
    }

    /**
     * Populates a {@link NativeContentAdView} object with data from a given
     * {@link NativeContentAd}.
     *
     * @param nativeContentAd the object containing the ad's assets
     * @param adView          the view to be populated
     */
    private void populateContentAdView(NativeContentAd nativeContentAd,
                                       NativeContentAdView adView) {
        // Assign native ad object to the native view.
        adView.setNativeAd(nativeContentAd);

        adView.setHeadlineView(adView.findViewById(R.id.contentad_headline));
        adView.setImageView(adView.findViewById(R.id.contentad_image));
        adView.setBodyView(adView.findViewById(R.id.contentad_body));
        adView.setCallToActionView(adView.findViewById(R.id.contentad_call_to_action));
        adView.setLogoView(adView.findViewById(R.id.contentad_logo));
        adView.setAdvertiserView(adView.findViewById(R.id.contentad_advertiser));

        ((TextView) adView.getHeadlineView()).setText(nativeContentAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeContentAd.getBody());
        ((TextView) adView.getCallToActionView()).setText(nativeContentAd.getCallToAction());
        ((TextView) adView.getAdvertiserView()).setText(nativeContentAd.getAdvertiser());

        List<NativeAd.Image> images = nativeContentAd.getImages();

        if (images != null && images.size() > 0) {
            ((ImageView) adView.getImageView())
                    .setImageDrawable(images.get(0).getDrawable());
        }

        NativeAd.Image logoImage = nativeContentAd.getLogo();

        if (logoImage != null) {
            ((ImageView) adView.getLogoView())
                    .setImageDrawable(logoImage.getDrawable());
        }

        // Handle the fact that this could be a Sample SDK native ad, which includes a
        // "degree of awesomeness" field.

        Bundle extras = nativeContentAd.getExtras();
        if (extras.containsKey(SampleCustomEvent.DEGREE_OF_AWESOMENESS)) {
            TextView degree = (TextView) adView.findViewById(R.id.appinstall_degreeofawesomeness);
            degree.setVisibility(View.VISIBLE);
            degree.setText(extras.getString(SampleCustomEvent.DEGREE_OF_AWESOMENESS));
        }
    }
}
