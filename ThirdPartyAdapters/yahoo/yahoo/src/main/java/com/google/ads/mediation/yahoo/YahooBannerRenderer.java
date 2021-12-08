package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooAdapter.initializeSDK;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.YASAds;
import com.yahoo.ads.inlineplacement.InlineAdView;
import com.yahoo.ads.inlineplacement.InlinePlacementConfig;
import com.yahoo.ads.placementcache.UnifiedAdManager;
import com.yahoo.ads.utils.TextUtils;
import com.yahoo.ads.utils.ThreadUtils;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;

final class YahooBannerRenderer implements InlineAdView.InlineAdListener {

  /**
   * The ad view's container.
   */
  private LinearLayout adContainer;

  /**
   * The mediation banner adapter weak reference.
   */
  private final WeakReference<MediationBannerAdapter> bannerAdapterWeakRef;

  /**
   * The mediation banner listener used to report banner ad event callbacks.
   */
  private MediationBannerListener bannerListener;

  /**
   * Yahoo ad view.
   */
  private InlineAdView inlineAdView;

  public YahooBannerRenderer(MediationBannerAdapter adapter) {
    bannerAdapterWeakRef = new WeakReference<>(adapter);
  }

  public void render(@NonNull final Context context, MediationBannerListener listener,
      Bundle serverParameters, AdSize adSize, MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    bannerListener = listener;
    String siteId = YahooAdapterUtils.getSiteId(serverParameters, mediationExtras);
    MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
    if (TextUtils.isEmpty(siteId)) {
      Log.e(TAG, "Failed to request ad: siteID is null or empty.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }
    if (!initializeSDK(context, siteId)) {
      Log.e(TAG, "Unable to initialize Yahoo Ads SDK.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter,
            AdRequest.ERROR_CODE_INTERNAL_ERROR);
      }
      return;
    }

    final String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      Log.e(TAG, "Failed to request ad: placementID is null or empty.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    if (adSize == null) {
      Log.w(TAG, "Fail to request banner ad, adSize is null.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    AdSize normalizedSize = YahooAdapterUtils.normalizeSize(context, adSize);
    if (normalizedSize == null) {
      Log.w(TAG,
          "The input ad size " + adSize.toString() + " is not currently supported.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }
    adContainer = new LinearLayout(context);
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.gravity = Gravity.CENTER_HORIZONTAL;
    adContainer.setLayoutParams(lp);
    com.yahoo.ads.inlineplacement.AdSize yahooAdSize =
        new com.yahoo.ads.inlineplacement.AdSize(normalizedSize.getWidth(),
            normalizedSize.getHeight());
    YASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));
    YahooAdapterUtils.setCoppaValue(mediationAdRequest);
    InlinePlacementConfig placementConfig = new InlinePlacementConfig(placementId,
        YahooAdapterUtils.getRequestMetadata(mediationAdRequest), Collections.singletonList(yahooAdSize));
    UnifiedAdManager.setPlacementConfig(placementId, placementConfig);
    UnifiedAdManager.fetchAds(context, placementId, new Function1<ErrorInfo, Unit>() {
        @Override
        public Unit invoke(final ErrorInfo errorInfo) {
        if (errorInfo != null) {
          onError(errorInfo);
        } else {
          onFetchComplete(context, placementId);
        }
        return null;
      }
    });
  }

  private void onFetchComplete(final Context context, final String placementId) {
    final InlineAdView inlineAdView = new InlineAdView(context, placementId, this);
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        ErrorInfo errorInfo = inlineAdView.load();
        if (errorInfo != null) {
           onError(errorInfo);
        } else {
           onLoaded(inlineAdView);
        }
      }
    });
  }

  @Override
  public void onError(final InlineAdView inlineAdView, final ErrorInfo errorInfo) {
    Log.e(TAG, "Yahoo Ads SDK inline ad error: " + errorInfo);
  }

  @Override
  public void onResized(final InlineAdView inlineAdView) {
    Log.d(TAG, "Yahoo Ads SDK on resized.");
  }

  @Override
  public void onExpanded(final InlineAdView inlineAdView) {
    Log.i(TAG, "Yahoo Ads SDK inline ad expanded.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdOpened(adapter);
        }
      }
    });
  }

  @Override
  public void onCollapsed(final InlineAdView inlineAdView) {
    Log.i(TAG, "Yahoo Ads SDK inline ad collapsed.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdClosed(adapter);
        }
      }
    });
  }

  @Override
  public void onClicked(final InlineAdView inlineAdView) {
    Log.i(TAG, "Yahoo Ads SDK inline ad clicked.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdClicked(adapter);
        }
      }
    });
  }

  @Override
  public void onAdLeftApplication(final InlineAdView inlineAdView) {
    Log.i(TAG, "Yahoo Ads SDK inline ad left application.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdLeftApplication(adapter);
        }
      }
    });
  }

  @Override
  public void onAdRefreshed(final InlineAdView inlineAdView) {
    // no op.  refreshing not supported in adapter
  }

  @Override
  public void onEvent(final InlineAdView inlineAdView, final String source, final String eventId,
      final Map<String, Object> arguments) {
    // no op.  events not supported in adapter
  }

  public void onLoaded(final InlineAdView inlineAdView) {
    this.inlineAdView = inlineAdView;
    Log.i(TAG, "Yahoo Ads SDK inline ad request succeeded.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        adContainer.addView(inlineAdView);
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdLoaded(adapter);
        }
      }
    });
  }


  public void onError(final ErrorInfo errorInfo) {
    Log.i(TAG, "Yahoo Ads SDK Inline Ad request failed (" + errorInfo.getErrorCode() + "): " +
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
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdFailedToLoad(adapter, errorCode);
        }
      }
    });
  }

  View getBannerView() {
    return adContainer;
  }

  void destroy() {
    if (inlineAdView != null) {
      inlineAdView.destroy();
    }
  }
}
