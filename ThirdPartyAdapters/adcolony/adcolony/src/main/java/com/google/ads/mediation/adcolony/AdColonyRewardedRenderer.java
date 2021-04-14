package com.google.ads.mediation.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_AD_ALREADY_REQUESTED;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_PRESENTATION_AD_NOT_LOADED;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createAdapterError;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.jirbo.adcolony.AdColonyManager;
import com.jirbo.adcolony.AdColonyManager.InitializationListener;
import java.util.ArrayList;

public class AdColonyRewardedRenderer implements MediationRewardedAd {

  private MediationRewardedAdCallback mRewardedAdCallback;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private final MediationRewardedAdConfiguration adConfiguration;
  private AdColonyInterstitial mAdColonyInterstitial;

  public AdColonyRewardedRenderer(
          @NonNull MediationRewardedAdConfiguration adConfiguration,
          @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback
  ) {
    this.adConfiguration = adConfiguration;
    this.mAdLoadCallback = callback;
  }

  public void render() {
    ArrayList<String> listFromServerParams =
        AdColonyManager.getInstance().parseZoneList(adConfiguration.getServerParameters());
    final String requestedZone = AdColonyManager
        .getInstance()
        .getZoneFromRequest(listFromServerParams, adConfiguration.getMediationExtras());

    if (AdColonyRewardedEventForwarder.getInstance().isListenerAvailable(requestedZone)
            && adConfiguration.getBidResponse().isEmpty()) {
      AdError error = createAdapterError(ERROR_AD_ALREADY_REQUESTED,
              "Failed to load ad from AdColony: Only a maximum of one ad can be loaded per Zone ID.");
      Log.e(TAG, error.getMessage());
      mAdLoadCallback.onFailure(error);
      return;
    }

    // Either configures the AdColony SDK if it has not yet been initialized, or short circuits to
    // the initialization success call if not needed to lead directly to the ad request.
    AdColonyManager.getInstance().configureAdColony(adConfiguration,
        new InitializationListener() {
          @Override
          public void onInitializeSuccess() {
            // Cannot request an ad without a valid zone.
            if (TextUtils.isEmpty(requestedZone)) {
              AdError error = createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
                      "Missing or invalid Zone ID.");
              Log.e(TAG, error.getMessage());
              mAdLoadCallback.onFailure(error);
              return;
            }
            AdColonyAdOptions adOptions =
                AdColonyManager.getInstance().getAdOptionsFromAdConfig(adConfiguration);
            AdColony.setRewardListener(AdColonyRewardedEventForwarder.getInstance());
            AdColonyRewardedEventForwarder.getInstance()
                .addListener(requestedZone, AdColonyRewardedRenderer.this);
            AdColony
                .requestInterstitial(requestedZone, AdColonyRewardedEventForwarder.getInstance(),
                    adOptions);
          }

          @Override
          public void onInitializeFailed(@NonNull AdError error) {
            Log.w(TAG, error.getMessage());
            mAdLoadCallback.onFailure(error);
          }
        });
  }

  //region AdColony Rewarded Events
  void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
    mAdColonyInterstitial = adColonyInterstitial;
    mRewardedAdCallback = mAdLoadCallback.onSuccess(AdColonyRewardedRenderer.this);
  }

  void onRequestNotFilled(AdColonyZone zone) {
    AdError error = createSdkError();
    Log.w(TAG, error.getMessage());
    mAdLoadCallback.onFailure(error);
  }

  void onExpiring(AdColonyInterstitial ad) {
    // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
    mAdColonyInterstitial = null;
    AdColony.requestInterstitial(ad.getZoneID(), AdColonyRewardedEventForwarder.getInstance());
  }

  void onClicked(AdColonyInterstitial ad) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdClicked();
    }
  }

  void onOpened(AdColonyInterstitial ad) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdOpened();
      mRewardedAdCallback.onVideoStart();
      mRewardedAdCallback.reportAdImpression();
    }
  }

  void onLeftApplication(AdColonyInterstitial ad) {
    // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
  }

  void onClosed(AdColonyInterstitial ad) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdClosed();
    }
  }

  void onIAPEvent(AdColonyInterstitial ad, String product_id, int engagement_type) {
    // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
  }

  void onReward(com.adcolony.sdk.AdColonyReward adColonyReward) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onVideoComplete();

      if (adColonyReward.success()) {
        AdColonyReward reward = new AdColonyReward(adColonyReward.getRewardName(),
            adColonyReward.getRewardAmount());
        mRewardedAdCallback.onUserEarnedReward(reward);
      }
    }
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (mAdColonyInterstitial == null) {
      AdError error = createAdapterError(ERROR_PRESENTATION_AD_NOT_LOADED, "No ad to show.");
      Log.w(TAG, error.getMessage());
      mRewardedAdCallback.onAdFailedToShow(error);
      return;
    }

    if (AdColony.getRewardListener() != AdColonyRewardedEventForwarder.getInstance()) {
      Log.w(TAG, "AdColony's reward listener has been changed since load time. Setting the "
          + "listener back to the Google AdColony adapter to be able to detect rewarded events.");
      AdColony.setRewardListener(AdColonyRewardedEventForwarder.getInstance());
    }

    mAdColonyInterstitial.show();
  }
}
