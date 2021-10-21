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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * A simple {@link android.app.Activity} that displays adds using the sample adapter and sample
 * custom event.
 */
public class MainActivity extends AppCompatActivity {
  // Radio button indicating if the integration type to test is the adapter.
  private RadioButton adapterRadioButton;

  // The banner ad view.
  private AdView adView;
  // A loaded interstitial ad.
  private InterstitialAd interstitial;
  // A loaded rewarded ad.
  private RewardedAd rewardedAd;
  // The load interstitial button.
  private Button loadInterstitialButton;
  // The show interstitial button.
  private Button showInterstitialButton;
  // The load rewarded ad button.
  private Button loadRewardedButton;
  // The load rewarded ad button.
  private Button showRewardedButton;
  // The ad loader.
  private AdLoader adLoader;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    adapterRadioButton = findViewById(R.id.integration_adapter);

    // Banner ads.
    Button loadBannerButton = findViewById(R.id.banner_load_ad);
    loadBannerButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        adView = new AdView(view.getContext());
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(getBannerAdUnitId());
        adView.setAdListener(new AdListener() {
          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            Toast.makeText(MainActivity.this,
                "Failed to load banner: " + loadAdError,
                Toast.LENGTH_SHORT).show();
          }
        });
        adView.loadAd(new AdRequest.Builder().build());

        // Add banner to view hierarchy.
        FrameLayout bannerContainer = findViewById(R.id.banner_container);
        bannerContainer.removeAllViews();
        bannerContainer.addView(adView);
      }
    });


    // Interstitial ads.
    loadInterstitialButton = (Button) findViewById(R.id.interstitial_load_button);
    loadInterstitialButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            loadInterstitialButton.setEnabled(false);
            InterstitialAd.load(MainActivity.this,
                getInterstitialAdUnitId(),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                  @Override
                  public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    interstitial = interstitialAd;
                    interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
                      @Override
                      public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                        Toast.makeText(MainActivity.this,
                            "Failed to show interstitial: " + error,
                            Toast.LENGTH_SHORT).show();
                        loadInterstitialButton.setEnabled(true);
                      }

                      @Override
                      public void onAdDismissedFullScreenContent() {
                        loadInterstitialButton.setEnabled(true);
                      }
                    });
                    showInterstitialButton.setEnabled(true);
                  }

                  @Override
                  public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Toast.makeText(MainActivity.this,
                        "Failed to load interstitial: " + loadAdError,
                        Toast.LENGTH_SHORT).show();
                    interstitial = null;
                    loadInterstitialButton.setEnabled(true);
                  }
                });
          }
        });

    showInterstitialButton = (Button) findViewById(R.id.interstitial_show_button);
    showInterstitialButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showInterstitialButton.setEnabled(false);
        if (interstitial != null) {
          interstitial.show(MainActivity.this);
        }
      }
    });

    //Sample Adapter Rewarded Ad Button.
    loadRewardedButton = (Button) findViewById(R.id.rewarded_load_button);
    loadRewardedButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        loadRewardedButton.setEnabled(false);
        RewardedAd.load(MainActivity.this,
            getRewardedAdUnitId(),
            new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
              @Override
              public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                  @Override
                  public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                    Toast.makeText(MainActivity.this,
                        "Failed to show interstitial: " + error,
                        Toast.LENGTH_SHORT).show();
                    loadRewardedButton.setEnabled(true);
                  }

                  @Override
                  public void onAdDismissedFullScreenContent() {
                    loadRewardedButton.setEnabled(true);
                  }
                });
                showRewardedButton.setEnabled(true);
              }

              @Override
              public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Toast.makeText(MainActivity.this,
                    "Failed to load rewarded ad: " + loadAdError,
                    Toast.LENGTH_SHORT).show();
                rewardedAd = null;
                loadRewardedButton.setEnabled(true);
              }
            });
      }
    });

    showRewardedButton = (Button) findViewById(R.id.rewarded_show_button);
    showRewardedButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showRewardedButton.setEnabled(false);
        if (rewardedAd != null) {
          rewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
              Toast.makeText(MainActivity.this,
                  String.format("User earned reward. Type: %s, amount: %d",
                      rewardItem.getType(), rewardItem.getAmount()),
                  Toast.LENGTH_SHORT).show();
            }
          });
        }
      }
    });

    // Native ads.
    final Button nativeLoadButton = (Button) findViewById(R.id.native_load_ad);
    nativeLoadButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        adLoader = new AdLoader.Builder(view.getContext(), getNativeAdUnitId())
            .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
              @Override
              public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                FrameLayout nativeContainer = findViewById(R.id.native_container);
                NativeAdView adView = (NativeAdView) getLayoutInflater()
                    .inflate(R.layout.native_ad, null);
                populateNativeAdView(nativeAd, adView);
                nativeContainer.removeAllViews();
                nativeContainer.addView(adView);
              }
            })
            .withAdListener(new AdListener() {
              @Override
              public void onAdFailedToLoad(@NonNull LoadAdError error) {
                Toast.makeText(MainActivity.this,
                    "Failed to load native ad: " + error,
                    Toast.LENGTH_SHORT).show();
              }
            }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
      }
    });
  }

  /**
   * Gets the banner ad unit ID to test.
   */
  private String getBannerAdUnitId() {
    if (adapterRadioButton.isChecked()) {
      return getResources().getString(R.string.adapter_banner_ad_unit_id);
    }
    return getResources().getString(R.string.customevent_banner_ad_unit_id);
  }

  /**
   * Gets the interstitial ad unit ID to test.
   */
  private String getInterstitialAdUnitId() {
    if (adapterRadioButton.isChecked()) {
      return getResources().getString(R.string.adapter_interstitial_ad_unit_id);
    }
    return getResources().getString(R.string.customevent_interstitial_ad_unit_id);
  }

  /**
   * Gets the rewarded ad unit ID to test.
   */
  private String getRewardedAdUnitId() {
    if (adapterRadioButton.isChecked()) {
      return getResources().getString(R.string.adapter_rewarded_ad_unit_id);
    }
    return getResources().getString(R.string.customevent_rewarded_ad_unit_id);
  }

  /**
   * Gets the native ad unit ID to test.
   */
  private String getNativeAdUnitId() {
    if (adapterRadioButton.isChecked()) {
      return getResources().getString(R.string.adapter_native_ad_unit_id);
    }
    return getResources().getString(R.string.customevent_native_ad_unit_id);
  }

  /**
   * Populates a {@link NativeAdView} object with data from a given {@link NativeAd}.
   *
   * @param nativeAd the object containing the ad's assets
   * @param adView the view to be populated
   */
  private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
    // Set the media view. Media content will be automatically populated in the media view once
    // adView.setNativeAd() is called.
    MediaView mediaView = adView.findViewById(R.id.ad_media);
    adView.setMediaView(mediaView);

    // Set other ad assets.
    adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
    adView.setBodyView(adView.findViewById(R.id.ad_body));
    adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
    adView.setIconView(adView.findViewById(R.id.ad_app_icon));
    adView.setPriceView(adView.findViewById(R.id.ad_price));
    adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
    adView.setStoreView(adView.findViewById(R.id.ad_store));
    adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

    // The headline is guaranteed to be in every NativeAd.
    ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

    // These assets aren't guaranteed to be in every NativeAd, so it's important to
    // check before trying to display them.
    if (nativeAd.getBody() == null) {
      adView.getBodyView().setVisibility(View.INVISIBLE);
    } else {
      adView.getBodyView().setVisibility(View.VISIBLE);
      ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
    }

    if (nativeAd.getCallToAction() == null) {
      adView.getCallToActionView().setVisibility(View.INVISIBLE);
    } else {
      adView.getCallToActionView().setVisibility(View.VISIBLE);
      ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
    }

    if (nativeAd.getIcon() == null) {
      adView.getIconView().setVisibility(View.GONE);
    } else {
      ((ImageView) adView.getIconView()).setImageDrawable(
          nativeAd.getIcon().getDrawable());
      adView.getIconView().setVisibility(View.VISIBLE);
    }

    if (nativeAd.getPrice() == null) {
      adView.getPriceView().setVisibility(View.INVISIBLE);
    } else {
      adView.getPriceView().setVisibility(View.VISIBLE);
      ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
    }

    if (nativeAd.getStore() == null) {
      adView.getStoreView().setVisibility(View.INVISIBLE);
    } else {
      adView.getStoreView().setVisibility(View.VISIBLE);
      ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
    }

    if (nativeAd.getStarRating() == null || nativeAd.getStarRating() < 3) {
      adView.getStarRatingView().setVisibility(View.INVISIBLE);
    } else {
      ((RatingBar) adView.getStarRatingView())
          .setRating(nativeAd.getStarRating().floatValue());
      adView.getStarRatingView().setVisibility(View.VISIBLE);
    }

    if (nativeAd.getAdvertiser() == null) {
      adView.getAdvertiserView().setVisibility(View.INVISIBLE);
    } else {
      ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
      adView.getAdvertiserView().setVisibility(View.VISIBLE);
    }

    // This method tells the Google Mobile Ads SDK that you have finished populating your
    // native ad view with this native ad. The SDK will populate the adView's MediaView
    // with the media content from this native ad.
    adView.setNativeAd(nativeAd);
  }
}
