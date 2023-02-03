package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooMediationAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.initializeSDK;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.nativeplacement.NativeAd;
import com.yahoo.ads.nativeplacement.NativeAd.NativeAdListener;
import com.yahoo.ads.nativeplacement.NativePlacementConfig;
import com.yahoo.ads.utils.TextUtils;
import com.yahoo.ads.utils.ThreadUtils;
import com.yahoo.ads.yahoonativecontroller.NativeComponent;
import java.lang.ref.WeakReference;
import java.util.Map;

final class YahooNativeRenderer implements NativeAdListener {

  /**
   * The mediation native adapter weak reference.
   */
  private final WeakReference<MediationNativeAdapter> nativeAdapterWeakRef;

  /**
   * The mediation native listener used to report native ad event callbacks.
   */
  private MediationNativeListener nativeListener;

  /**
   * The Context.
   */
  private Context context;
  /**
   * Yahoo native ad.
   */
  private NativeAd nativeAd;

  public YahooNativeRenderer(MediationNativeAdapter adapter) {
    this.nativeAdapterWeakRef = new WeakReference<>(adapter);
  }

  public void render(@NonNull Context context, @NonNull MediationNativeListener listener,
      @NonNull Bundle serverParameters, @NonNull NativeMediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {
    nativeListener = listener;
    this.context = context;
    String siteId = YahooAdapterUtils.getSiteId(serverParameters, mediationExtras);
    MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

    if (TextUtils.isEmpty(siteId)) {
      Log.e(TAG, "Failed to request ad: siteID is null.");
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
        return;
      }
    }

    if (!initializeSDK(context, siteId)) {
      Log.e(TAG, "Unable to initialize the Yahoo Mobile SDK.");
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
      }
      return;
    }

    String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      Log.e(TAG, "Failed to request ad: placementID is null or empty.");
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    YahooAdapterUtils.setCoppaValue(mediationAdRequest);

    String[] adTypes = new String[]{"100", "simpleImage"};
    NativePlacementConfig placementConfig = new NativePlacementConfig(placementId,
        YahooAdapterUtils.getRequestMetadata(mediationAdRequest), adTypes);
    placementConfig.skipAssets = true;
    nativeAd = new NativeAd(this.context, placementId, YahooNativeRenderer.this);
    nativeAd.load(placementConfig);
  }

  void destroy() {
    if (nativeAd != null) {
      nativeAd.destroy();
    }
  }

  // region Yahoo NativeAdListener implementation.

  @Override
  public void onLoaded(final NativeAd nativeAd) {
    this.nativeAd = nativeAd;
    Log.i(TAG, "Yahoo Mobile SDK loaded a native ad successfully.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {

        final MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
        final AdapterUnifiedNativeAdMapper mapper =
            new AdapterUnifiedNativeAdMapper(context, nativeAd);

        mapper.loadResources(new AdapterUnifiedNativeAdMapper.LoadListener() {
          @Override
          public void onLoadComplete() {
            ThreadUtils.postOnUiThread(new Runnable() {
              @Override
              public void run() {
                nativeListener.onAdLoaded(adapter, mapper);
              }
            });
          }

          @Override
          public void onLoadError() {
            ThreadUtils.postOnUiThread(new Runnable() {
              @Override
              public void run() {
                nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
              }
            });
          }
        });
      }
    });
  }

  @Override
  public void onLoadFailed(final NativeAd nativeAd, final ErrorInfo errorInfo) {
    Log.w(TAG, "Yahoo Mobile SDK failed to request a native ad with with error code: "
        + errorInfo.getErrorCode() + ", message: " + errorInfo.getDescription());
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
        if (nativeListener != null && adapter != null) {
          nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_MEDIATION_NO_FILL);
        }
      }
    });
  }

  @Override
  public void onError(final NativeAd nativeAd, final ErrorInfo errorInfo) {
    // This error callback is used if the native ad is loaded successfully, but an error
    // occurs while trying to display a component
    Log.w(TAG, "Yahoo Mobile SDK returned an error for native ad: " + errorInfo);
  }

  @Override
  public void onClosed(final NativeAd nativeAd) {
    Log.i(TAG, "Yahoo Mobile SDK closed a native ad.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
        if (nativeListener != null && adapter != null) {
          nativeListener.onAdClosed(adapter);
        }
      }
    });
  }

  @Override
  public void onClicked(final NativeAd nativeAd, final NativeComponent component) {
    Log.i(TAG, "Yahoo Mobile SDK recorded a click on a native ad.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
        if (nativeListener != null && adapter != null) {
          nativeListener.onAdOpened(adapter);
          nativeListener.onAdClicked(adapter);
        }
      }
    });
  }

  @Override
  public void onAdLeftApplication(final NativeAd nativeAd) {
    Log.i(TAG, "Yahoo Mobile SDK has caused the user to leave the application from a native ad.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
        if (nativeListener != null && adapter != null) {
          nativeListener.onAdLeftApplication(adapter);
        }
      }
    });
  }

  @Override
  public void onEvent(final NativeAd nativeAd, final String source, final String eventId,
      final Map<String, Object> arguments) {
    // No-op. Events not supported in adapter.
  }

  // endregion
}
