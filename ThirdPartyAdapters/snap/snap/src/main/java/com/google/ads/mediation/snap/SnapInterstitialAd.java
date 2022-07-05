package com.google.ads.mediation.snap;

import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_SNAP_SDK_LOAD_FAILURE;
import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_SNAP_SDK_NOT_INITIALIZED;
import static com.google.ads.mediation.snap.SnapMediationAdapter.SLOT_ID_KEY;

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
import com.snap.adkit.external.AdKitAudienceAdsNetwork;
import com.snap.adkit.external.AdKitSlotType;
import com.snap.adkit.external.AudienceNetworkAdsApi;
import com.snap.adkit.external.LoadAdConfig;
import com.snap.adkit.external.LoadAdConfigBuilder;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdDismissed;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdImpressionHappened;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdKitSlot;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdVisible;

public class SnapInterstitialAd implements MediationInterstitialAd {

  private static final String TAG = SnapInterstitialAd.class.getSimpleName();

  private final MediationInterstitialAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mediationAdLoadCallback;
  private MediationInterstitialAdCallback interstitialAdCallback;
  private String slotId;

  @Nullable
  private AudienceNetworkAdsApi adsNetworkApi;

  public SnapInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    this.adConfiguration = adConfiguration;
    this.mediationAdLoadCallback = callback;
  }

  public void loadAd() {
    adsNetworkApi = AdKitAudienceAdsNetwork.getAdsNetwork();
    if (adsNetworkApi == null) {
      AdError error = new AdError(ERROR_SNAP_SDK_NOT_INITIALIZED,
          "Failed to load interstitial ad from Snap: Snap Audience Network failed to initialize.",
          ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Bundle serverParameters = adConfiguration.getServerParameters();
    slotId = serverParameters.getString(SLOT_ID_KEY);
    if (TextUtils.isEmpty(slotId)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load interstitial ad from Snap: Missing or invalid Ad Slot ID.", ERROR_DOMAIN);
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String bid = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bid)) {
      AdError error = new AdError(ERROR_INVALID_BID_RESPONSE,
          "Failed to load interstitial ad from Snap: Missing or invalid bid response.",
          ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    adsNetworkApi.setupListener(
        new SnapAdEventListener() {
          @Override
          public void onEvent(SnapAdKitEvent snapAdKitEvent, String slotId) {
            handleEvent(snapAdKitEvent);
          }
        });

    LoadAdConfig loadAdConfig =
        new LoadAdConfigBuilder().withPublisherSlotId(slotId).withBid(bid).build();
    adsNetworkApi.loadInterstitial(loadAdConfig);
  }

  @Override
  public void showAd(@NonNull Context context) {
    adsNetworkApi.playAd(new SnapAdKitSlot(slotId, AdKitSlotType.INTERSTITIAL));
  }

  // TODO: This method is only called once and should be inlined.
  private void handleEvent(SnapAdKitEvent snapAdKitEvent) {
    if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
      interstitialAdCallback = mediationAdLoadCallback.onSuccess(SnapInterstitialAd.this);
      return;
    }

    if (snapAdKitEvent instanceof SnapAdLoadFailed) {
      SnapAdLoadFailed snapAdLoadFailedEvent = (SnapAdLoadFailed) snapAdKitEvent;
      String errorMessage = String
          .format("Snap SDK returned a interstitial ad load failed event with message: %s",
              snapAdLoadFailedEvent.getThrowable().getMessage());
      AdError error = new AdError(ERROR_SNAP_SDK_LOAD_FAILURE, errorMessage, ERROR_DOMAIN);
      Log.i(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    if (snapAdKitEvent instanceof SnapAdVisible) {
      if (interstitialAdCallback != null) {
        interstitialAdCallback.onAdOpened();
      }
      return;
    }

    if (snapAdKitEvent instanceof SnapAdClicked) {
      if (interstitialAdCallback != null) {
        interstitialAdCallback.reportAdClicked();
      }
      return;
    }

    if (snapAdKitEvent instanceof SnapAdImpressionHappened) {
      if (interstitialAdCallback != null) {
        interstitialAdCallback.reportAdImpression();
      }
      return;
    }

    if (snapAdKitEvent instanceof SnapAdDismissed) {
      if (interstitialAdCallback != null) {
        interstitialAdCallback.onAdClosed();
      }
    }
  }
}
