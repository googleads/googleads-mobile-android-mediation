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
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper;
import com.google.ads.mediation.inmobi.renderers.InMobiRewardedAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public class InMobiWaterfallRewardedAd extends InMobiRewardedAd {

  public InMobiWaterfallRewardedAd(
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
    final Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

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
        new Listener() {
          @Override
          public void onInitializeSuccess() {
            createAndLoadRewardAd(context, mediationRewardedAdConfiguration);
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
      @NonNull InMobiInterstitialWrapper inMobiRewardedAdWrapper,
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration) {
    InMobiExtras inMobiExtras =
        InMobiExtrasBuilder.build(
            mediationRewardedAdConfiguration.getContext(),
            mediationRewardedAdConfiguration.getMediationExtras(),
            InMobiAdapterUtils.PROTOCOL_WATERFALL);
    inMobiRewardedAdWrapper.setExtras(inMobiExtras.getParameterMap());
    inMobiRewardedAdWrapper.setKeywords(inMobiExtras.getKeywords());

    inMobiRewardedAdWrapper.load();
  }
}
