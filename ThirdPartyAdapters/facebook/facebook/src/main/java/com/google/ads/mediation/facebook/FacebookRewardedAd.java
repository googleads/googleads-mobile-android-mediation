package com.google.ads.mediation.facebook;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FAILED_TO_PRESENT_AD;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.getPlacementID;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.facebook.ads.Ad;
import com.facebook.ads.AdExperienceType;
import com.facebook.ads.ExtraHints;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdExtendedListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookRewardedAd implements MediationRewardedAd, RewardedVideoAdExtendedListener {

  private MediationRewardedAdConfiguration adConfiguration;
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mMediationAdLoadCallback;

  /**
   * Facebook rewarded video ad instance.
   */
  private RewardedVideoAd rewardedAd;

  /**
   * Flag to determine whether the rewarded ad has been presented.
   */
  private AtomicBoolean showAdCalled = new AtomicBoolean();

  /**
   * Mediation rewarded video ad listener used to forward rewarded video ad events from the Facebook
   * Audience Network SDK to the Google Mobile Ads SDK.
   */
  private MediationRewardedAdCallback mRewardedAdCallback;

  private boolean isRtbAd = false;
  private AtomicBoolean didRewardedAdClose = new AtomicBoolean();

  public FacebookRewardedAd(MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.mMediationAdLoadCallback = callback;
  }

  public void render() {
    final Context context = adConfiguration.getContext();
    Bundle serverParameters = adConfiguration.getServerParameters();
    final String placementID = getPlacementID(serverParameters);

    if (TextUtils.isEmpty(placementID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    String decodedBid = adConfiguration.getBidResponse();
    if (!TextUtils.isEmpty(decodedBid)) {
      isRtbAd = true;
    }

    setMixedAudience(adConfiguration);

    if (isRtbAd) {
      rewardedAd = new RewardedVideoAd(context, placementID);
      if (!TextUtils.isEmpty(adConfiguration.getWatermark())) {
        rewardedAd.setExtraHints(new ExtraHints.Builder()
            .mediationData(adConfiguration.getWatermark()).build());
      }
      rewardedAd.loadAd(
          rewardedAd.buildLoadAdConfig()
              .withAdListener(this)
              .withBid(decodedBid)
              .withAdExperience(getAdExperienceType())
              .build()
      );
    } else {
      FacebookInitializer.getInstance().initialize(context, placementID,
          new FacebookInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
              createAndLoadRewardedVideo(context, placementID);
            }

            @Override
            public void onInitializeError(AdError error) {
              Log.w(TAG, error.getMessage());
              if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(error);
              }
            }
          });
    }
  }

  @Override
  public void showAd(Context context) {
    showAdCalled.set(true);
    if (!rewardedAd.show()) {
      AdError error = new AdError(ERROR_FAILED_TO_PRESENT_AD, "Failed to present rewarded ad.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onAdFailedToShow(error);
      }
      rewardedAd.destroy();
      return;
    }

    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onVideoStart();
      mRewardedAdCallback.onAdOpened();
    }
  }

  @NonNull
  AdExperienceType getAdExperienceType() {
    return AdExperienceType.AD_EXPERIENCE_TYPE_REWARDED;
  }

  private void createAndLoadRewardedVideo(Context context, String placementID) {
    rewardedAd = new RewardedVideoAd(context, placementID);
    rewardedAd.loadAd(
        rewardedAd.buildLoadAdConfig()
            .withAdListener(this)
            .withAdExperience(getAdExperienceType())
            .build()
    );
  }

  @Override
  public void onRewardedVideoCompleted() {
    mRewardedAdCallback.onVideoComplete();
    mRewardedAdCallback.onUserEarnedReward(new FacebookReward());
  }

  @Override
  public void onError(Ad ad, com.facebook.ads.AdError adError) {
    AdError error = FacebookMediationAdapter.getAdError(adError);

    if (showAdCalled.get()) {
      Log.w(TAG, error.getMessage());
      if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onAdFailedToShow(error);
      }
    } else {
      Log.w(TAG, error.getMessage());
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure(error);
      }
    }

    rewardedAd.destroy();
  }

  @Override
  public void onAdLoaded(Ad ad) {
    if (mMediationAdLoadCallback != null) {
      mRewardedAdCallback = mMediationAdLoadCallback.onSuccess(this);
    }
  }

  @Override
  public void onAdClicked(Ad ad) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onLoggingImpression(Ad ad) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onRewardedVideoClosed() {
    if (!didRewardedAdClose.getAndSet(true) && mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdClosed();
    }
    if (rewardedAd != null) {
      rewardedAd.destroy();
    }
  }

  @Override
  public void onRewardedVideoActivityDestroyed() {
    if (!didRewardedAdClose.getAndSet(true) && mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdClosed();
    }
    if (rewardedAd != null) {
      rewardedAd.destroy();
    }
  }
}
