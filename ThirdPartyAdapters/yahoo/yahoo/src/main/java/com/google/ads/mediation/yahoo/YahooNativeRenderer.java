package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooAdapter.initializeSDK;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.yahoo.ads.Component;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.YASAds;
import com.yahoo.ads.nativeplacement.NativeAd;
import com.yahoo.ads.nativeplacement.NativePlacementConfig;
import com.yahoo.ads.placementcache.UnifiedAdManager;
import com.yahoo.ads.utils.TextUtils;
import com.yahoo.ads.utils.ThreadUtils;
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
      Log.e(TAG, "Failed to request ad: siteID is null.");
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
        return;
      }
    }

    if (!initializeSDK(context, siteId)) {
      Log.e(TAG, "Unable to initialize Yahoo Ads SDK.");
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
      }
      return;
    }

    final String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      Log.e(TAG, "Failed to request ad: placementID is null or empty.");
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    YahooAdapterUtils.setCoppaValue(mediationAdRequest);
    YASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));
    String[] adTypes = new String[] {"100", "simpleImage"};
    NativePlacementConfig placementConfig = new NativePlacementConfig(placementId,
        YahooAdapterUtils.getRequestMetadata(mediationAdRequest), adTypes);
    NativeAdOptions options = mediationAdRequest.getNativeAdOptions();
    placementConfig.skipAssets = ((options != null) && options.shouldReturnUrlsForImageAssets());
    UnifiedAdManager.setPlacementConfig(placementId, placementConfig);
    UnifiedAdManager.fetchAds(context, placementId, new Function1<ErrorInfo, Unit>() {
        @Override
        public Unit invoke(final ErrorInfo errorInfo) {
        if (errorInfo != null) {
          onError(errorInfo);
        } else {
          onFetchComplete(placementId);
        }
        return null;
      }
      });
  }

  private void onFetchComplete(final String placementId) {
    final NativeAd nativeAd = new NativeAd(placementId, this);
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        ErrorInfo errorInfo = nativeAd.load();
        if (errorInfo != null) {
          onError(errorInfo);
        } else {
          onLoaded(nativeAd);
        }
      }
    });
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
  public void onClicked(final NativeAd nativeAd, final Component component) {
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
                nativeListener.onAdFailedToLoad(adapter,
                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
              }
            });
          }
        });
      }
    });
  }

  public void onError(final ErrorInfo errorInfo) {
    Log.i(TAG, "Yahoo Ads SDK Native Ad request failed (" + errorInfo.getErrorCode() + "): " +
        errorInfo.getDescription());
    final int errorCode;
    switch (errorInfo.getErrorCode()) {
      case YASAds.ERROR_AD_REQUEST_FAILED:
        errorCode = AdRequest.ERROR_CODE_INTERNAL_ERROR;
        break;
      case YASAds.ERROR_AD_REQUEST_TIMED_OUT:
        errorCode = AdRequest.ERROR_CODE_NETWORK_ERROR;
        break;
      default:
        errorCode = AdRequest.ERROR_CODE_NO_FILL;
    }
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
        if (nativeListener != null && adapter != null) {
          nativeListener.onAdFailedToLoad(adapter, errorCode);
        }
      }
    });
  }

  void destroy() {
    if (nativeAd != null) {
      nativeAd.destroy();
    }
  }
}
