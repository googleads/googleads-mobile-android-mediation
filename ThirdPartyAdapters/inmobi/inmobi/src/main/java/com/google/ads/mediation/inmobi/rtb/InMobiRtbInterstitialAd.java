package com.google.ads.mediation.inmobi.rtb;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper;
import com.google.ads.mediation.inmobi.renderers.InMobiInterstitialAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class InMobiRtbInterstitialAd extends InMobiInterstitialAd {

  public InMobiRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    super(
        mediationInterstitialAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory);
  }

  @Override
  public void loadAd() {
    final Context context = mediationInterstitialAdConfiguration.getContext();
    final Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();

    final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);

    createAndLoadInterstitialAd(context, placementId);
  }

  @Override
  protected void internalLoadAd(InMobiInterstitialWrapper inMobiInterstitialWrapper) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(mediationInterstitialAdConfiguration.getContext(),
            mediationInterstitialAdConfiguration.getMediationExtras(), InMobiAdapterUtils.PROTOCOL_RTB);
    inMobiInterstitialWrapper.setExtras(inMobiExtras.getParameterMap());
    inMobiInterstitialWrapper.setKeywords(inMobiExtras.getKeywords());

    String bidToken = mediationInterstitialAdConfiguration.getBidResponse();
    inMobiInterstitialWrapper.load(bidToken.getBytes());
  }
}
