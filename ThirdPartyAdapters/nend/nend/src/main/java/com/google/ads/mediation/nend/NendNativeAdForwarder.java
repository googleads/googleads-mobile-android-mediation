package com.google.ads.mediation.nend;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import java.lang.ref.WeakReference;
import net.nend.android.NendAdNativeClient;

/** The {@link NendNativeAdForwarder} to load and show Nend native ads, Nend native video ads. */
class NendNativeAdForwarder {

  private static final String TAG = NendNativeAdForwarder.class.getSimpleName();

  static final String KEY_NATIVE_ADS_FORMAT_TYPE = "key_native_ads_format_type";

  /** The adapter owning this instance. */
  private NendMediationAdapter adapter;

  /** The native ad listener from the Google Mobile Ads SDK. */
  private MediationNativeListener mediationNativeListener;

  /** Weak reference to the {@link Context} used by Nend native ads. */
  private WeakReference<Context> contextWeakReference;

  /** Nend ad loader for native image and text ads. */
  private NativeAdLoader normalAdLoader;

  /** Nend ad loader for native video ads. */
  private NativeVideoAdLoader videoAdLoader;

  /** Nend native ad mapper for unified native ads. */
  private NendUnifiedNativeAdMapper unifiedNativeAdMapper;

  NendNativeAdForwarder(NendMediationAdapter adapter) {
    this.adapter = adapter;
  }

  void adLoaded() {
    if (canInvokeListenerEvent()) {
      mediationNativeListener.onAdLoaded(adapter, unifiedNativeAdMapper);
    }
  }

  void failedToLoad(int nendErrorCode) {
    int errorCode = ErrorUtil.convertErrorCodeFromNendVideoToAdMob(nendErrorCode);
    Log.w(TAG, "Failed to request ad from Nend, Error Code: " + errorCode);
    if (canInvokeListenerEvent()) {
      mediationNativeListener.onAdFailedToLoad(adapter, errorCode);
    }
  }

  void adImpression() {
    if (canInvokeListenerEvent()) {
      mediationNativeListener.onAdImpression(adapter);
    }
  }

  void adClicked() {
    if (canInvokeListenerEvent()) {
      mediationNativeListener.onAdClicked(adapter);
    }
  }

  void adOpened() {
    if (canInvokeListenerEvent()) {
      mediationNativeListener.onAdOpened(adapter);
    }
  }

  void adClosed() {
    if (canInvokeListenerEvent()) {
      mediationNativeListener.onAdClosed(adapter);
    }
  }

  void leftApplication() {
    if (canInvokeListenerEvent()) {
      mediationNativeListener.onAdLeftApplication(adapter);
    }
  }

  void endVideo() {
    if (canInvokeListenerEvent()) {
      mediationNativeListener.onVideoEnd(adapter);
    }
  }

  void onResume() {
    // Do nothing here
  }

  void onPause() {
    // Do nothing here
  }

  void onDestroy() {
    mediationNativeListener = null;
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

  void requestNativeAd(
      Context context,
      MediationNativeListener mediationNativeListener,
      Bundle serverParameters,
      NativeMediationAdRequest nativeMediationAdRequest,
      Bundle mediationExtras) {
    if (!nativeMediationAdRequest.isUnifiedNativeAdRequested()) {
      Log.e(TAG, "Failed to load ad. Request must be for unified native ads.");
      if (canInvokeListenerEvent()) {
        mediationNativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    AdUnitMapper mapper = AdUnitMapper.createAdUnitMapper(serverParameters);
    if (mapper == null) {
      if (canInvokeListenerEvent()) {
        mediationNativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }
    contextWeakReference = new WeakReference<>(context);
    this.mediationNativeListener = mediationNativeListener;

    if (mediationExtras != null
        && NendMediationAdapter.FormatType.TYPE_VIDEO
            == mediationExtras.getSerializable(KEY_NATIVE_ADS_FORMAT_TYPE)) {
      videoAdLoader =
          new NativeVideoAdLoader(this, mapper, nativeMediationAdRequest, mediationExtras);
      videoAdLoader.loadAd();
    } else {
      normalAdLoader =
          new NativeAdLoader(
              this,
              new NendAdNativeClient(context, mapper.spotId, mapper.apiKey),
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
    return (mediationNativeListener != null && adapter != null);
  }
}
