package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MetaData;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * The {@link UnityMediationAdapter} is used to initialize the Unity Ads SDK, load rewarded video
 * ads from Unity ads and mediate the callbacks between Google Mobile Ads SDK and Unity Ads SDK.
 */
public class UnityMediationAdapter extends Adapter implements MediationRewardedAd {

  static final String TAG = UnityMediationAdapter.class.getSimpleName();

  /**
   * Key to obtain Game ID, required for loading Unity Ads.
   */
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
  private MediationAdLoadCallback<MediationRewardedAd,
      MediationRewardedAdCallback> mMediationAdLoadCallback;

  /**
   * Mediation rewarded video ad listener used to forward rewarded ad events from {@link
   * UnitySingleton} to the Google Mobile Ads SDK.
   */
  private MediationRewardedAdCallback mMediationRewardedAdCallback;

  /**
   * Placement ID used to determine what type of ad to load.
   */
  private String mPlacementId;

  /**
   * Unity Ads UUID for Unity instrument analysis.
   */
  protected String uuid;

  /**
   * Unity Ads meta-data for storing Unity instrument analysis.
   */
  static MetaData metadata;

  /**
   * Unity adapter delegate to to forward the events from {@link UnitySingleton} to Google Mobile
   * Ads SDK.
   */
  private UnityAdapterDelegate mUnityAdapterRewardedAdDelegate = new UnityAdapterDelegate() {

    @Override
    public String getPlacementId() {
      return mPlacementId;
    }

    @Override
    public void onUnityAdsReady(String placementId) {
      // Unity Ads is ready to show ads for the given placementId. Send Ad Loaded event if the
      // adapter is currently loading ads.
      if (placementId.equals(getPlacementId()) && mMediationAdLoadCallback != null) {
        mMediationRewardedAdCallback = mMediationAdLoadCallback
            .onSuccess(UnityMediationAdapter.this);
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
    public void onUnityAdsPlacementStateChanged(String placementId,
        UnityAds.PlacementState oldState,
        UnityAds.PlacementState newState) {
      // Unity Ads SDK NO_FILL state to Google Mobile Ads SDK.
      if (placementId.equals(getPlacementId())) {
        if (newState.equals(UnityAds.PlacementState.NO_FILL) || newState
            .equals(UnityAds.PlacementState.DISABLED)) {
          if (mMediationAdLoadCallback != null) {
            mMediationAdLoadCallback.onFailure("UnityAds failed to load: " + placementId);
          }
          UnitySingleton.getInstance().stopTrackingPlacement(placementId);
        }
      }
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
      // Unity Ads ad closed.
      if (placementId.equals(getPlacementId()) && mMediationRewardedAdCallback != null) {
        // Reward is provided only if the ad is watched completely.
        if (finishState == UnityAds.FinishState.COMPLETED) {
          mMediationRewardedAdCallback.onVideoComplete();
          // Unity Ads doesn't provide a reward value. The publisher is expected to
          // override the reward in AdMob console.
          mMediationRewardedAdCallback.onUserEarnedReward(new UnityReward());
          mMediationRewardedAdCallback.onAdClosed();
        } else if (finishState == UnityAds.FinishState.ERROR) {
          mMediationRewardedAdCallback.onAdFailedToShow("UnityAds Show Error: " + placementId);
        } else {
          mMediationRewardedAdCallback.onAdClosed();
        }
      }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String placementId) {
      if (placementId.equals(getPlacementId())) {
        if (mMediationAdLoadCallback != null
            && unityAdsError.equals(UnityAds.UnityAdsError.NOT_INITIALIZED)) {
          mMediationAdLoadCallback.onFailure("UnityAds failed to load: " + placementId);
          return;
        }

        if (mMediationRewardedAdCallback != null) {
          mMediationRewardedAdCallback.onAdFailedToShow("UnityAds failed to show: " + placementId);
        }
      }
    }
  };

  //region Adapter implementation.

  /**
   * {@link Adapter} implementation
   */
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

    String logMessage = String.format("Unexpected adapter version format: %s." +
        "Returning 0.0.0 for adapter version.", versionString);
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

    String logMessage = String.format("Unexpected SDK version format: %s." +
        "Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(Context context,
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {
    if (!(context instanceof Activity)) {
      initializationCompleteCallback.onInitializationFailed("UnityAds SDK requires an " +
          "Activity context to initialize");
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
        String message = String.format("Multiple '%s' entries found: %s. " +
                "Using '%s' to initialize the UnityAds SDK",
            KEY_GAME_ID, gameIDs.toString(), gameID);
        Log.w(TAG, message);
      }
    }

    if (TextUtils.isEmpty(gameID)) {
      initializationCompleteCallback.onInitializationFailed(
          "Initialization failed: Missing or invalid Game ID.");
      return;
    }

    metadata = new MetaData(context);

    UnitySingleton.getInstance().initializeUnityAds((Activity) context, gameID);
    initializationCompleteCallback.onInitializationSucceeded();
  }
  //endregion

  //region MediationRewardedAd implementation.
  @Override
  public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback) {
    Context context = mediationRewardedAdConfiguration.getContext();
    if (!(context instanceof Activity)) {
      mediationAdLoadCallback.onFailure("Context is not an Activity." +
          " Unity Ads requires an Activity context to show ads.");
      return;
    }

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    String gameID = serverParameters.getString(KEY_GAME_ID);
    mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

    if (!isValidIds(gameID, mPlacementId)) {
      mediationAdLoadCallback.onFailure("Failed to load ad from UnityAds: " +
          "Missing or invalid game ID and placement ID.");
      return;
    }

    mMediationAdLoadCallback = mediationAdLoadCallback;

    UnitySingleton.getInstance().initializeUnityAds((Activity) context, gameID);
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
      String message = "An activity context is required to show Unity Ads.";
      Log.w(TAG, message);
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdFailedToShow(message);
      }
      return;
    }

    // Add isReady check to prevent ready to no fill case
    if (!UnityAds.isReady(mPlacementId) && mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onAdFailedToShow("Failed to show Unity Ads rewarded video.");
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
  //endregion

  /**
   * Checks whether or not the provided Unity Ads IDs are valid.
   *
   * @param gameId Unity Ads Game ID to be verified.
   * @param placementId Unity Ads Placement ID to be verified.
   * @return {@code true} if all the IDs provided are valid.
   */
  private static boolean isValidIds(String gameId, String placementId) {
    if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
      String ids = TextUtils.isEmpty(gameId) ? TextUtils.isEmpty(placementId)
          ? "Game ID and Placement ID" : "Game ID" : "Placement ID";
      Log.w(TAG, ids + " cannot be empty.");

      return false;
    }

    return true;
  }
}
