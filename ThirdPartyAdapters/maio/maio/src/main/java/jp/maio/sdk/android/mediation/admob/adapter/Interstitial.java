package jp.maio.sdk.android.mediation.admob.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.maio.MaioAdsManagerListener;
import com.google.ads.mediation.maio.MaioMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;

/**
 * maio mediation adapter for AdMob Interstitial videos.
 */
public class Interstitial extends MaioMediationAdapter
    implements MediationInterstitialAdapter, MaioAdsManagerListener {

  private MediationInterstitialListener mMediationInterstitialListener;

  // region MediationInterstitialAdapter implementation
  @Override
  public void onDestroy() {
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  @Override
  public void requestInterstitialAd(
      Context context,
      MediationInterstitialListener listener,
      Bundle serverParameters,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    this.mMediationInterstitialListener = listener;
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Maio SDK requires an Activity context to load ads.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (this.mMediationInterstitialListener != null) {
        this.mMediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
      }
      return;
    }

    this.mMediaID = serverParameters.getString(MaioAdsManager.KEY_MEDIA_ID);
    if (TextUtils.isEmpty(mMediaID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid Media ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (this.mMediationInterstitialListener != null) {
        this.mMediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
      }
      return;
    }

    this.mZoneID = serverParameters.getString(MaioAdsManager.KEY_ZONE_ID);
    if (TextUtils.isEmpty(mZoneID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid Zone ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (this.mMediationInterstitialListener != null) {
        this.mMediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
      }
      return;
    }

    MaioAds.setAdTestMode(mediationAdRequest.isTesting());
    MaioAdsManager.getManager(mMediaID)
        .initialize(
            (Activity) context,
            new MaioAdsManager.InitializationListener() {
              @Override
              public void onMaioInitialized() {
                MaioAdsManager.getManager(mMediaID).loadAd(mZoneID, Interstitial.this);
              }
            });
  }

  @Override
  public void showInterstitial() {
    MaioAdsManager.getManager(mMediaID).showAd(mZoneID, Interstitial.this);
  }
  // endregion

  // region MaioAdsManagerListener implementation
  @Override
  public void onChangedCanShow(String zoneId, boolean isAvailable) {
    if (this.mMediationInterstitialListener != null && isAvailable) {
      this.mMediationInterstitialListener.onAdLoaded(Interstitial.this);
    }
  }

  @Override
  public void onFailed(FailNotificationReason reason, String zoneId) {
    AdError error = MaioMediationAdapter.getAdError(reason);
    Log.w(TAG, error.getMessage());
    if (this.mMediationInterstitialListener != null) {
      this.mMediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
    }
  }

  @Override
  public void onAdFailedToShow(AdError error) {
    Log.w(TAG, error.getMessage());
    if (this.mMediationInterstitialListener != null) {
      this.mMediationInterstitialListener.onAdOpened(Interstitial.this);
      this.mMediationInterstitialListener.onAdClosed(Interstitial.this);
    }
  }

  @Override
  public void onAdFailedToLoad(AdError error) {
    Log.w(TAG, error.getMessage());
    if (this.mMediationInterstitialListener != null) {
      this.mMediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
    }
  }

  @Override
  public void onOpenAd(String zoneId) {
    if (this.mMediationInterstitialListener != null) {
      this.mMediationInterstitialListener.onAdOpened(Interstitial.this);
    }
  }

  @Override
  public void onStartedAd(String zoneId) {
    // No relevant Interstitial Ad event to forward to the Google Mobile Ads SDK.
  }

  @Override
  public void onClickedAd(String zoneId) {
    if (this.mMediationInterstitialListener != null) {
      this.mMediationInterstitialListener.onAdClicked(Interstitial.this);
      this.mMediationInterstitialListener.onAdLeftApplication(Interstitial.this);
    }
  }

  @Override
  public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
    // No relevant Interstitial Ad event to forward to the Google Mobile Ads SDK.
  }

  @Override
  public void onClosedAd(String zoneId) {
    if (this.mMediationInterstitialListener != null) {
      this.mMediationInterstitialListener.onAdClosed(Interstitial.this);
    }
  }
  // endregion

}
