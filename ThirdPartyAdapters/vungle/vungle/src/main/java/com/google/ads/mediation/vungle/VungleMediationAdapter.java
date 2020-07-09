package com.google.ads.mediation.vungle;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.vungle.mediation.BuildConfig;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Mediation network adapter for Vungle. */
public class VungleMediationAdapter extends Adapter
    implements MediationRewardedAd, LoadAdCallback, PlayAdCallback {

  private static final String TAG = VungleMediationAdapter.class.getSimpleName();
  private static final String KEY_APP_ID = "appid";

  private AdConfig mAdConfig;
  private String mUserID;
  private String mPlacement;
  private Handler mHandler = new Handler(Looper.getMainLooper());

  private static HashMap<String, WeakReference<VungleMediationAdapter>> mPlacementsInUse =
      new HashMap<>();

  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mMediationAdLoadCallback;
  private MediationRewardedAdCallback mMediationRewardedAdCallback;

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
    String versionString = com.vungle.warren.BuildConfig.VERSION_NAME;
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
      final InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {

    if (Vungle.isInitialized()) {
      initializationCompleteCallback.onInitializationSucceeded();
      return;
    }

    if (!(context instanceof Activity)) {
      initializationCompleteCallback.onInitializationFailed(
          "Vungle SDK requires an Activity context to initialize.");
      return;
    }

    HashSet<String> appIDs = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String appIDFromServer = serverParameters.getString(KEY_APP_ID);

      if (!TextUtils.isEmpty(appIDFromServer)) {
        appIDs.add(appIDFromServer);
      }
    }

    int count = appIDs.size();
    if (count <= 0) {
      initializationCompleteCallback.onInitializationFailed(
          "Initialization failed: Missing or Invalid App ID.");
      return;
    }

    String appID = appIDs.iterator().next();
    if (count > 1) {
      String logMessage =
          String.format(
              "Multiple '%s' entries found: %s. Using '%s' to initialize the Vungle SDK.",
              KEY_APP_ID, appIDs.toString(), appID);
      Log.w(TAG, logMessage);
    }

    VungleInitializer.getInstance()
        .initialize(
            appID,
            context.getApplicationContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                initializationCompleteCallback.onInitializationSucceeded();
              }

              @Override
              public void onInitializeError(String errorMessage) {
                initializationCompleteCallback.onInitializationFailed(
                    "Initialization Failed: " + errorMessage);
              }
            });
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    mMediationAdLoadCallback = mediationAdLoadCallback;

    Context context = mediationRewardedAdConfiguration.getContext();
    if (!(context instanceof Activity)) {
      mediationAdLoadCallback.onFailure("Vungle SDK requires an Activity context to initialize.");
      return;
    }

    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    if (mediationExtras != null) {
      mUserID = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);
    }

    mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacement)) {
      String logMessage = "Failed to load ad from Vungle: Missing or invalid Placement ID.";
      Log.w(TAG, logMessage);
      mediationAdLoadCallback.onFailure(logMessage);
      return;
    }

    if (mPlacementsInUse.containsKey(mPlacement)
        && mPlacementsInUse.get(mPlacement).get() != null) {
      String logMessage = "Only a maximum of one ad can be loaded per placement.";
      Log.w(TAG, logMessage);
      mediationAdLoadCallback.onFailure(logMessage);
      return;
    }

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      String logMessage = "Failed to load ad from Vungle: Missing or Invalid App ID.";
      Log.w(TAG, logMessage);
      mediationAdLoadCallback.onFailure(logMessage);
      return;
    }

    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
    VungleInitializer.getInstance()
        .initialize(
            appID,
            context.getApplicationContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                Vungle.setIncentivizedFields(mUserID, null, null, null, null);
                mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleMediationAdapter.this));

                if (Vungle.canPlayAd(mPlacement)) {
                  mMediationRewardedAdCallback =
                      mMediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
                  return;
                }

                Vungle.loadAd(mPlacement, VungleMediationAdapter.this);
              }

              @Override
              public void onInitializeError(String errorMessage) {
                Log.w(TAG, errorMessage);
                mMediationAdLoadCallback.onFailure(errorMessage);
                mPlacementsInUse.remove(mPlacement);
              }
            });
  }

  @Override
  public void showAd(Context context) {
    if (Vungle.canPlayAd(mPlacement)) {
      Vungle.playAd(mPlacement, mAdConfig, VungleMediationAdapter.this);
    } else {
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdFailedToShow("Not ready.");
      }
      mPlacementsInUse.remove(mPlacement);
    }
  }

  /** {@link LoadAdCallback} implemenatation from Vungle */
  @Override
  public void onAdLoad(final String placementID) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              mMediationRewardedAdCallback =
                  mMediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
            }
            mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleMediationAdapter.this));
          }
        });
  }

  /** {@link PlayAdCallback} implemenatation from Vungle */
  @Override
  public void onAdStart(final String placementID) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdOpened();
              mMediationRewardedAdCallback.onVideoStart();
              mMediationRewardedAdCallback.reportAdImpression();
            }
          }
        });
  }

  @Override
  @Deprecated
  public void onAdEnd(
      final String placementID,
      final boolean wasSuccessfulView,
      final boolean wasCallToActionClicked) {
  }

  @Override
  public void onAdEnd(final String placementID) {
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        if (mMediationRewardedAdCallback != null) {
          mMediationRewardedAdCallback.onAdClosed();
        }
        mPlacementsInUse.remove(placementID);
      }
    });
  }

  @Override
  public void onAdClick(String placementID) {
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        if (mMediationRewardedAdCallback != null) {
          mMediationRewardedAdCallback.reportAdClicked();
        }
      }
    });
  }

  @Override
  public void onAdRewarded(String placementID) {
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        if (mMediationRewardedAdCallback != null) {
          mMediationRewardedAdCallback.onVideoComplete();
          mMediationRewardedAdCallback.onUserEarnedReward(new VungleReward("vungle", 1));
        }
      }
    });
  }

  @Override
  public void onAdLeftApplication(String placementID) {
    //no op
  }

  // Vungle's LoadAdCallback and PlayAdCallback shares the same onError() call; when an
  // ad request to Vungle fails, and when an ad fails to play.
  @Override
  public void onError(final String placementID, final VungleException throwable) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              Log.w(TAG, "Failed to load ad from Vungle", throwable);
              mMediationAdLoadCallback.onFailure(throwable.getLocalizedMessage());
            }

            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdFailedToShow(throwable.getLocalizedMessage());
            }
            mPlacementsInUse.remove(placementID);
          }
        });
  }

  /**
   * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
   */
  private class VungleReward implements RewardItem {

    private final String mType;
    private final int mAmount;

    VungleReward(String type, int amount) {
      mType = type;
      mAmount = amount;
    }

    @Override
    public int getAmount() {
      return mAmount;
    }

    @Override
    public String getType() {
      return mType;
    }
  }
}
