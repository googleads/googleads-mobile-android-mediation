package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_AD_NOT_READY_TO_SHOW;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.YAHOO_MOBILE_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.initializeYahooSDK;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.interstitialplacement.InterstitialAd;
import com.yahoo.ads.interstitialplacement.InterstitialAd.InterstitialAdListener;
import com.yahoo.ads.interstitialplacement.InterstitialPlacementConfig;
import com.yahoo.ads.utils.ThreadUtils;
import java.lang.ref.WeakReference;
import java.util.Map;

final class YahooInterstitialRenderer implements InterstitialAdListener {

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

  public void render(@NonNull Context context, @NonNull MediationInterstitialListener listener,
      @NonNull MediationAdRequest mediationAdRequest, @NonNull Bundle serverParameters,
      @Nullable Bundle mediationExtras) {
    interstitialListener = listener;
    String siteId = YahooAdapterUtils.getSiteId(serverParameters, mediationExtras);
    MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
    if (TextUtils.isEmpty(siteId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Site ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      if (interstitialListener != null && adapter != null) {
        interstitialListener.onAdFailedToLoad(adapter, parameterError);
      }
      return;
    }

    AdError initializationError = initializeYahooSDK(context, siteId);
    if (initializationError != null) {
      Log.w(TAG, initializationError.toString());
      if (interstitialListener != null && adapter != null) {
        interstitialListener.onAdFailedToLoad(adapter, initializationError);
      }
      return;
    }

    String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Placement ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      if (interstitialListener != null && adapter != null) {
        interstitialListener.onAdFailedToLoad(adapter, parameterError);
      }
      return;
    }

    YahooAdapterUtils.setCoppaValue(mediationAdRequest);

    InterstitialPlacementConfig placementConfig = new InterstitialPlacementConfig(placementId,
        YahooAdapterUtils.getRequestMetadata(mediationAdRequest));
    interstitialAd = new InterstitialAd(context, placementId,
        YahooInterstitialRenderer.this);
    interstitialAd.load(placementConfig);
  }

  void showInterstitial(@NonNull Context context) {
    if (interstitialAd == null) {
      AdError showError = new AdError(ERROR_AD_NOT_READY_TO_SHOW, "No ads ready to be shown.",
          ERROR_DOMAIN);
      Log.w(TAG, showError.toString());
      return;
    }

    interstitialAd.show(context);
  }

  void destroy() {
    if (interstitialAd != null) {
      interstitialAd.destroy();
    }
  }

  // region Yahoo InterstitialAdListener implementation.

  @Override
  public void onLoaded(final InterstitialAd interstitialAd) {
    this.interstitialAd = interstitialAd;
    Log.i(TAG, "Yahoo Mobile SDK loaded an interstitial ad successfully.");
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
  public void onLoadFailed(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
    AdError loadError = new AdError(errorInfo.getErrorCode(), errorInfo.getDescription(),
        YAHOO_MOBILE_SDK_ERROR_DOMAIN);
    Log.w(TAG, loadError.toString());
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
        if (adapter != null && interstitialListener != null) {
          interstitialListener.onAdFailedToLoad(adapter, loadError);
        }
      }
    });
  }

  @Override
  public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
    AdError error = new AdError(errorInfo.getErrorCode(), errorInfo.getDescription(),
        YAHOO_MOBILE_SDK_ERROR_DOMAIN);
    Log.w(TAG, error.toString());

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
    Log.i(TAG, "Yahoo Mobile SDK showed an interstitial ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK closed an interstitial ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK recorded a click on an interstitial ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK has caused the user to leave the application "
        + "from an interstitial ad.");
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
    // No-op. Events not supported in adapter.
  }

  // endregion
}
