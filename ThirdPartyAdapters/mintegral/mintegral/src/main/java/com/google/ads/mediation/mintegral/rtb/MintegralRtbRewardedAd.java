package com.google.ads.mediation.mintegral.rtb;

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
import com.mbridge.msdk.out.MBBidRewardVideoHandler;

public class MintegralRtbRewardedAd extends MintegralRewardedAd {

  private MBBidRewardVideoHandler mbBidRewardVideoHandler;

  public MintegralRtbRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
    super(adConfiguration, adLoadCallback);
  }

  @Override
  public void loadAd() {
    String adUnitId = adConfiguration.getServerParameters()
            .getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters()
            .getString(MintegralConstants.PLACEMENT_ID);
    String bidToken = adConfiguration.getBidResponse();
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId, bidToken);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }
    mbBidRewardVideoHandler = new MBBidRewardVideoHandler(adConfiguration.getContext(), placementId,
            adUnitId);
    mbBidRewardVideoHandler.setRewardVideoListener(this);
    mbBidRewardVideoHandler.loadFromBid(bidToken);
  }

  @Override
  public void showAd(@NonNull Context context) {
    boolean muted = MintegralUtils.shouldMuteAudio(adConfiguration.getMediationExtras());
    mbBidRewardVideoHandler.playVideoMute(muted ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE
            : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
    mbBidRewardVideoHandler.showFromBid();
  }
}
