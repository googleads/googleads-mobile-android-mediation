package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.vungle.mediation.AdapterParametersParser;
import com.vungle.mediation.VungleBannerAdapter;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;

public class VungleRtbBannerAd implements MediationBannerAd {

  private static final String TAG = VungleRtbBannerAd.class.getSimpleName();

  private final MediationBannerAdConfiguration mediationBannerAdConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback;

  private VungleBannerAdapter vungleBannerAdapter;

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


    String placementForPlay = VungleManager
        .getInstance().findPlacement(mediationExtras, serverParameters);
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

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!VungleManager.getInstance().hasBannerSizeAd(context, adSize, adConfig)) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to load ad from Vungle. Invalid banner size.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    AdapterParametersParser.Config config = AdapterParametersParser.parse(appID, mediationExtras);
    // Adapter does not support multiple Banner instances playing for same placement except for
    // refresh.
    String uniqueRequestId = config.getRequestUniqueId();
    if (!VungleManager.getInstance().canRequestBannerAd(placementForPlay, uniqueRequestId)) {
      AdError error = new AdError(ERROR_AD_ALREADY_LOADED,
          "Vungle adapter does not support multiple banner instances for same placement.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String adMarkup = mediationBannerAdConfiguration.getBidResponse();
    Log.d(TAG, "Render banner mAdMarkup=" + adMarkup);

    vungleBannerAdapter = new VungleBannerAdapter(placementForPlay, uniqueRequestId, adConfig,
        VungleRtbBannerAd.this);
    Log.d(TAG, "New banner adapter: " + vungleBannerAdapter + "; size: " + adConfig.getAdSize());

    VungleBannerAd vungleBanner = new VungleBannerAd(placementForPlay, vungleBannerAdapter);
    VungleManager.getInstance().registerBannerAd(placementForPlay, vungleBanner);

    Log.d(TAG, "Requesting banner with ad size: " + adConfig.getAdSize());
    vungleBannerAdapter
        .requestBannerAd(context, config.getAppId(), adSize, adMarkup, mediationAdLoadCallback);
  }

  @NonNull
  @Override
  public View getView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return vungleBannerAdapter.getAdLayout();
  }
}
