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
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;

public class VungleRtbRewardedAd implements MediationRewardedAd, LoadAdCallback, PlayAdCallback {

  @NonNull
  private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;
  @NonNull
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mMediationAdLoadCallback;
  @Nullable
  private MediationRewardedAdCallback mMediationRewardedAdCallback;

  private AdConfig mAdConfig;
  private String mPlacement;
  private String mAdMarkup;
  private String mUserID;

  public VungleRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
    this.mMediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    if (mediationExtras != null) {
      mUserID = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);
    }

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacement)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    mAdMarkup = mediationRewardedAdConfiguration.getBidResponse();
    Log.d(TAG, "Render rewarded mAdMarkup=" + mAdMarkup);

    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());

    VungleInitializer.getInstance()
        .initialize(
            appID,
            mediationRewardedAdConfiguration.getContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                Vungle.setIncentivizedFields(mUserID, null, null, null, null);

                if (Vungle.canPlayAd(mPlacement, mAdMarkup)) {
                  mMediationRewardedAdCallback =
                      mMediationAdLoadCallback.onSuccess(VungleRtbRewardedAd.this);
                  return;
                }

                Vungle.loadAd(mPlacement, mAdMarkup, mAdConfig, VungleRtbRewardedAd.this);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.getMessage());
                mMediationAdLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    Vungle.playAd(mPlacement, mAdMarkup, mAdConfig, VungleRtbRewardedAd.this);
  }

  /**
   * {@link LoadAdCallback} implementation from Vungle.
   */
  @Override
  public void onAdLoad(final String placementId) {
    if (mMediationAdLoadCallback != null) {
      mMediationRewardedAdCallback =
          mMediationAdLoadCallback.onSuccess(VungleRtbRewardedAd.this);
    }
  }

  @Override
  public void creativeId(String creativeId) {
    // no-op
  }

  /**
   * {@link PlayAdCallback} implementation from Vungle
   */
  @Override
  public void onAdStart(final String placementId) {
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onAdOpened();
    }
  }

  @Override
  @Deprecated
  public void onAdEnd(final String placementId, final boolean wasSuccessfulView,
      final boolean wasCallToActionClicked) {
  }

  @Override
  public void onAdEnd(final String placementId) {
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onAdClick(String placementId) {
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdRewarded(String placementId) {
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onVideoComplete();
      mMediationRewardedAdCallback.onUserEarnedReward(new VungleMediationAdapter.VungleReward("vungle", 1));
    }
  }

  @Override
  public void onAdLeftApplication(String placementId) {
    // no op
  }

  // Vungle's LoadAdCallback and PlayAdCallback shares the same onError() call; when an
  // ad request to Vungle fails, and when an ad fails to play.
  @Override
  public void onError(final String placementId, final VungleException throwable) {
    AdError error = VungleMediationAdapter.getAdError(throwable);
    Log.w(TAG, error.getMessage());
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onAdFailedToShow(error);
    } else if (mMediationAdLoadCallback != null) {
      mMediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdViewed(String placementId) {
    mMediationRewardedAdCallback.onVideoStart();
    mMediationRewardedAdCallback.reportAdImpression();
  }
}