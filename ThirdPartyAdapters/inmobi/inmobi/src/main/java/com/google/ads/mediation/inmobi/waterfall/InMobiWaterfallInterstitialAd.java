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
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper;
import com.google.ads.mediation.inmobi.renderers.InMobiInterstitialAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class InMobiWaterfallInterstitialAd extends InMobiInterstitialAd {

  public InMobiWaterfallInterstitialAd(
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    super(
        mediationAdLoadCallback,
        inMobiInitializer,
        inMobiAdFactory);
  }

  @Override
  public void loadAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration) {
    final Context context = mediationInterstitialAdConfiguration.getContext();
    final Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();

    final String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
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
            createAndLoadInterstitialAd(context, mediationInterstitialAdConfiguration);
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
  protected void internalLoadAd(
      @NonNull InMobiInterstitialWrapper inMobiInterstitialWrapper,
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(
            mediationInterstitialAdConfiguration.getContext(),
            mediationInterstitialAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    inMobiInterstitialWrapper.setExtras(inMobiExtras.getParameterMap());
    inMobiInterstitialWrapper.setKeywords(inMobiExtras.getKeywords());

    inMobiInterstitialWrapper.load();
  }
}
