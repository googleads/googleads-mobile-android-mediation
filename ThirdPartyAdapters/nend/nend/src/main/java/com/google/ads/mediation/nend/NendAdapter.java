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

  private NendAdView mNendAdView;
  private MediationBannerListener mListener;

  private NendAdInterstitialVideo mNendAdInterstitialVideo;
  private MediationInterstitialListener mListenerInterstitial;

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

  private InterstitialVideoStatus mInterstitialVideoStatus = InterstitialVideoStatus.PLAYING;

  private WeakReference<Activity> mActivityWeakReference;

  private boolean mIsDetached = false;
  private boolean mIsRequireLoadAd = false;

  private boolean mIsRequestBannerAd = false;
  private boolean mIsPausingWebView = false;

  private final View.OnAttachStateChangeListener mAttachStateChangeListener =
      new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
          mNendAdView.setListener(NendAdapter.this);
          if (mIsRequireLoadAd) {
            mNendAdView.loadAd();
            mIsRequireLoadAd = false;
          }
          mIsDetached = false;
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
          mIsDetached = true;
        }
      };

  private void requireLoadAd() {
    if (mIsDetached && !mIsRequireLoadAd) {
      mIsRequireLoadAd = true;
    }
  }

  @Override
  public void onDestroy() {
    bannerContainerView = null;
    mNendAdView = null;
    mListener = null;
    mListenerInterstitial = null;
    if (mActivityWeakReference != null) {
      mActivityWeakReference.clear();
      mActivityWeakReference = null;
    }
    if (mNendAdInterstitialVideo != null) {
      mNendAdInterstitialVideo.releaseAd();
      mNendAdInterstitialVideo = null;
    }
  }

  @Override
  public void onPause() {
    if (mNendAdView != null) {
      if (mNendAdView.getChildAt(0) instanceof WebView) {
        mIsPausingWebView = true;
      }
      mNendAdView.pause();
      requireLoadAd();
    }
  }

  @Override
  public void onResume() {
    if (mNendAdView != null) {
      if (mIsPausingWebView) {
        mNendAdView.resume();
      }
      requireLoadAd();
      mIsPausingWebView = false;
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

    mListenerInterstitial = listener;

    Activity activity = (Activity) context;
    mActivityWeakReference = new WeakReference<>(activity);

    InterstitialType type = InterstitialType.TYPE_NORMAL;
    String userID = "";
    if (mediationExtras != null)  {
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
                  NEND_SDK_ERROR_DOMAIN);
              Log.e(TAG, error.getMessage());
              if (mListenerInterstitial != null) {
                mListenerInterstitial.onAdFailedToLoad(NendAdapter.this, error);
              }
              return;
            }

            if (mListenerInterstitial != null) {
              mListenerInterstitial.onAdLoaded(NendAdapter.this);
            }
          }
        });
  }

  private void requestNendInterstialVideo(Context context, String apikey, int spotId,
      String userId) {

    mNendAdInterstitialVideo = new NendAdInterstitialVideo(context, spotId, apikey);
    mNendAdInterstitialVideo.setMediationName(MEDIATION_NAME_ADMOB);
    if (!TextUtils.isEmpty(userId)) {
      mNendAdInterstitialVideo.setUserId(userId);
    }

    mNendAdInterstitialVideo.setActionListener(new NendAdVideoActionListener() {
      @Override
      public void onLoaded(@NonNull NendAdVideo nendAdVideo) {
        if (mNendAdInterstitialVideo.getType() == NendAdVideoType.NORMAL) {
          NendAdVideoPlayingState state = mNendAdInterstitialVideo.playingState();
          if (state != null) {
            state.setPlayingStateListener(new NendAdVideoPlayingStateListener() {
              @Override
              public void onStarted(@NonNull NendAdVideo nendAdVideo) {
                mInterstitialVideoStatus = InterstitialVideoStatus.PLAYING;
              }

              @Override
              public void onStopped(@NonNull NendAdVideo nendAdVideo) {
                if (mInterstitialVideoStatus != InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
                  mInterstitialVideoStatus = InterstitialVideoStatus.STOPPED;
                }
              }

              @Override
              public void onCompleted(@NonNull NendAdVideo nendAdVideo) {
                mInterstitialVideoStatus = InterstitialVideoStatus.STOPPED;
              }
            });
          }
        }

        if (mListenerInterstitial != null) {
          mListenerInterstitial.onAdLoaded(NendAdapter.this);
        }
      }

      @Override
      public void onFailedToLoad(@NonNull NendAdVideo nendAdVideo, int errorCode) {
        String errorMessage = String
            .format("Nend SDK returned an ad load failure callback with code: %d", errorCode);
        AdError error = new AdError(errorCode, errorMessage, NEND_SDK_ERROR_DOMAIN);
        Log.e(TAG, error.getMessage());
        if (mListenerInterstitial != null) {
          mListenerInterstitial.onAdFailedToLoad(NendAdapter.this, error);
        }
        mNendAdInterstitialVideo.releaseAd();
      }

      @Override
      public void onFailedToPlay(@NonNull NendAdVideo nendAdVideo) {
        Log.e(TAG, "Failed to play nend interstitial video ad.");
      }

      @Override
      public void onShown(@NonNull NendAdVideo nendAdVideo) {
        if (mListenerInterstitial != null) {
          mListenerInterstitial.onAdOpened(NendAdapter.this);
        }
      }

      @Override
      public void onClosed(@NonNull NendAdVideo nendAdVideo) {
        if (mListenerInterstitial != null) {
          mListenerInterstitial.onAdClosed(NendAdapter.this);
          if (mInterstitialVideoStatus == InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
            mListenerInterstitial.onAdLeftApplication(NendAdapter.this);
          }
        }
        mNendAdInterstitialVideo.releaseAd();
      }

      @Override
      public void onAdClicked(@NonNull NendAdVideo nendAdVideo) {
        if (mListenerInterstitial != null) {
          mListenerInterstitial.onAdClicked(NendAdapter.this);
        }

        switch (mInterstitialVideoStatus) {
          case PLAYING:
          case PLAYING_WHEN_CLICKED:
            mInterstitialVideoStatus = InterstitialVideoStatus.PLAYING_WHEN_CLICKED;
            break;
          default:
            if (mListenerInterstitial != null) {
              mListenerInterstitial.onAdLeftApplication(NendAdapter.this);
            }
            break;
        }
      }

      @Override
      public void onInformationClicked(@NonNull NendAdVideo nendAdVideo) {
        if (mListenerInterstitial != null) {
          mListenerInterstitial.onAdLeftApplication(NendAdapter.this);
        }
      }
    });

    mNendAdInterstitialVideo.loadAd();
  }

  @Override
  public void showInterstitial() {
    if (mNendAdInterstitialVideo != null) {
      showNendAdInterstitialVideo();
    } else {
      showNendAdInterstitial();
    }
  }

  private void showNendAdInterstitial() {
    if (mActivityWeakReference == null || mActivityWeakReference.get() == null) {
      Log.e(TAG, "Failed to show nend interstitial ad: The context object is null.");
      return;
    }

    NendAdInterstitialShowResult result = NendAdInterstitial.showAd(mActivityWeakReference.get(),
        new NendAdInterstitial.OnClickListener() {
          @Override
          public void onClick(NendAdInterstitialClickType clickType) {
            switch (clickType) {
              case CLOSE:
                if (mListenerInterstitial != null) {
                  mListenerInterstitial.onAdClosed(NendAdapter.this);
                }
                break;
              case DOWNLOAD:
                if (mListenerInterstitial != null) {
                  mListenerInterstitial.onAdClicked(NendAdapter.this);
                  mListenerInterstitial.onAdLeftApplication(NendAdapter.this);
                  mListenerInterstitial.onAdClosed(NendAdapter.this);
                }
                break;
              case INFORMATION:
                if (mListenerInterstitial != null) {
                  mListenerInterstitial.onAdLeftApplication(NendAdapter.this);
                  mListenerInterstitial.onAdClosed(NendAdapter.this);
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
      AdError error = new AdError(getMediationErrorCode(result), errorMessage,
          NEND_SDK_ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mListenerInterstitial.onAdOpened(NendAdapter.this);
      mListenerInterstitial.onAdClosed(NendAdapter.this);
      return;
    }

    if (mListenerInterstitial != null) {
      mListenerInterstitial.onAdOpened(NendAdapter.this);
    }
  }

  private void showNendAdInterstitialVideo() {
    if (!mNendAdInterstitialVideo.isLoaded()) {
      Log.w(TAG, "nend interstitial video ad is not ready.");
      return;
    }

    if (mActivityWeakReference == null || mActivityWeakReference.get() == null) {
      Log.e(TAG, "Failed to show nend interstitial ad: The context object is null.");
      return;
    }

    mNendAdInterstitialVideo.showAd(mActivityWeakReference.get());
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
      String errorMessage = String.format("Unsupported ad size: %s", adSize.toString());
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

    mNendAdView = new NendAdView(context, spotID, apiKey);
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
    bannerContainerView.addView(mNendAdView, adViewParams);

    mListener = listener;

    // NOTE: Use the reload function of AdMob mediation instead of NendAdView.
    // So, reload function of NendAdView should be stopped.
    mNendAdView.pause();

    mNendAdView.setListener(NendAdapter.this);
    mNendAdView.addOnAttachStateChangeListener(mAttachStateChangeListener);
    mNendAdView.loadAd();

    mIsRequestBannerAd = true;
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
    if (mListener != null && mIsRequestBannerAd) {
      // New request or auto reload from AdMob network.
      mListener.onAdLoaded(NendAdapter.this);
      mIsRequestBannerAd = false;
    } else {
      // This case is not need to send onAdLoaded to AdMob network.
      Log.d(TAG, "This ad is auto reloading by nend network.");
    }
  }

  @Override
  public void onFailedToReceiveAd(@NonNull NendAdView adView) {
    if (!mIsRequestBannerAd) {
      // This case is not need to call listener function to AdMob network.
      return;
    }

    mIsRequestBannerAd = false;
    NendError nendError = adView.getNendError();
    if (mListener != null) {
      String errorMessage = String
          .format("Nend SDK returned an ad load failure callback: ", nendError.toString());
      AdError error = new AdError(getMediationErrorCode(nendError), errorMessage,
          NEND_SDK_ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (mListener != null) {
        mListener.onAdFailedToLoad(NendAdapter.this, error);
      }
    }
  }

  @Override
  public void onClick(@NonNull NendAdView adView) {
    if (mListener != null) {
      mListener.onAdClicked(NendAdapter.this);
      mListener.onAdOpened(NendAdapter.this);
      mListener.onAdLeftApplication(NendAdapter.this);
    }
  }

  @Override
  public void onDismissScreen(@NonNull NendAdView adView) {
    if (mListener != null) {
      mListener.onAdClosed(NendAdapter.this);
    }
  }

  @Override
  public void onInformationButtonClick(@NonNull NendAdView adView) {
    if (mListener != null) {
      mListener.onAdLeftApplication(NendAdapter.this);
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
    mActivityWeakReference = new WeakReference<>((Activity) context);
  }
  // endregion
}
