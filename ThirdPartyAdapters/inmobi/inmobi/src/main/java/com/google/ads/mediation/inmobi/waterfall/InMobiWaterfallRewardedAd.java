package com.google.ads.mediation.inmobi.waterfall;

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

public class InMobiWaterfallRewardedAd extends InMobiRewardedAd {

  public InMobiWaterfallRewardedAd(
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
            mediationRewardedAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    inMobiRewardedAd.setExtras(inMobiExtras.getParameterMap());
    inMobiRewardedAd.setKeywords(inMobiExtras.getKeywords());

    inMobiRewardedAd.load();
  }
}
