package com.google.ads.mediation.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_PRESENTATION_AD_NOT_LOADED;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_PRESENTATION_AD_SHOW;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createAdapterError;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.ads.mediation.adcolony.AdColonyRewardListener.AdColonyRewardListenerExtended;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.jirbo.adcolony.AdColonyManager;
import com.jirbo.adcolony.AdColonyManager.InitializationListener;
import java.util.ArrayList;

public class AdColonyRewardedRenderer extends AdColonyInterstitialListener implements MediationRewardedAd, AdColonyRewardListenerExtended {

  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private final MediationRewardedAdConfiguration adConfiguration;
  private final AdColonyAdOptions adOptions;
  private MediationRewardedAdCallback mRewardedAdCallback;
  private AdColonyInterstitial adColonyInterstitial;
  private String requestedZone;

  public AdColonyRewardedRenderer(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback
  ) {
    this.mAdLoadCallback = callback;
    this.adConfiguration = adConfiguration;
    this.adOptions = AdColonyManager.getInstance().getAdOptionsFromAdConfig(adConfiguration);
  }

  public void render() {
    // Either configures the AdColony SDK if it has not yet been initialized, or short circuits to
    // the initialization success call if not needed to lead directly to the ad request.
    // Additionally validates zones in adConfiguration.
    AdColonyManager.getInstance().configureAdColony(adConfiguration,
        new InitializationListener() {
          @Override
          public void onInitializeSuccess() {
            ArrayList<String> listFromServerParams =
                AdColonyManager.getInstance().parseZoneList(adConfiguration.getServerParameters());
            requestedZone = AdColonyManager
                .getInstance()
                .getZoneFromRequest(listFromServerParams, adConfiguration.getMediationExtras());
            AdColony.requestInterstitial(requestedZone, AdColonyRewardedRenderer.this, adOptions);
          }

          @Override
          public void onInitializeFailed(@NonNull AdError error) {
            Log.w(TAG, error.getMessage());
            mAdLoadCallback.onFailure(error);
          }
        });
  }

  @Override
  public void showAd(@NonNull Context context) {
    AdError error = null;
    if (adColonyInterstitial == null) {
      error = createAdapterError(ERROR_PRESENTATION_AD_NOT_LOADED, "No ad to show.");
    } else if (!adColonyInterstitial.show()) {
      error = createAdapterError(ERROR_PRESENTATION_AD_SHOW, "Ad show failed.");
    }

    if (error == null) {
      AdColonyRewardListener.getInstance().addListener(this);
    } else {
      Log.w(TAG, error.getMessage());
      mRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
    this.adColonyInterstitial = adColonyInterstitial;
    mRewardedAdCallback = mAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onRequestNotFilled(AdColonyZone zone) {
    AdError error = createSdkError();
    Log.w(TAG, error.getMessage());
    mAdLoadCallback.onFailure(error);
  }

  @Override
  public void onExpiring(AdColonyInterstitial ad) {
    // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
    this.adColonyInterstitial = null;
    AdColony.requestInterstitial(ad.getZoneID(), this, adOptions);
  }

  @Override
  public void onClicked(AdColonyInterstitial ad) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onOpened(AdColonyInterstitial ad) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdOpened();
      mRewardedAdCallback.onVideoStart();
      mRewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onLeftApplication(AdColonyInterstitial ad) {
    // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
  }

  @Override
  public void onClosed(AdColonyInterstitial ad) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onIAPEvent(AdColonyInterstitial ad, String productId, int engagementType) {
    // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
  }

  @Override
  public void onReward(com.adcolony.sdk.AdColonyReward adColonyReward) {
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
  public String getZoneId() {
    return requestedZone;
  }
}
