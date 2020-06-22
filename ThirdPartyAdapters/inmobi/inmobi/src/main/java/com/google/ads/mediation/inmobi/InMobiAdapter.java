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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
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
import java.util.Iterator;
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

  /**
   * Converts a {@link com.inmobi.ads.InMobiAdRequestStatus.StatusCode} to Google Mobile Ads SDK
   * readable error code.
   *
   * @param statusCode the {@link com.inmobi.ads.InMobiAdRequestStatus.StatusCode} to be converted.
   * @return an {@link AdRequest} error code.
   */
  private static int getAdRequestErrorCode(InMobiAdRequestStatus.StatusCode statusCode) {
    switch (statusCode) {
      case INTERNAL_ERROR:
        return AdRequest.ERROR_CODE_INTERNAL_ERROR;
      case AD_ACTIVE:
      case REQUEST_INVALID:
      case REQUEST_PENDING:
      case EARLY_REFRESH_REQUEST:
      case MISSING_REQUIRED_DEPENDENCIES:
        return AdRequest.ERROR_CODE_INVALID_REQUEST;
      case REQUEST_TIMED_OUT:
      case NETWORK_UNREACHABLE:
        return AdRequest.ERROR_CODE_NETWORK_ERROR;
      case NO_FILL:
      case SERVER_ERROR:
      case AD_NO_LONGER_AVAILABLE:
      case NO_ERROR:
      default:
        return AdRequest.ERROR_CODE_NO_FILL;
    }
  }

  //region MediationAdapter implementation.
  @Override
  public void onDestroy() {
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }
  //endregion

  //region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(final Context context,
      final MediationBannerListener listener,
      Bundle serverParameters,
      AdSize mediationAdSize,
      final MediationAdRequest mediationAdRequest,
      final Bundle mediationExtras) {

    final AdSize inMobiMediationAdSize = getSupportedAdSize(context, mediationAdSize);
    final long placement = InMobiAdapterUtils.getPlacementId(serverParameters);
    mBannerListener = listener;

    if (inMobiMediationAdSize == null) {
      Log.w(TAG, "Failed to request ad, AdSize is null.");
      listener.onAdFailedToLoad(InMobiAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    if (TextUtils.isEmpty(accountID)) {
      Log.w(TAG, "Failed to initialize InMobi SDK: Missing or invalid Account ID.");
      listener.onAdFailedToLoad(InMobiAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return;
    }

    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadBannerAd(context, placement, inMobiMediationAdSize, mediationAdRequest,
            mediationExtras);
      }

      @Override
      public void onInitializeError(@NonNull Error error) {
        Log.w(TAG, error.getMessage());
        if (mBannerListener != null) {
          mBannerListener.onAdFailedToLoad(InMobiAdapter.this,
              AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
  //endregion

  //region MediationInterstitialAdapter implementation.
  @Override
  public void requestInterstitialAd(final Context context,
      final MediationInterstitialListener listener,
      Bundle serverParameters,
      final MediationAdRequest mediationAdRequest,
      final Bundle mediationExtras) {
    final long placement = InMobiAdapterUtils.getPlacementId(serverParameters);
    mInterstitialListener = listener;
    final String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    if (TextUtils.isEmpty(accountID)) {
      Log.w(TAG, "Failed to initialize InMobi SDK: Missing or invalid Account ID.");
      mInterstitialListener.onAdFailedToLoad(InMobiAdapter.this,
          AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return;
    }

    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadInterstitialAd(context, placement, mediationAdRequest, mediationExtras);
      }

      @Override
      public void onInitializeError(@NonNull Error error) {
        Log.w(TAG, error.getMessage());
        if (mInterstitialListener != null) {
          mInterstitialListener.onAdFailedToLoad(InMobiAdapter.this,
              AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
      }
    });
  }

  @Override
  public void showInterstitial() {
    if (mAdInterstitial.isReady()) {
      Log.d(TAG, "Ad is ready to show.");
      mAdInterstitial.show();
    }
  }
  //endregion

  //region MediationNativeAdapter implementation.
  @Override
  public void requestNativeAd(final Context context,
      final MediationNativeListener listener,
      Bundle serverParameters,
      final NativeMediationAdRequest mediationAdRequest,
      final Bundle mediationExtras) {
    mNativeMedAdReq = mediationAdRequest;
    final long placement = InMobiAdapterUtils.getPlacementId(serverParameters);
    mNativeListener = listener;

    if (!mNativeMedAdReq.isUnifiedNativeAdRequested()
        && !mNativeMedAdReq.isAppInstallAdRequested()) {
      Log.e(TAG, "Failed to request InMobi native ad: "
          + "Unified Native Ad or App install Ad should be requested.");
      mNativeListener.onAdFailedToLoad(this,
          AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    if (TextUtils.isEmpty(accountID)) {
      Log.w(TAG, "Failed to initialize InMobi SDK: Missing or invalid Account ID.");
      listener.onAdFailedToLoad(InMobiAdapter.this,
          AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return;
    }

    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadNativeAd(context, placement, mNativeMedAdReq, mediationExtras);
      }

      @Override
      public void onInitializeError(@NonNull Error error) {
        Log.w(TAG, error.getMessage());
        if (mNativeListener != null) {
          mNativeListener.onAdFailedToLoad(InMobiAdapter.this,
              AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
      }
    });
  }

  //endregion

  //region Banner adapter utility classes.
  private void createAndLoadBannerAd(Context context, long placement, AdSize mediationAdSize,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
    if (placement <= 0L) {
      Log.e(TAG, "Failed to request InMobi banner ad.");
      mBannerListener.onAdFailedToLoad(InMobiAdapter.this,
          AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return;
    }

    FrameLayout.LayoutParams wrappedLayoutParams = new FrameLayout.LayoutParams(
        mediationAdSize.getWidthInPixels(context), mediationAdSize.getHeightInPixels(context));
    InMobiBanner adView;
    try {
      adView = new InMobiBanner(context, placement);
    } catch (SdkNotInitializedException exception) {
      Log.e(TAG, "Failed to request InMobi banner ad.", exception);
      mBannerListener.onAdFailedToLoad(InMobiAdapter.this,
          AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
        Log.e(TAG, "InMobi banner failed to load: " + requestStatus.getMessage());
        mBannerListener.onAdFailedToLoad(InMobiAdapter.this,
            getAdRequestErrorCode(requestStatus.getStatusCode()));
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

  //region Interstitial adapter utility classes.
  private void createAndLoadInterstitialAd(Context context, long placement,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {

    if (placement <= 0L) {
      Log.e(TAG, "Failed to request InMobi interstitial ad.");
      mInterstitialListener.onAdFailedToLoad(InMobiAdapter.this,
          AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
              Log.d(TAG, "InMobi interstitial ad failed to show.");
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
              Log.e(TAG, "InMobi interstitial failed to load: " + requestStatus.getMessage());
              mInterstitialListener.onAdFailedToLoad(
                  InMobiAdapter.this,
                  getAdRequestErrorCode(requestStatus.getStatusCode()));
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
      Log.e(TAG, "Failed to request InMobi interstitial ad.", exception);
      mInterstitialListener.onAdFailedToLoad(InMobiAdapter.this,
          AdRequest.ERROR_CODE_INTERNAL_ERROR);
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

  //region Native adapter utility classes.
  private void createAndLoadNativeAd(final Context context, long placement,
      final NativeMediationAdRequest mNativeMedAdReq, Bundle mediationExtras) {

    if (placement <= 0L) {
      Log.e(TAG, "Failed to request InMobi native ad.");
      mNativeListener.onAdFailedToLoad(InMobiAdapter.this,
          AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return;
    }

    try {
      mAdNative = new InMobiNative(context, placement, new NativeAdEventListener() {
        @Override
        public void onAdLoadSucceeded(@NonNull final InMobiNative imNativeAd,
            @NonNull AdMetaInfo adMetaInfo) {
          Log.d(TAG, "InMobi native ad has been loaded.");

          //This setting decides whether to download images or not
          NativeAdOptions nativeAdOptions =
              InMobiAdapter.this.mNativeMedAdReq.getNativeAdOptions();
          boolean mIsOnlyUrl = false;

          if (null != nativeAdOptions) {
            mIsOnlyUrl = nativeAdOptions.shouldReturnUrlsForImageAssets();
          }

          if (InMobiAdapter.this.mNativeMedAdReq.isUnifiedNativeAdRequested()) {
            InMobiUnifiedNativeAdMapper inMobiUnifiedNativeAdMapper =
                new InMobiUnifiedNativeAdMapper(InMobiAdapter.this,
                    imNativeAd,
                    mIsOnlyUrl,
                    mNativeListener);
            inMobiUnifiedNativeAdMapper.mapUnifiedNativeAd(context);
          } else if (InMobiAdapter.this.mNativeMedAdReq.isAppInstallAdRequested()) {
            InMobiAppInstallNativeAdMapper inMobiAppInstallNativeAdMapper =
                new InMobiAppInstallNativeAdMapper(
                    InMobiAdapter.this,
                    imNativeAd,
                    mIsOnlyUrl,
                    mNativeListener);
            inMobiAppInstallNativeAdMapper.mapAppInstallAd(context);
          }
        }

        @Override
        public void onAdLoadFailed(@NonNull InMobiNative inMobiNative,
            @NonNull InMobiAdRequestStatus requestStatus) {
          Log.e(TAG, "InMobi native ad failed to load: " + requestStatus.getMessage());
          mNativeListener.onAdFailedToLoad(InMobiAdapter.this,
              getAdRequestErrorCode(requestStatus.getStatusCode()));
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
      Log.e(TAG, "Failed to request InMobi native ad.", exception);
      mNativeListener.onAdFailedToLoad(InMobiAdapter.this,
          AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
