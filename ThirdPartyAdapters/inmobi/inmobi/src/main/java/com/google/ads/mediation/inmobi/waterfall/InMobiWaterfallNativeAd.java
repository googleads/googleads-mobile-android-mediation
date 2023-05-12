package com.google.ads.mediation.inmobi.waterfall;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.renderers.InMobiNativeAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.inmobi.ads.InMobiNative;

public class InMobiWaterfallNativeAd extends InMobiNativeAd {

  public InMobiWaterfallNativeAd(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull
          MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
              mediationAdLoadCallback) {
    super(mediationNativeAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void internalLoadAd(InMobiNative inMobiNative) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(
            mediationNativeAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    inMobiNative.setExtras(inMobiExtras.getParameterMap());
    inMobiNative.setKeywords(inMobiExtras.getKeywords());

    inMobiNative.load();
  }
}
