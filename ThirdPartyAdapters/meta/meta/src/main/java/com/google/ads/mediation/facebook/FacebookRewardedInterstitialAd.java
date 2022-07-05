package com.google.ads.mediation.facebook;

import androidx.annotation.NonNull;
import com.facebook.ads.AdExperienceType;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public class FacebookRewardedInterstitialAd extends FacebookRewardedAd {

  public FacebookRewardedInterstitialAd(MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    super(adConfiguration, callback);
  }

  @NonNull
  @Override
  AdExperienceType getAdExperienceType() {
    return AdExperienceType.AD_EXPERIENCE_TYPE_REWARDED_INTERSTITIAL;
  }
}
