package com.google.ads.mediation.facebook;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FACEBOOK_INITIALIZATION;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.createAdapterError;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.createSdkError;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.getPlacementID;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.ExtraHints;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdExtendedListener;
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
      String message = createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad, placementID is null or empty.");
      Log.e(TAG, message);
      mMediationAdLoadCallback.onFailure(message);
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
            public void onInitializeError(String message) {
              String logMessage = "Failed to load ad from Facebook: " + message;
              String errorMessage = createAdapterError(ERROR_FACEBOOK_INITIALIZATION, logMessage);
              Log.w(TAG, errorMessage);
              if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(errorMessage);
              }
            }
          });
    }
  }

  @Override
  public void showAd(Context context) {
    if (rewardedAd.isAdLoaded()) {
      rewardedAd.show();
      if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onVideoStart();
        mRewardedAdCallback.onAdOpened();
      }
    } else {
      if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onAdFailedToShow("No ads to show.");
      }
    }
  }

  private void createAndLoadRewardedVideo(Context context, String placementID) {
    rewardedAd = new RewardedVideoAd(context, placementID);
    rewardedAd.loadAd(
        rewardedAd.buildLoadAdConfig()
            .withAdListener(this)
            .build()
    );
  }

  @Override
  public void onRewardedVideoCompleted() {
    mRewardedAdCallback.onVideoComplete();
    mRewardedAdCallback.onUserEarnedReward(new FacebookReward());
  }

  @Override
  public void onError(Ad ad, AdError adError) {
    String errorMessage = createSdkError(adError);
    Log.w(TAG, "Failed to load ad from Facebook: " + errorMessage);
    if (mMediationAdLoadCallback != null) {
      mMediationAdLoadCallback.onFailure(errorMessage);
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
      if (isRtbAd) {
        // TODO: Upon approval, add this callback back in.
        // mRewardedAdCallback.reportAdClicked();
      } else {
        mRewardedAdCallback.reportAdClicked();
      }
    }
  }

  @Override
  public void onLoggingImpression(Ad ad) {
    if (mRewardedAdCallback != null) {
      if (isRtbAd) {
        // TODO: Upon approval, add this callback back in.
        // mRewardedAdCallback.reportAdImpression();
      } else {
        mRewardedAdCallback.reportAdImpression();
      }
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
