package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.InMobiBanner.AnimationType;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.exceptions.SdkNotInitializedException;
import com.inmobi.ads.listeners.BannerAdEventListener;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.ads.listeners.NativeAdEventListener;
import com.inmobi.ads.listeners.VideoEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * InMobi Adapter for AdMob Mediation used to load and show banner, interstitial and native ads.
 * This class should not be used directly by publishers.
 */
public final class InMobiAdapter extends InMobiMediationAdapter
    implements MediationBannerAdapter, MediationInterstitialAdapter, MediationNativeAdapter {

  private static final String TAG = InMobiAdapter.class.getSimpleName();

  // Callback listeners.
  private MediationBannerListener mBannerListener;
  private MediationInterstitialListener mInterstitialListener;
  private MediationNativeListener mNativeListener;

  private InMobiInterstitial mAdInterstitial;
  private FrameLayout mWrappedAdView;

  private static Boolean sDisableHardwareFlag = false;

  private NativeMediationAdRequest mNativeMedAdReq;
  private InMobiNative mAdNative;

  // region MediationAdapter implementation.
  @Override
  public void onDestroy() {
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }
  // endregion

  // region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(final Context context, final MediationBannerListener listener,
      Bundle serverParameters, AdSize mediationAdSize, final MediationAdRequest mediationAdRequest,
      final Bundle mediationExtras) {

    final AdSize inMobiMediationAdSize = getSupportedAdSize(context, mediationAdSize);
    if (inMobiMediationAdSize == null) {
      String errorMessage = String
          .format("InMobi SDK supported banner sizes are not valid for the requested size: %s",
              mediationAdSize.toString());
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH, errorMessage, ERROR_DOMAIN);
      Log.w(TAG, errorMessage);
      listener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    if (TextUtils.isEmpty(accountID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Account ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    mBannerListener = listener;
    final long placement = InMobiAdapterUtils.getPlacementId(serverParameters);
    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadBannerAd(context, placement, inMobiMediationAdSize, mediationAdRequest,
            mediationExtras);
      }

      @Override
      public void onInitializeError(@NonNull AdError error) {
        Log.w(TAG, error.getMessage());
        if (mBannerListener != null) {
          mBannerListener.onAdFailedToLoad(InMobiAdapter.this, error);
        }
      }
    });
  }

  @Nullable
  private AdSize getSupportedAdSize(@NonNull Context context, @NonNull AdSize adSize) {
    /*
        Supported Sizes (ref: https://www.inmobi.com/ui/pdfs/ad-specs.pdf)
        320x50,
        300x250,
        728x90.
     */

    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(320, 50));
    potentials.add(new AdSize(300, 250));
    potentials.add(new AdSize(728, 90));
    return MediationUtils.findClosestSize(context, adSize, potentials);
  }

  @Override
  public View getBannerView() {
    return mWrappedAdView;
  }
  // endregion

  // region MediationInterstitialAdapter implementation.
  @Override
  public void requestInterstitialAd(final Context context,
      final MediationInterstitialListener listener, Bundle serverParameters,
      final MediationAdRequest mediationAdRequest, final Bundle mediationExtras) {

    final String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    if (TextUtils.isEmpty(accountID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Account ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    mInterstitialListener = listener;
    final long placement = InMobiAdapterUtils.getPlacementId(serverParameters);
    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadInterstitialAd(context, placement, mediationAdRequest, mediationExtras);
      }

      @Override
      public void onInitializeError(@NonNull AdError error) {
        Log.w(TAG, error.getMessage());
        if (mInterstitialListener != null) {
          mInterstitialListener.onAdFailedToLoad(InMobiAdapter.this, error);
        }
      }
    });
  }

  @Override
  public void showInterstitial() {
    if (!mAdInterstitial.isReady()) {
      AdError error = new AdError(ERROR_AD_NOT_READY,
          "InMobi Interstitial ad is not yet ready to be shown.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      return;
    }

    mAdInterstitial.show();
  }
  // endregion

  // region MediationNativeAdapter implementation.
  @Override
  public void requestNativeAd(final Context context, final MediationNativeListener listener,
      Bundle serverParameters, final NativeMediationAdRequest mediationAdRequest,
      final Bundle mediationExtras) {

    if (!mediationAdRequest.isUnifiedNativeAdRequested()) {
      AdError error = new AdError(ERROR_NON_UNIFIED_NATIVE_REQUEST,
          "Unified Native Ad should be requested.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(this, error);
      return;
    }

    String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    if (TextUtils.isEmpty(accountID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Account ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    mNativeListener = listener;
    mNativeMedAdReq = mediationAdRequest;
    final long placement = InMobiAdapterUtils.getPlacementId(serverParameters);
    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadNativeAd(context, placement, mNativeMedAdReq, mediationExtras);
      }

      @Override
      public void onInitializeError(@NonNull AdError error) {
        Log.w(TAG, error.getMessage());
        if (mNativeListener != null) {
          mNativeListener.onAdFailedToLoad(InMobiAdapter.this, error);
        }
      }
    });
  }
  // endregion

  // region Banner adapter utility classes.
  private void createAndLoadBannerAd(Context context, long placement, AdSize mediationAdSize,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {

    if (placement <= 0L) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mBannerListener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    FrameLayout.LayoutParams wrappedLayoutParams = new FrameLayout.LayoutParams(
        mediationAdSize.getWidthInPixels(context), mediationAdSize.getHeightInPixels(context));
    InMobiBanner adView;
    try {
      adView = new InMobiBanner(context, placement);
    } catch (SdkNotInitializedException exception) {
      AdError error = new AdError(ERROR_INMOBI_NOT_INITIALIZED, exception.getLocalizedMessage(),
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mBannerListener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    // Turn off automatic refresh.
    adView.setEnableAutoRefresh(false);
    // Turn off the animation.
    adView.setAnimationType(AnimationType.ANIMATION_OFF);

    if (mediationAdRequest.getKeywords() != null) {
      adView.setKeywords(TextUtils.join(", ", mediationAdRequest.getKeywords()));
    }

    // Create request parameters.
    HashMap<String, String> paramMap =
        InMobiAdapterUtils.createInMobiParameterMap(mediationAdRequest);
    adView.setExtras(paramMap);

    if (mediationExtras == null) {
      mediationExtras = new Bundle();
    }

    adView.setListener(new BannerAdEventListener() {
      @Override
      public void onUserLeftApplication(@NonNull InMobiBanner inMobiBanner) {
        Log.d(TAG, "InMobi banner left application.");
        mBannerListener.onAdLeftApplication(InMobiAdapter.this);
      }

      @Override
      public void onRewardsUnlocked(@NonNull InMobiBanner inMobiBanner,
          Map<Object, Object> rewards) {
        // No-op.
      }

      @Override
      public void onAdLoadSucceeded(@NonNull InMobiBanner inMobiBanner,
          @NonNull AdMetaInfo adMetaInfo) {
        Log.d(TAG, "InMobi banner has been loaded.");
        mBannerListener.onAdLoaded(InMobiAdapter.this);
      }

      @Override
      public void onAdLoadFailed(@NonNull InMobiBanner inMobiBanner,
          @NonNull InMobiAdRequestStatus requestStatus) {
        AdError error = new AdError(InMobiAdapterUtils.getMediationErrorCode(requestStatus),
            requestStatus.getMessage(), INMOBI_SDK_ERROR_DOMAIN);
        Log.w(TAG, error.getMessage());
        mBannerListener.onAdFailedToLoad(InMobiAdapter.this, error);
      }

      @Override
      public void onAdDisplayed(@NonNull InMobiBanner inMobiBanner) {
        Log.d(TAG, "InMobi banner opened a full screen view.");
        mBannerListener.onAdOpened(InMobiAdapter.this);
      }

      @Override
      public void onAdDismissed(@NonNull InMobiBanner inMobiBanner) {
        Log.d(TAG, "InMobi banner has been dismissed.");
        mBannerListener.onAdClosed(InMobiAdapter.this);
      }

      @Override
      public void onAdClicked(@NonNull InMobiBanner inMobiBanner,
          Map<Object, Object> map) {
        Log.d(TAG, "InMobi banner has been clicked.");
        mBannerListener.onAdClicked(InMobiAdapter.this);
      }
    });

    if (InMobiAdapter.sDisableHardwareFlag) {
      adView.disableHardwareAcceleration();
    }

    /*
     * We wrap the ad View in a FrameLayout to ensure that it's the right
     * size. Without this the ad takes up the maximum width possible,
     * causing artifacts on high density screens (like the Galaxy Nexus) or
     * in landscape view. If the underlying library sets the appropriate
     * size instead of match_parent, this wrapper can be removed.
     */
    mWrappedAdView = new FrameLayout(context);
    mWrappedAdView.setLayoutParams(wrappedLayoutParams);
    adView.setLayoutParams(
        new LinearLayout.LayoutParams(
            mediationAdSize.getWidthInPixels(context),
            mediationAdSize.getHeightInPixels(context)));
    mWrappedAdView.addView(adView);
    InMobiAdapterUtils.setGlobalTargeting(mediationAdRequest, mediationExtras);

    Log.d(TAG, "Requesting banner with ad size: " + mediationAdSize.toString());
    adView.load();
  }

  // region Interstitial adapter utility classes.
  private void createAndLoadInterstitialAd(Context context, long placement,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {

    if (placement <= 0L) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mInterstitialListener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    try {
      mAdInterstitial = new InMobiInterstitial(context, placement,
          new InterstitialAdEventListener() {

            @Override
            public void onUserLeftApplication(@NonNull InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi interstitial left application.");
              mInterstitialListener.onAdLeftApplication(InMobiAdapter.this);
            }

            @Override
            public void onRewardsUnlocked(@NonNull InMobiInterstitial inMobiInterstitial,
                Map<Object, Object> rewards) {
              // No op.
            }

            @Override
            public void onAdDisplayFailed(@NonNull InMobiInterstitial inMobiInterstitial) {
              AdError error = new AdError(ERROR_AD_DISPLAY_FAILED, "InMobi ad failed to show.",
                  ERROR_DOMAIN);
              Log.w(TAG, error.getMessage());
            }

            @Override
            public void onAdWillDisplay(@NonNull InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi interstitial ad will be shown.");
              // Using onAdDisplayed to send the onAdOpened callback.
            }

            @Override
            public void onAdLoadSucceeded(@NonNull InMobiInterstitial inMobiInterstitial,
                @NonNull AdMetaInfo adMetaInfo) {
              Log.d(TAG, "InMobi interstitial ad has been loaded.");
              mInterstitialListener.onAdLoaded(InMobiAdapter.this);
            }

            @Override
            public void onAdLoadFailed(@NonNull InMobiInterstitial inMobiInterstitial,
                @NonNull InMobiAdRequestStatus requestStatus) {
              AdError error = new AdError(InMobiAdapterUtils.getMediationErrorCode(requestStatus),
                  requestStatus.getMessage(), INMOBI_SDK_ERROR_DOMAIN);
              Log.w(TAG, error.getMessage());
              mInterstitialListener.onAdFailedToLoad(InMobiAdapter.this, error);
            }

            @Override
            public void onAdFetchSuccessful(@NonNull InMobiInterstitial inMobiInterstitial,
                @NonNull AdMetaInfo adMetaInfo) {
              Log.d(TAG, "InMobi interstitial ad fetched from server, "
                  + "but ad contents still need to be loaded.");
            }

            @Override
            public void onAdDisplayed(@NonNull InMobiInterstitial inMobiInterstitial,
                @NonNull AdMetaInfo adMetaInfo) {
              Log.d(TAG, "InMobi interstitial has been shown.");
              mInterstitialListener.onAdOpened(InMobiAdapter.this);

            }

            @Override
            public void onAdDismissed(@NonNull InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi interstitial ad has been dismissed.");
              mInterstitialListener.onAdClosed(InMobiAdapter.this);
            }

            @Override
            public void onAdClicked(@NonNull InMobiInterstitial inMobiInterstitial,
                Map<Object, Object> clickParameters) {
              Log.d(TAG, "InMobi interstitial ad has been clicked.");
              mInterstitialListener.onAdClicked(InMobiAdapter.this);
            }
          });
    } catch (SdkNotInitializedException exception) {
      AdError error = new AdError(ERROR_INMOBI_NOT_INITIALIZED, exception.getLocalizedMessage(),
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mInterstitialListener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    if (mediationAdRequest.getKeywords() != null) {
      mAdInterstitial.setKeywords(TextUtils.join(", ", mediationAdRequest.getKeywords()));
    }

    // Create request parameters.
    HashMap<String, String> paramMap =
        InMobiAdapterUtils.createInMobiParameterMap(mediationAdRequest);
    mAdInterstitial.setExtras(paramMap);

    if (InMobiAdapter.sDisableHardwareFlag) {
      mAdInterstitial.disableHardwareAcceleration();
    }

    InMobiAdapterUtils.setGlobalTargeting(mediationAdRequest, mediationExtras);
    mAdInterstitial.load();
  }

  // region Native adapter utility classes.
  private void createAndLoadNativeAd(final Context context, long placement,
      final NativeMediationAdRequest mNativeMedAdReq, Bundle mediationExtras) {

    if (placement <= 0L) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mNativeListener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    try {
      mAdNative = new InMobiNative(context, placement, new NativeAdEventListener() {
        @Override
        public void onAdLoadSucceeded(@NonNull final InMobiNative imNativeAd,
            @NonNull AdMetaInfo adMetaInfo) {
          Log.d(TAG, "InMobi native ad has been loaded.");

          // This setting decides whether to download images or not.
          NativeAdOptions nativeAdOptions = InMobiAdapter.this.mNativeMedAdReq
              .getNativeAdRequestOptions();
          boolean mIsOnlyUrl = false;

          if (null != nativeAdOptions) {
            mIsOnlyUrl = nativeAdOptions.shouldReturnUrlsForImageAssets();
          }

          InMobiUnifiedNativeAdMapper inMobiUnifiedNativeAdMapper =
              new InMobiUnifiedNativeAdMapper(InMobiAdapter.this, imNativeAd, mIsOnlyUrl,
                  mNativeListener);
          inMobiUnifiedNativeAdMapper.mapUnifiedNativeAd(context);
        }

        @Override
        public void onAdLoadFailed(@NonNull InMobiNative inMobiNative,
            @NonNull InMobiAdRequestStatus requestStatus) {
          AdError error = new AdError(InMobiAdapterUtils.getMediationErrorCode(requestStatus),
              requestStatus.getMessage(), INMOBI_SDK_ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mNativeListener.onAdFailedToLoad(InMobiAdapter.this, error);
        }

        @Override
        public void onAdFullScreenDismissed(@NonNull InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi native ad has been dismissed.");
          mNativeListener.onAdClosed(InMobiAdapter.this);
        }

        @Override
        public void onAdFullScreenWillDisplay(@NonNull InMobiNative inMobiNative) {
          // No op.
        }

        @Override
        public void onAdFullScreenDisplayed(@NonNull InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi native ad opened.");
          mNativeListener.onAdOpened(InMobiAdapter.this);
        }

        @Override
        public void onUserWillLeaveApplication(@NonNull InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi native ad left application.");
          mNativeListener.onAdLeftApplication(InMobiAdapter.this);
        }

        @Override
        public void onAdImpressed(@NonNull InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi native ad impression occurred.");
          mNativeListener.onAdImpression(InMobiAdapter.this);
        }

        @Override
        public void onAdClicked(@NonNull InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi native ad has been clicked.");
          mNativeListener.onAdClicked(InMobiAdapter.this);
        }

        @Override
        public void onAdStatusChanged(@NonNull InMobiNative inMobiNative) {
          // No op.
        }
      });
    } catch (SdkNotInitializedException exception) {
      AdError error = new AdError(ERROR_INMOBI_NOT_INITIALIZED, exception.getLocalizedMessage(),
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mNativeListener.onAdFailedToLoad(InMobiAdapter.this, error);
      return;
    }

    mAdNative.setVideoEventListener(new VideoEventListener() {
      @Override
      public void onVideoCompleted(final InMobiNative inMobiNative) {
        super.onVideoCompleted(inMobiNative);
        Log.d(TAG, "InMobi native video ad completed.");
        mNativeListener.onVideoEnd(InMobiAdapter.this);
      }

      @Override
      public void onVideoSkipped(final InMobiNative inMobiNative) {
        super.onVideoSkipped(inMobiNative);
        Log.d(TAG, "InMobi native video ad skipped.");
      }
    });

    // Setting mediation key words to native ad object
    Set<String> mediationKeyWords = mNativeMedAdReq.getKeywords();
    if (null != mediationKeyWords) {
      mAdNative.setKeywords(TextUtils.join(", ", mediationKeyWords));
    }

    /*
     *  Extra request params : Add any other extra request params here
     *  #1. Explicitly setting mediation supply parameter to AdMob
     *  #2. Landing url
     */
    HashMap<String, String> paramMap =
        InMobiAdapterUtils.createInMobiParameterMap(mNativeMedAdReq);
    mAdNative.setExtras(paramMap);

    InMobiAdapterUtils.setGlobalTargeting(mNativeMedAdReq, mediationExtras);
    mAdNative.load();
  }
}
