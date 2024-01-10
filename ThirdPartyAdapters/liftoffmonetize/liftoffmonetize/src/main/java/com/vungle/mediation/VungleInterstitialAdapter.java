// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_VUNGLE_BANNER_NULL;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.ads.mediation.vungle.VungleFactory;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BannerAd;
import com.vungle.ads.BannerAdListener;
import com.vungle.ads.BannerAdSize;
import com.vungle.ads.BaseAd;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.InterstitialAdListener;
import com.vungle.ads.VungleError;
import java.util.ArrayList;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Liftoff Monetize interstitial ads
 * using Google Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter
    implements MediationInterstitialAdapter, MediationBannerAdapter {

  private MediationInterstitialListener mediationInterstitialListener;
  private InterstitialAd interstitialAd;

  // banner/MREC
  private MediationBannerListener mediationBannerListener;
  private BannerAd bannerAd;
  private RelativeLayout bannerLayout;

  private final VungleFactory vungleFactory;

  @VisibleForTesting
  VungleInterstitialListener vungleInterstitialListener;

  @VisibleForTesting
  VungleBannerListener vungleBannerListener;

  public VungleInterstitialAdapter() {
    vungleFactory = new VungleFactory();
  }

  @VisibleForTesting
  VungleInterstitialAdapter(VungleFactory vungleFactory) {
    this.vungleFactory = vungleFactory;
  }

  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener interstitialListener,
      @NonNull Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {
    this.mediationInterstitialListener = interstitialListener;
    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load waterfall interstitial ad from Liftoff Monetize. "
              + "Missing or invalid App ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      interstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    String placement = serverParameters.getString(KEY_PLACEMENT_ID);
    if (TextUtils.isEmpty(placement)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load waterfall interstitial ad from Liftoff Monetize. "
              + "Missing or invalid Placement ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      interstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

    AdConfig adConfig = vungleFactory.createAdConfig();
    if (mediationExtras != null && mediationExtras.containsKey(KEY_ORIENTATION)) {
      adConfig.setAdOrientation(
          mediationExtras.getInt(KEY_ORIENTATION, AdConfig.AUTO_ROTATE));
    }

    VungleInitializer.getInstance()
        .initialize(
            appID, context,
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                interstitialAd = vungleFactory.createInterstitialAd(context, placement, adConfig);
                vungleInterstitialListener = new VungleInterstitialListener();
                interstitialAd.setAdListener(vungleInterstitialListener);
                interstitialAd.load(null);
              }

              @Override
              public void onInitializeError(AdError error) {
                interstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this, error);
                Log.w(TAG, error.toString());
              }
            });
  }

  @Override
  public void showInterstitial() {
    if (interstitialAd != null) {
      interstitialAd.play(null);
    }
  }

  @VisibleForTesting
  class VungleInterstitialListener implements InterstitialAdListener {

    @Override
    public void onAdLoaded(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void onAdStart(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void onAdEnd(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void onAdClicked(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void onAdLeftApplication(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void onAdFailedToPlay(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
      AdError error = VungleMediationAdapter.getAdError(vungleError);
      Log.w(TAG, error.toString());
      // Google Mobile Ads SDK doesn't have a matching event.
    }

    @Override
    public void onAdFailedToLoad(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
      AdError error = VungleMediationAdapter.getAdError(vungleError);
      Log.w(TAG, error.toString());
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
    }

    @Override
    public void onAdImpression(@NonNull BaseAd baseAd) {
      // Google Mobile Ads SDK doesn't have a matching event.
    }
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy: " + hashCode());
    if (bannerAd != null) {
      bannerLayout.removeAllViews();
      bannerAd.finishAd();
      bannerAd = null;
    }
  }

  @Override
  public void onPause() {
    // no-op
  }

  @Override
  public void onResume() {
    // no-op
  }

  @Override
  public void requestBannerAd(@NonNull Context context,
      @NonNull final MediationBannerListener bannerListener,
      @NonNull Bundle serverParameters, @NonNull AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {
    mediationBannerListener = bannerListener;
    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load waterfall banner ad from Liftoff Monetize. "
              + "Missing or invalid App ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      bannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

    String placement = serverParameters.getString(KEY_PLACEMENT_ID);
    if (TextUtils.isEmpty(placement)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load waterfall banner ad from Liftoff Monetize. "
              + "Missing or invalid Placement ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      bannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    BannerAdSize bannerAdSize = getVungleBannerAdSizeFromGoogleAdSize(context, adSize);
    if (bannerAdSize == null) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to load waterfall banner ad from Liftoff Monetize. Invalid banner size.",
          ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      bannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    Log.d(TAG,
        "requestBannerAd for Placement: " + placement + " ### Adapter instance: " + this
            .hashCode());

    VungleInitializer.getInstance()
        .initialize(
            appID, context,
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                bannerLayout = new RelativeLayout(context);
                int adLayoutHeight = adSize.getHeightInPixels(context);
                // If the height is 0 (e.g. for inline adaptive banner requests), use the closest supported size
                // as the height of the adLayout wrapper.
                if (adLayoutHeight <= 0) {
                  float density = context.getResources().getDisplayMetrics().density;
                  adLayoutHeight = Math.round(bannerAdSize.getHeight() * density);
                }
                RelativeLayout.LayoutParams adViewLayoutParams =
                    new RelativeLayout.LayoutParams(adSize.getWidthInPixels(context),
                        adLayoutHeight);
                bannerLayout.setLayoutParams(adViewLayoutParams);

                bannerAd = vungleFactory.createBannerAd(context, placement, bannerAdSize);
                vungleBannerListener = new VungleBannerListener();
                bannerAd.setAdListener(vungleBannerListener);

                bannerAd.load(null);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                if (mediationBannerListener != null) {
                  mediationBannerListener
                      .onAdFailedToLoad(VungleInterstitialAdapter.this, error);
                }
              }
            });
  }

  @VisibleForTesting
  class VungleBannerListener implements BannerAdListener {

    @Override
    public void onAdClicked(@NonNull BaseAd baseAd) {
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdClicked(VungleInterstitialAdapter.this);
        mediationBannerListener.onAdOpened(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void onAdEnd(@NonNull BaseAd baseAd) {
      // Google Mobile Ads SDK doesn't have a matching event.
    }

    @Override
    public void onAdImpression(@NonNull BaseAd baseAd) {
      // Google Mobile Ads SDK doesn't have a matching event.
    }

    @Override
    public void onAdLoaded(@NonNull BaseAd baseAd) {
      createBanner();
    }

    @Override
    public void onAdStart(@NonNull BaseAd baseAd) {
      // Google Mobile Ads SDK doesn't have a matching event.
    }

    @Override
    public void onAdFailedToPlay(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
      AdError error = VungleMediationAdapter.getAdError(vungleError);
      Log.w(TAG, error.toString());
      // Google Mobile Ads SDK doesn't have a matching event.
    }

    @Override
    public void onAdFailedToLoad(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
      AdError error = VungleMediationAdapter.getAdError(vungleError);
      Log.w(TAG, error.toString());
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
    }

    @Override
    public void onAdLeftApplication(@NonNull BaseAd baseAd) {
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdLeftApplication(VungleInterstitialAdapter.this);
      }
    }
  }

  private void createBanner() {
    if (bannerAd == null) {
      AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
          "Try to play banner ad but the Vungle BannerAd instance not created.",
          ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
      return;
    }

    View bannerView = bannerAd.getBannerView();
    // The Vungle SDK performs an internal check to determine if a banner ad is playable.
    // If the ad is not playable, such as if it has expired, the SDK will return `null` for the
    // banner view.
    if (bannerView == null) {
      AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
          "Vungle SDK returned a successful load callback, but getBannerView() returned null.",
          ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
      return;
    }

    // Add rules to ensure the banner ad is located at the center of the layout.
    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
    bannerView.setLayoutParams(adParams);
    bannerLayout.addView(bannerView);
    if (mediationBannerListener != null) {
      mediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
    }
  }

  @NonNull
  @Override
  public View getBannerView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return bannerLayout;
  }

  public static BannerAdSize getVungleBannerAdSizeFromGoogleAdSize(Context context, AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(BannerAdSize.BANNER_SHORT.getWidth(),
        BannerAdSize.BANNER_SHORT.getHeight()));
    potentials.add(new AdSize(BannerAdSize.BANNER.getWidth(),
        BannerAdSize.BANNER.getHeight()));
    potentials.add(new AdSize(BannerAdSize.BANNER_LEADERBOARD.getWidth(),
        BannerAdSize.BANNER_LEADERBOARD.getHeight()));
    potentials.add(new AdSize(BannerAdSize.VUNGLE_MREC.getWidth(),
        BannerAdSize.VUNGLE_MREC.getHeight()));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      return null;
    }
    Log.d(TAG,
        "Found closest Liftoff Monetize banner ad size: " + closestSize + " for requested ad size: "
            + adSize);

    if (closestSize.getWidth() == BannerAdSize.BANNER_SHORT.getWidth()
        && closestSize.getHeight() == BannerAdSize.BANNER_SHORT.getHeight()) {
      return BannerAdSize.BANNER_SHORT;
    } else if (closestSize.getWidth() == BannerAdSize.BANNER.getWidth()
        && closestSize.getHeight() == BannerAdSize.BANNER.getHeight()) {
      return BannerAdSize.BANNER;
    } else if (closestSize.getWidth() == BannerAdSize.BANNER_LEADERBOARD.getWidth()
        && closestSize.getHeight() == BannerAdSize.BANNER_LEADERBOARD.getHeight()) {
      return BannerAdSize.BANNER_LEADERBOARD;
    } else if (closestSize.getWidth() == BannerAdSize.VUNGLE_MREC.getWidth()
        && closestSize.getHeight() == BannerAdSize.VUNGLE_MREC.getHeight()) {
      return BannerAdSize.VUNGLE_MREC;
    }

    return null;
  }

}
