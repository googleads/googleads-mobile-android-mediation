package com.google.ads.mediation.inmobi.waterfall;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.InMobiNativeWrapper;
import com.google.ads.mediation.inmobi.renderers.InMobiNativeAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;

public class InMobiWaterfallNativeAd extends InMobiNativeAd {

  public InMobiWaterfallNativeAd(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull
          MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
              mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    super(
        mediationNativeAdConfiguration,
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory);
  }

  @Override
  public void internalLoadAd(InMobiNativeWrapper inMobiNativeWrapper) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(mediationNativeAdConfiguration.getContext(),
            mediationNativeAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    inMobiNativeWrapper.setExtras(inMobiExtras.getParameterMap());
    inMobiNativeWrapper.setKeywords(inMobiExtras.getKeywords());

    inMobiNativeWrapper.load();
  }
}
