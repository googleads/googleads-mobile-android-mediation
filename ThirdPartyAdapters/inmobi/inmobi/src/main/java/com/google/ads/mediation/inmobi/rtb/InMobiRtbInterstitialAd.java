package com.google.ads.mediation.inmobi.rtb;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.renderers.InMobiInterstitialAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.inmobi.ads.InMobiInterstitial;

public class InMobiRtbInterstitialAd extends InMobiInterstitialAd {

  public InMobiRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationAdLoadCallback) {
    super(mediationInterstitialAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  protected void internalLoadAd(InMobiInterstitial inMobiInterstitial) {
    InMobiExtras inMobiExtras =
        InMobiAdapterUtils.buildInMobiExtras(
            mediationInterstitialAdConfiguration.getMediationExtras(), InMobiAdapterUtils.PROTOCOL_RTB);
    inMobiInterstitial.setExtras(inMobiExtras.getParameterMap());
    inMobiInterstitial.setKeywords(inMobiExtras.getKeywords());

    String bidToken = mediationInterstitialAdConfiguration.getBidResponse();
    inMobiInterstitial.load(bidToken.getBytes());
  }
}
