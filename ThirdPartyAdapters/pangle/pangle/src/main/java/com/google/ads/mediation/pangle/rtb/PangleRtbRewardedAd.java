package com.google.ads.mediation.pangle.rtb;


import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

public class PangleRtbRewardedAd implements MediationRewardedAd {

  private final MediationRewardedAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback;
  private MediationRewardedAdCallback rewardedAdCallback;
  private PAGRewardedAd pagRewardedAd;

  public PangleRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    adConfiguration = mediationRewardedAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    PangleMediationAdapter.setCoppa(adConfiguration.taggedForChildDirectedTreatment());
    PangleMediationAdapter.setUserData(adConfiguration.getMediationExtras());

    String placementId = adConfiguration.getServerParameters()
        .getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error = PangleConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load rewarded ad from Pangle. Missing or invalid Placement ID.");
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    String bidResponse = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bidResponse)) {
      AdError error = PangleConstants.createAdapterError(ERROR_INVALID_BID_RESPONSE,
          "Failed to load rewarded ad from Pangle. Missing or invalid bid response.");
      Log.w(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    PAGRewardedRequest request = new PAGRewardedRequest();
    request.setAdString(bidResponse);
    PAGRewardedAd.loadAd(placementId, request, new PAGRewardedAdLoadListener() {
      @Override
      public void onError(int errorCode, String errorMessage) {
        AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
        Log.w(TAG, error.toString());
        adLoadCallback.onFailure(error);
      }

      @Override
      public void onAdLoaded(PAGRewardedAd rewardedAd) {
        rewardedAdCallback = adLoadCallback.onSuccess(PangleRtbRewardedAd.this);
        pagRewardedAd = rewardedAd;
      }
    });
  }

  @Override
  public void showAd(@NonNull Context context) {
    pagRewardedAd.setAdInteractionListener(
        new PAGRewardedAdInteractionListener() {
          @Override
          public void onAdShowed() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdOpened();
              rewardedAdCallback.reportAdImpression();
            }
          }

          @Override
          public void onAdClicked() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.reportAdClicked();
            }
          }

          @Override
          public void onAdDismissed() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdClosed();
            }
          }

          @Override
          public void onUserEarnedReward(final PAGRewardItem pagRewardItem) {
            RewardItem rewardItem = new RewardItem() {
              @NonNull
              @Override
              public String getType() {
                return pagRewardItem.getRewardName();
              }

              @Override
              public int getAmount() {
                return pagRewardItem.getRewardAmount();
              }
            };
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onUserEarnedReward(rewardItem);
            }
          }

          @Override
          public void onUserEarnedRewardFail(int errorCode, String errorMessage) {
            String rewardErrorMessage = String.format(
                "Failed to request rewarded ad from Pangle: %s", errorMessage);
            AdError error = PangleConstants.createSdkError(errorCode, rewardErrorMessage);
            Log.d(TAG, error.toString());
          }
        });
    if (context instanceof Activity) {
      pagRewardedAd.show((Activity) context);
      return;
    }
    // If the context is not an Activity, the application context will be used to render the ad.
    pagRewardedAd.show(null);
  }
}
