package com.google.ads.mediation.zucks;

import android.content.Context;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public class ZucksRewardedLoader implements MediationRewardedAd {

  /**
   * Configuration of the rewarded ad request.
   */
  MediationRewardedAdConfiguration adConfiguration;

  /**
   * The mediation callback for ad load events.
   */
  MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback;

  /**
   * The mediation callback for rewarded ad events.
   */
  MediationRewardedAdCallback rewardedAdCallback;

  ZucksRewardedLoader(MediationRewardedAdConfiguration mediationRewardedAdConfiguration) {
    adConfiguration = mediationRewardedAdConfiguration;
  }

  void loadAd(MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback) {
    adLoadCallback = mediationAdLoadCallback;

    // TODO: Load rewarded ad and forward the success callback:
    rewardedAdCallback = adLoadCallback.onSuccess(ZucksRewardedLoader.this);
  }

  @Override
  public void showAd(Context context) {
    // TODO: Show the rewarded ad.
  }

}
