package com.google.ads.mediation.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.ERROR_PRESENTATION_AD_NOT_LOADED;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createAdapterError;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.content.Context;
import android.util.Log;
import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.jirbo.adcolony.AdColonyManager;
import java.util.ArrayList;

public class AdColonyRewardedRenderer implements MediationRewardedAd {

  private MediationRewardedAdCallback mRewardedAdCallback;
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private MediationRewardedAdConfiguration adConfiguration;
  private AdColonyInterstitial mAdColonyInterstitial;

  public AdColonyRewardedRenderer(
      MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.mAdLoadCallback = callback;
  }

  public void render() {
    AdColonyAdOptions adOptions =
        AdColonyManager.getInstance().getAdOptionsFromAdConfig(adConfiguration);
    ArrayList<String> listFromServerParams =
        AdColonyManager.getInstance().parseZoneList(adConfiguration.getServerParameters());
    String requestedZone = AdColonyManager
        .getInstance()
        .getZoneFromRequest(listFromServerParams, adConfiguration.getMediationExtras());
    AdColonyRewardedEventForwarder.getInstance()
        .addListener(requestedZone, AdColonyRewardedRenderer.this);
    AdColony.requestInterstitial(requestedZone, AdColonyRewardedEventForwarder.getInstance(), adOptions);
  }

  //region AdColony Rewarded Events
  void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
    mAdColonyInterstitial = adColonyInterstitial;

    if (mAdLoadCallback != null) {
      mRewardedAdCallback = mAdLoadCallback.onSuccess(AdColonyRewardedRenderer.this);
    }
  }

  void onRequestNotFilled(AdColonyZone zone) {
    if (mAdLoadCallback != null) {
      String errorMessage = createSdkError();
      Log.w(TAG, errorMessage);
      mAdLoadCallback.onFailure(errorMessage);
    }
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
  public void showAd(Context context) {
    if (mAdColonyInterstitial == null) {
      String errorMessage = createAdapterError(ERROR_PRESENTATION_AD_NOT_LOADED, "No ad to show.");
      Log.w(TAG, errorMessage);
      mRewardedAdCallback.onAdFailedToShow(errorMessage);
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
