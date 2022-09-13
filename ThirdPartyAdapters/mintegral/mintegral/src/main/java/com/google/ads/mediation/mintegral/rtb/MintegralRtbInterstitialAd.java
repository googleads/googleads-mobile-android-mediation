package com.google.ads.mediation.mintegral.rtb;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;

import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler;
import com.mbridge.msdk.newinterstitial.out.NewInterstitialListener;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.RewardInfo;


public class MintegralRtbInterstitialAd implements MediationInterstitialAd, NewInterstitialListener {

  private final MediationInterstitialAdConfiguration adConfiguration;

  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> adLoadCallback;

  private MBBidNewInterstitialHandler mbBidNewInterstitialHandler;
  private MediationInterstitialAdCallback interstitialAdCallback;

  public MintegralRtbInterstitialAd(MediationInterstitialAdConfiguration adConfiguration,
                                    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = callback;
  }


  public void loadAd() {
    String adUnitId = adConfiguration.getServerParameters().getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters().getString(MintegralConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(adUnitId)) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS, "Failed to load interstitial ad from MIntegral. Missing or invalid adUnitId");
      adLoadCallback.onFailure(error);
      return;
    }
    if (TextUtils.isEmpty(placementId)) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS, "Failed to load interstitial ad from MIntegral. Missing or invalid placementId");
      adLoadCallback.onFailure(error);
      return;
    }
    mbBidNewInterstitialHandler = new MBBidNewInterstitialHandler(adConfiguration.getContext(), placementId, adUnitId);
    mbBidNewInterstitialHandler.setInterstitialVideoListener(this);
    String token = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(token)) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_BID_RESPONSE, "Failed to load rewarded ad from MIntegral. Missing or invalid bid response.");
      adLoadCallback.onFailure(error);
      return;
    }
    mbBidNewInterstitialHandler.loadFromBid(token);
  }

  @Override
  public void showAd(@NonNull Context context) {
    boolean muted = MintegralUtils.shouldMuteAudio(adConfiguration.getMediationExtras());
    mbBidNewInterstitialHandler.playVideoMute(muted ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
    mbBidNewInterstitialHandler.showFromBid();
  }

  @Override
  public void onLoadCampaignSuccess(MBridgeIds mBridgeIds) {
    //No-op, AdMob has no corresponding method
  }

  @Override
  public void onResourceLoadSuccess(MBridgeIds mBridgeIds) {
    interstitialAdCallback = adLoadCallback.onSuccess(this);
  }

  @Override
  public void onResourceLoadFail(MBridgeIds mBridgeIds, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorMessage);
    Log.w(TAG, error.toString());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onAdShow(MBridgeIds mBridgeIds) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdOpened();
      interstitialAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdClose(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdClosed();
    }
  }

  @Override
  public void onShowFail(MBridgeIds mBridgeIds, String errorMessage) {
    if (interstitialAdCallback != null) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_MINTEGRAL_SDK, errorMessage);
      Log.w(TAG, error.toString());
      interstitialAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdClicked(MBridgeIds mBridgeIds) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onVideoComplete(MBridgeIds mBridgeIds) {
    //No-op, AdMob has no corresponding method
  }

  @Override
  public void onAdCloseWithNIReward(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
    //No-op, AdMob has no corresponding method
  }

  @Override
  public void onEndcardShow(MBridgeIds mBridgeIds) {
    //No-op, AdMob has no corresponding method
  }
}
