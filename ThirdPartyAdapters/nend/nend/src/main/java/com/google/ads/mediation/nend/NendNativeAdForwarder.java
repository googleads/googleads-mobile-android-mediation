package com.google.ads.mediation.nend;

import static com.google.ads.mediation.nend.NendMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.nend.NendMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import java.lang.ref.WeakReference;
import net.nend.android.NendAdNativeClient;

/**
 * The {@link NendNativeAdForwarder} to load and show Nend native ads, Nend native video ads.
 */
class NendNativeAdForwarder {

  private static final String TAG = NendNativeAdForwarder.class.getSimpleName();

  static final String KEY_NATIVE_ADS_FORMAT_TYPE = "key_native_ads_format_type";

  /**
   * The adapter owning this instance.
   */
  private final NendMediationAdapter adapter;

  /**
   * The native ad listener from the Google Mobile Ads SDK.
   */
  private MediationNativeListener nativeListener;

  /**
   * Weak reference to the {@link Context} used by Nend native ads.
   */
  private WeakReference<Context> contextWeakReference;

  /**
   * Nend ad loader for native image and text ads.
   */
  private NativeAdLoader normalAdLoader;

  /**
   * Nend ad loader for native video ads.
   */
  private NativeVideoAdLoader videoAdLoader;

  /**
   * Nend native ad mapper for unified native ads.
   */
  private NendUnifiedNativeAdMapper unifiedNativeAdMapper;

  NendNativeAdForwarder(NendMediationAdapter adapter) {
    this.adapter = adapter;
  }

  void adLoaded() {
    if (canInvokeListenerEvent()) {
      nativeListener.onAdLoaded(adapter, unifiedNativeAdMapper);
    }
  }

  void failedToLoad(AdError error) {
    if (canInvokeListenerEvent()) {
      nativeListener.onAdFailedToLoad(adapter, error);
    }
  }

  void adImpression() {
    if (canInvokeListenerEvent()) {
      nativeListener.onAdImpression(adapter);
    }
  }

  void adClicked() {
    if (canInvokeListenerEvent()) {
      nativeListener.onAdClicked(adapter);
    }
  }

  void adOpened() {
    if (canInvokeListenerEvent()) {
      nativeListener.onAdOpened(adapter);
    }
  }

  void adClosed() {
    if (canInvokeListenerEvent()) {
      nativeListener.onAdClosed(adapter);
    }
  }

  void leftApplication() {
    if (canInvokeListenerEvent()) {
      nativeListener.onAdLeftApplication(adapter);
    }
  }

  void endVideo() {
    if (canInvokeListenerEvent()) {
      nativeListener.onVideoEnd(adapter);
    }
  }

  void onResume() {
    // Do nothing here
  }

  void onPause() {
    // Do nothing here
  }

  void onDestroy() {
    nativeListener = null;
    normalAdLoader = null;
    if (unifiedNativeAdMapper instanceof NendUnifiedNativeVideoAdMapper) {
      ((NendUnifiedNativeVideoAdMapper) unifiedNativeAdMapper).deactivate();
      unifiedNativeAdMapper = null;
    }
    if (videoAdLoader != null) {
      videoAdLoader.releaseLoader();
      videoAdLoader = null;
    }
    contextWeakReference = null;
  }

  void requestNativeAd(@NonNull Context context,
      @NonNull MediationNativeListener mediationNativeListener, @NonNull Bundle serverParameters,
      @NonNull NativeMediationAdRequest nativeMediationAdRequest,
      @Nullable Bundle mediationExtras) {
    String apiKey = serverParameters.getString(NendMediationAdapter.KEY_API_KEY);
    if (TextUtils.isEmpty(apiKey)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid API key.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationNativeListener.onAdFailedToLoad(adapter, error);
      return;
    }

    int spotID = Integer
        .parseInt(serverParameters.getString(NendMediationAdapter.KEY_SPOT_ID, "0"));
    if (spotID <= 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid spot ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationNativeListener.onAdFailedToLoad(adapter, error);
      return;
    }

    contextWeakReference = new WeakReference<>(context);
    nativeListener = mediationNativeListener;

    if (mediationExtras != null &&
        NendMediationAdapter.FormatType.TYPE_VIDEO == mediationExtras
            .getSerializable(KEY_NATIVE_ADS_FORMAT_TYPE)) {
      String userID = mediationExtras.getString(NendMediationAdapter.KEY_USER_ID, "");
      videoAdLoader = new NativeVideoAdLoader(NendNativeAdForwarder.this, spotID, apiKey,
          nativeMediationAdRequest, userID);
      videoAdLoader.loadAd();
    } else {
      normalAdLoader = new NativeAdLoader(NendNativeAdForwarder.this,
          new NendAdNativeClient(context, spotID, apiKey),
          nativeMediationAdRequest.getNativeAdOptions());
      normalAdLoader.loadAd();
    }
  }

  void setUnifiedNativeAdMapper(@Nullable NendUnifiedNativeAdMapper mapper) {
    unifiedNativeAdMapper = mapper;
  }

  @Nullable
  Context getContextFromWeakReference() {
    if (contextWeakReference == null) {
      return null;
    }
    return contextWeakReference.get();
  }

  private boolean canInvokeListenerEvent() {
    return (nativeListener != null && adapter != null);
  }
}
