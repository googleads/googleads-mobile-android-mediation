package com.google.ads.mediation.facebook.rtb;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FAILED_TO_PRESENT_AD;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.getAdError;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.facebook.ads.Ad;
import com.facebook.ads.ExtraHints;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdExtendedListener;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookRtbInterstitialAd implements MediationInterstitialAd,
    InterstitialAdExtendedListener {

  private final MediationInterstitialAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      callback;
  private InterstitialAd interstitialAd;
  private MediationInterstitialAdCallback interstitalAdCallback;
  private final AtomicBoolean showAdCalled = new AtomicBoolean();
  private final AtomicBoolean didInterstitialAdClose = new AtomicBoolean();

  public FacebookRtbInterstitialAd(MediationInterstitialAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    this.adConfiguration = adConfiguration;
    this.callback = callback;
  }

  public void render() {
    Bundle serverParameters = adConfiguration.getServerParameters();
    String placementID = FacebookMediationAdapter.getPlacementID(serverParameters);
    if (TextUtils.isEmpty(placementID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty. ", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    setMixedAudience(adConfiguration);
    interstitialAd = new InterstitialAd(adConfiguration.getContext(), placementID);
    if (!TextUtils.isEmpty(adConfiguration.getWatermark())) {
      interstitialAd.setExtraHints(new ExtraHints.Builder()
          .mediationData(adConfiguration.getWatermark()).build());
    }

    interstitialAd.loadAd(
        interstitialAd.buildLoadAdConfig()
            .withBid(adConfiguration.getBidResponse())
            .withAdListener(this)
            .build());
  }

  @Override
  public void showAd(@NonNull Context context) {
    showAdCalled.set(true);
    if (!interstitialAd.show()) {
      AdError showError = new AdError(ERROR_FAILED_TO_PRESENT_AD,
          "Failed to present interstitial ad.", ERROR_DOMAIN);
      Log.w(TAG, showError.toString());

      if (interstitalAdCallback != null) {
        interstitalAdCallback.onAdFailedToShow(showError);
      }
    }
  }

  @Override
  public void onInterstitialDisplayed(Ad ad) {
    if (interstitalAdCallback != null) {
      interstitalAdCallback.onAdOpened();
    }
  }

  @Override
  public void onInterstitialDismissed(Ad ad) {
    if (!didInterstitialAdClose.getAndSet(true) && interstitalAdCallback != null) {
      interstitalAdCallback.onAdClosed();
    }
  }

  @Override
  public void onError(Ad ad, com.facebook.ads.AdError adError) {
    AdError error = getAdError(adError);
    Log.w(TAG, error.getMessage());
    if (showAdCalled.get()) {
      if (interstitalAdCallback != null) {
        interstitalAdCallback.onAdOpened();
        interstitalAdCallback.onAdClosed();
      }
      return;
    }
    callback.onFailure(error);
  }

  @Override
  public void onAdLoaded(Ad ad) {
    interstitalAdCallback = callback.onSuccess(this);
  }

  @Override
  public void onAdClicked(Ad ad) {
    if (interstitalAdCallback != null) {
      interstitalAdCallback.reportAdClicked();
      interstitalAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onLoggingImpression(Ad ad) {
    if (interstitalAdCallback != null) {
      interstitalAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onInterstitialActivityDestroyed() {
    if (!didInterstitialAdClose.getAndSet(true) && interstitalAdCallback != null) {
      interstitalAdCallback.onAdClosed();
    }
  }

  @Override
  public void onRewardedAdCompleted() {
    //no-op
  }

  @Override
  public void onRewardedAdServerSucceeded() {
    //no-op
  }

  @Override
  public void onRewardedAdServerFailed() {
    //no-op
  }
}
