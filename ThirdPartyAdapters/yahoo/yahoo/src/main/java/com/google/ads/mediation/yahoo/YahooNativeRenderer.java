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
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.nativeplacement.NativeAd;
import com.yahoo.ads.nativeplacement.NativeAd.NativeAdListener;
import com.yahoo.ads.nativeplacement.NativePlacementConfig;
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

  /** Yahoo Factory to create ad objects. */
  private final YahooFactory yahooFactory;

  public YahooNativeRenderer(MediationNativeAdapter adapter, YahooFactory yahooFactory) {
    this.nativeAdapterWeakRef = new WeakReference<>(adapter);
    this.yahooFactory = yahooFactory;
  }

  public void render(@NonNull Context context, @NonNull MediationNativeListener listener,
      @NonNull Bundle serverParameters, @NonNull NativeMediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {
    nativeListener = listener;
    this.context = context;
    String siteId = YahooAdapterUtils.getSiteId(serverParameters, mediationExtras);
    MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

    if (TextUtils.isEmpty(siteId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Site ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, parameterError);
        return;
      }
    }

    AdError initializationError = initializeYahooSDK(context, siteId);
    if (initializationError != null) {
      Log.w(TAG, initializationError.toString());
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, initializationError);
      }
      return;
    }

    String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Placement ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      if (nativeListener != null && adapter != null) {
        nativeListener.onAdFailedToLoad(adapter, parameterError);
      }
      return;
    }

    YahooAdapterUtils.setCoppaValue(mediationAdRequest);

    String[] adTypes = new String[]{"100", "simpleImage"};
    NativePlacementConfig placementConfig =
        yahooFactory.createNativePlacementConfig(placementId, mediationAdRequest, adTypes);
    placementConfig.skipAssets = true;
    nativeAd = yahooFactory.createNativeAd(this.context, placementId, YahooNativeRenderer.this);
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
          public void onLoadError(@NonNull AdError loadError) {
            Log.w(TAG, loadError.toString());
            ThreadUtils.postOnUiThread(new Runnable() {
              @Override
              public void run() {
                nativeListener.onAdFailedToLoad(adapter, loadError);
              }
            });
          }
        });
      }
    });
  }

  @Override
  public void onLoadFailed(final NativeAd nativeAd, final ErrorInfo errorInfo) {
    AdError loadError = new AdError(errorInfo.getErrorCode(), errorInfo.getDescription(),
        YAHOO_MOBILE_SDK_ERROR_DOMAIN);
    Log.w(TAG, loadError.toString());
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
        if (nativeListener != null && adapter != null) {
          nativeListener.onAdFailedToLoad(adapter, loadError);
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
