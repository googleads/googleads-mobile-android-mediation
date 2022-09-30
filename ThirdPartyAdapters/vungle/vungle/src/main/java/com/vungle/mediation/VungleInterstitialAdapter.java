package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_VUNGLE_BANNER_NULL;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
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
import com.vungle.ads.BaseAd;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.InterstitialAdListener;
import com.vungle.ads.VungleException;
import java.util.ArrayList;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using Google
 * Mobile Ads SDK mediation.
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

  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener mediationInterstitialListener,
      @NonNull Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    this.mediationInterstitialListener = mediationInterstitialListener;
    String placement = PlacementFinder.findPlacement(mediationExtras, serverParameters);
    if (placement == null || placement.isEmpty()) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      this.mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

    VungleInitializer.getInstance()
        .initialize(
            appID, context,
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                interstitialAd = new InterstitialAd(placement, adConfig);
                interstitialAd.setAdListener(new VungleInterstitialListener());

                if (interstitialAd.canPlayAd()) {
                  mediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
                  return;
                }
                interstitialAd.load();
              }

              @Override
              public void onInitializeError(AdError error) {
                mediationInterstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this, error);
                Log.w(TAG, error.toString());
              }
            });
  }

  @Override
  public void showInterstitial() {
    interstitialAd.play();
  }

  private class VungleInterstitialListener implements InterstitialAdListener {

    @Override
    public void adClick(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void adEnd(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void adImpression(@NonNull BaseAd baseAd) {

    }

    @Override
    public void adLoaded(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void adStart(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
      AdError error = VungleMediationAdapter.getAdError(e);
      Log.w(TAG, error.toString());
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
    }

    @Override
    public void onAdLeftApplication(@NonNull BaseAd baseAd) {
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
      }
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
          "Failed to load ad from Vungle. Missing or invalid app ID.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      bannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

    String placement = PlacementFinder.findPlacement(mediationExtras, serverParameters);
    Log.d(TAG,
        "requestBannerAd for Placement: " + placement + " ### Adapter instance: " + this
            .hashCode());

    if (placement == null || placement.isEmpty()) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      bannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
    if (!hasBannerSizeAd(context, adSize, adConfig)) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to load ad from Vungle. Invalid banner size.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      bannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      return;
    }

    VungleInitializer.getInstance()
        .initialize(
            appID, context,
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                bannerLayout = new RelativeLayout(context);
                int adLayoutHeight = adSize.getHeightInPixels(context);
                if (adLayoutHeight <= 0) {
                  float density = context.getResources().getDisplayMetrics().density;
                  adLayoutHeight = Math.round(adConfig.getAdSize().getHeight() * density);
                }
                RelativeLayout.LayoutParams adViewLayoutParams =
                    new RelativeLayout.LayoutParams(adSize.getWidthInPixels(context),
                        adLayoutHeight);
                bannerLayout.setLayoutParams(adViewLayoutParams);

                bannerAd = new BannerAd(context, placement, adConfig);
                bannerAd.setAdListener(new VungleBannerListener());

                bannerAd.load();
              }

              @Override
              public void onInitializeError(AdError error) {
                mediationInterstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this, error);
                Log.w(TAG, error.toString());
              }
            });
  }

  private class VungleBannerListener implements BannerAdListener {

    @Override
    public void adClick(@NonNull BaseAd baseAd) {
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdClicked(VungleInterstitialAdapter.this);
        mediationBannerListener.onAdOpened(VungleInterstitialAdapter.this);
      }
    }

    @Override
    public void adEnd(@NonNull BaseAd baseAd) {
      // no-op
    }

    @Override
    public void adImpression(@NonNull BaseAd baseAd) {
      // no-op
    }

    @Override
    public void adLoaded(@NonNull BaseAd baseAd) {
      createBanner();
    }

    @Override
    public void adStart(@NonNull BaseAd baseAd) {
      // no-op
    }

    @Override
    public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
      AdError error = VungleMediationAdapter.getAdError(e);
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
    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
    View bannerView = bannerAd.getBannerView();
    if (bannerView != null) {
      bannerView.setLayoutParams(adParams);
      bannerLayout.addView(bannerView);
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
      }
    } else {
      AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
          "Vungle SDK returned a successful load callback, but Banners.getBanner() or "
              + "Vungle.getNativeAd() returned null.",
          ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
      }
    }
  }

  @NonNull
  @Override
  public View getBannerView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return bannerLayout;
  }

  private boolean hasBannerSizeAd(Context context, AdSize adSize, AdConfig adConfig) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(com.vungle.ads.AdSize.BANNER_SHORT.getWidth(),
        com.vungle.ads.AdSize.BANNER_SHORT.getHeight()));
    potentials.add(new AdSize(com.vungle.ads.AdSize.BANNER.getWidth(),
        com.vungle.ads.AdSize.BANNER.getHeight()));
    potentials.add(new AdSize(com.vungle.ads.AdSize.BANNER_LEADERBOARD.getWidth(),
        com.vungle.ads.AdSize.BANNER_LEADERBOARD.getHeight()));
    potentials.add(new AdSize(com.vungle.ads.AdSize.VUNGLE_MREC.getWidth(),
        com.vungle.ads.AdSize.VUNGLE_MREC.getHeight()));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      Log.i(TAG, "Not found closest ad size: " + adSize);
      return false;
    }
    Log.i(TAG, "Found closest ad size: " + closestSize + " for requested ad size: " + adSize);

    if (closestSize.getWidth() == com.vungle.ads.AdSize.BANNER_SHORT.getWidth()
        && closestSize.getHeight() == com.vungle.ads.AdSize.BANNER_SHORT.getHeight()) {
      adConfig.setAdSize(com.vungle.ads.AdSize.BANNER_SHORT);
    } else if (closestSize.getWidth() == com.vungle.ads.AdSize.BANNER.getWidth()
        && closestSize.getHeight() == com.vungle.ads.AdSize.BANNER.getHeight()) {
      adConfig.setAdSize(com.vungle.ads.AdSize.BANNER);
    } else if (closestSize.getWidth() == com.vungle.ads.AdSize.BANNER_LEADERBOARD.getWidth()
        && closestSize.getHeight() == com.vungle.ads.AdSize.BANNER_LEADERBOARD.getHeight()) {
      adConfig.setAdSize(com.vungle.ads.AdSize.BANNER_LEADERBOARD);
    } else if (closestSize.getWidth() == com.vungle.ads.AdSize.VUNGLE_MREC.getWidth()
        && closestSize.getHeight() == com.vungle.ads.AdSize.VUNGLE_MREC.getHeight()) {
      adConfig.setAdSize(com.vungle.ads.AdSize.VUNGLE_MREC);
    }

    return true;
  }

}
