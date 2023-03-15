package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_VUNGLE_BANNER_NULL;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BannerAd;
import com.vungle.ads.BannerAdListener;
import com.vungle.ads.BaseAd;
import com.vungle.ads.VungleException;
import com.vungle.mediation.PlacementFinder;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleInterstitialAdapter;

public class VungleRtbBannerAd implements MediationBannerAd, BannerAdListener {

  private static final String TAG = VungleRtbBannerAd.class.getSimpleName();

  private final MediationBannerAdConfiguration mediationBannerAdConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback;
  private MediationBannerAdCallback mediationBannerAdCallback;

  private BannerAd bannerAd;
  private RelativeLayout bannerLayout;

  public VungleRtbBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    this.mediationBannerAdConfiguration = mediationBannerAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    Bundle mediationExtras = mediationBannerAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or invalid app ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String placementForPlay = PlacementFinder.findPlacement(mediationExtras, serverParameters);
    Log.d(TAG,
        "requestBannerAd for Placement: " + placementForPlay + " ### Adapter instance: " + this
            .hashCode());

    if (TextUtils.isEmpty(placementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Context context = mediationBannerAdConfiguration.getContext();
    AdSize adSize = mediationBannerAdConfiguration.getAdSize();

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
    if (!VungleInterstitialAdapter.hasBannerSizeAd(context, adSize, adConfig)) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to load ad from Vungle. Invalid banner size.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String adMarkup = mediationBannerAdConfiguration.getBidResponse();
    Log.d(TAG, "Render banner mAdMarkup=" + adMarkup);

    VungleInitializer.getInstance()
        .initialize(appID, context,
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

                bannerAd = new BannerAd(context, placementForPlay, adConfig);
                bannerAd.setAdListener(VungleRtbBannerAd.this);
                String watermark = mediationBannerAdConfiguration.getWatermark();
                if (!TextUtils.isEmpty(watermark)) {
                  adConfig.setWatermark(watermark);
                }
                bannerAd.load(adMarkup);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                mediationAdLoadCallback.onFailure(error);
              }
            });
  }

  @NonNull
  @Override
  public View getView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return bannerLayout;
  }

  //region VungleBannerAdListener
  @Override
  public void adClick(@NonNull BaseAd baseAd) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdClicked();
      mediationBannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void adEnd(@NonNull BaseAd baseAd) {
    // No-op
  }

  @Override
  public void adImpression(@NonNull BaseAd baseAd) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdImpression();
    }
  }

  @Override
  public void adLoaded(@NonNull BaseAd baseAd) {
    createBanner();
  }

  @Override
  public void adStart(@NonNull BaseAd baseAd) {
    // No-op
  }

  @Override
  public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
    AdError error = VungleMediationAdapter.getAdError(e);
    Log.w(TAG, error.toString());
    if (mediationBannerAdCallback != null) {
      // No-op
    } else {
      mediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.onAdLeftApplication();
    }
  }
  //endregion

  private void createBanner() {
    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
    View bannerView = bannerAd.getBannerView();
    if (bannerView != null) {
      bannerView.setLayoutParams(adParams);
      bannerLayout.addView(bannerView);
      mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(this);
    } else {
      AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
          "Vungle SDK returned a successful load callback, but getBannerView() returned null.",
          ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
    }
  }

}
