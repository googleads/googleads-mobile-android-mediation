package com.mintegral.mediation.rtb;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralMediationAdapter;
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
import com.mintegral.mediation.MintegralUtils;


public class MintegralRtbInterstitialAd implements MediationInterstitialAd, NewInterstitialListener {
  private static final String TAG = MintegralMediationAdapter.class.getSimpleName();
  /**
   * Data used to render an RTB interstitial ad.
   */
  private final MediationInterstitialAdConfiguration adConfiguration;

  /**
   * Callback object to notify the Google Mobile Ads SDK if ad rendering succeeded or failed.
   */
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> adLoadCallback;

  private MBBidNewInterstitialHandler mbBidNewInterstitialHandler;
  private MediationInterstitialAdCallback interstitialAdCallback;

  public MintegralRtbInterstitialAd(MediationInterstitialAdConfiguration adConfiguration,
                                    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = callback;
  }


  public void loadAd() {
    String unitId = adConfiguration.getServerParameters().getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters().getString(MintegralConstants.PLACEMENT_ID);
    mbBidNewInterstitialHandler = new MBBidNewInterstitialHandler(adConfiguration.getContext(), placementId, unitId);
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
  public void onResourceLoadFail(MBridgeIds mBridgeIds, String s) {
    AdError error = MintegralConstants.createSdkError(MintegralConstants.ERROR_SDK_INTER_ERROR, s);
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
  public void onShowFail(MBridgeIds mBridgeIds, String s) {
    if (interstitialAdCallback != null) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_SDK_INTER_ERROR, s);
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
