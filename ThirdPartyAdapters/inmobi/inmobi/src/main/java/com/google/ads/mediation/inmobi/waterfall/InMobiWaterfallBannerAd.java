package com.google.ads.mediation.inmobi.waterfall;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiBannerWrapper;
import com.google.ads.mediation.inmobi.InMobiConstants;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.renderers.InMobiBannerAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

public class InMobiWaterfallBannerAd extends InMobiBannerAd {

  public InMobiWaterfallBannerAd(
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
    final AdSize closestBannerSize =
        InMobiAdapterUtils.findClosestBannerSize(
            context, mediationBannerAdConfiguration.getAdSize());
    if (closestBannerSize == null) {
      AdError bannerSizeError =
          InMobiConstants.createAdapterError(
              ERROR_BANNER_SIZE_MISMATCH,
              String.format(
                  "The requested banner size: %s is not supported by InMobi SDK.",
                  mediationBannerAdConfiguration.getAdSize()));
      Log.e(TAG, bannerSizeError.toString());
      mediationAdLoadCallback.onFailure(bannerSizeError);
      return;
    }

    final Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();
    final String accountId = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
    AdError error = InMobiAdapterUtils.validateInMobiAdLoadParams(accountId, placementId);
    if (error != null) {
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    inMobiInitializer.init(
        context,
        accountId,
        new InMobiInitializer.Listener() {
          @Override
          public void onInitializeSuccess() {
            createAndLoadBannerAd(context, closestBannerSize, mediationBannerAdConfiguration);
          }

          @Override
          public void onInitializeError(@NonNull AdError error) {
            Log.w(TAG, error.toString());
            mediationAdLoadCallback.onFailure(error);
          }
        });
  }

  @Override
  public void internalLoadAd(
      @NonNull InMobiBannerWrapper adView,
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(
            mediationBannerAdConfiguration.getContext(),
            mediationBannerAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    adView.setExtras(inMobiExtras.getParameterMap());
    adView.setKeywords(inMobiExtras.getKeywords());
    adView.load();
  }
}
