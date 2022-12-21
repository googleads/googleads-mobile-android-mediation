package com.google.ads.mediation.mintegral.mediation;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.content.Context;
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
import com.mbridge.msdk.newinterstitial.out.MBNewInterstitialHandler;
import com.mbridge.msdk.newinterstitial.out.NewInterstitialListener;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.RewardInfo;


public class MintegralInterstitialAd implements MediationInterstitialAd,
    NewInterstitialListener {

  private final MediationInterstitialAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> adLoadCallback;
  private MBNewInterstitialHandler mbNewInterstitialHandler;
  private MediationInterstitialAdCallback interstitialAdCallback;

  public MintegralInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
                                 @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = callback;
  }

  public void loadAd() {
    String adUnitId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.PLACEMENT_ID);
    AdError error = MintegralUtils.validateMintegralAdLoadParams(
        adUnitId, placementId);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }
    mbNewInterstitialHandler = new MBNewInterstitialHandler(adConfiguration.getContext(),
        placementId, adUnitId);
    mbNewInterstitialHandler.setInterstitialVideoListener(this);
    mbNewInterstitialHandler.load();
  }

  @Override
  public void showAd(@NonNull Context context) {
    boolean muted = MintegralUtils.shouldMuteAudio(adConfiguration.getMediationExtras());
    mbNewInterstitialHandler.playVideoMute(muted ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE
        : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
    mbNewInterstitialHandler.show();
  }

  @Override
  public void onLoadCampaignSuccess(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
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
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_MINTEGRAL_SDK,
          errorMessage);
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
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdCloseWithNIReward(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onEndcardShow(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }
}
