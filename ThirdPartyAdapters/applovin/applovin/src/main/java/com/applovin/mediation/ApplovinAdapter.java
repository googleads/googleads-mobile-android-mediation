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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The {@link ApplovinAdapter} class is used to load AppLovin Banner, interstitial & rewarded-based
 * video ads and to mediate the callbacks between the AppLovin SDK and the Google Mobile Ads SDK.
 */
public class ApplovinAdapter extends AppLovinMediationAdapter
    implements MediationBannerAdapter, MediationInterstitialAdapter,
    OnContextChangedListener, MediationRewardedAd {

  private static final boolean LOGGING_ENABLED = true;

  // Interstitial globals.
  private static final HashMap<String, Queue<AppLovinAd>> INTERSTITIAL_AD_QUEUES =
      new HashMap<>();
  private static final Object INTERSTITIAL_AD_QUEUES_LOCK = new Object();

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

  //region MediationInterstitialAdapter implementation.
  @Override
  public void requestInterstitialAd(Context context,
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

    // Store parent objects.
    mSdk = AppLovinUtils.retrieveSdk(serverParameters, context);
    mContext = context;
    mNetworkExtras = networkExtras;
    mMediationInterstitialListener = interstitialListener;

    mZoneId = AppLovinUtils.retrieveZoneId(serverParameters);

    log(DEBUG, "Requesting interstitial for zone: " + mZoneId);

    // Create Ad Load listener.
    final AppLovinAdLoadListener adLoadListener = new AppLovinAdLoadListener() {
      @Override
      public void adReceived(final AppLovinAd ad) {
        log(DEBUG, "Interstitial did load ad: " + ad.getAdIdNumber() + " for zone: "
            + mZoneId);

        synchronized (INTERSTITIAL_AD_QUEUES_LOCK) {
          Queue<AppLovinAd> preloadedAds = INTERSTITIAL_AD_QUEUES.get(mZoneId);
          if (preloadedAds == null) {
            preloadedAds = new LinkedList<>();
            INTERSTITIAL_AD_QUEUES.put(mZoneId, preloadedAds);
          }

          preloadedAds.offer(ad);

          AppLovinSdkUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mMediationInterstitialListener.onAdLoaded(ApplovinAdapter.this);
            }
          });
        }
      }

      @Override
      public void failedToReceiveAd(final int code) {
        String errorMessage = createSDKError(code);
        log(ERROR, errorMessage);
        AppLovinSdkUtils.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mMediationInterstitialListener.onAdFailedToLoad(
                ApplovinAdapter.this, code);
          }
        });
      }
    };

    synchronized (INTERSTITIAL_AD_QUEUES_LOCK) {
      final Queue<AppLovinAd> queue = INTERSTITIAL_AD_QUEUES.get(mZoneId);
      if (queue == null || (queue != null && queue.isEmpty())) {
        // If we don't already have enqueued ads, fetch from SDK.

        if (!TextUtils.isEmpty(mZoneId)) {
          mSdk.getAdService().loadNextAdForZoneId(mZoneId, adLoadListener);
        } else {
          mSdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, adLoadListener);
        }
      } else {
        log(DEBUG, "Enqueued interstitial found. Finishing load...");

        AppLovinSdkUtils.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mMediationInterstitialListener.onAdLoaded(ApplovinAdapter.this);
          }
        });
      }
    }
  }

  @Override
  public void showInterstitial() {
    synchronized (INTERSTITIAL_AD_QUEUES_LOCK) {
      // Update mute state.
      mSdk.getSettings().setMuted(AppLovinUtils.shouldMuteAudio(mNetworkExtras));

      final Queue<AppLovinAd> queue = INTERSTITIAL_AD_QUEUES.get(mZoneId);
      final AppLovinAd dequeuedAd = (queue != null) ? queue.poll() : null;

      final AppLovinInterstitialAdDialog interstitialAd =
          AppLovinInterstitialAd.create(mSdk, mContext);

      final AppLovinInterstitialAdListener listener =
          new AppLovinInterstitialAdListener(this, mMediationInterstitialListener);
      interstitialAd.setAdDisplayListener(listener);
      interstitialAd.setAdClickListener(listener);
      interstitialAd.setAdVideoPlaybackListener(listener);

      if (dequeuedAd != null) {
        log(DEBUG, "Showing interstitial for zone: " + mZoneId);
        interstitialAd.showAndRender(dequeuedAd);
      } else {
        log(DEBUG, "Attempting to show interstitial before one was loaded");

        // Check if we have a default zone interstitial available.
        if (TextUtils.isEmpty(mZoneId) && interstitialAd.isAdReadyToDisplay()) {
          log(DEBUG, "Showing interstitial preloaded by SDK");
          interstitialAd.show();
        }
        // TODO: Show ad for zone identifier if exists
        else {
          mMediationInterstitialListener.onAdOpened(this);
          mMediationInterstitialListener.onAdClosed(this);
        }
      }
    }
  }
  //endregion

  //region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(Context context,
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

    log(DEBUG, "Requesting banner of size " + adSize + " for zone: " + mZoneId);

    // Convert requested size to AppLovin Ad Size.
    final AppLovinAdSize appLovinAdSize = AppLovinUtils
        .appLovinAdSizeFromAdMobAdSize(context, adSize);
    if (appLovinAdSize != null) {
      mAdView = new AppLovinAdView(mSdk, appLovinAdSize, context);

      final AppLovinBannerAdListener listener = new AppLovinBannerAdListener(
          mZoneId, mAdView, this, mediationBannerListener);
      mAdView.setAdDisplayListener(listener);
      mAdView.setAdClickListener(listener);
      mAdView.setAdViewEventListener(listener);

      if (!TextUtils.isEmpty(mZoneId)) {
        mSdk.getAdService().loadNextAdForZoneId(mZoneId, listener);
      } else {
        mSdk.getAdService().loadNextAd(appLovinAdSize, listener);
      }
    } else {
      String errorMessage = createAdapterError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to request banner with unsupported size");
      log(ERROR, errorMessage);
      if (mediationBannerListener != null) {
        AppLovinSdkUtils.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            mediationBannerListener.onAdFailedToLoad(
                ApplovinAdapter.this, ERROR_BANNER_SIZE_MISMATCH);
          }
        });
      }
    }
  }

  @Override
  public View getBannerView() {
    return mAdView;
  }
  //endregion

  //region MediationAdapter.
  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  @Override
  public void onDestroy() {
  }
  //endregion

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
}
