// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createAdError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.unity.eventadapters.UnityInterstitialEventAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAds.UnityAdsLoadError;
import com.unity3d.ads.UnityAds.UnityAdsShowError;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityAdapter extends UnityMediationAdapter implements MediationInterstitialAdapter {

  /**
   * Mediation interstitial listener used to forward events to Google Mobile Ads SDK.
   */
  private MediationInterstitialListener mediationInterstitialListener;

  /**
   * Placement ID used to determine what type of ad to load.
   */
  private String placementId;

  /**
   * Object ID used to track loaded/shown ads.
   */
  private String objectId;

  /**
   * An Android {@link Activity} weak reference used to show ads.
   */
  private WeakReference<Activity> activityWeakReference;

  /**
   * UnityInterstitialEventAdapter instance to send events from the mediationInterstitialListener.
   */
  private UnityInterstitialEventAdapter eventAdapter;

  /**
   * IUnityAdsLoadListener instance.
   */
  private final IUnityAdsLoadListener unityLoadListener = new IUnityAdsLoadListener() {
    @Override
    public void onUnityAdsAdLoaded(String placementId) {
      String logMessage = String.format(
          "Unity Ads interstitial ad successfully loaded for placement ID: %s", placementId);
      Log.d(TAG, logMessage);
      UnityAdapter.this.placementId = placementId;
      eventAdapter.sendAdEvent(AdEvent.LOADED);
    }

    @Override
    public void onUnityAdsFailedToLoad(String placementId, UnityAdsLoadError error,
        String message) {
      UnityAdapter.this.placementId = placementId;

      AdError loadError = createSDKError(error, message);
      Log.w(TAG, loadError.toString());
      if (mediationInterstitialListener != null) {
        mediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, loadError);
      }
    }
  };

  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener mediationInterstitialListener,
      @NonNull Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {
    this.mediationInterstitialListener = mediationInterstitialListener;
    eventAdapter = new UnityInterstitialEventAdapter(this.mediationInterstitialListener, this);

    final String gameId = serverParameters.getString(KEY_GAME_ID);
    placementId = serverParameters.getString(KEY_PLACEMENT_ID);
    if (!UnityAdsAdapterUtils.areValidIds(gameId, placementId)) {
      sendAdFailedToLoad(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid server parameters.");
      return;
    }

    if (!(context instanceof Activity)) {
      sendAdFailedToLoad(ERROR_CONTEXT_NOT_ACTIVITY,
          "Unity Ads requires an Activity context to load ads.");
      return;
    }
    Activity activity = (Activity) context;
    activityWeakReference = new WeakReference<>(activity);

    UnityInitializer.getInstance()
        .initializeUnityAds(context, gameId, new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            String logMessage = String.format("Unity Ads is initialized for game ID '%s' "
                + "and can now load interstitial ad with placement ID: %s", gameId, placementId);
            Log.d(TAG, logMessage);
          }

          @Override
          public void onInitializationFailed(
              UnityAds.UnityAdsInitializationError unityAdsInitializationError,
              String errorMessage) {
            String adErrorMessage = String.format(
                "Unity Ads initialization failed for game ID '%s' with error message: %s", gameId,
                errorMessage);
            AdError adError = createSDKError(unityAdsInitializationError, adErrorMessage);
            Log.w(TAG, adError.toString());

            if (UnityAdapter.this.mediationInterstitialListener != null) {
              UnityAdapter.this.mediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                  adError);
            }
          }
        });

    UnityAdsAdapterUtils.setCoppa(
        MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment(), context);

    objectId = UUID.randomUUID().toString();
    UnityAdsLoadOptions unityAdsLoadOptions = new UnityAdsLoadOptions();
    unityAdsLoadOptions.setObjectId(objectId);
    UnityAds.load(placementId, unityAdsLoadOptions, unityLoadListener);
  }

  private void sendAdFailedToLoad(int errorCode, String errorDescription) {
    AdError adError = createAdError(errorCode, errorDescription);
    Log.w(TAG, adError.toString());
    if (mediationInterstitialListener != null) {
      mediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, adError);
    }
  }

  /**
   * This method shows a Unity interstitial ad.
   */
  @Override
  public void showInterstitial() {
    Activity activityReference = activityWeakReference == null ? null : activityWeakReference.get();
    if (activityReference == null) {
      Log.w(TAG, "Failed to show interstitial ad for placement ID '" + placementId
          + "' from Unity Ads: Activity context is null.");
      eventAdapter.sendAdEvent(AdEvent.CLOSED);
      return;
    }

    if (placementId == null) {
      Log.w(TAG, "Unity Ads received call to show before successfully loading an ad.");
    }

    UnityAdsShowOptions unityAdsShowOptions = new UnityAdsShowOptions();
    unityAdsShowOptions.setObjectId(objectId);
    // UnityAds can handle a null placement ID so show is always called here.
    UnityAds.show(activityReference, placementId, unityAdsShowOptions, unityShowListener);
  }

  /**
   * IUnityAdsShowListener instance. Contains logic for callbacks when showing ads.
   */
  private final IUnityAdsShowListener unityShowListener = new IUnityAdsShowListener() {
    @Override
    public void onUnityAdsShowStart(String placementId) {
      String logMessage = String.format("Unity Ads interstitial ad started for placement ID: %s",
          UnityAdapter.this.placementId);
      Log.d(TAG, logMessage);

      // Unity Ads does not have an "ad opened" callback.
      // Sending Ad Opened event when the video ad starts playing.
      eventAdapter.sendAdEvent(AdEvent.OPENED);
    }

    @Override
    public void onUnityAdsShowClick(String placementId) {
      String logMessage = String.format(
          "Unity Ads interstitial ad was clicked for placement ID: %s",
          UnityAdapter.this.placementId);
      Log.d(TAG, logMessage);

      // Unity Ads ad clicked.
      eventAdapter.sendAdEvent(AdEvent.CLICKED);

      // Unity Ads doesn't provide a "leaving application" event, so assuming that the
      // user is leaving the application when a click is received, forwarding an on ad
      // left application event.
      eventAdapter.sendAdEvent(AdEvent.LEFT_APPLICATION);
    }

    @Override
    public void onUnityAdsShowComplete(String placementId,
        UnityAds.UnityAdsShowCompletionState state) {
      String logMessage = String.format(
          "Unity Ads interstitial ad finished playing for placement ID: %s",
          UnityAdapter.this.placementId);
      Log.d(TAG, logMessage);

      // Unity Ads ad closed.
      eventAdapter.sendAdEvent(AdEvent.CLOSED);
    }

    @Override
    public void onUnityAdsShowFailure(String placementId, UnityAdsShowError error, String message) {
      // Unity Ads ad failed to show.
      AdError adError = createSDKError(error, message);
      Log.w(TAG, adError.toString());

      eventAdapter.sendAdEvent(AdEvent.OPENED);
      eventAdapter.sendAdEvent(AdEvent.CLOSED);
    }
  };

  @Override
  public void onDestroy() {
    mediationInterstitialListener = null;
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }
}
