package com.google.ads.mediation.inmobi.waterfall;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.renderers.InMobiInterstitialAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.inmobi.ads.InMobiInterstitial;

public class InMobiWaterfallInterstitialAd extends InMobiInterstitialAd {

  public InMobiWaterfallInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
    super(mediationInterstitialAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  protected void internalLoadAd(InMobiInterstitial inMobiInterstitial) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(
            mediationInterstitialAdConfiguration.getMediationExtras(), InMobiAdapterUtils.PROTOCOL_WATERFALL);
    inMobiInterstitial.setExtras(inMobiExtras.getParameterMap());
    inMobiInterstitial.setKeywords(inMobiExtras.getKeywords());

    inMobiInterstitial.load();
  }
}
