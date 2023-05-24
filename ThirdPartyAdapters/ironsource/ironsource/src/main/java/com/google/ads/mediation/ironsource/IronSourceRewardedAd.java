package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_SHOW_UNAUTHORIZED;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.ironsource.mediationsdk.IronSource;

import java.util.concurrent.ConcurrentHashMap;

public class IronSourceRewardedAd implements MediationRewardedAd {

  private static final ConcurrentHashMap<String, IronSourceRewardedAd> availableInstances =
      new ConcurrentHashMap<>();

  private static final IronSourceRewardedAdListener ironSourceRewardedListener =
      new IronSourceRewardedAdListener();

  /**
   * Mediation listener used to forward rewarded ad events from IronSource SDK to Google Mobile Ads
   * SDK while ad is presented
   */
  private MediationRewardedAdCallback mediationRewardedAdCallback;

  /**
   * Mediation listener used to forward rewarded ad events from IronSource SDK to Google Mobile Ads
   * SDK for loading phases of the ad
   */
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  /** This is the id of the rewarded video instance requested. */
  private final String instanceID;

  public IronSourceRewardedAd(
      @NonNull MediationRewardedAdConfiguration rewardedAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              mediationAdLoadCallback) {
    Bundle serverParameters = rewardedAdConfiguration.getServerParameters();
    instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    IronSource.setISDemandOnlyRewardedVideoListener(ironSourceRewardedListener);
  }

	/** Getters and Setters */
	public static IronSourceRewardedAd getFromAvailableInstances(String instanceId) {
		return availableInstances.get(instanceId);
	}

	public static void removeFromAvailableInstances(String instanceId) {
		availableInstances.remove(instanceId);
	}

	public static IronSourceRewardedAdListener getIronSourceRewardedListener(){
		return ironSourceRewardedListener;
	}

	public MediationRewardedAdCallback getRewardedAdCallback(){
		return mediationRewardedAdCallback;
	}

	public void setRewardedAdCallback(MediationRewardedAdCallback adCallback){
		mediationRewardedAdCallback = adCallback;
	}

	public MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> getMediationAdLoadCallback(){
		return mediationAdLoadCallback;
	}


	/** Interstitial Manager Section */
  public void loadRewardedVideo(@NonNull Context context, @NonNull String instanceId) {
    if (!(context instanceof Activity)) {
      String errorMessage =
          ERROR_REQUIRES_ACTIVITY_CONTEXT + "IronSource requires an Activity context to load ads.";
      AdError contextError =
          new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, errorMessage, ERROR_DOMAIN);
      onAdFailedToLoad(contextError);
      return;
    }

    Activity activity = (Activity) context;

    if (TextUtils.isEmpty(instanceId)) {
      AdError loadError =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid instance ID.", ERROR_DOMAIN);
      onAdFailedToLoad(loadError);
      return;
    }

    if (!canLoadRewardedVideoInstance(instanceId)) {
      String errorMessage =
          String.format("An ad is already loading for instance ID: %s", instanceId);
      AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
      onAdFailedToLoad(concurrentError);
      return;
    }

    availableInstances.put(instanceId, this);
    Log.d(TAG, String.format("Loading IronSource rewarded ad with instance ID: %s", instanceID));
    IronSource.loadISDemandOnlyRewardedVideo(activity, instanceId);
  }

  private boolean canLoadRewardedVideoInstance(@NonNull String instanceId) {
    IronSourceRewardedAd ironSourceRewardedAd = availableInstances.get(instanceId);
    return (ironSourceRewardedAd == null);
  }

  /** Rewarded Video show Ad */
  @Override
  public void showAd(@NonNull Context context) {
    Log.d(
        TAG, String.format("Showing IronSource rewarded ad for instance ID: %s", this.instanceID));
    IronSourceRewardedAd ironSourceRewardedAd = availableInstances.get(this.instanceID);
    if (ironSourceRewardedAd == null) {
      AdError showError =
          new AdError(
              ERROR_AD_SHOW_UNAUTHORIZED,
              "IronSource adapter does not have authority to show this instance.",
              ERROR_DOMAIN);
      Log.w(TAG, showError.getMessage());
      if (mediationAdLoadCallback != null) {
        mediationAdLoadCallback.onFailure(showError);
      }
      return;
    }

    // IronSource may call onRewardedVideoAdRewarded() after onRewardedVideoAdClosed() on some
    // rewarded ads. Store the adapter reference so callbacks can be forwarded properly regardless
    // of order.
    IronSource.showISDemandOnlyRewardedVideo(this.instanceID);
  }

  /** Pass Load Fail from IronSource SDK to Google Mobile Ads */
  private void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.w(TAG, loadError.getMessage());
    mediationAdLoadCallback.onFailure(loadError);
  }
}
