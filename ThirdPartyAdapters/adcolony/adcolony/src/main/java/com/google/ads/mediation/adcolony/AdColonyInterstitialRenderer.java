package com.google.ads.mediation.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_PRESENTATION_AD_NOT_LOADED;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_PRESENTATION_AD_SHOW;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createAdapterError;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.jirbo.adcolony.AdColonyManager;

import java.util.ArrayList;

public class AdColonyInterstitialRenderer extends AdColonyInterstitialListener implements
    MediationInterstitialAd {

  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mAdLoadCallback;
  private final MediationInterstitialAdConfiguration adConfiguration;
  private final AdColonyAdOptions adOptions;
  private MediationInterstitialAdCallback mInterstitialAdCallback;
  private AdColonyInterstitial adColonyInterstitial;

  AdColonyInterstitialRenderer(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback
  ) {
    this.mAdLoadCallback = callback;
    this.adConfiguration = adConfiguration;
    this.adOptions = AdColonyManager.getInstance().getAdOptionsFromAdConfig(adConfiguration);
  }

  public void render() {
    ArrayList<String> listFromServerParams =
        AdColonyManager.getInstance().parseZoneList(adConfiguration.getServerParameters());
    String requestedZone = AdColonyManager
        .getInstance()
        .getZoneFromRequest(listFromServerParams, adConfiguration.getMediationExtras());

    // Cannot request an ad without a valid zone.
    if (TextUtils.isEmpty(requestedZone)) {
      AdError error = createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Zone ID.");
      Log.e(TAG, error.getMessage());
      mAdLoadCallback.onFailure(error);
      return;
    }

    AdColony.requestInterstitial(requestedZone, this, adOptions);
  }

  @Override
  public void showAd(@NonNull Context context) {
    AdError error = null;
    if (adColonyInterstitial == null) {
      error = createAdapterError(ERROR_PRESENTATION_AD_NOT_LOADED, "No ad to show.");
    } else if (!adColonyInterstitial.show()) {
      error = createAdapterError(ERROR_PRESENTATION_AD_SHOW, "Ad show failed.");
    }

    if (error != null) {
      Log.w(TAG, error.getMessage());
      mInterstitialAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
    this.adColonyInterstitial = adColonyInterstitial;
    mInterstitialAdCallback = mAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onRequestNotFilled(AdColonyZone zone) {
    AdError error = createSdkError();
    Log.w(TAG, error.getMessage());
    mAdLoadCallback.onFailure(error);
  }

  @Override
  public void onLeftApplication(AdColonyInterstitial ad) {
    super.onLeftApplication(ad);

    mInterstitialAdCallback.reportAdClicked();
    mInterstitialAdCallback.onAdLeftApplication();
  }

  @Override
  public void onOpened(AdColonyInterstitial ad) {
    super.onOpened(ad);

    mInterstitialAdCallback.onAdOpened();
    mInterstitialAdCallback.reportAdImpression();
  }

  @Override
  public void onClosed(AdColonyInterstitial ad) {
    super.onClosed(ad);

    mInterstitialAdCallback.onAdClosed();
  }

  @Override
  public void onExpiring(AdColonyInterstitial ad) {
    super.onExpiring(ad);

    AdColony.requestInterstitial(ad.getZoneID(), this, adOptions);
  }
}
