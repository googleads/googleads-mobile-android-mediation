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
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.YASAds;
import com.yahoo.ads.nativeplacement.NativeAd;
import com.yahoo.ads.nativeplacement.NativePlacementConfig;
import com.yahoo.ads.utils.TextUtils;
import com.yahoo.ads.utils.ThreadUtils;
import com.yahoo.ads.yahoonativecontroller.NativeComponent;

import java.lang.ref.WeakReference;
import java.util.Map;

final class YahooNativeRenderer implements NativeAd.NativeAdListener {

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

  public void render(@NonNull final Context context, MediationNativeListener listener,
      Bundle serverParameters, NativeMediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    nativeListener = listener;
    this.context = context;
    String siteId = YahooAdapterUtils.getSiteId(serverParameters, mediationExtras);
    MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

    if (TextUtils.isEmpty(siteId)) {
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, "Failed to request ad: siteID is null.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, error);
        return;
      }
    }

    if (!initializeSDK(context, siteId)) {
      AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR, "Unable to initialize Yahoo Ads SDK.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, error);
      }
      return;
    }

    final String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, "Failed to request ad: placementID is null or empty.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, error);
      }
      return;
    }

    YahooAdapterUtils.setCoppaValue(mediationAdRequest);
    YASAds.setLocationAccessMode((mediationAdRequest.getLocation() != null) ? YASAds.LocationAccessMode.IMPRECISE : YASAds.LocationAccessMode.DENIED);
    String[] adTypes = new String[] {"100", "simpleImage"};
    NativeAd nativeAd = new NativeAd(context, placementId, this);
    NativePlacementConfig nativePlacementConfig = new NativePlacementConfig(placementId, YahooAdapterUtils.getRequestMetadata(mediationAdRequest), adTypes);
    nativePlacementConfig.skipAssets = true;
    nativeAd.load(nativePlacementConfig);
  }

  @Override
  public void onError(final NativeAd nativeAd, final ErrorInfo errorInfo) {
    // This error callback is used if the native ad is loaded successfully, but an error
    // occurs while trying to display a component
    Log.e(TAG, "Yahoo Ads SDK native ad error: " + errorInfo);
  }

  @Override
  public void onClosed(final NativeAd nativeAd) {
    Log.i(TAG, "Yahoo Ads SDK native ad closed.");
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
  public void onClicked(NativeAd nativeAd, NativeComponent nativeComponent) {
    Log.i(TAG, "Yahoo Ads SDK native ad clicked.");
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
    Log.i(TAG, "Yahoo Ads SDK native ad left application.");
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

    // no op.  events not supported in adapter
  }

  public void onLoaded(final NativeAd nativeAd) {
    this.nativeAd = nativeAd;
    Log.i(TAG, "Yahoo Ads SDK native ad request succeeded: Loading succeeded.");
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
                AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR, "Failed to load native ad.", ERROR_DOMAIN);
                nativeListener.onAdFailedToLoad(adapter, error);
              }
            });
          }
        });
      }
    });
  }

  @Override
  public void onLoadFailed(NativeAd nativeAd, ErrorInfo errorInfo) {
    AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, "Failed to load native ad.", ERROR_DOMAIN);
    Log.e(TAG, error.getMessage());
    final MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
    if (nativeListener != null && adapter != null) {
      nativeListener.onAdFailedToLoad(adapter, error);
    }
  }

  void destroy() {
    if (nativeAd != null) {
      nativeAd.destroy();
    }
  }
}
