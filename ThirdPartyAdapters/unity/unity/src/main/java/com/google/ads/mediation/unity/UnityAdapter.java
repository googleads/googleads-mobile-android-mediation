// Copyright 2016 Google Inc.
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
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;
import java.lang.ref.WeakReference;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityAdapter extends UnityMediationAdapter
    implements MediationInterstitialAdapter, MediationBannerAdapter {

  /**
   * Mediation interstitial listener used to forward events from {@link UnitySingleton} to Google
   * Mobile Ads SDK.
   */
  private MediationInterstitialListener mMediationInterstitialListener;

  /** Placement ID used to determine what type of ad to load. */
  private String mPlacementId;

  /** Placement ID for banner if requested. */
  private String bannerPlacementId;

  /** The view for the banner instance. */
  private BannerView mBannerView;

  /** Callback object for Google's Banner Lifecycle. */
  private MediationBannerListener mMediationBannerListener;

  /** An Android {@link Activity} weak reference used to show ads. */
  private WeakReference<Activity> mActivityWeakReference;

  /**
   * Unity adapter delegate to to forward the events from {@link UnitySingleton} to Google Mobile
   * Ads SDK.
   */
  private final UnityAdapterDelegate mUnityAdapterDelegate =
      new UnityAdapterDelegate() {

        @Override
        public String getPlacementId() {
          return mPlacementId;
        }

        @Override
        public void onUnityAdsReady(String placementId) {
          // Unity Ads is ready to show ads for the given placementId. Send Ad Loaded event if the
          // adapter is currently loading ads.
          if (placementId.equals(getPlacementId()) && mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
          }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
          // Unity Ads video ad started playing.
          if (placementId.equals(getPlacementId()) && mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdOpened(UnityAdapter.this);
          }
        }

        @Override
        public void onUnityAdsClick(String s) {
          // Unity Ads ad clicked.
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdClicked(UnityAdapter.this);
            // Unity Ads doesn't provide a "leaving application" event, so assuming that the
            // user is leaving the application when a click is received, forwarding an on ad
            // left application event.
            mMediationInterstitialListener.onAdLeftApplication(UnityAdapter.this);
          }
        }

        @Override
        public void onUnityAdsPlacementStateChanged(
            String placementId,
            UnityAds.PlacementState oldState,
            UnityAds.PlacementState newState) {
          if (!placementId.equals(getPlacementId())) {
            return;
          }

          // If new state returns as NO_FILL or DISABLED, then it is treated as a no fill for Unity.
          if (newState.equals(UnityAds.PlacementState.NO_FILL)) {
            String errorMessage =
                createAdapterError(
                    ERROR_PLACEMENT_STATE_NO_FILL,
                    "Received onUnityAdsPlacementStateChanged() callback "
                        + "with state NO_FILL for placement ID: "
                        + placementId);
            Log.w(TAG, errorMessage);

            if (mMediationInterstitialListener != null) {
              mMediationInterstitialListener.onAdFailedToLoad(
                  UnityAdapter.this, ERROR_PLACEMENT_STATE_NO_FILL);
            }
            UnitySingleton.getInstance().stopTrackingPlacement(placementId);
            return;
          }

          if (newState.equals(UnityAds.PlacementState.DISABLED)) {
            String errorMessage =
                createAdapterError(
                    ERROR_PLACEMENT_STATE_DISABLED,
                    "Received onUnityAdsPlacementStateChanged() callback "
                        + "with state DISABLED for placement ID: "
                        + placementId);
            Log.w(TAG, errorMessage);

            if (mMediationInterstitialListener != null) {
              mMediationInterstitialListener.onAdFailedToLoad(
                  UnityAdapter.this, ERROR_PLACEMENT_STATE_DISABLED);
            }
            UnitySingleton.getInstance().stopTrackingPlacement(placementId);
          }
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
          if (!placementId.equals(getPlacementId())) {
            return;
          }

          if (finishState == UnityAds.FinishState.ERROR) {
            String errorMessage =
                createAdapterError(
                    ERROR_FINISH,
                    "UnityAds SDK called onUnityAdsFinish() with finish state ERROR.");
            Log.w(TAG, errorMessage);
          }

          // Unity Ads ad closed.
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
          }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String errorMessage) {
          String sdkError = createSDKError(unityAdsError, errorMessage);
          Log.w(TAG, "Unity Ads returned an error: " + sdkError);

          if (mMediationInterstitialListener != null) {
            if (unityAdsError.equals(UnityAds.UnityAdsError.NOT_INITIALIZED)
                || unityAdsError.equals(UnityAds.UnityAdsError.INITIALIZE_FAILED)
                || unityAdsError.equals(UnityAds.UnityAdsError.INIT_SANITY_CHECK_FAIL)
                || unityAdsError.equals(UnityAds.UnityAdsError.INVALID_ARGUMENT)
                || unityAdsError.equals(UnityAds.UnityAdsError.AD_BLOCKER_DETECTED)) {
              int errorCode = getMediationErrorCode(unityAdsError);
              mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, errorCode);
            } else {
              mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
            }
          }
        }

        @Override
        public void onAdFailedToLoad(@AdapterError int errorCode, @NonNull String errorMessage) {
          String adapterError = createAdapterError(errorCode, errorMessage);
          Log.w(TAG, "Failed to load ad: " + adapterError);
          if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, errorCode);
          }
        }
      };

  private BannerView.IListener mUnityBannerListener =
      new BannerView.Listener() {
        @Override
        public void onBannerLoaded(BannerView bannerAdView) {
          Log.d(
              TAG,
              "Unity Ads Banner finished loading banner for placement: "
                  + mBannerView.getPlacementId());
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdLoaded(UnityAdapter.this);
          }
        }

        @Override
        public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {
          String sdkError = createSDKError(errorInfo);
          Log.w(TAG, "Unity Ads banner failed to load: " + sdkError);

          if (mMediationBannerListener != null) {
            int errorCode = getMediationErrorCode(errorInfo);
            mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this, errorCode);
          }
        }

        @Override
        public void onBannerClick(BannerView bannerAdView) {
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdClicked(UnityAdapter.this);
          }
        }

        @Override
        public void onBannerLeftApplication(BannerView bannerAdView) {
          if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdLeftApplication(UnityAdapter.this);
          }
        }
      };

  /**
   * Checks whether or not the provided Unity Ads IDs are valid.
   *
   * @param gameId Unity Ads Game ID to be verified.
   * @param placementId Unity Ads Placement ID to be verified.
   * @return {@code true} if all the IDs provided are valid.
   */
  private static boolean isValidIds(String gameId, String placementId) {
    if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
      String ids =
          TextUtils.isEmpty(gameId)
              ? TextUtils.isEmpty(placementId) ? "Game ID and Placement ID" : "Game ID"
              : "Placement ID";
      Log.w(TAG, ids + " cannot be empty.");

      return false;
    }

    return true;
  }

  // region MediationInterstitialAdapter implementation.
  @Override
  public void requestInterstitialAd(
      Context context,
      MediationInterstitialListener mediationInterstitialListener,
      Bundle serverParameters,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    mMediationInterstitialListener = mediationInterstitialListener;

    String gameId = serverParameters.getString(KEY_GAME_ID);
    mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
    if (!isValidIds(gameId, mPlacementId)) {
      String adapterError =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid server parameters.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdFailedToLoad(
            UnityAdapter.this, ERROR_INVALID_SERVER_PARAMETERS);
      }
      return;
    }

    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to load ads.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdFailedToLoad(
            UnityAdapter.this, ERROR_CONTEXT_NOT_ACTIVITY);
      }
      return;
    }

    Activity activity = (Activity) context;
    mActivityWeakReference = new WeakReference<>(activity);

    boolean success = UnitySingleton.getInstance().initializeUnityAds(activity, gameId);
    if (!success) {
      String adapterError =
          createAdapterError(
              ERROR_UNITY_ADS_NOT_SUPPORTED, "The current device is not supported by Unity Ads.");
      Log.w(TAG, adapterError);
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdFailedToLoad(
            UnityAdapter.this, ERROR_UNITY_ADS_NOT_SUPPORTED);
      }
      return;
    }
    MetaData metadata = new MetaData(activity);
    metadata.setCategory("mediation_adapter");
    metadata.set(uuid, "load-interstitial");
    metadata.set(uuid, mPlacementId);
    metadata.commit();
    UnitySingleton.getInstance().loadAd(mUnityAdapterDelegate);
  }

  @Override
  public void showInterstitial() {
    if (mActivityWeakReference == null || mActivityWeakReference.get() == null) {
      String adapterError = createAdapterError(ERROR_NULL_CONTEXT, "Context is null.");
      Log.w(TAG, "Failed to show Unity Ads Interstitial: " + adapterError);
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdOpened(UnityAdapter.this);
        mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
      }
      return;
    }

    // Add isReady check to prevent ready to no fill case
    if (!UnityAds.isReady(mPlacementId)) {
      String adapterError = createAdapterError(ERROR_AD_NOT_READY, "Ad is not ready to be shown.");
      Log.w(TAG, "Failed to show Unity Ads Interstitial: " + adapterError);
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdOpened(UnityAdapter.this);
        mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
      }

      MetaData metadata = new MetaData(mActivityWeakReference.get());
      metadata.setCategory("mediation_adapter");
      metadata.set(uuid, "fail-to-show-interstitial");
      metadata.set(uuid, mPlacementId);
      metadata.commit();
      return;
    }

    MetaData metadata = new MetaData(mActivityWeakReference.get());
    metadata.setCategory("mediation_adapter");
    metadata.set(uuid, "show-interstitial");
    metadata.set(uuid, mPlacementId);
    metadata.commit();
    UnitySingleton.getInstance().showAd(mUnityAdapterDelegate, mActivityWeakReference.get());
  }
  // endregion

  // region MediationAdapter implementation.
  @Override
  public void onDestroy() {
    MetaData metadata = new MetaData(mActivityWeakReference.get());
    metadata.setCategory("mediation_adapter");
    metadata.set(uuid, "destroy");
    metadata.set(uuid, null);
    metadata.commit();

    if (mBannerView != null) {
      mBannerView.destroy();
    }
    mBannerView = null;
    mUnityBannerListener = null;
    mMediationInterstitialListener = null;
    mMediationBannerListener = null;
  }

  @Override
  public void onPause() {}

  @Override
  public void onResume() {}
  // endregion

  // region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(
      Context context,
      MediationBannerListener listener,
      Bundle serverParameters,
      AdSize adSize,
      MediationAdRequest adRequest,
      Bundle mediationExtras) {
    Log.v(TAG, "Requesting Unity Ads Banner.");
    mMediationBannerListener = listener;

    String gameId = serverParameters.getString(KEY_GAME_ID);
    bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
    if (!isValidIds(gameId, bannerPlacementId)) {
      String adapterError =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid server parameters.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationBannerListener != null) {
        mMediationBannerListener.onAdFailedToLoad(
            UnityAdapter.this, ERROR_INVALID_SERVER_PARAMETERS);
      }
      return;
    }

    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to load ads.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationBannerListener != null) {
        mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this, ERROR_CONTEXT_NOT_ACTIVITY);
      }
      return;
    }
    Activity activity = (Activity) context;

    // Even though we are a banner request, we still need to initialize UnityAds.
    boolean success = UnitySingleton.getInstance().initializeUnityAds(activity, gameId);
    if (!success) {
      String adapterError =
          createAdapterError(
              ERROR_UNITY_ADS_NOT_SUPPORTED, "The current device is not supported by Unity Ads.");
      Log.w(TAG, adapterError);
      if (mMediationBannerListener != null) {
        mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this, ERROR_UNITY_ADS_NOT_SUPPORTED);
      }
      return;
    }

    float density = context.getResources().getDisplayMetrics().density;
    int bannerWidth = Math.round(adSize.getWidthInPixels(context) / density);
    int bannerHeight = Math.round(adSize.getHeightInPixels(context) / density);

    UnityBannerSize size = new UnityBannerSize(bannerWidth, bannerHeight);

    if (mBannerView == null) {
      mBannerView = new BannerView((Activity) context, bannerPlacementId, size);
    }

    mBannerView.setListener(mUnityBannerListener);
    mBannerView.load();
  }

  @Override
  public View getBannerView() {
    return mBannerView;
  }
  // endregion
}
