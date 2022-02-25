package com.google.ads.mediation.pangle.rtb;


import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_SERVER_PARAMETERS;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.google.ads.mediation.pangle.PangleConstant;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

public class PangleRtbRewardedAd implements MediationRewardedAd {

  private static final String TAG = PangleRtbRewardedAd.class.getSimpleName();
  private final MediationRewardedAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback;
  private MediationRewardedAdCallback rewardedAdCallback;
  private TTRewardVideoAd ttRewardVideoAd;

  public PangleRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    adConfiguration = mediationRewardedAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    PangleMediationAdapter.setCoppa(adConfiguration);

    String placementId = adConfiguration.getServerParameters()
        .getString(PangleConstant.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error = PangleConstant.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Pangle. Missing or invalid Placement ID.");
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    String bidResponse = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bidResponse)) {
      AdError error = PangleConstant.createAdapterError(ERROR_INVALID_BID_RESPONSE,
          "Failed to load ad from Pangle. Missing or invalid bid Response");
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
    TTAdNative mTTAdNative = mTTAdManager
        .createAdNative(adConfiguration.getContext().getApplicationContext());

    AdSlot adSlot = new AdSlot.Builder()
        .setCodeId(placementId)
        .withBid(bidResponse)
        .build();

    mTTAdNative.loadRewardVideoAd(adSlot, new TTAdNative.RewardVideoAdListener() {
      @Override
      public void onError(int errorCode, String errorMessage) {
        AdError error = PangleConstant.createSdkError(errorCode, errorMessage);
        Log.w(TAG, error.getMessage());
        adLoadCallback.onFailure(error);
      }

      @Override
      public void onRewardVideoAdLoad(TTRewardVideoAd rewardVideoAd) {
        rewardedAdCallback = adLoadCallback.onSuccess(PangleRtbRewardedAd.this);
        ttRewardVideoAd = rewardVideoAd;
      }

      @Override
      public void onRewardVideoCached() {

      }
    });
  }

  @Override
  public void showAd(@NonNull Context context) {
    ttRewardVideoAd
        .setRewardAdInteractionListener(new TTRewardVideoAd.RewardAdInteractionListener() {
          @Override
          public void onAdShow() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdOpened();
              rewardedAdCallback.reportAdImpression();
            }
          }

          @Override
          public void onAdVideoBarClick() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.reportAdClicked();
            }
          }

          @Override
          public void onAdClose() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdClosed();
            }
          }

          @Override
          public void onVideoComplete() {

          }

          @Override
          public void onVideoError() {

          }

          @Override
          public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName,
              int errorCode, String errorMsg) {
            if (!rewardVerify) {
              String newErrorMsg = String
                  .format("Failed to request rewarded ad from Pangle. The reward isn't valid. " +
                      "The specific reason is: %s", errorMsg);
              AdError error = PangleConstant.createSdkError(errorCode, newErrorMsg);
              Log.d(TAG, error.getMessage());
              return;
            }
            final String rewardType = rewardName;
            final int amount = rewardAmount;

            RewardItem rewardItem = new RewardItem() {
              @NonNull
              @Override
              public String getType() {
                return rewardType;
              }

              @Override
              public int getAmount() {
                return amount;
              }
            };
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onUserEarnedReward(rewardItem);
            }
          }

          @Override
          public void onSkippedVideo() {

          }
        });
    if (context instanceof Activity) {
      ttRewardVideoAd.showRewardVideoAd((Activity) context);
      return;
    }
    ttRewardVideoAd.showRewardVideoAd(null);
  }
}
