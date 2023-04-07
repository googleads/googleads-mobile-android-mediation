// Copyright 2017 Google LLC
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

package com.google.ads.mediation.nend;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import net.nend.android.NendAdInformationListener;
import net.nend.android.NendAdInterstitial;
import net.nend.android.NendAdInterstitial.NendAdInterstitialClickType;
import net.nend.android.NendAdInterstitial.NendAdInterstitialShowResult;
import net.nend.android.NendAdInterstitial.NendAdInterstitialStatusCode;
import net.nend.android.NendAdInterstitial.OnCompletionListener;
import net.nend.android.NendAdInterstitialVideo;
import net.nend.android.NendAdVideo;
import net.nend.android.NendAdVideoActionListener;
import net.nend.android.NendAdVideoPlayingState;
import net.nend.android.NendAdVideoPlayingStateListener;
import net.nend.android.NendAdVideoType;
import net.nend.android.NendAdView;
import net.nend.android.NendAdView.NendError;

/**
 * The {@link NendAdapter} class is used to load and show Nend interstitial and banner ads.
 */
public class NendAdapter extends NendMediationAdapter implements MediationBannerAdapter,
    MediationInterstitialAdapter, NendAdInformationListener, OnContextChangedListener {

  private NendAdView nendAdView;
  private MediationBannerListener listener;

  private NendAdInterstitialVideo nendAdInterstitialVideo;
  private MediationInterstitialListener listenerInterstitial;

  private FrameLayout bannerContainerView;

  static final String KEY_INTERSTITIAL_TYPE = "key_interstitial_type";

  public enum InterstitialType {
    TYPE_VIDEO,
    TYPE_NORMAL
  }

  private enum InterstitialVideoStatus {
    PLAYING,
    PLAYING_WHEN_CLICKED,
    STOPPED
  }

  private InterstitialVideoStatus interstitialVideoStatus = InterstitialVideoStatus.PLAYING;

  private WeakReference<Activity> activityWeakReference;

  private boolean isDetached = false;
  private boolean isRequireLoadAd = false;

  private boolean isRequestBannerAd = false;
  private boolean isPausingWebView = false;

  private final View.OnAttachStateChangeListener attachStateChangeListener =
      new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
          nendAdView.setListener(NendAdapter.this);
          if (isRequireLoadAd) {
            nendAdView.loadAd();
            isRequireLoadAd = false;
          }
          isDetached = false;
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
          isDetached = true;
        }
      };

  private void requireLoadAd() {
    if (isDetached && !isRequireLoadAd) {
      isRequireLoadAd = true;
    }
  }

  @Override
  public void onDestroy() {
    bannerContainerView = null;
    nendAdView = null;
    listener = null;
    listenerInterstitial = null;
    if (activityWeakReference != null) {
      activityWeakReference.clear();
      activityWeakReference = null;
    }
    if (nendAdInterstitialVideo != null) {
      nendAdInterstitialVideo.releaseAd();
      nendAdInterstitialVideo = null;
    }
  }

  @Override
  public void onPause() {
    if (nendAdView != null) {
      if (nendAdView.getChildAt(0) instanceof WebView) {
        isPausingWebView = true;
      }
      nendAdView.pause();
      requireLoadAd();
    }
  }

  @Override
  public void onResume() {
    if (nendAdView != null) {
      if (isPausingWebView) {
        nendAdView.resume();
      }
      requireLoadAd();
      isPausingWebView = false;
    }
  }

  // region MediationInterstitialAdapter implementation
  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener listener,
      @NonNull Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {

    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Nend requires an Activity context to load an ad.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(NendAdapter.this, error);
      return;
    }

    String apiKey = serverParameters.getString(KEY_API_KEY);
    if (TextUtils.isEmpty(apiKey)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid API key.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(NendAdapter.this, error);
      return;
    }

    int spotID = Integer.parseInt(serverParameters.getString(KEY_SPOT_ID, "0"));
    if (spotID <= 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid spot ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(NendAdapter.this, error);
      return;
    }

    listenerInterstitial = listener;

    Activity activity = (Activity) context;
    activityWeakReference = new WeakReference<>(activity);

    InterstitialType type = InterstitialType.TYPE_NORMAL;
    String userID = "";
    if (mediationExtras != null) {
      try {
        type = (InterstitialType) mediationExtras.getSerializable(KEY_INTERSTITIAL_TYPE);
        userID = mediationExtras.getString(KEY_USER_ID);
      } catch (Exception exception) {
        // Ignore exception.
      }
    }

    if (type == InterstitialType.TYPE_VIDEO) {
      requestNendInterstialVideo(context, apiKey, spotID, userID);
    } else {
      requestNendInterstitial(context, apiKey, spotID);
    }
  }

  private void requestNendInterstitial(Context context, String apikey, int spotId) {
    NendAdInterstitial.loadAd(context, apikey, spotId);
    NendAdInterstitial.isAutoReloadEnabled = false;
    NendAdInterstitial.setListener(
        new OnCompletionListener() {
          @Override
          public void onCompletion(NendAdInterstitialStatusCode status) {
            if (status != NendAdInterstitialStatusCode.SUCCESS) {
              String errorMessage = String
                  .format("Failed to load interstitial ad from nend: %s", status.toString());
              AdError error = new AdError(getMediationErrorCode(status), errorMessage,
                  ERROR_DOMAIN);
              Log.e(TAG, error.getMessage());
              if (listenerInterstitial != null) {
                listenerInterstitial.onAdFailedToLoad(NendAdapter.this, error);
              }
              return;
            }

            if (listenerInterstitial != null) {
              listenerInterstitial.onAdLoaded(NendAdapter.this);
            }
          }
        });
  }

  private void requestNendInterstialVideo(Context context, String apikey, int spotId,
      String userId) {

    nendAdInterstitialVideo = new NendAdInterstitialVideo(context, spotId, apikey);
    nendAdInterstitialVideo.setMediationName(MEDIATION_NAME_ADMOB);
    if (!TextUtils.isEmpty(userId)) {
      nendAdInterstitialVideo.setUserId(userId);
    }

    nendAdInterstitialVideo.setActionListener(new NendAdVideoActionListener() {
      @Override
      public void onLoaded(@NonNull NendAdVideo nendAdVideo) {
        if (nendAdInterstitialVideo.getType() == NendAdVideoType.NORMAL) {
          NendAdVideoPlayingState state = nendAdInterstitialVideo.playingState();
          if (state != null) {
            state.setPlayingStateListener(new NendAdVideoPlayingStateListener() {
              @Override
              public void onStarted(@NonNull NendAdVideo nendAdVideo) {
                interstitialVideoStatus = InterstitialVideoStatus.PLAYING;
              }

              @Override
              public void onStopped(@NonNull NendAdVideo nendAdVideo) {
                if (interstitialVideoStatus != InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
                  interstitialVideoStatus = InterstitialVideoStatus.STOPPED;
                }
              }

              @Override
              public void onCompleted(@NonNull NendAdVideo nendAdVideo) {
                interstitialVideoStatus = InterstitialVideoStatus.STOPPED;
              }
            });
          }
        }

        if (listenerInterstitial != null) {
          listenerInterstitial.onAdLoaded(NendAdapter.this);
        }
      }

      @Override
      public void onFailedToLoad(@NonNull NendAdVideo nendAdVideo, int errorCode) {
        String errorMessage = String
            .format("Nend SDK returned an ad load failure callback with code: %d", errorCode);
        AdError error = new AdError(errorCode, errorMessage, NEND_SDK_ERROR_DOMAIN);
        Log.e(TAG, error.getMessage());
        if (listenerInterstitial != null) {
          listenerInterstitial.onAdFailedToLoad(NendAdapter.this, error);
        }
        nendAdInterstitialVideo.releaseAd();
      }

      @Override
      public void onFailedToPlay(@NonNull NendAdVideo nendAdVideo) {
        Log.e(TAG, "Failed to play nend interstitial video ad.");
      }

      @Override
      public void onShown(@NonNull NendAdVideo nendAdVideo) {
        if (listenerInterstitial != null) {
          listenerInterstitial.onAdOpened(NendAdapter.this);
        }
      }

      @Override
      public void onClosed(@NonNull NendAdVideo nendAdVideo) {
        if (listenerInterstitial != null) {
          listenerInterstitial.onAdClosed(NendAdapter.this);
          if (interstitialVideoStatus == InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
            listenerInterstitial.onAdLeftApplication(NendAdapter.this);
          }
        }
        nendAdInterstitialVideo.releaseAd();
      }

      @Override
      public void onAdClicked(@NonNull NendAdVideo nendAdVideo) {
        if (listenerInterstitial != null) {
          listenerInterstitial.onAdClicked(NendAdapter.this);
        }

        switch (interstitialVideoStatus) {
          case PLAYING:
          case PLAYING_WHEN_CLICKED:
            interstitialVideoStatus = InterstitialVideoStatus.PLAYING_WHEN_CLICKED;
            break;
          default:
            if (listenerInterstitial != null) {
              listenerInterstitial.onAdLeftApplication(NendAdapter.this);
            }
            break;
        }
      }

      @Override
      public void onInformationClicked(@NonNull NendAdVideo nendAdVideo) {
        if (listenerInterstitial != null) {
          listenerInterstitial.onAdLeftApplication(NendAdapter.this);
        }
      }
    });

    nendAdInterstitialVideo.loadAd();
  }

  @Override
  public void showInterstitial() {
    if (nendAdInterstitialVideo != null) {
      showNendAdInterstitialVideo();
    } else {
      showNendAdInterstitial();
    }
  }

  private void showNendAdInterstitial() {
    if (activityWeakReference == null || activityWeakReference.get() == null) {
      Log.e(TAG, "Failed to show nend interstitial ad: The context object is null.");
      return;
    }

    NendAdInterstitialShowResult result = NendAdInterstitial.showAd(activityWeakReference.get(),
        new NendAdInterstitial.OnClickListener() {
          @Override
          public void onClick(NendAdInterstitialClickType clickType) {
            switch (clickType) {
              case CLOSE:
                if (listenerInterstitial != null) {
                  listenerInterstitial.onAdClosed(NendAdapter.this);
                }
                break;
              case DOWNLOAD:
                if (listenerInterstitial != null) {
                  listenerInterstitial.onAdClicked(NendAdapter.this);
                  listenerInterstitial.onAdLeftApplication(NendAdapter.this);
                  listenerInterstitial.onAdClosed(NendAdapter.this);
                }
                break;
              case INFORMATION:
                if (listenerInterstitial != null) {
                  listenerInterstitial.onAdLeftApplication(NendAdapter.this);
                  listenerInterstitial.onAdClosed(NendAdapter.this);
                }
                break;
              default:
                break;
            }
          }
        });

    if (result != NendAdInterstitialShowResult.AD_SHOW_SUCCESS) {
      String errorMessage = String
          .format("Failed to show interstitial ad from nend: %s", result.toString());
      AdError error = new AdError(getMediationErrorCode(result), errorMessage, ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      listenerInterstitial.onAdOpened(NendAdapter.this);
      listenerInterstitial.onAdClosed(NendAdapter.this);
      return;
    }

    if (listenerInterstitial != null) {
      listenerInterstitial.onAdOpened(NendAdapter.this);
    }
  }

  private void showNendAdInterstitialVideo() {
    if (!nendAdInterstitialVideo.isLoaded()) {
      Log.w(TAG, "nend interstitial video ad is not ready.");
      return;
    }

    if (activityWeakReference == null || activityWeakReference.get() == null) {
      Log.e(TAG, "Failed to show nend interstitial ad: The context object is null.");
      return;
    }

    nendAdInterstitialVideo.showAd(activityWeakReference.get());
  }
  // endregion

  // region MediationBannerAdapter implementation
  @Override
  public void requestBannerAd(@NonNull final Context context,
      @NonNull MediationBannerListener listener, @NonNull Bundle serverParameters,
      @NonNull AdSize adSize, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {

    final AdSize supportedAdSize = getSupportedAdSize(context, adSize);
    if (supportedAdSize == null) {
      String errorMessage = String.format("Unsupported ad size: %s", adSize);
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH, errorMessage, ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(NendAdapter.this, error);
      return;
    }

    String apiKey = serverParameters.getString(KEY_API_KEY);
    if (TextUtils.isEmpty(apiKey)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid API key.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(NendAdapter.this, error);
      return;
    }

    int spotID = Integer.parseInt(serverParameters.getString(KEY_SPOT_ID, "0"));
    if (spotID <= 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid spot ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(NendAdapter.this, error);
      return;
    }

    nendAdView = new NendAdView(context, spotID, apiKey);
    bannerContainerView = new FrameLayout(context);
    FrameLayout.LayoutParams containerViewParams =
        new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    bannerContainerView.setLayoutParams(containerViewParams);

    int adViewParamHeight = adSize.getHeightInPixels(context);
    if (adViewParamHeight <= 0) {
      adViewParamHeight = LayoutParams.WRAP_CONTENT;
    }

    FrameLayout.LayoutParams adViewParams =
        new FrameLayout.LayoutParams(adSize.getWidthInPixels(context), adViewParamHeight);
    adViewParams.gravity = Gravity.CENTER;
    bannerContainerView.addView(nendAdView, adViewParams);

    this.listener = listener;

    // NOTE: Use the reload function of AdMob mediation instead of NendAdView.
    // So, reload function of NendAdView should be stopped.
    nendAdView.pause();

    nendAdView.setListener(NendAdapter.this);
    nendAdView.addOnAttachStateChangeListener(attachStateChangeListener);
    nendAdView.loadAd();

    isRequestBannerAd = true;
  }

  @Override
  @NonNull
  public View getBannerView() {
    return bannerContainerView;
  }
  // endregion

  // region Banner ad utility methods.
  @Nullable
  private AdSize getSupportedAdSize(@NonNull Context context, @NonNull AdSize adSize) {
    // Check if the specified adSize is a Smart banner since nend supports any Smart banner with at
    // least a height of 50.
    if (adSize.getWidth() == AdSize.FULL_WIDTH && adSize.getHeight() == AdSize.AUTO_HEIGHT) {
      DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
      final int heightPixel = adSize.getHeightInPixels(context);
      final int heightDip = Math.round(heightPixel / displayMetrics.density);
      if (heightDip >= 50) {
        return adSize;
      }
    }

    /*
       Supported Sizes:
       https://github.com/fan-ADN/nendSDK-Android/wiki/About-Ad-Sizes#available-ad-sizes-are-listed-below.
       320 × 50
       320 × 100
       300 × 100
       300 × 250
       728 × 90
    */
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LARGE_BANNER);
    potentials.add(new AdSize(300, 100));
    potentials.add(AdSize.MEDIUM_RECTANGLE);
    potentials.add(AdSize.LEADERBOARD);
    return MediationUtils.findClosestSize(context, adSize, potentials);
  }
  // endregion

  // region NendAdListener callbacks.
  @Override
  public void onReceiveAd(@NonNull NendAdView adView) {
    if (listener != null && isRequestBannerAd) {
      // New request or auto reload from AdMob network.
      listener.onAdLoaded(NendAdapter.this);
      isRequestBannerAd = false;
    } else {
      // This case is not need to send onAdLoaded to AdMob network.
      Log.d(TAG, "This ad is auto reloading by nend network.");
    }
  }

  @Override
  public void onFailedToReceiveAd(@NonNull NendAdView adView) {
    if (!isRequestBannerAd) {
      // This case is not need to call listener function to AdMob network.
      return;
    }

    isRequestBannerAd = false;
    NendError nendError = adView.getNendError();
    if (listener != null) {
      String errorMessage = String
          .format("Nend SDK returned an ad load failure callback: %s", nendError.toString());
      AdError error = new AdError(getMediationErrorCode(nendError), errorMessage, ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (listener != null) {
        listener.onAdFailedToLoad(NendAdapter.this, error);
      }
    }
  }

  @Override
  public void onClick(@NonNull NendAdView adView) {
    if (listener != null) {
      listener.onAdClicked(NendAdapter.this);
      listener.onAdOpened(NendAdapter.this);
      listener.onAdLeftApplication(NendAdapter.this);
    }
  }

  @Override
  public void onDismissScreen(@NonNull NendAdView adView) {
    if (listener != null) {
      listener.onAdClosed(NendAdapter.this);
    }
  }

  @Override
  public void onInformationButtonClick(@NonNull NendAdView adView) {
    if (listener != null) {
      listener.onAdLeftApplication(NendAdapter.this);
    }
  }
  // endregion

  // region OnContextChangedListener implementation
  @Override
  public void onContextChanged(@NonNull Context context) {
    if (!(context instanceof Activity)) {
      Log.w(TAG, "Nend Ads require an Activity context to show ads.");
      return;
    }
    activityWeakReference = new WeakReference<>((Activity) context);
  }
  // endregion
}
