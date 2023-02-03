package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooMediationAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.initializeSDK;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.inlineplacement.InlineAdView;
import com.yahoo.ads.inlineplacement.InlineAdView.InlineAdListener;
import com.yahoo.ads.inlineplacement.InlinePlacementConfig;
import com.yahoo.ads.utils.TextUtils;
import com.yahoo.ads.utils.ThreadUtils;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;

final class YahooBannerRenderer implements InlineAdListener {

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

  public void render(@NonNull Context context, @NonNull MediationBannerListener listener,
      @NonNull Bundle serverParameters, @NonNull AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {
    bannerListener = listener;
    MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
    String siteId = YahooAdapterUtils.getSiteId(serverParameters, mediationExtras);
    if (TextUtils.isEmpty(siteId)) {
      Log.e(TAG, "Failed to request ad: siteID is null or empty.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    if (!initializeSDK(context, siteId)) {
      Log.e(TAG, "Unable to initialize the Yahoo Mobile SDK.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
      }
      return;
    }

    String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      Log.e(TAG, "Failed to request ad: placementID is null or empty.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    AdSize normalizedSize = YahooAdapterUtils.normalizeSize(context, adSize);
    if (normalizedSize == null) {
      Log.w(TAG, "The input ad size " + adSize + " is not currently supported.");
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    adContainer = new LinearLayout(context);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.gravity = Gravity.CENTER_HORIZONTAL;
    adContainer.setLayoutParams(lp);

    YahooAdapterUtils.setCoppaValue(mediationAdRequest);

    com.yahoo.ads.inlineplacement.AdSize yahooAdSize = new com.yahoo.ads.inlineplacement.AdSize(
        normalizedSize.getWidth(), normalizedSize.getHeight());
    InlinePlacementConfig placementConfig = new InlinePlacementConfig(placementId,
        YahooAdapterUtils.getRequestMetadata(mediationAdRequest),
        Collections.singletonList(yahooAdSize));
    inlineAdView = new InlineAdView(context, placementId, YahooBannerRenderer.this);
    inlineAdView.load(placementConfig);
  }

  @NonNull
  View getBannerView() {
    return adContainer;
  }

  void destroy() {
    if (inlineAdView != null) {
      inlineAdView.destroy();
    }
  }

  // region Yahoo InlineAdListener implementation.

  @Override
  public void onLoaded(final InlineAdView inlineAdView) {
    this.inlineAdView = inlineAdView;
    Log.i(TAG, "Yahoo Mobile SDK loaded a banner ad successfully.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        adContainer.addView(inlineAdView);
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdLoaded(adapter);
        }
      }
    });
  }

  @Override
  public void onLoadFailed(InlineAdView inlineAdFactory, final ErrorInfo errorInfo) {
    Log.w(TAG, "Yahoo Mobile SDK failed to request a banner ad with error code: "
        + errorInfo.getErrorCode() + ", message: " + errorInfo.getDescription());
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_MEDIATION_NO_FILL);
        }
      }
    });
  }

  @Override
  public void onError(final InlineAdView inlineAdView, final ErrorInfo errorInfo) {
    Log.w(TAG, "Yahoo Mobile SDK returned an error for banner ad: " + errorInfo);
  }

  @Override
  public void onResized(final InlineAdView inlineAdView) {
    Log.d(TAG, "Yahoo Mobile SDK resized a banner ad.");
  }

  @Override
  public void onExpanded(final InlineAdView inlineAdView) {
    Log.i(TAG, "Yahoo Mobile SDK expanded a banner ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK collapsed a banner ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK recorded a click on a banner ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK has caused the user to leave the application from a banner ad.");
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
    // No-op. Events not supported in adapter.
  }

  @Override
  public void onEvent(final InlineAdView inlineAdView, final String source, final String eventId,
      final Map<String, Object> arguments) {
    // No-op. Events not supported in adapter.
  }

  // endregion.
}
