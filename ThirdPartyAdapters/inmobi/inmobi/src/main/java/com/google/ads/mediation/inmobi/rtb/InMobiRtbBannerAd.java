package com.google.ads.mediation.inmobi.rtb;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiBannerWrapper;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.renderers.InMobiBannerAd;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

public class InMobiRtbBannerAd extends InMobiBannerAd {

  public InMobiRtbBannerAd(
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    super(
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory);
  }

  @Override
  public void loadAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration) {
    final Context context = mediationBannerAdConfiguration.getContext();

    // Try to find the closest banner size that is one of the standard banner sizes supported by
    // InMobi
    AdSize bannerSize =
        InMobiAdapterUtils.findClosestBannerSize(
            context, mediationBannerAdConfiguration.getAdSize());
    // If cannot find a standard banner size that is close to the requested size, just pass the
    // requested size as the banner size and let InMobi SDK handle the non-standard size.
    if (bannerSize == null) {
      bannerSize = mediationBannerAdConfiguration.getAdSize();
    }

    createAndLoadBannerAd(context, bannerSize, mediationBannerAdConfiguration);
  }

  @Override
  public void internalLoadAd(
      @NonNull InMobiBannerWrapper adView,
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(
            mediationBannerAdConfiguration.getContext(),
            mediationBannerAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_RTB);
    adView.setExtras(inMobiExtras.getParameterMap());
    adView.setKeywords(inMobiExtras.getKeywords());

    String bidToken = mediationBannerAdConfiguration.getBidResponse();
    adView.load(bidToken.getBytes());
  }
}
