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
import com.google.ads.mediation.inmobi.renderers.InMobiRewardedAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public class InMobiRtbRewardedAd extends InMobiRewardedAd {

  public InMobiRtbRewardedAd(
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
  public void loadAd() {
    final Context context = mediationRewardedAdConfiguration.getContext();
    final Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);

    createAndLoadRewardAd(context, placementId, mediationAdLoadCallback);
  }

  @Override
  protected void internalLoadAd(InMobiInterstitialWrapper inMobiRewardedAdWrapper) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(mediationRewardedAdConfiguration.getContext(),
            mediationRewardedAdConfiguration.getMediationExtras(), InMobiAdapterUtils.PROTOCOL_RTB);
    inMobiRewardedAdWrapper.setExtras(inMobiExtras.getParameterMap());
    inMobiRewardedAdWrapper.setKeywords(inMobiExtras.getKeywords());

    String bidToken = mediationRewardedAdConfiguration.getBidResponse();
    inMobiRewardedAdWrapper.load(bidToken.getBytes());
  }
}
