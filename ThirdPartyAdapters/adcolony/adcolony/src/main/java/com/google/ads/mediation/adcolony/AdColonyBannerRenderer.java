package com.google.ads.mediation.adcolony;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.jirbo.adcolony.AdColonyManager;

import java.util.ArrayList;

import static com.google.ads.mediation.adcolony.AdColonyAdapterUtils.convertPixelsToDp;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createAdapterError;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

public class AdColonyBannerRenderer extends AdColonyAdViewListener implements MediationBannerAd {

  private MediationBannerAdCallback mBannerAdCallback;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mAdLoadCallback;
  private AdColonyAdView adColonyAdView;
  private final MediationBannerAdConfiguration adConfiguration;

  public AdColonyBannerRenderer(
          @NonNull MediationBannerAdConfiguration adConfiguration,
          @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback
  ) {
    this.mAdLoadCallback = callback;
    this.adConfiguration = adConfiguration;
  }

  public void render() {
    if (adConfiguration.getAdSize() == null) {
      AdError error = createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to request banner with unsupported size: null");
      Log.e(TAG, error.getMessage());
      mAdLoadCallback.onFailure(error);
      return;
    }

    AdColonyAdOptions adOptions = AdColonyManager
            .getInstance()
            .getAdOptionsFromAdConfig(adConfiguration);
    ArrayList<String> listFromServerParams = AdColonyManager
            .getInstance()
            .parseZoneList(adConfiguration.getServerParameters());
    String requestedZone = AdColonyManager
            .getInstance()
            .getZoneFromRequest(listFromServerParams, adConfiguration.getMediationExtras());
    // Setting the requested size as it is as AdColony view size
    AdColonyAdSize adSize = new AdColonyAdSize(
            convertPixelsToDp(adConfiguration.getAdSize().getWidthInPixels(adConfiguration.getContext())),
            convertPixelsToDp(adConfiguration.getAdSize().getHeightInPixels(adConfiguration.getContext()))
    );
    AdColony.requestAdView(requestedZone, this, adSize, adOptions);
  }

  @Override
  public void onRequestFilled(AdColonyAdView adColonyAdView) {
    this.adColonyAdView = adColonyAdView;
    this.mBannerAdCallback = mAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onRequestNotFilled(AdColonyZone zone) {
    AdError error = createSdkError();
    Log.w(TAG, error.getMessage());
    this.mAdLoadCallback.onFailure(error);
  }

  @Override
  public void onLeftApplication(AdColonyAdView ad) {
    this.mBannerAdCallback.onAdLeftApplication();
  }

  @Override
  public void onOpened(AdColonyAdView ad) {
    this.mBannerAdCallback.onAdOpened();
  }

  @Override
  public void onClosed(AdColonyAdView ad) {
    this.mBannerAdCallback.onAdClosed();
  }

  @Override
  public void onClicked(AdColonyAdView ad) {
    this.mBannerAdCallback.reportAdClicked();
  }

  @NonNull
  @Override
  public View getView() {
    return this.adColonyAdView;
  }
}
