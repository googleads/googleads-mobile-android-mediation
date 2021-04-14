package com.google.ads.mediation.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.content.Context;
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

  private MediationInterstitialAdCallback mInterstitialAdCallback;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mAdLoadCallback;
  private AdColonyInterstitial adColonyInterstitial;
  private final MediationInterstitialAdConfiguration adConfiguration;

  AdColonyInterstitialRenderer(
          @NonNull MediationInterstitialAdConfiguration adConfiguration,
          @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback
  ) {
    this.mAdLoadCallback = callback;
    this.adConfiguration = adConfiguration;
  }

  public void render() {
    AdColonyAdOptions adOptions = AdColonyManager.getInstance().getAdOptionsFromAdConfig(adConfiguration);
    ArrayList<String> listFromServerParams =
            AdColonyManager.getInstance().parseZoneList(adConfiguration.getServerParameters());
    String requestedZone = AdColonyManager
            .getInstance()
            .getZoneFromRequest(listFromServerParams, adConfiguration.getMediationExtras());
    AdColony.requestInterstitial(requestedZone, this, adOptions);
  }

  @Override
  public void showAd(@NonNull Context context) {
    adColonyInterstitial.show();
  }

  @Override
  public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
    AdColonyInterstitialRenderer.this.adColonyInterstitial = adColonyInterstitial;
    mInterstitialAdCallback = mAdLoadCallback.onSuccess(AdColonyInterstitialRenderer.this);
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

    AdColony.requestInterstitial(ad.getZoneID(), this);
  }
}
