package com.google.ads.mediation.mintegral.mediation;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.MBRewardVideoHandler;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.RewardInfo;
import com.mbridge.msdk.out.RewardVideoListener;

public class MintegralRewardedAd implements MediationRewardedAd, RewardVideoListener {

  private final MediationRewardedAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;
  private MBRewardVideoHandler mbRewardVideoHandler;
  private MediationRewardedAdCallback rewardedAdCallback;

  public MintegralRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
                             @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          adLoadCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  public void loadAd() {
    String adUnitId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.PLACEMENT_ID);
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }
    mbRewardVideoHandler = new MBRewardVideoHandler(adConfiguration.getContext(), placementId,
        adUnitId);
    mbRewardVideoHandler.setRewardVideoListener(this);
    mbRewardVideoHandler.load();
  }

  @Override
  public void showAd(@NonNull Context context) {
    boolean muted = MintegralUtils.shouldMuteAudio(adConfiguration.getMediationExtras());
    mbRewardVideoHandler.playVideoMute(muted ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE
        : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
    mbRewardVideoHandler.show();
  }

  @Override
  public void onVideoLoadSuccess(MBridgeIds mBridgeIds) {
    rewardedAdCallback = adLoadCallback.onSuccess(this);
  }

  @Override
  public void onLoadSuccess(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onVideoLoadFail(MBridgeIds mBridgeIds, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorMessage);
    Log.w(TAG, error.toString());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onAdShow(MBridgeIds mBridgeIds) {
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdOpened();
      rewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdClose(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
    if (rewardedAdCallback == null) {
      return;
    }
    if (rewardInfo != null && rewardInfo.isCompleteView()) {
      RewardItem rewardItem = new RewardItem() {
        @NonNull
        @Override
        public String getType() {
          return rewardInfo.getRewardName();
        }
        @Override
        public int getAmount() {
          int amount = 0;
          try {
            amount = Integer.getInteger(rewardInfo.getRewardAmount());
          } catch (Exception exception) {
            Log.w(TAG, "Failed to get reward amount.", exception);
          }
          return amount;
        }
      };
      rewardedAdCallback.onUserEarnedReward(rewardItem);
    } else {
      Log.w(TAG, "Mintegral SDK failed to reward user due to missing reward settings "
              + "or rewarded ad playback not completed.");
    }
    rewardedAdCallback.onAdClosed();
  }

  @Override
  public void onShowFail(MBridgeIds mBridgeIds, String errorMessage) {
    if (rewardedAdCallback != null) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_MINTEGRAL_SDK,
          errorMessage);
      Log.w(TAG, error.toString());
      rewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onVideoAdClicked(MBridgeIds mBridgeIds) {
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onVideoComplete(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onEndcardShow(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }
}
