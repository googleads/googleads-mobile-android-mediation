package com.google.ads.mediation.mintegral.waterfall;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralRewardedAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.MBRewardVideoHandler;

public class MintegralWaterfallRewardedAd extends MintegralRewardedAd {

  private MBRewardVideoHandler mbRewardVideoHandler;

  public MintegralWaterfallRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
                                      @NonNull MediationAdLoadCallback<MediationRewardedAd,
                                              MediationRewardedAdCallback> adLoadCallback) {
    super(adConfiguration, adLoadCallback);
  }

  @Override
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
}
