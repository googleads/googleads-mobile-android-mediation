package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createAdapterError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAds.FinishState;
import com.unity3d.ads.UnityAds.PlacementState;
import com.unity3d.ads.metadata.MetaData;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * The {@link UnityMediationAdapter} is used to initialize the Unity Ads SDK, load rewarded video
 * ads from Unity ads and mediate the callbacks between Google Mobile Ads SDK and Unity Ads SDK.
 */
public class UnityMediationAdapter extends Adapter implements MediationRewardedAd {

  static final String TAG = UnityMediationAdapter.class.getSimpleName();

  // region Error Codes
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
        ERROR_INVALID_SERVER_PARAMETERS,
        ERROR_PLACEMENT_STATE_NO_FILL,
        ERROR_PLACEMENT_STATE_DISABLED,
        ERROR_NULL_CONTEXT,
        ERROR_CONTEXT_NOT_ACTIVITY,
        ERROR_AD_NOT_READY,
        ERROR_UNITY_ADS_NOT_SUPPORTED,
        ERROR_AD_ALREADY_LOADING,
        ERROR_FINISH
      })
  @interface AdapterError {}

  /** Invalid server parameters. */
  static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /** UnityAds returned a placement with a {@link PlacementState#NO_FILL} state. */
  static final int ERROR_PLACEMENT_STATE_NO_FILL = 102;

  /** UnityAds returned a placement with a {@link PlacementState#DISABLED} state. */
  static final int ERROR_PLACEMENT_STATE_DISABLED = 103;

  /** Tried to show an ad with a {@code null} context. */
  static final int ERROR_NULL_CONTEXT = 104;

  /**
   * Context used to initialize, load and/or show ads from Unity Ads is not an {@link Activity}
   * instance.
   */
  static final int ERROR_CONTEXT_NOT_ACTIVITY = 105;

  /** Tried to show an ad that's not ready to be shown. */
  static final int ERROR_AD_NOT_READY = 106;

  /** UnityAds is not supported on the device. */
  static final int ERROR_UNITY_ADS_NOT_SUPPORTED = 107;

  /** UnityAds can only load 1 ad per placement at a time. */
  static final int ERROR_AD_ALREADY_LOADING = 108;

  /** UnityAds finished with a {@link FinishState#ERROR} state. */
  static final int ERROR_FINISH = 109;
  // endregion

  /** Key to obtain Game ID, required for loading Unity Ads. */
  static final String KEY_GAME_ID = "gameId";

  /**
   * Key to obtain Placement ID, used to set the type of ad to be shown. Unity Ads has changed the
   * name from Zone ID to Placement ID in Unity Ads SDK 2.0.0. To maintain backwards compatibility
   * the key is not changed.
   */
  static final String KEY_PLACEMENT_ID = "zoneId";

  /**
   * Mediation rewarded video ad listener used to forward ad load status from {@link UnitySingleton}
   * to the Google Mobile Ads SDK.
   */
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mMediationAdLoadCallback;

  /**
   * Mediation rewarded video ad listener used to forward rewarded ad events from {@link
   * UnitySingleton} to the Google Mobile Ads SDK.
   */
  private MediationRewardedAdCallback mMediationRewardedAdCallback;

  /** Placement ID used to determine what type of ad to load. */
  private String mPlacementId;

  /** Unity Ads UUID for Unity instrument analysis. */
  protected String uuid;

  /** Unity Ads meta-data for storing Unity instrument analysis. */
  static MetaData metadata;

  /**
   * Unity adapter delegate to to forward the events from {@link UnitySingleton} to Google Mobile
   * Ads SDK.
   */
  private final UnityAdapterDelegate mUnityAdapterRewardedAdDelegate =
      new UnityAdapterDelegate() {

        @Override
        public String getPlacementId() {
          return mPlacementId;
        }

        @Override
        public void onUnityAdsReady(String placementId) {
          // Unity Ads is ready to show ads for the given placementId. Send Ad Loaded event if the
          // adapter is currently loading ads.
          if (placementId.equals(getPlacementId()) && mMediationAdLoadCallback != null) {
            mMediationRewardedAdCallback =
                mMediationAdLoadCallback.onSuccess(UnityMediationAdapter.this);
          }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
          // Unity Ads video ad started playing. Send Video Started event if this is a rewarded
          // video adapter.
          if (placementId.equals(getPlacementId()) && mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback.onVideoStart();
            mMediationRewardedAdCallback.onAdOpened();
            mMediationRewardedAdCallback.reportAdImpression();
          }
        }

        @Override
        public void onUnityAdsClick(String placementId) {
          // Unity Ads ad clicked.
          if (placementId.equals(getPlacementId()) && mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback.reportAdClicked();
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

            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(errorMessage);
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

            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(errorMessage);
            }
            UnitySingleton.getInstance().stopTrackingPlacement(placementId);
          }
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
          if (!placementId.equals(getPlacementId())) {
            return;
          }

          if (mMediationRewardedAdCallback != null) {
            // Reward is provided only if the ad is watched completely.
            if (finishState == UnityAds.FinishState.COMPLETED) {
              mMediationRewardedAdCallback.onVideoComplete();
              // Unity Ads doesn't provide a reward value. The publisher is expected to
              // override the reward in AdMob console.
              mMediationRewardedAdCallback.onUserEarnedReward(new UnityReward());
            } else if (finishState == UnityAds.FinishState.ERROR) {
              String errorMessage =
                  createAdapterError(
                      ERROR_FINISH,
                      "UnityAds SDK called onUnityAdsFinish() with finish state ERROR.");
              Log.w(TAG, errorMessage);
              mMediationRewardedAdCallback.onAdFailedToShow(errorMessage);
            }
            mMediationRewardedAdCallback.onAdClosed();
          }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String errorMessage) {
          String sdkError = createSDKError(unityAdsError, errorMessage);
          Log.w(TAG, "Unity Ads returned an error: " + sdkError);

          if (unityAdsError.equals(UnityAds.UnityAdsError.NOT_INITIALIZED)
              || unityAdsError.equals(UnityAds.UnityAdsError.INITIALIZE_FAILED)
              || unityAdsError.equals(UnityAds.UnityAdsError.INIT_SANITY_CHECK_FAIL)
              || unityAdsError.equals(UnityAds.UnityAdsError.INVALID_ARGUMENT)
              || unityAdsError.equals(UnityAds.UnityAdsError.AD_BLOCKER_DETECTED)) {
            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(sdkError);
            }
            return;
          }

          if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback.onAdFailedToShow(sdkError);
          }
        }

        @Override
        public void onAdFailedToLoad(@AdapterError int errorCode, @NonNull String errorMessage) {
          String adapterError = createAdapterError(errorCode, errorMessage);
          Log.w(TAG, "Failed to load ad: " + adapterError);
          if (mMediationAdLoadCallback != null) {
            mMediationAdLoadCallback.onFailure(adapterError);
          }
        }
      };

  // region Adapter implementation.
  public UnityMediationAdapter() {
    uuid = UUID.randomUUID().toString();

    if (metadata != null) {
      metadata.setCategory("mediation_adapter");
      metadata.set(uuid, "create-adapter");
      metadata.commit();
    }
  }

  @Override
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.VERSION_NAME;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = UnityAds.getVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(
      Context context,
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {
    if (UnityAds.isInitialized()) {
      initializationCompleteCallback.onInitializationSucceeded();
      return;
    }

    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to initialize.");
      initializationCompleteCallback.onInitializationFailed(adapterError);
      return;
    }

    HashSet<String> gameIDs = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String gameIDFromServer = serverParameters.getString(KEY_GAME_ID);

      if (!TextUtils.isEmpty(gameIDFromServer)) {
        gameIDs.add(gameIDFromServer);
      }
    }

    String gameID = "";
    int count = gameIDs.size();
    if (count > 0) {
      gameID = gameIDs.iterator().next();

      if (count > 1) {
        String message =
            String.format(
                "Multiple '%s' entries found: %s. " + "Using '%s' to initialize the UnityAds SDK",
                KEY_GAME_ID, gameIDs, gameID);
        Log.w(TAG, message);
      }
    }

    if (TextUtils.isEmpty(gameID)) {
      String adapterError =
          createAdapterError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Game ID.");
      initializationCompleteCallback.onInitializationFailed(adapterError);
      return;
    }

    metadata = new MetaData(context);

    boolean success = UnitySingleton.getInstance().initializeUnityAds((Activity) context, gameID);
    if (!success) {
      String adapterError =
          createAdapterError(
              ERROR_UNITY_ADS_NOT_SUPPORTED, "The current device is not supported by Unity Ads.");
      initializationCompleteCallback.onInitializationFailed(adapterError);
      return;
    }
    initializationCompleteCallback.onInitializationSucceeded();
  }
  // endregion

  // region MediationRewardedAd implementation.
  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    Context context = mediationRewardedAdConfiguration.getContext();
    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to load ads.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      mediationAdLoadCallback.onFailure(adapterError);
      return;
    }

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    String gameID = serverParameters.getString(KEY_GAME_ID);
    mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

    if (!isValidIds(gameID, mPlacementId)) {
      String adapterError =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid server parameters.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      mediationAdLoadCallback.onFailure(adapterError);
      return;
    }

    boolean success = UnitySingleton.getInstance().initializeUnityAds((Activity) context, gameID);
    if (!success) {
      String adapterError =
          createAdapterError(
              ERROR_UNITY_ADS_NOT_SUPPORTED, "The current device is not supported by Unity Ads.");
      Log.w(TAG, adapterError);
      mediationAdLoadCallback.onFailure(adapterError);
      return;
    }

    mMediationAdLoadCallback = mediationAdLoadCallback;

    MetaData metadata = new MetaData(context);
    metadata.setCategory("mediation_adapter");
    metadata.set(uuid, "load-rewarded");
    metadata.set(uuid, mPlacementId);
    metadata.commit();
    UnitySingleton.getInstance().loadAd(mUnityAdapterRewardedAdDelegate);
  }

  @Override
  public void showAd(Context context) {
    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to show ads.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdFailedToShow(adapterError);
      }
      return;
    }

    // Add isReady check to prevent ready to no fill case
    if (!UnityAds.isReady(mPlacementId)) {
      String adapterError = createAdapterError(ERROR_AD_NOT_READY, "Ad is not ready to be shown.");
      Log.w(TAG, "Failed to show Unity Ads Rewarded ad: " + adapterError);
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdFailedToShow(adapterError);
      }

      MetaData metadata = new MetaData(context);
      metadata.setCategory("mediation_adapter");
      metadata.set(uuid, "fail-to-show-rewarded");
      metadata.set(uuid, mPlacementId);
      metadata.commit();
      return;
    }

    Activity activity = (Activity) context;
    MetaData metadata = new MetaData(context);
    metadata.setCategory("mediation_adapter");
    metadata.set(uuid, "show-rewarded");
    metadata.set(uuid, mPlacementId);
    metadata.commit();
    UnitySingleton.getInstance().showAd(mUnityAdapterRewardedAdDelegate, activity);
  }
  // endregion

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
}
