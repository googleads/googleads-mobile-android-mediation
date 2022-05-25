package com.google.ads.mediation.vungle;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class VungleRewardedAd implements MediationRewardedAd, LoadAdCallback, PlayAdCallback {

  private static final String TAG = VungleRewardedAd.class.getSimpleName();

  public static final String KEY_APP_ID = "appid";

  private AdConfig mAdConfig;
  private String mUserID;
  private String mPlacement;
  private String mAdMarkup;

  private static final HashMap<String, WeakReference<VungleRewardedAd>> mPlacementsInUse =
      new HashMap<>();

  private final MediationRewardedAdConfiguration mAdConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mMediationAdLoadCallback;

  private MediationRewardedAdCallback mMediationRewardedAdCallback;

  public VungleRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    this.mAdConfiguration = adConfiguration;
    this.mMediationAdLoadCallback = mediationAdLoadCallback;
    VungleInitializer.getInstance().updateCoppaStatus(mAdConfiguration.taggedForChildDirectedTreatment());
  }

  public void render() {
    Bundle mediationExtras = mAdConfiguration.getMediationExtras();
    Bundle serverParameters = mAdConfiguration.getServerParameters();

    if (mediationExtras != null) {
      mUserID = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);
    }

    mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacement)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    if (mPlacementsInUse.containsKey(mPlacement)
        && mPlacementsInUse.get(mPlacement).get() != null) {
      AdError error = new AdError(ERROR_AD_ALREADY_LOADED,
          "Only a maximum of one ad can be loaded per placement.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    mAdMarkup = mAdConfiguration.getBidResponse();
    if (TextUtils.isEmpty(mAdMarkup)) {
      mAdMarkup = null;
    }
    Log.d(TAG, "Render rewarded mAdMarkup=" + mAdMarkup);

    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);

    VungleInitializer.getInstance()
        .updateCoppaStatus(mAdConfiguration.taggedForChildDirectedTreatment());

    VungleInitializer.getInstance()
        .initialize(
            appID,
            mAdConfiguration.getContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                Vungle.setIncentivizedFields(mUserID, null, null, null, null);
                mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleRewardedAd.this));

                if (Vungle.canPlayAd(mPlacement, mAdMarkup)) {
                  mMediationRewardedAdCallback =
                      mMediationAdLoadCallback.onSuccess(VungleRewardedAd.this);
                  return;
                }

                Vungle.loadAd(mPlacement, mAdMarkup, mAdConfig, VungleRewardedAd.this);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.getMessage());
                mMediationAdLoadCallback.onFailure(error);
                mPlacementsInUse.remove(mPlacement);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    Vungle.playAd(mPlacement, mAdMarkup, mAdConfig, this);
  }

  @Override
  public void onAdLoad(String placementId) {
    if (mMediationAdLoadCallback != null) {
      mMediationRewardedAdCallback =
          mMediationAdLoadCallback.onSuccess(VungleRewardedAd.this);
    }
    mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleRewardedAd.this));
  }

  @Override
  public void creativeId(String creativeId) {
    // no-op
  }

  @Override
  public void onAdStart(String placementId) {
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onAdOpened();
    }
  }

  @Override
  @Deprecated
  public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) {
  }

  @Override
  public void onAdEnd(String placementId) {
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onAdClosed();
    }
    mPlacementsInUse.remove(placementId);
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
      mMediationRewardedAdCallback.onUserEarnedReward(new VungleReward("vungle", 1));
    }
  }

  @Override
  public void onAdLeftApplication(String placementId) {
    // no op
  }

  @Override
  public void onError(String placementId, VungleException exception) {
    final AdError error = VungleMediationAdapter.getAdError(exception);
    Log.w(TAG, error.getMessage());
    if (mMediationAdLoadCallback != null) {
      mMediationAdLoadCallback.onFailure(error);
    }
    if (mMediationRewardedAdCallback != null) {
      AdError error1 = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to show ad from Vungle.", ERROR_DOMAIN);
      mMediationRewardedAdCallback.onAdFailedToShow(error1);
    }
    mPlacementsInUse.remove(placementId);
  }

  @Override
  public void onAdViewed(String placementId) {
    mMediationRewardedAdCallback.onVideoStart();
    mMediationRewardedAdCallback.reportAdImpression();
  }

  /**
   * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
   */
  private static class VungleReward implements RewardItem {

    private final String mType;
    private final int mAmount;

    VungleReward(String type, int amount) {
      mType = type;
      mAmount = amount;
    }

    @Override
    public int getAmount() {
      return mAmount;
    }

    @NonNull
    @Override
    public String getType() {
      return mType;
    }
  }
}
