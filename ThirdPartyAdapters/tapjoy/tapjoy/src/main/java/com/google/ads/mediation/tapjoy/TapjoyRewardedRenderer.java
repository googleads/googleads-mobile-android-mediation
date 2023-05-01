// Copyright 2019 Google LLC
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

package com.google.ads.mediation.tapjoy;

import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.ERROR_AD_ALREADY_REQUESTED;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.ERROR_NO_CONTENT_AVAILABLE;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.ERROR_PRESENTATION_VIDEO_PLAYBACK;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.ERROR_TAPJOY_INITIALIZATION;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.MEDIATION_AGENT;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.PLACEMENT_NAME_SERVER_PARAMETER_KEY;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.SDK_KEY_SERVER_PARAMETER_KEY;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.TAG;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.TAPJOY_INTERNAL_ADAPTER_VERSION;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.TAPJOY_SDK_ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyAuctionFlags;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Hashtable;
import org.json.JSONException;
import org.json.JSONObject;

public class TapjoyRewardedRenderer implements MediationRewardedAd, TJPlacementVideoListener {

  private static final String TAPJOY_DEBUG_FLAG_KEY = "enable_debug";

  private TJPlacement videoPlacement;

  private static boolean isRtbAd = false;

  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;
  private MediationRewardedAdCallback mediationRewardedAdCallback;
  private final MediationRewardedAdConfiguration adConfiguration;

  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private static final HashMap<String, WeakReference<TapjoyRewardedRenderer>> placementsInUse =
      new HashMap<>();

  public TapjoyRewardedRenderer(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          rewardedAdCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = rewardedAdCallback;
  }

  public void render() {
    if (!adConfiguration.getBidResponse().equals("")) {
      isRtbAd = true;
    }

    Context context = adConfiguration.getContext();
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Tapjoy SDK requires an Activity context to request ads.", ERROR_DOMAIN);
      if (error != null) {
        Log.e(TAG, error.getMessage());
        adLoadCallback.onFailure(error);
      }
      return;
    }
    Activity activity = (Activity) context;

    final Bundle serverParameters = adConfiguration.getServerParameters();
    String sdkKey = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);
    if (TextUtils.isEmpty(sdkKey)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid SDK key.",
          ERROR_DOMAIN);
      if (error != null) {
        Log.e(TAG, error.getMessage());
        adLoadCallback.onFailure(error);
      }
      return;
    }

    Bundle networkExtras = adConfiguration.getMediationExtras();
    Hashtable<String, Object> connectFlags = new Hashtable<>();
    if (networkExtras.containsKey(TAPJOY_DEBUG_FLAG_KEY)) {
      connectFlags.put("TJC_OPTION_ENABLE_LOGGING",
          networkExtras.getBoolean(TAPJOY_DEBUG_FLAG_KEY, false));
    }

    Log.i(TAG, "Loading ad for Tapjoy-AdMob adapter");
    Tapjoy.setActivity(activity);
    TapjoyInitializer.getInstance().initialize(activity, sdkKey, connectFlags,
        new TapjoyInitializer.Listener() {
          @Override
          public void onInitializeSucceeded() {
            String placementName = serverParameters.getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);
            if (TextUtils.isEmpty(placementName)) {
              AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                  "Missing or invalid Tapjoy placement name.", ERROR_DOMAIN);
              if (error != null) {
                Log.e(TAG, error.getMessage());
                adLoadCallback.onFailure(error);
              }
              return;
            }

            if (placementsInUse.containsKey(placementName) &&
                placementsInUse.get(placementName).get() != null) {
              String errorMessage = String
                  .format("An ad has already been requested for placement: %s.", placementName);
              AdError error = new AdError(ERROR_AD_ALREADY_REQUESTED, errorMessage, ERROR_DOMAIN);
              if (error != null) {
                Log.e(TAG, error.getMessage());
                adLoadCallback.onFailure(error);
              }
              return;
            }

            placementsInUse.put(placementName,
                new WeakReference<>(TapjoyRewardedRenderer.this));
            createVideoPlacementAndRequestContent(placementName);
          }

          @Override
          public void onInitializeFailed(String message) {
            AdError error = new AdError(ERROR_TAPJOY_INITIALIZATION, message, ERROR_DOMAIN);
            if (error != null) {
              Log.e(TAG, error.getMessage());
              adLoadCallback.onFailure(error);
            }
          }
        });
  }

  private void createVideoPlacementAndRequestContent(final String placementName) {
    Log.i(TAG, "Creating video placement for AdMob adapter.");

    videoPlacement = Tapjoy.getPlacement(placementName, new TJPlacementListener() {
      // Placement Callbacks
      @Override
      public void onRequestSuccess(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (!videoPlacement.isContentAvailable()) {
              placementsInUse.remove(placementName);

              AdError error = new AdError(ERROR_NO_CONTENT_AVAILABLE,
                  "Tapjoy request successful but no content was returned.", ERROR_DOMAIN);
              if (error != null) {
                Log.w(TAG, error.getMessage());
                if (adLoadCallback != null) {
                  adLoadCallback.onFailure(error);
                }
              }
            }
          }
        });
      }

      @Override
      public void onRequestFailure(TJPlacement tjPlacement, final TJError tjError) {
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            placementsInUse.remove(placementName);

            String errorMessage =
                tjError.message == null ? "Tapjoy request failed." : tjError.message;
            AdError error = new AdError(tjError.code, errorMessage, TAPJOY_SDK_ERROR_DOMAIN);
            if (error != null) {
              Log.e(TAG, error.getMessage());
              if (adLoadCallback != null) {
                adLoadCallback.onFailure(error);
              }
            }
          }
        });
      }

      @Override
      public void onContentReady(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "Tapjoy Rewarded Ad is available.");
            if (adLoadCallback != null) {
              mediationRewardedAdCallback = adLoadCallback.onSuccess(TapjoyRewardedRenderer.this);
            }
          }
        });
      }

      @Override
      public void onContentShow(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "Tapjoy Rewarded Ad has been opened.");
            if (mediationRewardedAdCallback != null) {
              mediationRewardedAdCallback.onAdOpened();
            }
          }
        });
      }

      @Override
      public void onContentDismiss(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "Tapjoy Rewarded Ad has been closed.");
            if (mediationRewardedAdCallback != null) {
              mediationRewardedAdCallback.onAdClosed();
            }
            placementsInUse.remove(placementName);
          }
        });
      }

      @Override
      public void onPurchaseRequest(TJPlacement tjPlacement,
          TJActionRequest tjActionRequest,
          String s) {
        // no-op
      }

      @Override
      public void onRewardRequest(TJPlacement tjPlacement,
          TJActionRequest tjActionRequest,
          String s,
          int i) {
        // no-op
      }

      @Override
      public void onClick(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "Tapjoy Rewarded Ad has been clicked.");
            if (mediationRewardedAdCallback != null) {
              mediationRewardedAdCallback.reportAdClicked();
            }
          }
        });
      }
    });

    videoPlacement.setMediationName(MEDIATION_AGENT);
    videoPlacement.setAdapterVersion(TAPJOY_INTERNAL_ADAPTER_VERSION);
    if (isRtbAd) {
      HashMap<String, String> auctionData = new HashMap<>();
      try {
        String bidResponse = adConfiguration.getBidResponse();
        JSONObject bidData = new JSONObject(bidResponse);
        String id = bidData.getString(TapjoyAuctionFlags.AUCTION_ID);
        String extData = bidData.getString(TapjoyAuctionFlags.AUCTION_DATA);
        auctionData.put(TapjoyAuctionFlags.AUCTION_ID, id);
        auctionData.put(TapjoyAuctionFlags.AUCTION_DATA, extData);
      } catch (JSONException e) {
        Log.e(TAG, "Bid Response JSON Error: " + e.getMessage());
      }
      videoPlacement.setAuctionData(auctionData);
    }
    videoPlacement.setVideoListener(this);
    videoPlacement.requestContent();
  }


  @Override
  public void showAd(@NonNull Context context) {
    Log.i(TAG, "Show video content for Tapjoy-AdMob adapter.");
    if (videoPlacement != null && videoPlacement.isContentAvailable()) {
      videoPlacement.showContent();
    } else if (mediationRewardedAdCallback != null) {
      AdError error = new AdError(ERROR_NO_CONTENT_AVAILABLE, "Tapjoy content not available.",
          ERROR_DOMAIN);
      if (error != null) {
        Log.w(TAG, error.getMessage());
        mediationRewardedAdCallback.onAdFailedToShow(error);
      }
    }
  }

  /**
   * {@link TJPlacementVideoListener} implementation.
   */

  @Override
  public void onVideoStart(TJPlacement tjPlacement) {
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Tapjoy Rewarded Ad has started playing.");
        if (mediationRewardedAdCallback != null) {
          mediationRewardedAdCallback.onVideoStart();
          mediationRewardedAdCallback.reportAdImpression();
        }
      }
    });
  }

  @Override
  public void onVideoError(final TJPlacement tjPlacement, final String errorMessage) {
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        placementsInUse.remove(tjPlacement.getName());

        AdError error = new AdError(ERROR_PRESENTATION_VIDEO_PLAYBACK, errorMessage, ERROR_DOMAIN);
        if (error != null) {
          Log.w(TAG, error.getMessage());
          if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onAdFailedToShow(error);
          }
        }
      }
    });
  }

  @Override
  public void onVideoComplete(TJPlacement tjPlacement) {
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Tapjoy Rewarded Ad has finished playing.");
        if (mediationRewardedAdCallback != null) {
          mediationRewardedAdCallback.onVideoComplete();
          mediationRewardedAdCallback.onUserEarnedReward(new TapjoyReward());
        }
      }
    });
  }

  /**
   * A {@link RewardItem} used to map Tapjoy reward to Google's reward.
   */
  public class TapjoyReward implements RewardItem {

    @NonNull
    @Override
    public String getType() {
      // Tapjoy only supports fixed rewards and doesn't provide a reward type.
      return "";
    }

    @Override
    public int getAmount() {
      // Tapjoy only supports fixed rewards and doesn't provide a reward amount.
      return 1;
    }
  }
}
