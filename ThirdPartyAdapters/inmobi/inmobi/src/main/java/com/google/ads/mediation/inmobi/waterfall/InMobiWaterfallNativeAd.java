package com.google.ads.mediation.inmobi.waterfall;

import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiExtras;
import com.google.ads.mediation.inmobi.InMobiExtrasBuilder;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.InMobiNativeWrapper;
import com.google.ads.mediation.inmobi.renderers.InMobiNativeAd;
import com.google.android.gms.ads.AdError;
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
  public void loadAd() {
    final Context context = mediationNativeAdConfiguration.getContext();
    Bundle serverParameters = mediationNativeAdConfiguration.getServerParameters();

    String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
    AdError error = InMobiAdapterUtils.validateInMobiAdLoadParams(accountID, placementId);
    if (error != null) {
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    inMobiInitializer.init(
        context,
        accountID,
        new InMobiInitializer.Listener() {
          @Override
          public void onInitializeSuccess() {
            createAndLoadNativeAd(context, placementId);
          }

          @Override
          public void onInitializeError(@NonNull AdError error) {
            Log.w(TAG, error.toString());
            if (mediationAdLoadCallback != null) {
              mediationAdLoadCallback.onFailure(error);
            }
          }
        });
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
