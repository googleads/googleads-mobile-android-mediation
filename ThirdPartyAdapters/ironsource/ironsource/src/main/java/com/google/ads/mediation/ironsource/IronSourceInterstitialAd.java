package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.ironsource.mediationsdk.IronSource;
import java.util.concurrent.ConcurrentHashMap;

public class IronSourceInterstitialAd implements MediationInterstitialAd {

  private static final ConcurrentHashMap<String, IronSourceInterstitialAd>
      availableInterstitialInstances = new ConcurrentHashMap<>();

  private static final IronSourceInterstitialAdListener ironSourceInterstitialListener =
      new IronSourceInterstitialAdListener();

  /**
   * Mediation listener used to forward interstitial ad events from IronSource SDK to Google Mobile
   * Ads SDK while ad is presented
   */
  private MediationInterstitialAdCallback interstitialAdCallback;

  /**
   * Mediation listener used to forward interstitial ad events from IronSource SDK to Google Mobile
   * Ads SDK for loading phases of the ad
   */
  public MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mediationInterstitialAdLoadCallback;

  /** IronSource interstitial instance ID. */
  private final String instanceID;

  public IronSourceInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration interstitialAdConfig,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationInterstitialAdLoadCallback) {
    Bundle serverParameters = interstitialAdConfig.getServerParameters();
    this.instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
    this.mediationInterstitialAdLoadCallback = mediationInterstitialAdLoadCallback;
  }

	/** Getters and Setters */
	public static IronSourceInterstitialAd getFromAvailableInstances(String instanceId) {
		return availableInterstitialInstances.get(instanceId);
	}

	public static void removeFromAvailableInstances(String instanceId) {
		availableInterstitialInstances.remove(instanceId);
	}

	public static IronSourceInterstitialAdListener getIronSourceInterstitialListener(){
		return ironSourceInterstitialListener;
	}

	public MediationInterstitialAdCallback getInterstitialAdCallback(){
		return interstitialAdCallback;
	}

	public void setInterstitialAdCallback(MediationInterstitialAdCallback adCallback){
		interstitialAdCallback = adCallback;
	}

	/** Interstitial Manager Section */
	public void loadInterstitial(
      @Nullable Context context,
      @NonNull String instanceId,
      @NonNull IronSourceInterstitialAd adapter) {
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

    if (!canLoadInterstitialInstance(instanceId)) {
      String errorMessage =
          String.format("An ad is already loading for instance ID: %s", instanceId);
      AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
      onAdFailedToLoad(concurrentError);
      return;
    }

    availableInterstitialInstances.put(instanceId, adapter);
    Log.d(
        TAG, String.format("Loading IronSource interstitial ad with instance ID: %s", instanceId));
    IronSource.loadISDemandOnlyInterstitial(activity, instanceId);
  }

  private boolean canLoadInterstitialInstance(@NonNull String instanceId) {
    IronSourceInterstitialAd ironSourceInterstitialAd =
        availableInterstitialInstances.get(instanceId);
    return (ironSourceInterstitialAd == null);
  }

  /** Interstitial show Ad */
  @Override
  public void showAd(@NonNull Context context) {
    IronSource.showISDemandOnlyInterstitial(this.instanceID);
  }

  /** Pass Load Fail from IronSource SDK to Google Mobile Ads */
  private void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.e(TAG, loadError.getMessage());
    if (mediationInterstitialAdLoadCallback != null) {
      mediationInterstitialAdLoadCallback.onFailure(loadError);
    }
  }
}
