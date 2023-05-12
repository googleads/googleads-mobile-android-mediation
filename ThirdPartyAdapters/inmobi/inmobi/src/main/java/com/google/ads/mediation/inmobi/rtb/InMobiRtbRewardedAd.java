package com.google.ads.mediation.inmobi.rtb;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.renderers.InMobiRewardedAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.inmobi.ads.InMobiInterstitial;

public class InMobiRtbRewardedAd extends InMobiRewardedAd {

  public InMobiRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              mediationAdLoadCallback) {
    super(mediationRewardedAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  protected void internalLoadAd(InMobiInterstitial inMobiRewardedAd) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(
            mediationRewardedAdConfiguration.getMediationExtras(), InMobiAdapterUtils.PROTOCOL_RTB);
    inMobiRewardedAd.setExtras(inMobiExtras.getParameterMap());
    inMobiRewardedAd.setKeywords(inMobiExtras.getKeywords());

    String bidToken = mediationRewardedAdConfiguration.getBidResponse();
    inMobiRewardedAd.load(bidToken.getBytes());
  }
}
