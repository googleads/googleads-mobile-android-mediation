package com.google.ads.mediation.inmobi.waterfall;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper;
import com.google.ads.mediation.inmobi.renderers.InMobiRewardedAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public class InMobiWaterfallRewardedAd extends InMobiRewardedAd {

  public InMobiWaterfallRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    super(
        mediationRewardedAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory);
  }

  @Override
  protected void internalLoadAd(InMobiInterstitialWrapper inMobiRewardedAdWrapper) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(mediationRewardedAdConfiguration.getContext(),
            mediationRewardedAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    inMobiRewardedAdWrapper.setExtras(inMobiExtras.getParameterMap());
    inMobiRewardedAdWrapper.setKeywords(inMobiExtras.getKeywords());

    inMobiRewardedAdWrapper.load();
  }
}
