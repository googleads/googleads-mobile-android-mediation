package com.google.ads.mediation.inmobi.rtb;

import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.renderers.InMobiNativeAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.inmobi.ads.InMobiNative;

public class InMobiRtbNativeAd extends InMobiNativeAd {

  public InMobiRtbNativeAd(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull
          MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
              mediationAdLoadCallback) {
    super(mediationNativeAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void internalLoadAd(InMobiNative inMobiNative) {
    InMobiExtras inMobiExtras =
        InMobiAdapterUtils.buildInMobiExtras(
            mediationNativeAdConfiguration.getMediationExtras(), InMobiAdapterUtils.PROTOCOL_RTB);
    inMobiNative.setExtras(inMobiExtras.getParameterMap());
    inMobiNative.setKeywords(inMobiExtras.getKeywords());

    String bidToken = mediationNativeAdConfiguration.getBidResponse();
    inMobiNative.load(bidToken.getBytes());
  }
}
