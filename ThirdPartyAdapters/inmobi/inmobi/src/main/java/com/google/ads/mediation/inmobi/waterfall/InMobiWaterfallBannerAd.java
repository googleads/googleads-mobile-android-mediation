package com.google.ads.mediation.inmobi.waterfall;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.renderers.InMobiBannerAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.inmobi.ads.InMobiBanner;

public class InMobiWaterfallBannerAd extends InMobiBannerAd {

  public InMobiWaterfallBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback) {
    super(mediationBannerAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void internalLoadAd(InMobiBanner adView) {
    InMobiExtras inMobiExtras =
        InMobiAdapterUtils.buildInMobiExtras(
            mediationBannerAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    adView.setExtras(inMobiExtras.getParameterMap());
    adView.setKeywords(inMobiExtras.getKeywords());
    adView.load();
  }
}
