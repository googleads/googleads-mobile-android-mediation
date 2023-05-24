package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerLayout;

import java.util.concurrent.ConcurrentHashMap;

public class IronSourceBannerAd implements MediationBannerAd {

  /** A map holding direct reference to each available banner instance by instance ID */
  private static final ConcurrentHashMap<String, IronSourceBannerAd> availableBannerInstances =
      new ConcurrentHashMap<>();

  /** A single class-level listener for handling IronSource SDK callbacks */
  private static final IronSourceBannerAdListener ironSourceBannerListener =
      new IronSourceBannerAdListener();

  /**
   * Mediation listener used to forward banner ad events from IronSource SDK to Google Mobile Ads
   * SDK while ad is presented
   */
  private MediationBannerAdCallback bannerAdCallback;

  /**
   * Mediation listener used to forward banner ad events from IronSource SDK to Google Mobile Ads
   * SDK for loading phases of the ad
   */
  private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback;

  private FrameLayout ironSourceAdView;

  /** IronSource banner instance view. */
  private ISDemandOnlyBannerLayout ironSourceBannerLayout;

  public IronSourceBannerAd(
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback) {
    this.adLoadCallback = mediationAdLoadCallback;
  }

  /** Getters and Setters */
  public MediationBannerAdCallback getBannerAdCallback() {
    return bannerAdCallback;
  }

  public MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> getAdLoadCallback() {
    return adLoadCallback;
  }

  public void setBannerAdCallback(MediationBannerAdCallback adCallback) {
    bannerAdCallback = adCallback;
  }

  public ISDemandOnlyBannerLayout getIronSourceBannerLayout() {
    return ironSourceBannerLayout;
  }

  public FrameLayout getIronSourceAdView() {
    return ironSourceAdView;
  }

  /** Instance map access */
  public static IronSourceBannerAd getFromAvailableInstances(String instanceId) {
    return availableBannerInstances.get(instanceId);
  }

  public static void removeFromAvailableInstances(String instanceId) {
    availableBannerInstances.remove(instanceId);
  }

  /**
   * Removes from the available instances map and destroys all instances except for the instance
   * with the given instance ID
   */
  public static void clearAllAvailableInstancesExceptOne(String instanceToKeep) {
    for (String otherInstanceInMap : availableBannerInstances.keySet()) {
      if (!otherInstanceInMap.equals(instanceToKeep)) {
        Log.d(
            TAG,
            String.format("IronSource Banner Destroy ad with instance ID: %s", otherInstanceInMap));
        IronSource.destroyISDemandOnlyBanner(otherInstanceInMap);
        removeFromAvailableInstances(otherInstanceInMap);
      }
    }
  }

  /** Banner Manager Section */
  public void loadBanner(
      @NonNull Context context,
      String instanceId,
      IronSourceBannerAd ironSourceBannerAd,
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration) {

    if (availableBannerInstances.get(instanceId) != null) {
      AdError loadError =
          new AdError(
              ERROR_AD_ALREADY_LOADED,
              "A banner is already loaded for instance ID: " + instanceId,
              ERROR_DOMAIN);
      onAdFailedToLoad(loadError);
      return;
    }

    if (TextUtils.isEmpty(instanceId)) {
      AdError loadError =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid instance ID.", ERROR_DOMAIN);
      onAdFailedToLoad(loadError);
      return;
    }

    ISBannerSize bannerSize =
        IronSourceAdapterUtils.getISBannerSize(context, mediationBannerAdConfiguration.getAdSize());
    if (bannerSize == null) {
      AdError sizeError =
          new AdError(
              ERROR_BANNER_SIZE_MISMATCH,
              "There is no matching IronSource ad size for Google ad size: %s" + "adSize",
              ERROR_DOMAIN);
      Log.e(TAG, sizeError.toString());
      onAdFailedToLoad(sizeError);
      return;
    }

    if (!(context instanceof Activity)) {
      String errorMessage =
          ERROR_REQUIRES_ACTIVITY_CONTEXT + "IronSource requires an Activity context to load ads.";
      AdError contextError =
          new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, errorMessage, ERROR_DOMAIN);
      onAdFailedToLoad(contextError);
      return;
    }

    Activity activity = (Activity) context;
    availableBannerInstances.put(instanceId, ironSourceBannerAd);
    ironSourceAdView = new FrameLayout(context);
    ironSourceBannerLayout = IronSource.createBannerForDemandOnly(activity, bannerSize);
    ironSourceBannerLayout.setBannerDemandOnlyListener(ironSourceBannerListener);
    Log.d(TAG, String.format("Loading IronSource banner ad with instance ID: %s", instanceId));
    IronSource.loadISDemandOnlyBanner(activity, ironSourceBannerLayout, instanceId);
  }

  @NonNull
  @Override
  public View getView() {
    return ironSourceAdView;
  }

  /** Pass Load Fail from IronSource SDK to Google Mobile Ads */
  private void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.w(TAG, loadError.getMessage());
    if (adLoadCallback != null) {
      adLoadCallback.onFailure(loadError);
    }
  }
}
