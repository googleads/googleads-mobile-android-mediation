package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.WATERMARK;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKInitializationError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKLoadError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKShowError;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_MISSING_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.KEY_GAME_ID;
import static com.google.ads.mediation.unity.UnityMediationAdapter.KEY_PLACEMENT_ID;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.InterstitialAd;
import com.unity3d.ads.InterstitialShowListener;
import com.unity3d.ads.LoadListener;
import com.unity3d.ads.ShowConfiguration;
import com.unity3d.ads.ShowFinishState;
import com.unity3d.ads.UnityAdsError;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The {@link UnityInterstitialAd} is used to load Unity Interstitial ads and mediate the callbacks
 * between Google Mobile Ads SDK and Unity Ads SDK.
 */
public class UnityInterstitialAd
    implements MediationInterstitialAd, LoadListener<InterstitialAd>, InterstitialShowListener {

  static final String ERROR_MSG_INTERSTITIAL_INITIALIZATION_FAILED =
      "Unity Ads initialization failed for game ID '%s' with error message: %s";

  /** The loaded InterstitialAd instance from Unity Ads SDK. */
  @Nullable
  private InterstitialAd loadedInterstitialAd;

  /** Object ID used to track loaded/shown ads. */
  private String objectId;

  /** Callback object for Interstitial Ad Load. */
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      adLoadCallback;

  private final UnityInitializer unityInitializer;

  private final UnityAdsLoader unityAdsLoader;

  /** Callback object for Google's Interstitial Lifecycle. */
  @Nullable private MediationInterstitialAdCallback interstitialAdCallback;

  /** Placement ID used to determine what type of ad to load. */
  private String placementId;

  private final String watermark;

  public UnityInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              adLoadCallback,
      @NonNull UnityInitializer unityInitializer,
      @NonNull UnityAdsLoader unityAdsLoader) {
    watermark = adConfiguration.getWatermark();
    this.adLoadCallback = adLoadCallback;
    this.unityInitializer = unityInitializer;
    this.unityAdsLoader = unityAdsLoader;
  }

  @Override
  public void onAdLoaded(
      @Nullable InterstitialAd ad,
      @Nullable UnityAdsError error) {
    if (error == null) {
      // Success
      String logMessage1 = String.format(
          "Unity Ads interstitial ad successfully loaded for placement ID: %s", placementId);
      Log.d(UnityMediationAdapter.TAG, logMessage1);
      loadedInterstitialAd = ad;
      interstitialAdCallback = adLoadCallback.onSuccess(UnityInterstitialAd.this);
    } else {
      // Failure
      AdError loadError = createSDKLoadError(error, error.getMessage());
      Log.w(UnityMediationAdapter.TAG, loadError.toString());
      adLoadCallback.onFailure(loadError);
    }
  }

  @Override
  public void onStarted(InterstitialAd interstitialAd) {
    String logMessage =
        String.format("Unity Ads interstitial ad started for placement ID: %s", placementId);
    Log.d(UnityMediationAdapter.TAG, logMessage);

    // Unity Ads does not have an "ad opened" callback.
    // Sending Ad Opened event when the video ad starts playing.
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdOpened();
    }
  }

  @Override
  public void onClicked(InterstitialAd interstitialAd) {
    String logMessage =
        String.format("Unity Ads interstitial ad was clicked for placement ID: %s", placementId);
    Log.d(UnityMediationAdapter.TAG, logMessage);

    if (interstitialAdCallback == null) {
      return;
    }

    // Unity Ads ad clicked.
    interstitialAdCallback.reportAdClicked();

    // Unity Ads doesn't provide a "leaving application" event, so assuming that the
    // user is leaving the application when a click is received, forwarding an on ad
    // left application event.
    interstitialAdCallback.onAdLeftApplication();
  }

  @Override
  public void onCompleted(
      InterstitialAd interstitialAd, @NonNull ShowFinishState showFinishState) {
    String logMessage =
        String.format(
            "Unity Ads interstitial ad finished playing for placement ID: %s", placementId);
    Log.d(UnityMediationAdapter.TAG, logMessage);

    if (interstitialAdCallback != null) {
      // Unity Ads ad closed.
      interstitialAdCallback.onAdClosed();
    }
  }

  @Override
  public void onFailed(InterstitialAd interstitialAd, @NonNull UnityAdsError error) {
    // Unity Ads ad failed to show.
    AdError adError = createSDKShowError(error, error.getMessage());
    Log.w(UnityMediationAdapter.TAG, adError.toString());

    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdFailedToShow(adError);
    }
  }

  public void loadAd(MediationInterstitialAdConfiguration adConfiguration) {
    Context context = adConfiguration.getContext();
    Bundle serverParameters = adConfiguration.getServerParameters();

    final String gameId = serverParameters.getString(KEY_GAME_ID);
    placementId = serverParameters.getString(KEY_PLACEMENT_ID);
    if (!UnityAdsAdapterUtils.areValidIds(gameId, placementId)) {
      AdError adError =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, ERROR_MSG_MISSING_PARAMETERS, ADAPTER_ERROR_DOMAIN);
      adLoadCallback.onFailure(adError);
      return;
    }

    final String adMarkup = adConfiguration.getBidResponse();

    unityInitializer.initializeUnityAds(
        context,
        gameId,
        new InitializationListener() {
          @Override
          public void onInitializationComplete(@Nullable UnityAdsError unityAdsError) {
            if (unityAdsError == null) {
              String logMessage =
                  String.format(
                      "Unity Ads is initialized for game ID '%s' "
                          + "and can now load interstitial ad with placement ID: %s",
                      gameId, placementId);
              Log.d(UnityMediationAdapter.TAG, logMessage);
              UnityAdsAdapterUtils.setUnityAdsPrivacy(MobileAds.getRequestConfiguration());

              objectId = UUID.randomUUID().toString();

              // Use new load API
              unityAdsLoader.loadInterstitial(
                      placementId, adMarkup, objectId, UnityInterstitialAd.this);
            } else {
              String adErrorMessage =
                  String.format(ERROR_MSG_INTERSTITIAL_INITIALIZATION_FAILED, gameId,
                      unityAdsError.getMessage());
              AdError adError = createSDKInitializationError(unityAdsError, adErrorMessage);
              Log.w(UnityMediationAdapter.TAG, adError.toString());

              adLoadCallback.onFailure(adError);

            }
          }
        });
  }

  @Override
  public void showAd(Context context) {
    if (loadedInterstitialAd == null) {
      Log.w(
          UnityMediationAdapter.TAG,
          "Unity Ads received call to show before successfully loading an ad.");
      if (interstitialAdCallback != null) {
        AdError adError = new AdError(
            UnityMediationAdapter.ERROR_AD_NOT_READY, "InterstitialAd is not loaded",
            ADAPTER_ERROR_DOMAIN);
        interstitialAdCallback.onAdFailedToShow(adError);
      }
      return;
    }

    ShowConfiguration.Builder builder = new ShowConfiguration.Builder();

    if (watermark != null) {
      Map<String, String> extras = new HashMap<>();
      extras.put(WATERMARK, watermark);
      builder.withExtras(extras);
    }

    ShowConfiguration showConfig = builder.build();
    // Note: Context here is the activity that the publisher passed to GMA SDK's show() method
    // (https://developers.google.com/admob/android/reference/com/google/android/gms/ads/appopen/AppOpenAd#show(android.app.Activity)).
    // So, this is guaranteed to be an activity context.
    loadedInterstitialAd.show((Activity) context, showConfig, this);
  }
}
