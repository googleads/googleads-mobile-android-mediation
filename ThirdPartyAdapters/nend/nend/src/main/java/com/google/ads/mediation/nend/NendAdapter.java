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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.OnContextChangedListener;

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

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/*
 * The {@link NendAdapter} class is used to load and show Nend interstitial and banner ads.
 */
@SuppressWarnings("unused")
public class NendAdapter extends NendMediationAdapter
        implements MediationBannerAdapter, MediationInterstitialAdapter,
        NendAdInformationListener, OnContextChangedListener {

    private NendAdView mNendAdView;
    private MediationBannerListener mListener;

    private NendAdInterstitialVideo mNendAdInterstitialVideo;
    private MediationInterstitialListener mListenerInterstitial;

    private FrameLayout smartBannerAdjustContainer;
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;
    private int smartBannerWidthPixel;
    private int smartBannerHeightPixel;

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
        if (smartBannerAdjustContainer != null) {
            removeOnGlobalLayoutListener(
                    smartBannerAdjustContainer.getViewTreeObserver(),globalLayoutListener);
            smartBannerAdjustContainer = null;
        }
        globalLayoutListener = null;
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
    public void requestInterstitialAd(Context context,
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

        String apiKey = serverParameters.getString(KEY_API_KEY);
        if (TextUtils.isEmpty(apiKey)) {
            Log.w(TAG, "Failed to request ad from Nend: Missing or invalid API Key.");
            adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        int spotId = Integer.parseInt(serverParameters.getString(KEY_SPOT_ID, "0"));
        if (spotId <= 0) {
            Log.w(TAG, "Failed to request ad from Nend: Missing or invalid Spot ID.");
            adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mActivityWeakReference = new WeakReference<>((Activity) context);

        if (mediationExtras != null) {
            final InterstitialType type =
                    (InterstitialType) mediationExtras.getSerializable(KEY_INTERSTITIAL_TYPE);
            if (type == InterstitialType.TYPE_VIDEO) {
                requestNendInterstialVideo(context, apiKey, spotId,
                        mediationExtras.getString(KEY_USER_ID, ""),
                        mediationAdRequest);
                return;
            }
        }

        requestNendInterstitial(context, apiKey, spotId);
    }

    private void requestNendInterstitial(Context context, String apikey, int spotId) {
        NendAdInterstitial.loadAd(context, apikey, spotId);
        NendAdInterstitial.isAutoReloadEnabled = false;
        NendAdInterstitial.setListener(new OnCompletionListener() {
            @Override
            public void onCompletion(NendAdInterstitialStatusCode status) {
                switch (status) {
                    case SUCCESS:
                        adLoaded();
                        break;
                    case FAILED_AD_DOWNLOAD:
                        adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    case FAILED_AD_INCOMPLETE:
                        adFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
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

    private void requestNendInterstialVideo(Context context,
                                            String apikey,
                                            int spotId,
                                            String userId,
                                            MediationAdRequest mediationAdRequest) {
        mNendAdInterstitialVideo = new NendAdInterstitialVideo(context, spotId, apikey);
        mNendAdInterstitialVideo.setMediationName("AdMob");
        if (!TextUtils.isEmpty(userId)) {
            mNendAdInterstitialVideo.setUserId(userId);
        }
        mNendAdInterstitialVideo.setAdListener(new NendAdVideoListener() {
            @Override
            public void onLoaded(NendAdVideo nendAdVideo) {
                adLoaded();
            }

            @Override
            public void onFailedToLoad(NendAdVideo nendAdVideo, int errorCode) {
                adFailedToLoad(ErrorUtil.convertErrorCodeFromNendVideoToAdMob(errorCode));
            }

            @Override
            public void onFailedToPlay(NendAdVideo nendAdVideo) {
                Log.w(TAG, "Interstitial video ad failed to play...");
            }

            @Override
            public void onShown(NendAdVideo nendAdVideo) {
                adOpened();
            }

            @Override
            public void onClosed(NendAdVideo nendAdVideo) {
                adClosed();
                if (mInterstitialVideoStatus == InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
                    adLeftApplication();
                }
            }

            @Override
            public void onStarted(NendAdVideo nendAdVideo) {
                mInterstitialVideoStatus = InterstitialVideoStatus.PLAYING;
            }

            @Override
            public void onStopped(NendAdVideo nendAdVideo) {
                if (mInterstitialVideoStatus != InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
                    mInterstitialVideoStatus = InterstitialVideoStatus.STOPPED;
                }
            }

            @Override
            public void onCompleted(NendAdVideo nendAdVideo) {
                mInterstitialVideoStatus = InterstitialVideoStatus.STOPPED;
            }

            @Override
            public void onAdClicked(NendAdVideo nendAdVideo) {
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
            public void onInformationClicked(NendAdVideo nendAdVideo) {
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
            Log.e(TAG, "Failed to show Nend Interstitial ad: "
                    + "An activity context is required to show Nend ads.");
            return;
        }

        NendAdInterstitialShowResult result =
                NendAdInterstitial.showAd(mActivityWeakReference.get(),
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
                Log.e(TAG, "Failed to show Nend Interstitial ad: "
                        + "An activity context is required to show Nend ads.");
            }
        } else {
            Log.w(TAG, "Nend Interstitial video ad is not ready.");
        }
    }

    // region Methods to display nend banner even in SmartBanner.
    private int pxToDp(int pixel) {
        return Math.round(pixel / Resources.getSystem().getDisplayMetrics().density);
    }

    private boolean isSmartBanner(AdSize adSize) {
        return (adSize.getWidth() == AdSize.FULL_WIDTH) &&
                (adSize.getHeight() == AdSize.AUTO_HEIGHT);
    }

    private boolean canDisplayAtSmartBanner(Context context, AdSize adSize) {
        if (isSmartBanner(adSize)) {
            DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
            final int heightPixel = adSize.getHeightInPixels(context);
            final int heightDip = Math.round(heightPixel / displayMetrics.density);
            return heightDip >= 50; // Minimum support height of nend banner.
        }
        return false;
    }

    private void prepareContainerAndLayout(Context context, AdSize adSize) {
        // Need this for adjust the container size of nend banner.
        globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                FrameLayout.LayoutParams containerViewParams = new FrameLayout.LayoutParams(
                        smartBannerWidthPixel,
                        smartBannerHeightPixel);
                smartBannerAdjustContainer.setLayoutParams(containerViewParams);

                removeOnGlobalLayoutListener(smartBannerAdjustContainer.getViewTreeObserver(),
                        globalLayoutListener);
            }
        };

        smartBannerWidthPixel = adSize.getWidthInPixels(context);
        smartBannerHeightPixel = adSize.getHeightInPixels(context);

        FrameLayout.LayoutParams containerViewParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        smartBannerAdjustContainer = new FrameLayout(context);
        smartBannerAdjustContainer.setLayoutParams(containerViewParams);

        smartBannerAdjustContainer.getViewTreeObserver()
                .addOnGlobalLayoutListener(globalLayoutListener);

        FrameLayout.LayoutParams adViewParams =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        adViewParams.gravity = Gravity.CENTER;
        smartBannerAdjustContainer.addView(mNendAdView, adViewParams);
    }

    private void removeOnGlobalLayoutListener(ViewTreeObserver observer,
                                              ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (observer == null || listener == null) {
            return;
        }
        observer.removeOnGlobalLayoutListener(listener);
    }
    // endregion

    @Override
    public View getBannerView() {
        if (smartBannerAdjustContainer != null) {
            return smartBannerAdjustContainer;
        }
        return mNendAdView;
    }

    @Override
    public void requestBannerAd(Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {

        boolean availableAtSmartBanner = false;

        if (canDisplayAtSmartBanner(context, adSize)) {
            availableAtSmartBanner = true;
        } else {
            adSize = getSupportedAdSize(context, adSize);
            if (adSize == null) {
                Log.w(TAG, "Failed to request ad, AdSize is null.");
                if (listener != null) {
                    listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
                return;
            }
        }

        int adSizeWidth = adSize.getWidth();
        int adSizeHeight = adSize.getHeight();

        mListener = listener;

        // Available ad sizes are listed below.
        // https://github.com/fan-ADN/nendSDK-Android/wiki/About-Ad-Sizes#available-ad-sizes-are-listed-below.
        if ((adSizeWidth == 320 && adSizeHeight == 50) ||
                (adSizeWidth == 320 && adSizeHeight == 100) ||
                (adSizeWidth == 300 && adSizeHeight == 250) ||
                (adSizeWidth == 728 && adSizeHeight == 90) ||
                availableAtSmartBanner) {
            String apiKey = serverParameters.getString(KEY_API_KEY);
            String spotId = serverParameters.getString(KEY_SPOT_ID);
            if (!TextUtils.isEmpty(apiKey) && !TextUtils.isEmpty(spotId)) {
                int intSpotId = Integer.parseInt(spotId);
                mNendAdView = new NendAdView(context, intSpotId, apiKey);

                if (availableAtSmartBanner) {
                    prepareContainerAndLayout(context, adSize);
                }

                // NOTE: Use the reload function of AdMob mediation instead of NendAdView.
                // So, reload function of NendAdView should be stopped.
                mNendAdView.pause();

                mNendAdView.setListener(this);
                mNendAdView.addOnAttachStateChangeListener(mAttachStateChangeListener);
                mNendAdView.loadAd();

                mIsRequestBannerAd = true;
            } else {
                Log.w(TAG, "Failed to load ad from Nend:" +
                        "Missing or Invalid API Key and/or Spot ID.");
                if (mListener != null) {
                    mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        } else {
            Log.w(TAG, "Invalid Ad type");
            if (mListener != null) {
                mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

  AdSize getSupportedAdSize(Context context, AdSize adSize) {
    AdSize original = new AdSize(adSize.getWidth(), adSize.getHeight());

    /*
       Supported Sizes:
       320 × 50
       320 × 100
       300 × 100
       300 × 250
       728 × 90
    */
    ArrayList<AdSize> potentials = new ArrayList<AdSize>(5);
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LARGE_BANNER);
    potentials.add(new AdSize(300, 100));
    potentials.add(AdSize.MEDIUM_RECTANGLE);
    potentials.add(AdSize.LEADERBOARD);

    return findClosestSize(context, original, potentials);
  }

  // Start of helper code to remove when available in SDK
  /**
   * Find the closest supported AdSize from the list of potentials to the provided size. Returns
   * null if none are within given threshold size range.
   */
  public static AdSize findClosestSize(
      Context context, AdSize original, ArrayList<AdSize> potentials) {
    if (potentials == null || original == null) {
      return null;
    }
    float density = context.getResources().getDisplayMetrics().density;
    int actualWidth = Math.round(original.getWidthInPixels(context) / density);
    int actualHeight = Math.round(original.getHeightInPixels(context) / density);
    original = new AdSize(actualWidth, actualHeight);

    AdSize largestPotential = null;
    for (AdSize potential : potentials) {
      if (isSizeInRange(original, potential)) {
        if (largestPotential == null) {
          largestPotential = potential;
        } else {
          largestPotential = getLargerByArea(largestPotential, potential);
        }
      }
    }
    return largestPotential;
  }

  private static boolean isSizeInRange(AdSize original, AdSize potential) {
    if (potential == null) {
      return false;
    }
    double minWidthRatio = 0.5;
    double minHeightRatio = 0.7;

    int originalWidth = original.getWidth();
    int potentialWidth = potential.getWidth();
    int originalHeight = original.getHeight();
    int potentialHeight = potential.getHeight();

    if (originalWidth * minWidthRatio > potentialWidth || originalWidth < potentialWidth) {
      return false;
    }

    if (originalHeight * minHeightRatio > potentialHeight || originalHeight < potentialHeight) {
      return false;
    }
    return true;
  }

  private static AdSize getLargerByArea(AdSize size1, AdSize size2) {
    int area1 = size1.getWidth() * size1.getHeight();
    int area2 = size2.getWidth() * size2.getHeight();
    return area1 > area2 ? size1 : size2;
  }
  // End code to remove when available in SDK

  // region NendAdListener callbacks.
  @Override
  public void onReceiveAd(NendAdView adView) {
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
    public void onFailedToReceiveAd(NendAdView adView) {
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
    public void onClick(NendAdView adView) {
        if (mListener != null) {
            mListener.onAdClicked(this);
            mListener.onAdOpened(this);
            mListener.onAdLeftApplication(this);
        }
    }

    @Override
    public void onDismissScreen(NendAdView adView) {
        if (mListener != null) {
            mListener.onAdClosed(this);
        }
    }

    @Override
    public void onInformationButtonClick(NendAdView adView) {
        if (mListener != null) {
            mListener.onAdLeftApplication(this);
        }
    }
    //endregion

    //region MediationInterstitialListener callbacks.
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
    //endregion

    //region OnContextChangedListener implementation
    @Override
    public void onContextChanged(Context context) {
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Nend Ads require an Activity context to show ads.");
            return;
        }

        mActivityWeakReference = new WeakReference<>((Activity) context);
    }
    //endregion
}