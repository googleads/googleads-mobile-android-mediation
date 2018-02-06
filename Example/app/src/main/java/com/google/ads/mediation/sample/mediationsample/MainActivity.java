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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.mediation.sample.adapter.SampleAdapter;
import com.google.ads.mediation.sample.customevent.SampleCustomEvent;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.google.android.gms.ads.formats.NativeContentAdView;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import java.util.List;

/**
 * A simple {@link android.app.Activity} that displays adds using the sample adapter and sample
 * custom event.
 */
public class MainActivity extends AppCompatActivity {

    private InterstitialAd mCustomEventInterstitial;
    private InterstitialAd mAdapterInterstitial;
    private RewardedVideoAd mRewardedVideoAd;
    private Button mCustomEventButton;
    private Button mAdapterButton;
    private Button mAdapterVideoButton;
    private AdLoader adapterNativeLoader;
    private AdLoader customEventNativeLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Sample Custom Event.
         * 1) Create the sample custom event banner.
         * 2) Set up the on click listener for the sample custom event interstitial button.
         * 3) Create the sample custom event interstitial.
         */
        // Sample custom event banner.
        AdView mCustomEventAdView = (AdView) findViewById(R.id.customevent_adview);
        mCustomEventAdView.loadAd(new AdRequest.Builder().build());

        // Sample custom event interstitial button.
        mCustomEventButton = (Button) findViewById(R.id.customevent_button);
        mCustomEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCustomEventInterstitial.isLoaded()) {
                    mCustomEventInterstitial.show();
                }
            }
        });

        // Sample custom event interstitial.
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
        mCustomEventInterstitial.loadAd(new AdRequest.Builder().build());

        /**
         * Sample Adapter.
         * 1) Create the sample adapter banner.
         * 2) Set up the on click listener for the sample adapter interstitial button.
         * 3) Create the sample adapter interstitial.
         */
        // Sample adapter banner.
        AdView mAdapterAdView = (AdView) findViewById(R.id.adapter_adview);

        // The sample adapter provides a builder to make it easier for publisher to create
        // bundles containing "extra" values the get passed to the adapter when an ad is
        // requested. Here, the sample app uses the bundle builder to include some additional ad
        // request information that's supported by the Sample SDK (but not by the Google Mobile
        // Ads SDK).
        Bundle extras = new SampleAdapter.MediationExtrasBundleBuilder()
                .setIncome(100000)
                .setShouldAddAwesomeSauce(true)
                .build();
        AdRequest bannerAdRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(SampleAdapter.class, extras)
                .build();
        mAdapterAdView.loadAd(bannerAdRequest);
        
        // Sample adapter interstitial button.
        mAdapterButton = (Button) findViewById(R.id.adapter_button);
        mAdapterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdapterInterstitial.isLoaded()) {
                    mAdapterInterstitial.show();
                }
            }
        });

        // Sample adapter interstitial.
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

        AdRequest interstitialAdRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(SampleAdapter.class, extras)
                .build();
        mAdapterInterstitial.loadAd(interstitialAdRequest);

        /**
         * Sample Custom Event Native ad.
         */
        customEventNativeLoader = new AdLoader.Builder(this,
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

        customEventNativeLoader.loadAd(new AdRequest.Builder().build());
        Button refreshCustomEvent = (Button) findViewById(R.id.customeventnative_button);
        refreshCustomEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View unusedView) {
                customEventNativeLoader.loadAd(new AdRequest.Builder().build());
            }
        });


        /**
         * Sample Adapter Native ad.
         */
        adapterNativeLoader = new AdLoader.Builder(this,
                getResources().getString(R.string.adapter_native_ad_unit_id))
                .forAppInstallAd(new NativeAppInstallAd.OnAppInstallAdLoadedListener() {
                    @Override
                    public void onAppInstallAdLoaded(NativeAppInstallAd ad) {
                        FrameLayout frameLayout =
                                (FrameLayout) findViewById(R.id.adapternative_framelayout);
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
                                (FrameLayout) findViewById(R.id.adapternative_framelayout);
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
                                "Sample adapter native ad failed with code: " + errorCode,
                                Toast.LENGTH_SHORT).show();
                    }
                }).build();

        loadAdapterNativeAd(extras);
        Button refreshAdapterNative = (Button) findViewById(R.id.adapternative_button);
        refreshAdapterNative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View unusedView) {
                loadAdapterNativeAd(new SampleAdapter.MediationExtrasBundleBuilder()
                        .setIncome(100000)
                        .setShouldAddAwesomeSauce(true)
                        .build());
            }
        });

        // Sample adapter rewarded video button.
        mAdapterVideoButton = (Button) findViewById(R.id.adapter_rewarded_button);
        mAdapterVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRewardedVideoAd.isLoaded()) {
                    mRewardedVideoAd.show();
                } else {
                    loadRewardedVideoAd();
                }
            }
        });

        /**
         * Sample adapter rewarded video ad.
         */
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoAdLoaded() {
                mAdapterVideoButton.setEnabled(true);
                mAdapterVideoButton.setText("Show SampleAdapter Rewarded Video");
            }

            @Override
            public void onRewardedVideoAdOpened() {}

            @Override
            public void onRewardedVideoStarted() {}

            @Override
            public void onRewardedVideoAdClosed() {
                loadRewardedVideoAd();
            }

            @Override
            public void onRewarded(RewardItem rewardItem) {}

            @Override
            public void onRewardedVideoAdLeftApplication() {}

            @Override
            public void onRewardedVideoAdFailedToLoad(int errorCode) {
                Toast.makeText(MainActivity.this,
                        "Sample adapter rewarded video ad failed with code: " + errorCode,
                        Toast.LENGTH_SHORT).show();
                mAdapterVideoButton.setEnabled(true);
                mAdapterVideoButton.setText("Load SampleAdapter Rewarded Video");
            }
        });

        loadRewardedVideoAd();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Activity resumed, update the current activity in Sample SDK's sample rewarded video.
        mRewardedVideoAd.resume(MainActivity.this);
    }

    private void loadRewardedVideoAd() {
        mAdapterVideoButton.setEnabled(false);
        mRewardedVideoAd.loadAd(getString(R.string.adapter_rewarded_video_ad_unit_id),
                new AdRequest.Builder().build());
    }

    private void loadAdapterNativeAd(Bundle extras) {
        AdRequest nativeAdRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(SampleAdapter.class, extras)
                .build();
        adapterNativeLoader.loadAd(nativeAdRequest);
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
        VideoController videoController = nativeAppInstallAd.getVideoController();

        // Assign native ad object to the native view.
        adView.setNativeAd(nativeAppInstallAd);

        adView.setHeadlineView(adView.findViewById(R.id.appinstall_headline));
        adView.setBodyView(adView.findViewById(R.id.appinstall_body));
        adView.setCallToActionView(adView.findViewById(R.id.appinstall_call_to_action));
        adView.setIconView(adView.findViewById(R.id.appinstall_app_icon));
        adView.setPriceView(adView.findViewById(R.id.appinstall_price));
        adView.setStarRatingView(adView.findViewById(R.id.appinstall_stars));
        adView.setStoreView(adView.findViewById(R.id.appinstall_store));

        // Some assets are guaranteed to be in every NativeAppInstallAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAppInstallAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeAppInstallAd.getBody());
        ((Button) adView.getCallToActionView()).setText(nativeAppInstallAd.getCallToAction());
        ((ImageView) adView.getIconView()).setImageDrawable(nativeAppInstallAd.getIcon()
                .getDrawable());

        MediaView sampleMediaView = adView.findViewById(R.id.appinstall_media);
        ImageView imageView = adView.findViewById(R.id.appinstall_image);

        if (videoController.hasVideoContent()) {
            imageView.setVisibility(View.GONE);
            adView.setMediaView(sampleMediaView);
        } else {
            sampleMediaView.setVisibility(View.GONE);
            adView.setImageView(imageView);

            List<NativeAd.Image> images = nativeAppInstallAd.getImages();

            if (images.size() > 0) {
                ((ImageView) adView.getImageView()).setImageDrawable(images.get(0).getDrawable());
            }
        }

        // Some aren't guaranteed, however, and should be checked.
        if (nativeAppInstallAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAppInstallAd.getPrice());
        }

        if (nativeAppInstallAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAppInstallAd.getStore());
        }

        if (nativeAppInstallAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAppInstallAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
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

        // Some assets are guaranteed to be in every NativeContentAd.
        ((TextView) adView.getHeadlineView()).setText(nativeContentAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeContentAd.getBody());
        ((TextView) adView.getCallToActionView()).setText(nativeContentAd.getCallToAction());
        ((TextView) adView.getAdvertiserView()).setText(nativeContentAd.getAdvertiser());

        List<NativeAd.Image> images = nativeContentAd.getImages();

        if (images.size() > 0) {
            ((ImageView) adView.getImageView()).setImageDrawable(images.get(0).getDrawable());
        }

        // Some aren't guaranteed, however, and should be checked.
        NativeAd.Image logoImage = nativeContentAd.getLogo();

        if (logoImage == null) {
            adView.getLogoView().setVisibility(View.INVISIBLE);
        } else {
            ((ImageView) adView.getLogoView())
                    .setImageDrawable(logoImage.getDrawable());
            adView.getLogoView().setVisibility(View.VISIBLE);
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
