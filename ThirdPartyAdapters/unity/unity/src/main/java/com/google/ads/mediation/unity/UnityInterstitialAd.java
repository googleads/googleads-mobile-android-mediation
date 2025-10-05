package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_MISSING_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.KEY_GAME_ID;
import static com.google.ads.mediation.unity.UnityMediationAdapter.KEY_PLACEMENT_ID;
import static com.google.ads.mediation.unity.UnityMediationAdapter.KEY_WATERMARK;

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
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAds.UnityAdsLoadError;
import com.unity3d.ads.UnityAds.UnityAdsShowError;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;
import java.util.UUID;

/**
 * The {@link UnityInterstitialAd} is used to load Unity Interstitial ads and mediate the callbacks
 * between Google Mobile Ads SDK and Unity Ads SDK.
 */
public class UnityInterstitialAd
    implements MediationInterstitialAd, IUnityAdsLoadListener, IUnityAdsShowListener {

  static final String ERROR_MSG_INTERSTITIAL_INITIALIZATION_FAILED =
      "Unity Ads initialization failed for game ID '%s' with error message: %s";

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
  public void onUnityAdsAdLoaded(String placementId) {
    String logMessage =
        String.format(
            "Unity Ads interstitial ad successfully loaded for placement ID: %s", placementId);
    Log.d(UnityMediationAdapter.TAG, logMessage);
    this.placementId = placementId;
    interstitialAdCallback = adLoadCallback.onSuccess(this);
  }

  @Override
  public void onUnityAdsFailedToLoad(String placementId, UnityAdsLoadError error, String message) {
    this.placementId = placementId;

    AdError loadError = createSDKError(error, message);
    Log.w(UnityMediationAdapter.TAG, loadError.toString());
    adLoadCallback.onFailure(loadError);
  }

  @Override
  public void onUnityAdsShowStart(String placementId) {
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
  public void onUnityAdsShowClick(String placementId) {
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
  public void onUnityAdsShowComplete(
      String placementId, UnityAds.UnityAdsShowCompletionState state) {
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
  public void onUnityAdsShowFailure(String placementId, UnityAdsShowError error, String message) {
    // Unity Ads ad failed to show.
    AdError adError = createSDKError(error, message);
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
        new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            String logMessage =
                String.format(
                    "Unity Ads is initialized for game ID '%s' "
                        + "and can now load interstitial ad with placement ID: %s",
                    gameId, placementId);
            Log.d(UnityMediationAdapter.TAG, logMessage);
            // TODO(b/280861464): Add setCoppa test when loading ad
            UnityAdsAdapterUtils.setCoppa(
                MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment(), context);

            objectId = UUID.randomUUID().toString();
            UnityAdsLoadOptions unityAdsLoadOptions =
                unityAdsLoader.createUnityAdsLoadOptionsWithId(objectId);
            if (adMarkup != null) {
              unityAdsLoadOptions.setAdMarkup(adMarkup);
            }
            unityAdsLoader.load(placementId, unityAdsLoadOptions, UnityInterstitialAd.this);
          }

          @Override
          public void onInitializationFailed(
              UnityAds.UnityAdsInitializationError unityAdsInitializationError,
              String errorMessage) {
            String adErrorMessage =
                String.format(ERROR_MSG_INTERSTITIAL_INITIALIZATION_FAILED, gameId, errorMessage);
            AdError adError = createSDKError(unityAdsInitializationError, adErrorMessage);
            Log.w(UnityMediationAdapter.TAG, adError.toString());

            adLoadCallback.onFailure(adError);
          }
        });
  }

  @Override
  public void showAd(Context context) {
    if (placementId == null) {
      Log.w(
          UnityMediationAdapter.TAG,
          "Unity Ads received call to show before successfully loading an ad.");
    }

    UnityAdsShowOptions unityAdsShowOptions =
        unityAdsLoader.createUnityAdsShowOptionsWithId(objectId);
    unityAdsShowOptions.set(KEY_WATERMARK, watermark);
    // UnityAds can handle a null placement ID so show is always called here.
    // Note: Context here is the activity that the publisher passed to GMA SDK's show() method
    // (https://developers.google.com/admob/android/reference/com/google/android/gms/ads/appopen/AppOpenAd#show(android.app.Activity)).
    // So, this is guaranteed to be an activity context.
    unityAdsLoader.show((Activity) context, placementId, unityAdsShowOptions, this);
  }
}
