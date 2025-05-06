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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import java.util.Locale;

/**
 * A simple {@link android.app.Activity} that displays adds using the sample custom event.
 */
public class MainActivity extends AppCompatActivity {

  private final String LOG_TAG = "MediationExample";

  // The banner ad view.
  private AdView adView;
  // A loaded app open ad.
  private AppOpenAd appOpenAd;
  // The load app open button.
  private Button loadAppOpenButton;
  // The show app open button.
  private Button showAppOpenButton;
  // A loaded interstitial ad.
  private InterstitialAd interstitial;
  // The load interstitial button.
  private Button loadInterstitialButton;
  // The show interstitial button.
  private Button showInterstitialButton;
  // A loaded rewarded ad.
  private RewardedAd rewardedAd;
  // The load rewarded ad button.
  private Button loadRewardedButton;
  // The load rewarded ad button.
  private Button showRewardedButton;
  // A loaded rewarded interstitial ad.
  private RewardedInterstitialAd rewardedInterstitialAd;
  // The load rewarded interstitial ad button.
  private Button loadRewardedInterstitialButton;
  // The load rewarded interstitial ad button.
  private Button showRewardedInterstitialButton;
  // The ad loader.
  private AdLoader adLoader;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // App Open ads.
    loadAppOpenButton = findViewById(R.id.app_open_load_button);
    loadAppOpenButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        loadAppOpenAd();
      }
    });
    showAppOpenButton = findViewById(R.id.app_open_show_button);
    showAppOpenButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        showAppOpenAd();
      }
    });

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
          public void onAdLoaded() {
            Log.d(LOG_TAG, "Banner Ad loaded: " + adView.getResponseInfo());
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            Log.d(LOG_TAG, "Failed to load banner ad: " + loadAdError.getResponseInfo());
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
                    Log.d(LOG_TAG, "Interstitial Ad loaded: " + interstitialAd.getResponseInfo());

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
                    Log.d(LOG_TAG,
                        "Failed to load interstitial ad: " + loadAdError.getResponseInfo());
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

    // Rewarded Ads.
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
                Log.d(LOG_TAG, "Rewarded Ad loaded: " + ad.getResponseInfo());

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
                Log.d(LOG_TAG, "Failed to load rewarded ad: " + loadAdError.getResponseInfo());
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
                  String.format(Locale.getDefault(), "User earned reward. Type: %s, amount: %d",
                      rewardItem.getType(), rewardItem.getAmount()),
                  Toast.LENGTH_SHORT).show();
            }
          });
        }
      }
    });

    // Rewarded Interstitial Ads.
    loadRewardedInterstitialButton = findViewById(R.id.rewarded_interstitial_load_button);
    loadRewardedInterstitialButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        loadRewardedInterstitial();
      }
    });
    showRewardedInterstitialButton = findViewById(R.id.rewarded_interstitial_show_button);
    showRewardedInterstitialButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        showRewardedInterstitial();
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
                Log.d(LOG_TAG, "Native Ad loaded: " + nativeAd.getResponseInfo());

                FrameLayout nativeContainer = findViewById(R.id.native_container);
                nativeContainer.removeAllViews();
                getLayoutInflater().inflate(R.layout.native_ad, nativeContainer, true);
                NativeAdView adView =
                    (NativeAdView) nativeContainer.findViewById(R.id.native_ad_view);
                populateNativeAdView(nativeAd, adView);
              }
            })
            .withAdListener(new AdListener() {
              @Override
              public void onAdFailedToLoad(@NonNull LoadAdError error) {
                Log.d(LOG_TAG, "Failed to load native ad: " + error.getResponseInfo());
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
   * Gets the app open ad unit ID to test.
   */
  private String getAppOpenAdUnitId() {
    return getResources().getString(R.string.customevent_app_open_ad_unit_id);
  }

  /**
   * Gets the banner ad unit ID to test.
   */
  private String getBannerAdUnitId() {
    return getResources().getString(R.string.customevent_banner_ad_unit_id);
  }

  /**
   * Gets the interstitial ad unit ID to test.
   */
  private String getInterstitialAdUnitId() {
    return getResources().getString(R.string.customevent_interstitial_ad_unit_id);
  }

  /**
   * Gets the rewarded ad unit ID to test.
   */
  private String getRewardedAdUnitId() {
    return getResources().getString(R.string.customevent_rewarded_ad_unit_id);
  }

  /**
   * Gets the rewarded interstitial ad unit ID to test.
   */
  private String getRewardedInterstitialAdUnitId() {
    return getResources().getString(R.string.customevent_rewarded_interstitial_ad_unit_id);
  }

  /**
   * Gets the native ad unit ID to test.
   */
  private String getNativeAdUnitId() {
    return getResources().getString(R.string.customevent_native_ad_unit_id);
  }

  /**
   * Loads the app open ad.
   */
  private void loadAppOpenAd() {
    loadAppOpenButton.setEnabled(false);

    AppOpenAd.load(MainActivity.this,
        getAppOpenAdUnitId(),
        new AdRequest.Builder().build(),
        new AppOpenAdLoadCallback() {
          @Override
          public void onAdLoaded(@NonNull AppOpenAd ad) {
            Log.d(LOG_TAG, "App Open Ad loaded: " + ad.getResponseInfo());

            appOpenAd = ad;
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
              @Override
              public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                Log.d(LOG_TAG, "Failed to show app open ad: " + error);
                Toast.makeText(MainActivity.this,
                    "Failed to show app open ad. See logcat for details.",
                    Toast.LENGTH_SHORT).show();
                loadAppOpenButton.setEnabled(true);
              }

              @Override
              public void onAdDismissedFullScreenContent() {
                loadAppOpenButton.setEnabled(true);
                showAppOpenButton.setEnabled(false);
              }
            });
            showAppOpenButton.setEnabled(true);
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            Log.d(LOG_TAG, "Failed to load app open ad: " + loadAdError.getResponseInfo());
            Toast.makeText(MainActivity.this,
                "Failed to load app open ad. See logcat for details.",
                Toast.LENGTH_SHORT).show();
            appOpenAd = null;
            loadAppOpenButton.setEnabled(true);
          }
        });
  }

  /**
   * Shows the app open ad.
   */
  private void showAppOpenAd() {
    if (appOpenAd == null) {
      Toast.makeText(MainActivity.this,
          "App open ad is not ready to be shown.",
          Toast.LENGTH_SHORT).show();
      return;
    }

    showRewardedButton.setEnabled(false);
    appOpenAd.show(MainActivity.this);
  }

  /**
   * Loads the rewarded interstitial ad.
   */
  private void loadRewardedInterstitial() {
    loadRewardedInterstitialButton.setEnabled(false);

    RewardedInterstitialAd.load(MainActivity.this,
        getRewardedInterstitialAdUnitId(),
        new AdRequest.Builder().build(),
        new RewardedInterstitialAdLoadCallback() {
          @Override
          public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
            Log.d(LOG_TAG, "Rewarded Interstitial Ad loaded: " + ad.getResponseInfo());

            rewardedInterstitialAd = ad;
            rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
              @Override
              public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                Log.d(LOG_TAG, "Failed to show rewarded interstitial ad: " + error);
                Toast.makeText(MainActivity.this,
                    "Failed to show rewarded interstitial ad. See logcat for details.",
                    Toast.LENGTH_SHORT).show();
                loadRewardedInterstitialButton.setEnabled(true);
              }

              @Override
              public void onAdDismissedFullScreenContent() {
                loadRewardedInterstitialButton.setEnabled(true);
                showRewardedInterstitialButton.setEnabled(false);
              }
            });
            showRewardedInterstitialButton.setEnabled(true);
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            Log.d(LOG_TAG, "Failed to load rewarded interstitial ad: " + loadAdError.getResponseInfo());
            Toast.makeText(MainActivity.this,
                "Failed to load rewarded interstitial ad. See logcat for details.",
                Toast.LENGTH_SHORT).show();
            appOpenAd = null;
            loadRewardedInterstitialButton.setEnabled(true);
          }
        });
  }

  /**
   * Shows the rewarded interstitial ad.
   */
  private void showRewardedInterstitial() {
    if (rewardedInterstitialAd == null) {
      Toast.makeText(MainActivity.this,
          "Rewarded interstitial ad is not ready to be shown.",
          Toast.LENGTH_SHORT).show();
      return;
    }

    showRewardedInterstitialButton.setEnabled(false);
    rewardedInterstitialAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
      @Override
      public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
        String rewardMessage = String.format(Locale.getDefault(),
            "User earned reward. Type: %s, amount: %d",
            rewardItem.getType(), rewardItem.getAmount());
        Log.d(LOG_TAG, rewardMessage);
        Toast.makeText(MainActivity.this, rewardMessage, Toast.LENGTH_SHORT).show();
      }
    });
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
