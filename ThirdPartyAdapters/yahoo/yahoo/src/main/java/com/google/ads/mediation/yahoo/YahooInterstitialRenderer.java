package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooAdapter.initializeSDK;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.YASAds;
import com.yahoo.ads.interstitialplacement.InterstitialAd;
import com.yahoo.ads.interstitialplacement.InterstitialPlacementConfig;
import com.yahoo.ads.utils.TextUtils;
import com.yahoo.ads.utils.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.Map;


final class YahooInterstitialRenderer implements InterstitialAd.InterstitialAdListener {

  /**
   * The mediation interstitial adapter weak reference.
   */
  private final WeakReference<MediationInterstitialAdapter> interstitialAdapterWeakRef;

  /**
   * The mediation interstitial listener used to report interstitial ad event callbacks.
   */
  private MediationInterstitialListener interstitialListener;

  /**
   * Yahoo interstitial ad.
   */
  private InterstitialAd interstitialAd;

  public YahooInterstitialRenderer(final MediationInterstitialAdapter adapter) {
    interstitialAdapterWeakRef = new WeakReference<>(adapter);
  }

  public void render(@NonNull final Context context, MediationInterstitialListener listener,
      MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle mediationExtras) {
    interstitialListener = listener;
    String siteId = YahooAdapterUtils.getSiteId(serverParameters, mediationExtras);
    MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
    if (TextUtils.isEmpty(siteId)) {
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, "Failed to request ad: siteID is null or empty.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (interstitialListener != null && adapter != null) {
        interstitialListener.onAdFailedToLoad(adapter, error);
      }
      return;
    }

    if (!initializeSDK(context, siteId)) {
      AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR, "Unable to initialize Yahoo Ads SDK.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (interstitialListener != null && adapter != null) {
        interstitialListener.onAdFailedToLoad(adapter, error);
      }
      return;
    }

    final String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, "Failed to request ad: placementID is null or empty.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      interstitialListener.onAdFailedToLoad(adapter, error);
      return;
    }

    YahooAdapterUtils.setCoppaValue(mediationAdRequest);
    YASAds.setLocationAccessMode((mediationAdRequest.getLocation() != null) ? YASAds.LocationAccessMode.IMPRECISE : YASAds.LocationAccessMode.DENIED);
    InterstitialAd interstitialAd = new InterstitialAd(context, placementId, this);
    interstitialAd.load(new InterstitialPlacementConfig(placementId, YahooAdapterUtils.getRequestMetadata(mediationAdRequest)));
  }

  @Override
  public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
    Log.e(TAG, "Yahoo Ads SDK interstitial error: " + errorInfo);
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
        if (adapter != null && interstitialListener != null) {
          interstitialListener.onAdOpened(adapter);
          interstitialListener.onAdClosed(adapter);
        }
      }
    });
  }

  @Override
  public void onShown(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Ads SDK interstitial shown.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
        if (adapter != null && interstitialListener != null) {
          interstitialListener.onAdOpened(adapter);
        }
      }
    });
  }

  @Override
  public void onClosed(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Ads SDK ad closed");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
        if (adapter != null && interstitialListener != null) {
          interstitialListener.onAdClosed(adapter);
        }
      }
    });
  }

  @Override
  public void onClicked(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Ads SDK interstitial clicked.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
        if (adapter != null && interstitialListener != null) {
          interstitialListener.onAdClicked(adapter);
        }
      }
    });
  }

  @Override
  public void onAdLeftApplication(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Ads SDK interstitial left application.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
        if (adapter != null && interstitialListener != null) {
          interstitialListener.onAdLeftApplication(adapter);
        }
      }
    });
  }

  @Override
  public void onEvent(final InterstitialAd interstitialAd, final String source,
      final String eventId, final Map<String, Object> arguments) {
    // no op.  events not supported in adapter
  }

  @Override
  public void onLoaded(final InterstitialAd interstitialAd) {

    this.interstitialAd = interstitialAd;
    Log.i(TAG, "Yahoo Ads SDK interstitial loaded.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
        if (adapter != null && interstitialListener != null) {
          interstitialListener.onAdLoaded(adapter);
        }
      }
    });
  }

  @Override
  public void onLoadFailed(InterstitialAd interstitialAd, ErrorInfo errorInfo) {
    final AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR, "Yahoo Ads SDK interstitial request failed.", ERROR_DOMAIN);
    Log.w(TAG, error.getMessage());
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

        if (adapter != null && interstitialListener != null) {
          interstitialListener.onAdFailedToLoad(adapter, error);
        }
      }
    });
  }

  void showInterstitial(@NonNull Context context) {
    if (interstitialAd == null) {
      Log.e(TAG, "Failed to show: No ads to show.");
      return;
    }

    interstitialAd.show(context);
  }

  void destroy() {
    if (interstitialAd != null) {
      interstitialAd.destroy();
    }
  }
}
