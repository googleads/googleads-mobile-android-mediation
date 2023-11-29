// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.YAHOO_MOBILE_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.initializeYahooSDK;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.inlineplacement.InlineAdView;
import com.yahoo.ads.inlineplacement.InlineAdView.InlineAdListener;
import com.yahoo.ads.inlineplacement.InlinePlacementConfig;
import com.yahoo.ads.utils.ThreadUtils;
import java.lang.ref.WeakReference;
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

  /** Yahoo Factory to create ad objects. */
  private final YahooFactory yahooFactory;

  public YahooBannerRenderer(MediationBannerAdapter adapter, YahooFactory yahooFactory) {
    bannerAdapterWeakRef = new WeakReference<>(adapter);
    this.yahooFactory = yahooFactory;
  }

  public void render(@NonNull Context context, @NonNull MediationBannerListener listener,
      @NonNull Bundle serverParameters, @NonNull AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {
    bannerListener = listener;
    MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

    String siteId = YahooAdapterUtils.getSiteId(serverParameters, mediationExtras);
    if (TextUtils.isEmpty(siteId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Site ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, parameterError);
      }
      return;
    }

    AdError initializationError = initializeYahooSDK(context, siteId);
    if (initializationError != null) {
      Log.w(TAG, initializationError.toString());
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, initializationError);
      }
      return;
    }

    String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Placement ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, parameterError);
      }
      return;
    }

    AdSize normalizedSize = YahooAdapterUtils.normalizeSize(context, adSize);
    if (normalizedSize == null) {
      String message = String.format(
          "The requested banner size is not supported by Yahoo Mobile SDK: %s", adSize);
      AdError adSizeError = new AdError(ERROR_BANNER_SIZE_MISMATCH, message, ERROR_DOMAIN);
      Log.e(TAG, adSizeError.toString());
      if (bannerListener != null && adapter != null) {
        bannerListener.onAdFailedToLoad(adapter, adSizeError);
      }
      return;
    }

    adContainer = new LinearLayout(context);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.gravity = Gravity.CENTER_HORIZONTAL;
    adContainer.setLayoutParams(lp);

    YahooAdapterUtils.setCoppaValue(mediationAdRequest);

    com.yahoo.ads.inlineplacement.AdSize yahooAdSize =
        yahooFactory.createYahooAdSize(normalizedSize);
    InlinePlacementConfig placementConfig =
        yahooFactory.createInlinePlacementConfig(placementId, mediationAdRequest, yahooAdSize);
    inlineAdView = yahooFactory.createInlineAd(context, placementId, YahooBannerRenderer.this);
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
    AdError loadError = new AdError(errorInfo.getErrorCode(), errorInfo.getDescription(),
        YAHOO_MOBILE_SDK_ERROR_DOMAIN);
    Log.w(TAG, loadError.toString());
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
        if (bannerListener != null && adapter != null) {
          bannerListener.onAdFailedToLoad(adapter, loadError);
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
