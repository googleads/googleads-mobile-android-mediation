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
import com.google.android.gms.ads.AdRequest;
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
import net.nend.android.NendAdVideoListener;
import net.nend.android.NendAdView;
import net.nend.android.NendAdView.NendError;

/** The {@link NendAdapter} class is used to load and show Nend interstitial and banner ads. */
@SuppressWarnings("unused")
public class NendAdapter extends NendMediationAdapter
    implements MediationBannerAdapter,
        MediationInterstitialAdapter,
        NendAdInformationListener,
        OnContextChangedListener {

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

  private View.OnAttachStateChangeListener mAttachStateChangeListener =
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

  @Override
  public void requestInterstitialAd(
      Context context,
      MediationInterstitialListener listener,
      Bundle serverParameters,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    mListenerInterstitial = listener;

    if (!(context instanceof Activity)) {
      Log.w(TAG, "Failed to request ad from Nend: Context not an Activity.");
      adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    AdUnitMapper mapper = AdUnitMapper.createAdUnitMapper(serverParameters);
    if (mapper == null) {
      Log.w(TAG, "Failed to request ad from Nend: Invalid Spot ID or API Key.");
      adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    mActivityWeakReference = new WeakReference<>((Activity) context);

    if (mediationExtras != null) {
      final InterstitialType type =
          (InterstitialType) mediationExtras.getSerializable(KEY_INTERSTITIAL_TYPE);
      if (type == InterstitialType.TYPE_VIDEO) {
        requestNendInterstialVideo(
            context,
            mapper.apiKey,
            mapper.spotId,
            mediationExtras.getString(KEY_USER_ID, ""),
            mediationAdRequest);
        return;
      }
    }

    requestNendInterstitial(context, mapper.apiKey, mapper.spotId);
  }

  private void requestNendInterstitial(Context context, String apikey, int spotId) {
    NendAdInterstitial.loadAd(context, apikey, spotId);
    NendAdInterstitial.isAutoReloadEnabled = false;
    NendAdInterstitial.setListener(
        new OnCompletionListener() {
          @Override
          public void onCompletion(NendAdInterstitialStatusCode status) {
            switch (status) {
              case SUCCESS:
                adLoaded();
                break;
              case FAILED_AD_DOWNLOAD:
                adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                break;
              case FAILED_AD_REQUEST:
                adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                break;
              case INVALID_RESPONSE_TYPE:
                adFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                break;
            }
          }
        });
  }

  private void requestNendInterstialVideo(
      Context context,
      String apikey,
      int spotId,
      String userId,
      MediationAdRequest mediationAdRequest) {
    mNendAdInterstitialVideo = new NendAdInterstitialVideo(context, spotId, apikey);
    mNendAdInterstitialVideo.setMediationName(MEDIATION_NAME_ADMOB);
    if (!TextUtils.isEmpty(userId)) {
      mNendAdInterstitialVideo.setUserId(userId);
    }
    mNendAdInterstitialVideo.setAdListener(
        new NendAdVideoListener() {
          @Override
          public void onLoaded(@NonNull NendAdVideo nendAdVideo) {
            adLoaded();
          }

          @Override
          public void onFailedToLoad(@NonNull NendAdVideo nendAdVideo, int errorCode) {
            adFailedToLoad(ErrorUtil.convertErrorCodeFromNendVideoToAdMob(errorCode));
          }

          @Override
          public void onFailedToPlay(@NonNull NendAdVideo nendAdVideo) {
            Log.w(TAG, "Interstitial video ad failed to play...");
          }

          @Override
          public void onShown(@NonNull NendAdVideo nendAdVideo) {
            adOpened();
          }

          @Override
          public void onClosed(@NonNull NendAdVideo nendAdVideo) {
            adClosed();
            if (mInterstitialVideoStatus == InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
              adLeftApplication();
            }
          }

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

          @Override
          public void onAdClicked(@NonNull NendAdVideo nendAdVideo) {
            adClicked();
            switch (mInterstitialVideoStatus) {
              case PLAYING:
              case PLAYING_WHEN_CLICKED:
                mInterstitialVideoStatus = InterstitialVideoStatus.PLAYING_WHEN_CLICKED;
                break;
              default:
                adLeftApplication();
                break;
            }
          }

          @Override
          public void onInformationClicked(@NonNull NendAdVideo nendAdVideo) {
            adLeftApplication();
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
      Log.e(
          TAG,
          "Failed to show Nend Interstitial ad: "
              + "An activity context is required to show Nend ads.");
      return;
    }

    NendAdInterstitialShowResult result =
        NendAdInterstitial.showAd(
            mActivityWeakReference.get(),
            new NendAdInterstitial.OnClickListener() {
              @Override
              public void onClick(NendAdInterstitialClickType clickType) {
                switch (clickType) {
                  case CLOSE:
                    adClosed();
                    break;
                  case DOWNLOAD:
                    adClicked();
                    adLeftApplication();
                    adClosed();
                    break;
                  case INFORMATION:
                    adLeftApplication();
                    adClosed();
                    break;
                  default:
                    break;
                }
              }
            });

    switch (result) {
      case AD_SHOW_SUCCESS:
        adOpened();
        break;
      case AD_SHOW_ALREADY:
        break;
      case AD_FREQUENCY_NOT_REACHABLE:
        break;
      case AD_REQUEST_INCOMPLETE:
        break;
      case AD_LOAD_INCOMPLETE:
        // Request is not start yet or requesting now.
        break;
      case AD_DOWNLOAD_INCOMPLETE:
        break;
      default:
        break;
    }
  }

  private void showNendAdInterstitialVideo() {
    if (mNendAdInterstitialVideo.isLoaded()) {
      if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
        mNendAdInterstitialVideo.showAd(mActivityWeakReference.get());
      } else {
        Log.e(
            TAG,
            "Failed to show Nend Interstitial ad: "
                + "An activity context is required to show Nend ads.");
      }
    } else {
      Log.w(TAG, "Nend Interstitial video ad is not ready.");
    }
  }

  @Override
  public View getBannerView() {
    return bannerContainerView;
  }

  @Override
  public void requestBannerAd(
      final Context context,
      MediationBannerListener listener,
      Bundle serverParameters,
      AdSize adSize,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {

    final AdSize supportedAdSize = getSupportedAdSize(context, adSize);
    if (supportedAdSize == null) {
      Log.w(TAG, "Failed to request ad, Unsupported ad size: " + adSize.toString());
      listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    AdUnitMapper mapper = AdUnitMapper.createAdUnitMapper(serverParameters);
    if (mapper == null) {
      Log.w(TAG, "Failed to request ad from Nend: Invalid Spot ID or API Key.");
      listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return;
    }

    mNendAdView = new NendAdView(context, mapper.spotId, mapper.apiKey);
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

    mNendAdView.setListener(this);
    mNendAdView.addOnAttachStateChangeListener(mAttachStateChangeListener);
    mNendAdView.loadAd();

    mIsRequestBannerAd = true;
  }

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
      mListener.onAdLoaded(this);
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
      switch (nendError) {
        case INVALID_RESPONSE_TYPE:
          mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
          break;
        case FAILED_AD_DOWNLOAD:
          mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
          break;
        case FAILED_AD_REQUEST:
          mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
          break;
        case AD_SIZE_TOO_LARGE:
          mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
          break;
        case AD_SIZE_DIFFERENCES:
          mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
          break;
      }
    }
  }

  @Override
  public void onClick(@NonNull NendAdView adView) {
    if (mListener != null) {
      mListener.onAdClicked(this);
      mListener.onAdOpened(this);
      mListener.onAdLeftApplication(this);
    }
  }

  @Override
  public void onDismissScreen(@NonNull NendAdView adView) {
    if (mListener != null) {
      mListener.onAdClosed(this);
    }
  }

  @Override
  public void onInformationButtonClick(@NonNull NendAdView adView) {
    if (mListener != null) {
      mListener.onAdLeftApplication(this);
    }
  }
  // endregion

  // region MediationInterstitialListener callbacks.
  public void adLeftApplication() {
    if (mListenerInterstitial != null) {
      mListenerInterstitial.onAdLeftApplication(this);
    }
  }

  public void adClicked() {
    if (mListenerInterstitial != null) {
      mListenerInterstitial.onAdClicked(this);
    }
  }

  public void adClosed() {
    if (mListenerInterstitial != null) {
      mListenerInterstitial.onAdClosed(this);
    }
  }

  public void adFailedToLoad(int errorCode) {
    if (mListenerInterstitial != null) {
      mListenerInterstitial.onAdFailedToLoad(this, errorCode);
    }
  }

  public void adLoaded() {
    if (mListenerInterstitial != null) {
      mListenerInterstitial.onAdLoaded(this);
    }
  }

  public void adOpened() {
    if (mListenerInterstitial != null) {
      mListenerInterstitial.onAdOpened(this);
    }
  }
  // endregion

  // region OnContextChangedListener implementation
  @Override
  public void onContextChanged(Context context) {
    if (!(context instanceof Activity)) {
      Log.w(TAG, "Nend Ads require an Activity context to show ads.");
      return;
    }

    mActivityWeakReference = new WeakReference<>((Activity) context);
  }
  // endregion
}
