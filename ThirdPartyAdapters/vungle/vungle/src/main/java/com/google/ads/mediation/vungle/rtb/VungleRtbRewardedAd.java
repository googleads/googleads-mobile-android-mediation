package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleException;
import com.vungle.mediation.PlacementFinder;
import com.vungle.mediation.VungleExtrasBuilder;

public class VungleRtbRewardedAd implements MediationRewardedAd, RewardedAdListener {

  @NonNull
  private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

  @NonNull
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  @Nullable
  private MediationRewardedAdCallback mediationRewardedAdCallback;

  private RewardedAd rewardedAd;

  public VungleRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    String userId = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error =
          new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String placement = PlacementFinder.findPlacement(mediationExtras, serverParameters);
    if (placement == null || placement.isEmpty()) {
      AdError error =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load ad from Vungle. Missing or invalid Placement ID.",
              ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String adMarkup = mediationRewardedAdConfiguration.getBidResponse();

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());

    Context context = mediationRewardedAdConfiguration.getContext();

    VungleInitializer.getInstance()
        .initialize(appID, context,
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                rewardedAd = new RewardedAd(context, placement, adConfig);
                rewardedAd.setAdListener(VungleRtbRewardedAd.this);
                if (!TextUtils.isEmpty(userId)) {
                  rewardedAd.setUserId(userId);
                }
                String watermark = mediationRewardedAdConfiguration.getWatermark();
                if (!TextUtils.isEmpty(watermark)) {
                  adConfig.setWatermark(watermark);
                }
                rewardedAd.load(adMarkup);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                mediationAdLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    rewardedAd.play();
  }

  @Override
  public void adClick(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void adEnd(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void adImpression(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onVideoStart();
      mediationRewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void adLoaded(@NonNull BaseAd baseAd) {
    mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(VungleRtbRewardedAd.this);
  }

  @Override
  public void adStart(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdOpened();
    }
  }

  @Override
  public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
    AdError error = VungleMediationAdapter.getAdError(e);
    Log.w(TAG, error.toString());
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdFailedToShow(error);
    } else {
      mediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    // no-op
  }

  @Override
  public void adRewarded(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onVideoComplete();
      mediationRewardedAdCallback.onUserEarnedReward(
          new VungleMediationAdapter.VungleReward("vungle", 1));
    }
  }
}
