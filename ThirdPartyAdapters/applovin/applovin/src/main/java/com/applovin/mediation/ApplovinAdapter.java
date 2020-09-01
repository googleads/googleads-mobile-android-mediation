package com.applovin.mediation;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.ads.mediation.applovin.AppLovinMediationAdapter;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * The {@link ApplovinAdapter} class is used to load AppLovin Banner, interstitial & rewarded-based
 * video ads and to mediate the callbacks between the AppLovin SDK and the Google Mobile Ads SDK.
 */
public class ApplovinAdapter extends AppLovinMediationAdapter
    implements MediationBannerAdapter,
        MediationInterstitialAdapter,
        OnContextChangedListener,
        MediationRewardedAd {

  private static final boolean LOGGING_ENABLED = true;

  // Interstitial globals.
  private static final HashMap<String, WeakReference<ApplovinAdapter>> appLovinInterstitialAds =
      new HashMap<>();
  private AppLovinAd appLovinInterstitialAd;

  // Parent objects.
  private AppLovinSdk mSdk;
  private Context mContext;
  private Bundle mNetworkExtras;

  // Interstitial objects.
  private MediationInterstitialListener mMediationInterstitialListener;

  // Banner objects.
  private AppLovinAdView mAdView;

  // Controlled fields.
  private String mZoneId;

  // region MediationInterstitialAdapter implementation.
  @Override
  public void requestInterstitialAd(
      Context context,
      MediationInterstitialListener interstitialListener,
      Bundle serverParameters,
      MediationAdRequest mediationAdRequest,
      Bundle networkExtras) {

    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "AppLovin requires an Activity context to load ads.");
      log(ERROR, "Failed to load interstitial ad from AppLovin: " + adapterError);
      interstitialListener.onAdFailedToLoad(ApplovinAdapter.this, ERROR_CONTEXT_NOT_ACTIVITY);
      return;
    }

    mZoneId = AppLovinUtils.retrieveZoneId(serverParameters);
    if (appLovinInterstitialAds.containsKey(mZoneId)
        && appLovinInterstitialAds.get(mZoneId).get() != null) {
      String errorMessage =
          createAdapterError(
              ERROR_AD_ALREADY_REQUESTED,
              "Cannot load multiple interstitial ads with the same Zone ID. "
                  + "Display one ad before attempting to load another.");
      log(ERROR, errorMessage);
      interstitialListener.onAdFailedToLoad(ApplovinAdapter.this, ERROR_AD_ALREADY_REQUESTED);
      return;
    }
    appLovinInterstitialAds.put(mZoneId, new WeakReference<>(ApplovinAdapter.this));

    // Store parent objects.
    mSdk = AppLovinUtils.retrieveSdk(serverParameters, context);
    mContext = context;
    mNetworkExtras = networkExtras;
    mMediationInterstitialListener = interstitialListener;

    log(DEBUG, "Requesting interstitial for zone: " + mZoneId);

    // Create Ad Load listener.
    final AppLovinAdLoadListener adLoadListener =
        new AppLovinAdLoadListener() {
          @Override
          public void adReceived(final AppLovinAd ad) {
            log(DEBUG, "Interstitial did load ad: " + ad.getAdIdNumber() + " for zone: " + mZoneId);
            appLovinInterstitialAd = ad;

            AppLovinSdkUtils.runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    mMediationInterstitialListener.onAdLoaded(ApplovinAdapter.this);
                  }
                });
          }

          @Override
          public void failedToReceiveAd(final int code) {
            String errorMessage = createSDKError(code);
            log(ERROR, errorMessage);

            ApplovinAdapter.this.unregister();
            AppLovinSdkUtils.runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    mMediationInterstitialListener.onAdFailedToLoad(ApplovinAdapter.this, code);
                  }
                });
          }
        };

    if (!TextUtils.isEmpty(mZoneId)) {
      mSdk.getAdService().loadNextAdForZoneId(mZoneId, adLoadListener);
    } else {
      mSdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, adLoadListener);
    }
  }

  @Override
  public void showInterstitial() {
    // Update mute state.
    mSdk.getSettings().setMuted(AppLovinUtils.shouldMuteAudio(mNetworkExtras));

    final AppLovinInterstitialAdDialog interstitialAdDialog =
        AppLovinInterstitialAd.create(mSdk, mContext);

    final AppLovinInterstitialAdListener listener =
        new AppLovinInterstitialAdListener(ApplovinAdapter.this, mMediationInterstitialListener);
    interstitialAdDialog.setAdDisplayListener(listener);
    interstitialAdDialog.setAdClickListener(listener);
    interstitialAdDialog.setAdVideoPlaybackListener(listener);

    if (appLovinInterstitialAd == null) {
      log(DEBUG, "Attempting to show interstitial before one was loaded.");

      // Check if we have a default zone interstitial available.
      if (TextUtils.isEmpty(mZoneId) && interstitialAdDialog.isAdReadyToDisplay()) {
        log(DEBUG, "Showing interstitial preloaded by SDK.");
        interstitialAdDialog.show();
      }
      // TODO: Show ad for zone identifier if exists
      else {
        mMediationInterstitialListener.onAdOpened(this);
        mMediationInterstitialListener.onAdClosed(this);
      }
      return;
    }

    log(DEBUG, "Showing interstitial for zone: " + mZoneId);
    interstitialAdDialog.showAndRender(appLovinInterstitialAd);
  }
  // endregion

  // region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(
      Context context,
      final MediationBannerListener mediationBannerListener,
      Bundle serverParameters,
      AdSize adSize,
      MediationAdRequest mediationAdRequest,
      Bundle networkExtras) {

    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "AppLovin requires an Activity context to load ads.");
      log(ERROR, "Failed to load banner ad from AppLovin: " + adapterError);
      mediationBannerListener.onAdFailedToLoad(ApplovinAdapter.this, ERROR_CONTEXT_NOT_ACTIVITY);
      return;
    }

    // Store parent objects
    mSdk = AppLovinUtils.retrieveSdk(serverParameters, context);
    mZoneId = AppLovinUtils.retrieveZoneId(serverParameters);

    // Convert requested size to AppLovin Ad Size.
    final AppLovinAdSize appLovinAdSize =
        AppLovinUtils.appLovinAdSizeFromAdMobAdSize(context, adSize);
    if (appLovinAdSize == null) {
      String errorMessage =
          createAdapterError(
              ERROR_BANNER_SIZE_MISMATCH,
              "Failed to request banner with unsupported size: " + adSize.toString());
      log(ERROR, errorMessage);
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdFailedToLoad(ApplovinAdapter.this, ERROR_BANNER_SIZE_MISMATCH);
      }
    }

    log(DEBUG, "Requesting banner of size " + appLovinAdSize + " for zone: " + mZoneId);
    mAdView = new AppLovinAdView(mSdk, appLovinAdSize, context);

    final AppLovinBannerAdListener listener =
        new AppLovinBannerAdListener(mZoneId, mAdView, this, mediationBannerListener);
    mAdView.setAdDisplayListener(listener);
    mAdView.setAdClickListener(listener);
    mAdView.setAdViewEventListener(listener);

    if (!TextUtils.isEmpty(mZoneId)) {
      mSdk.getAdService().loadNextAdForZoneId(mZoneId, listener);
    } else {
      mSdk.getAdService().loadNextAd(appLovinAdSize, listener);
    }
  }

  @Override
  public View getBannerView() {
    return mAdView;
  }
  // endregion

  // region MediationAdapter.
  @Override
  public void onPause() {}

  @Override
  public void onResume() {}

  @Override
  public void onDestroy() {}
  // endregion

  // OnContextChangedListener Method.
  @Override
  public void onContextChanged(Context context) {
    if (context != null) {
      log(DEBUG, "Context changed: " + context);
      mContext = context;
    }
  }

  // Logging
  public static void log(int priority, final String message) {
    if (LOGGING_ENABLED) {
      Log.println(priority, "AppLovinAdapter", message);
    }
  }

  // Utilities
  void unregister() {
    if (!TextUtils.isEmpty(mZoneId)
        && appLovinInterstitialAds.containsKey(mZoneId)
        && ApplovinAdapter.this.equals(appLovinInterstitialAds.get(mZoneId).get())) {
      appLovinInterstitialAds.remove(mZoneId);
    }
  }
}
