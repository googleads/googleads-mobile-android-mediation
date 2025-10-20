package com.google.ads.mediation.maio;

import static com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.maio.MaioMediationAdapter.TAG;
import static com.google.ads.mediation.maio.MaioMediationAdapter.getAdError;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager;
import jp.maio.sdk.android.v2.interstitial.IInterstitialLoadCallback;
import jp.maio.sdk.android.v2.interstitial.IInterstitialShowCallback;
import jp.maio.sdk.android.v2.interstitial.Interstitial;
import jp.maio.sdk.android.v2.request.MaioRequest;

/**
 * The {@link MaioInterstitialAd} is used to load maio ads and mediate the callbacks between Google
 * Mobile Ads SDK and the maio SDK.
 *
 * <p><b>Note:</b> This class is not thread-safe.
 */
public class MaioInterstitialAd implements MediationInterstitialAd, IInterstitialShowCallback {

  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      adLoadCallback;
  private MediationInterstitialAdCallback interstitialAdCallback;
  private Interstitial maioInterstitial;

  public MaioInterstitialAd(
      MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    adLoadCallback = callback;
  }

  public void loadAd(MediationInterstitialAdConfiguration mediationAdConfiguration) {
    Context context = mediationAdConfiguration.getContext();
    Bundle serverParameters = mediationAdConfiguration.getServerParameters();

    String mediaID = serverParameters.getString(MaioAdsManager.KEY_MEDIA_ID);
    if (TextUtils.isEmpty(mediaID)) {
      AdError error =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    String zoneID = serverParameters.getString(MaioAdsManager.KEY_ZONE_ID);
    if (TextUtils.isEmpty(zoneID)) {
      AdError error =
          new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    maioInterstitial =
        Interstitial.loadAd(
            new MaioRequest(zoneID, mediationAdConfiguration.isTestRequest(), /* bidData= */ ""),
            context,
            new IInterstitialLoadCallback() {
              @Override
              public void loaded(@NonNull Interstitial interstitial) {
                interstitialAdCallback = adLoadCallback.onSuccess(MaioInterstitialAd.this);
              }

              @Override
              public void failed(@NonNull Interstitial interstitial, int errorCode) {
                AdError error = getAdError(errorCode);
                Log.w(TAG, error.getMessage());
                adLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    maioInterstitial.show(context, MaioInterstitialAd.this);
  }

  @Override
  public void opened(@NonNull Interstitial interstitial) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdOpened();
    }
  }

  @Override
  public void closed(@NonNull Interstitial interstitial) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdClosed();
    }
  }

  @Override
  public void clicked(@NonNull Interstitial interstitial) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.reportAdClicked();
      interstitialAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void failed(
      @NonNull jp.maio.sdk.android.v2.interstitial.Interstitial interstitial, int errorCode) {
    AdError error = getAdError(errorCode);
    Log.w(TAG, error.getMessage());
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdOpened();
      interstitialAdCallback.onAdClosed();
    }
  }
}
