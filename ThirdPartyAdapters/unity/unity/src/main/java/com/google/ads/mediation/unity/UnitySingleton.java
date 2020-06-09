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

import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_AD_ALREADY_LOADING;

import android.app.Activity;
import com.google.android.gms.ads.MobileAds;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link UnitySingleton} class is used to load {@link UnityAds}, handle multiple {@link
 * UnityAdapter} instances and mediate their callbacks.
 */
public final class UnitySingleton implements IUnityAdsExtendedListener {

  /**
   * A list of adapter listeners with their respective placement IDs to prevent duplicate requests.
   */
  private HashMap<String, WeakReference<UnityAdapterDelegate>> mPlacementsInUse;

  /**
   * A weak reference to the {@link UnityAdapterDelegate} of the {@link UnityAdapter} that is
   * currently displaying an ad.
   */
  private WeakReference<UnityAdapterDelegate> mAdShowingAdapterDelegate;

  /**
   * The only instance of {@link com.google.ads.mediation.unity.UnitySingleton}.
   */
  private static UnitySingleton unitySingletonInstance;

  /**
   * This method will return a {@link com.google.ads.mediation.unity.UnitySingleton} instance.
   *
   * @return the {@link #unitySingletonInstance}.
   */
  public static UnitySingleton getInstance() {
    if (unitySingletonInstance == null) {
      unitySingletonInstance = new UnitySingleton();
    }
    return unitySingletonInstance;
  }

  private UnitySingleton() {
    mPlacementsInUse = new HashMap<>();
  }

  /**
   * This method will initialize {@link UnityAds}.
   *
   * @param activity The Activity context.
   * @param gameId   Unity Ads Game ID.
   * @return {@code true} if the {@link UnityAds} has initialized successfully, {@code false}
   *     otherwise.
   */
  public boolean initializeUnityAds(Activity activity, String gameId) {
    // Check if the current device is supported by Unity Ads before initializing.
    if (!UnityAds.isSupported()) {
      return false;
    }

    if (UnityAds.isInitialized()) {
      return true;
    }

    // Set mediation meta data before initializing.
    MediationMetaData mediationMetaData = new MediationMetaData(activity);
    mediationMetaData.setName("AdMob");
    mediationMetaData.setVersion(MobileAds.getVersionString());
    mediationMetaData.set("adapter_version", BuildConfig.VERSION_NAME);
    mediationMetaData.commit();

    UnityAds.addListener(UnitySingleton.this);
    UnityAds.initialize(activity, gameId, false, true);

    return true;
  }

  /**
   * This method will load Unity ads for a given Placement ID and send the ad loaded event if the
   * ads have already loaded.
   *
   * @param delegate Used to forward Unity Ads events to the adapter.
   */
  protected void loadAd(UnityAdapterDelegate delegate) {
    if (mPlacementsInUse.containsKey(delegate.getPlacementId())
        && mPlacementsInUse.get(delegate.getPlacementId()).get() != null) {
      delegate.onAdFailedToLoad(
          ERROR_AD_ALREADY_LOADING,
          "An ad is already loading for placement ID: " + delegate.getPlacementId());
      return;
    }

    mPlacementsInUse.put(delegate.getPlacementId(), new WeakReference<>(delegate));
    UnityAds.load(delegate.getPlacementId());
  }

  /**
   * This method will show an ad from Unity Ads.
   *
   * @param delegate Used to forward Unity Ads events to the adapter.
   * @param activity An Android {@link Activity} required to show an ad.
   */
  protected void showAd(UnityAdapterDelegate delegate, Activity activity) {
    mAdShowingAdapterDelegate = new WeakReference<>(delegate);

    // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
    // Unity Ads fails to shown an ad).
    UnityAds.show(activity, delegate.getPlacementId());
  }

  public void stopTrackingPlacement(String placementId) {
    mPlacementsInUse.remove(placementId);
  }
  
  /**
   * {@link IUnityAdsExtendedListener} implementation
   */
  @Override
  public void onUnityAdsReady(String placementId) {
    // Unity Ads is ready to show ads for the given placementId. Send ready callback to the
    // appropriate delegate.
    if (mPlacementsInUse.containsKey(placementId)
        && mPlacementsInUse.get(placementId).get() != null) {
      mPlacementsInUse.get(placementId).get().onUnityAdsReady(placementId);
    }
  }

  @Override
  public void onUnityAdsStart(String placementId) {
    // Unity Ads video ad started. Send video started event to currently showing
    // adapter's delegate.
    if (mAdShowingAdapterDelegate != null) {
      UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
      if (delegate != null) {
        delegate.onUnityAdsStart(placementId);
      }
    }
  }

  @Override
  public void onUnityAdsClick(String placementId) {
    // An Unity Ads ad has been clicked. Send ad clicked event to currently showing
    // adapter's delegate.
    if (mAdShowingAdapterDelegate != null) {
      UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
      if (delegate != null) {
        delegate.onUnityAdsClick(placementId);
      }
    }
  }

  @Override
  public void onUnityAdsPlacementStateChanged(
      String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
    if (mPlacementsInUse.containsKey(placementId)
        && mPlacementsInUse.get(placementId).get() != null) {
      mPlacementsInUse
          .get(placementId)
          .get()
          .onUnityAdsPlacementStateChanged(placementId, oldState, newState);
    }
  }

  @Override
  public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
    // An Unity Ads ad has been closed. Forward the finish event to the currently showing
    // adapter's delegate.
    if (mAdShowingAdapterDelegate != null) {
      UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
      if (delegate != null) {
        delegate.onUnityAdsFinish(placementId, finishState);
        mPlacementsInUse.remove(placementId);
      }
    }
  }

  @Override
  public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
    // An initialization error occurred with Unity Ads. Send error event to all delegates.
    if (unityAdsError.equals(UnityAds.UnityAdsError.NOT_INITIALIZED)
        || unityAdsError.equals(UnityAds.UnityAdsError.INITIALIZE_FAILED)
        || unityAdsError.equals(UnityAds.UnityAdsError.INIT_SANITY_CHECK_FAIL)
        || unityAdsError.equals(UnityAds.UnityAdsError.INVALID_ARGUMENT)
        || unityAdsError.equals(UnityAds.UnityAdsError.AD_BLOCKER_DETECTED)) {
      for (Map.Entry<String, WeakReference<UnityAdapterDelegate>> entry :
          mPlacementsInUse.entrySet()) {
        if (entry.getValue().get() != null) {
          entry.getValue().get().onUnityAdsError(unityAdsError, message);
        }
      }
      mPlacementsInUse.clear();
    } else if (mAdShowingAdapterDelegate != null) {
      // Handle show time error.
      UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
      if (delegate != null) {
        delegate.onUnityAdsError(unityAdsError, message);
      }
    }
  }

}
