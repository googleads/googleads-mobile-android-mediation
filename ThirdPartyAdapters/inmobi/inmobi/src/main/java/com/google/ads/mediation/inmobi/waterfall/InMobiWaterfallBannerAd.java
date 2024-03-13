package com.google.ads.mediation.inmobi.waterfall;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiBannerWrapper;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.renderers.InMobiBannerAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

public class InMobiWaterfallBannerAd extends InMobiBannerAd {

  public InMobiWaterfallBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    super(
        mediationBannerAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory);
  }

  @Override
  public void internalLoadAd(InMobiBannerWrapper adView) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(mediationBannerAdConfiguration.getContext(),
            mediationBannerAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    adView.setExtras(inMobiExtras.getParameterMap());
    adView.setKeywords(inMobiExtras.getKeywords());
    adView.load();
  }
}
