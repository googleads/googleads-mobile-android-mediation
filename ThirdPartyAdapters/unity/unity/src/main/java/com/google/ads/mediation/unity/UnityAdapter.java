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

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createAdapterError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import java.lang.ref.WeakReference;
import java.util.HashMap;

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
   * A list of placement IDs that are currently loaded to prevent duplicate requests.
   */
  private static HashMap<String, WeakReference<UnityAdapter>> mPlacementsInUse = new HashMap<>();

  /**
   * IUnityAdsLoadListener instance.
   */
  private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
    @Override
    public void onUnityAdsAdLoaded(String placementId) {
      Log.d(TAG, "Unity Ads interstitial ad successfully loaded for placement ID '"
          + placementId + "'.");
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
      }
    }

    @Override
    public void onUnityAdsFailedToLoad(String placementId) {
      mPlacementsInUse.remove(mPlacementId);
      String errorMessage = createAdapterError(
          ERROR_PLACEMENT_STATE_NO_FILL,
          "UnityAds failed to load for placement ID: "
              + placementId);
      Log.w(TAG, errorMessage);
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener
            .onAdFailedToLoad(UnityAdapter.this, ERROR_PLACEMENT_STATE_NO_FILL);
      }
    }
  };

  /**
   * IUnityAdsExtendedListener instance for call backs from show method only.
   */
  private IUnityAdsExtendedListener mUnityShowListener = new IUnityAdsExtendedListener() {
    @Override
    public void onUnityAdsClick(String s) {
      Log.d(TAG, "Unity interstitial ad for placement ID '" + mPlacementId + "' was clicked.");
      // Unity Ads ad clicked.
      if (mMediationInterstitialListener == null) {
        return;
      }
      mMediationInterstitialListener.onAdClicked(UnityAdapter.this);
      // Unity Ads doesn't provide a "leaving application" event, so assuming that the
      // user is leaving the application when a click is received, forwarding an on ad
      // left application event.
      mMediationInterstitialListener.onAdLeftApplication(UnityAdapter.this);
    }

    @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
      // This callback is not forwarded to Google Mobile Ads SDK.
    }

    @Override
    public void onUnityAdsReady(String placementId) {
      // Logic to mark a placement ready has moved to the IUnityAdsLoadListener function
      // onUnityAdsAdLoaded.
    }

    @Override
    public void onUnityAdsStart(String placementId) {
      // Unity Ads video ad started playing. Google Mobile Ads SDK does not support
      // callbacks for Interstitial ads when they start playing.
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {

      // Unity Ads ad closed.
      UnityAds.removeListener(mUnityShowListener);

      if (finishState == UnityAds.FinishState.ERROR) {
        Log.v(TAG, "Unity interstitial ad for placement ID '" + mPlacementId +
            "' finished with an error.");
      } else {
        Log.v(TAG, "Unity interstitial ad for placement ID '" + mPlacementId +
            "' finished playing.");
      }

      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
      }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String errorMessage) {
      // Unity Ads ad failed to show.
      String sdkError = createSDKError(unityAdsError, errorMessage);
      Log.w(TAG, "Unity Ads returned an error: " + sdkError);

      UnityAds.removeListener(this);
    }
  };

  /**
   * Checks whether or not the provided Unity Ads IDs are valid.
   *
   * @param gameId      Unity Ads Game ID to be verified.
   * @param placementId Unity Ads Placement ID to be verified.
   * @return {@code true} if all the IDs provided are valid.
   */
  public static boolean isValidIds(String gameId, String placementId) {
    if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
      return false;
    }

    return true;
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

    if (!isValidIds(gameId, mPlacementId)) {
      String adapterError = createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid server parameters.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener
            .onAdFailedToLoad(UnityAdapter.this, ERROR_INVALID_SERVER_PARAMETERS);
      }
      return;
    }

    if (context == null || !(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to load ads.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener
            .onAdFailedToLoad(UnityAdapter.this, ERROR_CONTEXT_NOT_ACTIVITY);
      }
      return;
    }

    Activity activity = (Activity) context;
    mActivityWeakReference = new WeakReference<>(activity);

    UnityInitializer.getInstance().initializeUnityAds(context, gameId,
        new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            Log.d(TAG, "Unity Ads successfully initialized, " +
                "can now load interstitial ad for placement ID '" + mPlacementId +
                "' in game '" + gameId + "'.");
          }

          @Override
          public void onInitializationFailed(UnityAds.UnityAdsInitializationError
              unityAdsInitializationError, String errorMessage) {
            String adapterError = createAdapterError(INITIALIZATION_FAILURE,
                "Unity Ads initialization failed: [" +
                    unityAdsInitializationError + "] " + errorMessage +
                    ", cannot load interstitial ad for placement ID '" + mPlacementId
                    + "' in game '" + gameId + "'");
            Log.e(TAG, adapterError);
            if (mMediationInterstitialListener != null) {
              mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                  INITIALIZATION_FAILURE);
            }
          }
        });

    if (mPlacementsInUse.containsKey(mPlacementId)) {
      WeakReference<UnityAdapter> adapterRef = mPlacementsInUse.get(mPlacementId);
      if (adapterRef != null && adapterRef.get() != null) {
        if (mMediationInterstitialListener != null) {
          mMediationInterstitialListener
                  .onAdFailedToLoad(UnityAdapter.this, ERROR_AD_ALREADY_LOADING);
        }
        return;
      }
    }
    mPlacementsInUse.put(mPlacementId, new WeakReference<UnityAdapter>(UnityAdapter.this));
    UnityAds.load(mPlacementId, mUnityLoadListener);

  }

  /**
   * This method will show a Unity Ad.
   */
  @Override
  public void showInterstitial() {

    // Unity Ads does not have an ad opened callback. Sending Ad Opened event before showing the
    // ad.
    mMediationInterstitialListener.onAdOpened(UnityAdapter.this);
    mPlacementsInUse.remove(mPlacementId);

    Activity activityReference =
        mActivityWeakReference == null ? null : mActivityWeakReference.get();
    if (activityReference == null) {
      Log.w(TAG, "Failed to show interstitial ad for placement ID '" + mPlacementId +
          "' from Unity Ads: Activity context is null.");
      mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
      return;
    }

    if (!UnityAds.isReady(mPlacementId)) {
      Log.w(TAG,
          "Unity Ads failed to show interstitial ad for placement ID '" + mPlacementId +
              "'. Placement is not ready.");
      mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
      return;
    }

    // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
    // Unity Ads fails to show an ad).
    UnityAds.addListener(mUnityShowListener);
    UnityAds.show(activityReference, mPlacementId);
  }

  // region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(
      Context context,
      MediationBannerListener listener,
      Bundle serverParameters,
      AdSize adSize,
      MediationAdRequest adRequest,
      Bundle mediationExtras) {
    bannerAd = new UnityBannerAd();
    bannerAd.requestBannerAd(context, listener, serverParameters, adSize, adRequest, mediationExtras);
  }

  @Override
  public void onDestroy() {
    mMediationInterstitialListener = null;
    if (bannerAd == null) {
      return;
    }
    bannerAd.onDestroy();
  }

  @Override
  public void onPause() {
    if (bannerAd == null) {
      return;
    }
    bannerAd.onPause();
  }

  @Override
  public void onResume() {
    if (bannerAd == null) {
      return;
    }
    bannerAd.onResume();
  }

  @Override
  public View getBannerView() {
    if (bannerAd == null) {
      return null;
    }
    return bannerAd.getBannerView();
  }
  // endregion
}
