package com.google.ads.mediation.mintegral.waterfall;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralInterstitialAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.newinterstitial.out.MBNewInterstitialHandler;


public class MintegralWaterfallInterstitialAd extends MintegralInterstitialAd {

  private MBNewInterstitialHandler mbNewInterstitialHandler;

  public MintegralWaterfallInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    super(adConfiguration, callback);
  }

  @Override
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
}
