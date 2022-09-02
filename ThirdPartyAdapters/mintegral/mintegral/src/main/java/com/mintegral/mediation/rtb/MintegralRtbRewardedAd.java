package com.mintegral.mediation.rtb;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.MBBidRewardVideoHandler;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.RewardInfo;
import com.mbridge.msdk.out.RewardVideoListener;
import com.mintegral.mediation.MintegralExtrasBuilder;
import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

public class MintegralRtbRewardedAd implements MediationRewardedAd, RewardVideoListener {


  private final MediationRewardedAdConfiguration adConfiguration;

  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          callback;

  private MBBidRewardVideoHandler mbBidRewardVideoHandler;
  private MediationRewardedAdCallback rewardedAdCallback;

  public MintegralRtbRewardedAd(MediationRewardedAdConfiguration adConfiguration,
                                MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.callback = callback;
  }

  public void loadAd() {
    String adUnitId = adConfiguration.getServerParameters().getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters().getString(MintegralConstants.PLACEMENT_ID);
    mbBidRewardVideoHandler = new MBBidRewardVideoHandler(adConfiguration.getContext(), placementId, adUnitId);
    mbBidRewardVideoHandler.setRewardVideoListener(this);
    String token = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(token)) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_BID_RESPONSE, "Failed to load rewarded ad from MIntegral. Missing or invalid bid response.");
      callback.onFailure(error);
      return;
    }
    mbBidRewardVideoHandler.loadFromBid(token);
  }

  @Override
  public void showAd(@NonNull Context context) {
    boolean muted = adConfiguration.getMediationExtras().getBoolean(MintegralExtrasBuilder.MUTE_AUDIO);
    mbBidRewardVideoHandler.playVideoMute(muted ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
    mbBidRewardVideoHandler.showFromBid();
  }

  @Override
  public void onVideoLoadSuccess(MBridgeIds mBridgeIds) {
    rewardedAdCallback = callback.onSuccess(this);
  }

  @Override
  public void onLoadSuccess(MBridgeIds mBridgeIds) {
    //No-op, AdMob has no corresponding method
  }

  @Override
  public void onVideoLoadFail(MBridgeIds mBridgeIds, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorMessage);
    Log.w(TAG, error.toString());
    callback.onFailure(error);
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
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdClosed();
    }
    if (rewardInfo == null || !rewardInfo.isCompleteView()) {
      return;
    }
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
        } catch (Exception e) {
          e.printStackTrace();
        }
        return amount;
      }
    };
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onUserEarnedReward(rewardItem);
    }


  }

  @Override
  public void onShowFail(MBridgeIds mBridgeIds, String errorMessage) {
    if (rewardedAdCallback != null) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_SDK_INTER_ERROR, errorMessage);
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
    //No-op, AdMob has no corresponding method
  }

  @Override
  public void onEndcardShow(MBridgeIds mBridgeIds) {
    //No-op, AdMob has no corresponding method
  }
}
