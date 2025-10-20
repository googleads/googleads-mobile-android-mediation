package com.google.ads.mediation.inmobi.rtb;

import android.content.Context;
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
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    super(
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory);
  }

  @Override
  public void loadAd(@NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration) {
    final Context context = mediationRewardedAdConfiguration.getContext();
    createAndLoadRewardAd(context, mediationRewardedAdConfiguration);
  }

  @Override
  protected void internalLoadAd(
      @NonNull InMobiInterstitialWrapper inMobiRewardedAdWrapper,
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(
            mediationRewardedAdConfiguration.getContext(),
            mediationRewardedAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_RTB);
    inMobiRewardedAdWrapper.setExtras(inMobiExtras.getParameterMap());
    inMobiRewardedAdWrapper.setKeywords(inMobiExtras.getKeywords());

    String bidToken = mediationRewardedAdConfiguration.getBidResponse();
    inMobiRewardedAdWrapper.load(bidToken.getBytes());
  }
}
