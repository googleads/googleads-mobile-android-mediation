package com.google.ads.mediation.facebook.rtb;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.createAdapterError;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.createSdkError;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.ExtraHints;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdExtendedListener;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookRtbInterstitialAd implements MediationInterstitialAd,
    InterstitialAdExtendedListener {

  private MediationInterstitialAdConfiguration adConfiguration;
  private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      callback;
  private InterstitialAd interstitialAd;
  private MediationInterstitialAdCallback mInterstitalAdCallback;
  private AtomicBoolean didInterstitialAdClose = new AtomicBoolean();

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
      String ErrorMessage = createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad, placementID is null or empty.");
      Log.e(TAG, ErrorMessage);
      callback.onFailure(ErrorMessage);
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
  public void showAd(Context context) {
    if (interstitialAd.isAdLoaded()) {
      interstitialAd.show();
    }
  }

  @Override
  public void onInterstitialDisplayed(Ad ad) {
    if (mInterstitalAdCallback != null) {
      mInterstitalAdCallback.onAdOpened();
    }
  }

  @Override
  public void onInterstitialDismissed(Ad ad) {
    if (!didInterstitialAdClose.getAndSet(true) && mInterstitalAdCallback != null) {
      mInterstitalAdCallback.onAdClosed();
    }
  }

  @Override
  public void onError(Ad ad, AdError adError) {
    String errorMessage = createSdkError(adError);
    callback.onFailure(errorMessage);
  }

  @Override
  public void onAdLoaded(Ad ad) {
    mInterstitalAdCallback = callback.onSuccess(this);
  }

  @Override
  public void onAdClicked(Ad ad) {
    if (mInterstitalAdCallback != null) {
      // TODO: Upon approval, add this callback back in.
      // mInterstitalAdCallback.reportAdClicked();
      mInterstitalAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onLoggingImpression(Ad ad) {
    if (mInterstitalAdCallback != null) {
      // TODO: Upon approval, add this callback back in.
      // mInterstitalAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onInterstitialActivityDestroyed() {
    if (!didInterstitialAdClose.getAndSet(true) && mInterstitalAdCallback != null) {
      mInterstitalAdCallback.onAdClosed();
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
