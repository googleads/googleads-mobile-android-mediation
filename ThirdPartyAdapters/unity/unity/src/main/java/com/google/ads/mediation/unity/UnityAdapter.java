// Copyright 2020 Google Inc.
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

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createAdError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAds.UnityAdsLoadError;
import com.unity3d.ads.UnityAds.UnityAdsShowError;
import java.lang.ref.WeakReference;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityAdapter extends UnityMediationAdapter implements MediationInterstitialAdapter,
    MediationBannerAdapter {

  /**
   * Mediation interstitial listener used to forward events to Google Mobile Ads SDK.
   */
  private MediationInterstitialListener mMediationInterstitialListener;

  /**
   * Placement ID used to determine what type of ad to load.
   */
  private String mPlacementId;

  /**
   * An Android {@link Activity} weak reference used to show ads.
   */
  private WeakReference<Activity> mActivityWeakReference;

  /**
   * UnityBannerAd instance.
   */
  private UnityBannerAd bannerAd;

  /**
   * IUnityAdsLoadListener instance.
   */
  private final IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
    @Override
    public void onUnityAdsAdLoaded(String placementId) {
      Log.d(TAG, "Unity Ads interstitial ad successfully loaded for placement ID '"
          + placementId + "'.");
      mPlacementId = placementId;
      sendInterstitialAdEvent(AdEvent.LOADED);
    }

    @Override
    public void onUnityAdsFailedToLoad(String placementId, UnityAdsLoadError error,
        String message) {
      mPlacementId = placementId;
      AdError adError = createSDKError(error, message);
      Log.w(TAG, adError.toString());
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener
            .onAdFailedToLoad(UnityAdapter.this, adError);
      }
    }
  };

  /**
   * Checks whether or not the provided Unity Ads IDs are valid.
   *
   * @param gameId      Unity Ads Game ID to be verified.
   * @param placementId Unity Ads Placement ID to be verified.
   * @return {@code true} if all the IDs provided are valid.
   */
  public static boolean areValidIds(String gameId, String placementId) {
    return !TextUtils.isEmpty(gameId) && !TextUtils.isEmpty(placementId);
  }

  @Override
  public void requestInterstitialAd(Context context,
      MediationInterstitialListener mediationInterstitialListener,
      Bundle serverParameters,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    mMediationInterstitialListener = mediationInterstitialListener;

    final String gameId = serverParameters.getString(KEY_GAME_ID);
    mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

    if (!areValidIds(gameId, mPlacementId)) {
      sendAdFailedToLoad(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid server parameters.");
      return;
    }

    if (context == null || !(context instanceof Activity)) {
      sendAdFailedToLoad(ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to load ads.");
      return;
    }

    Activity activity = (Activity) context;
    mActivityWeakReference = new WeakReference<>(activity);

    UnityInitializer.getInstance().initializeUnityAds(context, gameId,
        new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            Log.d(TAG, "Unity Ads is initialized, " +
                "can now load interstitial ad for placement ID '" + mPlacementId +
                "' in game '" + gameId + "'.");
          }

          @Override
          public void onInitializationFailed(UnityAds.UnityAdsInitializationError
              unityAdsInitializationError, String errorMessage) {
            AdError adError = createSDKError(unityAdsInitializationError,
                "Unity Ads initialization failed: [" +
                    unityAdsInitializationError + "] " + errorMessage +
                    ", cannot load interstitial ad for placement ID '" + mPlacementId
                    + "' in game '" + gameId + "'");
            Log.e(TAG, adError.toString());
            if (mMediationInterstitialListener != null) {
              mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, adError);
            }
          }
        });

    UnityAds.load(mPlacementId, mUnityLoadListener);
  }

  private void sendAdFailedToLoad(int errorCode, String errorDescription) {
    Log.e(TAG, "Failed to load interstitial ad: " + errorDescription);
    if (mMediationInterstitialListener != null) {
      AdError adError = createAdError(errorCode, errorDescription);
      mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, adError);
    }
  }

  /**
   * This method will show a Unity Ad.
   */
  @Override
  public void showInterstitial() {
    // Unity Ads does not have an ad opened callback. Sending Ad Opened event before showing the
    // ad.
    sendInterstitialAdEvent(AdEvent.OPEN);

    Activity activityReference =
        mActivityWeakReference == null ? null : mActivityWeakReference.get();
    if (activityReference == null) {
      Log.w(TAG, "Failed to show interstitial ad for placement ID '" + mPlacementId +
          "' from Unity Ads: Activity context is null.");
      sendInterstitialAdEvent(AdEvent.CLOSE);
      return;
    }

    if (mPlacementId == null) {
      Log.w(TAG,
          "Unity Ads received call to show before successfully loading an ad");
    }

    // UnityAds can handle a null placement ID so show is always called here.
    UnityAds.show(activityReference, mPlacementId, mUnityShowListener);
  }

  /**
   * IUnityAdsShowListener instance. Contains logic for callbacks when showing ads.
   */
  private final IUnityAdsShowListener mUnityShowListener = new IUnityAdsShowListener() {
    @Override
    public void onUnityAdsShowStart(String placementId) {
      Log.d(TAG, "Unity interstitial ad for placement ID '" + mPlacementId + "' started.");
      // Unity Ads video ad started playing. Google Mobile Ads SDK does not support
      // callbacks for Interstitial ads when they start playing.
    }

    @Override
    public void onUnityAdsShowClick(String placementId) {
      Log.d(TAG, "Unity interstitial ad for placement ID '" + mPlacementId + "' was clicked.");
      // Unity Ads ad clicked.
      sendInterstitialAdEvent(AdEvent.CLICK);
      // Unity Ads doesn't provide a "leaving application" event, so assuming that the
      // user is leaving the application when a click is received, forwarding an on ad
      // left application event.
      sendInterstitialAdEvent(AdEvent.LEFT_APPLICATION);
    }

    @Override
    public void onUnityAdsShowComplete(String placementId,
        UnityAds.UnityAdsShowCompletionState state) {
      // Unity Ads ad closed.
      Log.v(TAG, "Unity interstitial ad for placement ID '" + mPlacementId +
          "' finished playing.");
      sendInterstitialAdEvent(AdEvent.CLOSE);
    }

    @Override
    public void onUnityAdsShowFailure(String placementId, UnityAdsShowError error, String message) {
      // Unity Ads ad failed to show.
      AdError adError = createSDKError(error, message);
      Log.w(TAG, adError.toString());
      sendInterstitialAdEvent(AdEvent.CLOSE);
    }
  };

  @Override
  public void requestBannerAd(
      Context context,
      MediationBannerListener listener,
      Bundle serverParameters,
      AdSize adSize,
      MediationAdRequest adRequest,
      Bundle mediationExtras) {
    bannerAd = new UnityBannerAd();
    bannerAd
        .requestBannerAd(context, listener, serverParameters, adSize, adRequest, mediationExtras);
  }

  @Override
  public void onDestroy() {
    mMediationInterstitialListener = null;
    if (bannerAd != null) {
      bannerAd.onDestroy();
    }
  }

  @Override
  public void onPause() {
    if (bannerAd != null) {
      bannerAd.onPause();
    }
  }

  @Override
  public void onResume() {
    if (bannerAd != null) {
      bannerAd.onResume();
    }
  }

  @Override
  public View getBannerView() {
    if (bannerAd != null) {
      return bannerAd.getBannerView();
    }
    return null;
  }

  private void sendInterstitialAdEvent(AdEvent adEvent) {
    if (mMediationInterstitialListener == null) {
      return;
    }

    switch (adEvent) {
      case LOADED:
        mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
        break;
      case OPEN:
        mMediationInterstitialListener.onAdOpened(UnityAdapter.this);
        break;
      case CLICK:
        mMediationInterstitialListener.onAdClicked(UnityAdapter.this);
        break;
      case CLOSE:
        mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
        break;
      case LEFT_APPLICATION:
        mMediationInterstitialListener.onAdLeftApplication(UnityAdapter.this);
        break;
      default:
        Log.e(TAG, "Unknown ad event");
        break;
    }
  }
}
