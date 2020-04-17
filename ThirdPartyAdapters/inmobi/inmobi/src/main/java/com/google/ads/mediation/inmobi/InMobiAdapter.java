package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
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
      public void onInitializeError(Error error) {
        Log.w(TAG, error.getMessage());
        if (mBannerListener != null) {
          mBannerListener.onAdFailedToLoad(InMobiAdapter.this,
              AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
      }

    });
  }

  private AdSize getSupportedAdSize(Context context, AdSize adSize) {
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
    return InMobiAdapterUtils.findClosestSize(context, adSize, potentials);
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
      public void onInitializeError(Error error) {
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
      Log.d(TAG, "Ad is ready to show");
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
      public void onInitializeError(Error error) {
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
      public void onUserLeftApplication(InMobiBanner inMobiBanner) {
        Log.d(TAG, "InMobi Banner onUserLeftApplication");
        mBannerListener.onAdLeftApplication(InMobiAdapter.this);
      }

      @Override
      public void onRewardsUnlocked(InMobiBanner inMobiBanner,
          Map<Object, Object> rewards) {
        Log.d(TAG, "InMobi Banner onRewardsUnlocked.");

        if (rewards != null) {
          Iterator<Object> iterator = rewards.keySet().iterator();
          while (iterator.hasNext()) {
            String key = iterator.next().toString();
            String value = rewards.get(key).toString();
            Log.d("Rewards: ", key + ":" + value);
          }
        }
      }

      @Override
      public void onAdLoadSucceeded(InMobiBanner inMobiBanner) {
        Log.d(TAG, "InMobi Banner onAdLoadSucceeded");
        mBannerListener.onAdLoaded(InMobiAdapter.this);
      }

      @Override
      public void onAdLoadFailed(InMobiBanner inMobiBanner,
          InMobiAdRequestStatus requestStatus) {
        Log.e(TAG, "InMobiBanner onAdLoadFailed: " + requestStatus.getMessage());
        mBannerListener.onAdFailedToLoad(InMobiAdapter.this,
            getAdRequestErrorCode(requestStatus.getStatusCode()));
      }

      @Override
      public void onAdDisplayed(InMobiBanner inMobiBanner) {
        Log.d(TAG, "InMobi Banner onAdDisplayed");
        mBannerListener.onAdOpened(InMobiAdapter.this);
      }

      @Override
      public void onAdDismissed(InMobiBanner inMobiBanner) {
        Log.d(TAG, "InMobi Banner onAdDismissed");
        mBannerListener.onAdClosed(InMobiAdapter.this);
      }

      @Override
      public void onAdClicked(InMobiBanner inMobiBanner,
          Map<Object, Object> map) {
        Log.d(TAG, "InMobi Banner onBannerClick");
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
            public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi Interstitial onUserLeftApplication");
              mInterstitialListener.onAdLeftApplication(InMobiAdapter.this);
            }

            @Override
            public void onRewardsUnlocked(InMobiInterstitial inMobiInterstitial,
                Map<Object, Object> rewards) {
              Log.d(TAG, "InMobi Interstitial onRewardsUnlocked.");

              if (rewards != null) {
                for (Object reward : rewards.keySet()) {
                  String key = reward.toString();
                  String value = rewards.get(key).toString();
                  Log.d("Rewards: ", key + ": " + value);
                }
              }
            }

            @Override
            public void onAdDisplayFailed(InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi Interstitial Ad Display failed.");
            }

            @Override
            public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi Interstitial Ad Will Display.");
              // Using onAdDisplayed to send the onAdOpened callback.
            }

            @Override
            public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi Interstitial onAdLoadSucceeded");
              mInterstitialListener.onAdLoaded(InMobiAdapter.this);
            }

            @Override
            public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                InMobiAdRequestStatus requestStatus) {
              Log.e(TAG, "InMobi Interstitial onAdLoadFailed: " + requestStatus.getMessage());
              mInterstitialListener.onAdFailedToLoad(
                  InMobiAdapter.this,
                  getAdRequestErrorCode(requestStatus.getStatusCode()));

            }

            @Override
            public void onAdReceived(InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi Interstitial server responded with an Ad.");
            }

            @Override
            public void onAdDisplayed(InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi Interstitial onAdDisplayed");
              mInterstitialListener.onAdOpened(InMobiAdapter.this);

            }

            @Override
            public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi Interstitial onAdDismissed");
              mInterstitialListener.onAdClosed(InMobiAdapter.this);
            }

            @Override
            public void onAdClicked(InMobiInterstitial inMobiInterstitial,
                Map<Object, Object> clickParameters) {
              Log.d(TAG, "InMobi Interstitial Clicked");
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
        public void onAdLoadSucceeded(final InMobiNative imNativeAd) {
          Log.d(TAG, "InMobi Native Ad onAdLoadSucceeded");

          if (null == imNativeAd) {
            return;
          }

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
        public void onAdLoadFailed(InMobiNative inMobiNative,
            InMobiAdRequestStatus requestStatus) {
          Log.e(TAG, "InMobi Native Ad onAdLoadFailed: " + requestStatus.getMessage());
          mNativeListener.onAdFailedToLoad(InMobiAdapter.this,
              getAdRequestErrorCode(requestStatus.getStatusCode()));
        }

        @Override
        public void onAdFullScreenDismissed(InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi Native Ad onAdDismissed");
          mNativeListener.onAdClosed(InMobiAdapter.this);
        }

        @Override
        public void onAdFullScreenWillDisplay(InMobiNative inMobiNative) {

        }

        @Override
        public void onAdFullScreenDisplayed(InMobiNative inMobiNative) {
          mNativeListener.onAdOpened(InMobiAdapter.this);
        }

        @Override
        public void onUserWillLeaveApplication(InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi Native Ad onUserLeftApplication");
          mNativeListener.onAdLeftApplication(InMobiAdapter.this);
        }

        @Override
        public void onAdImpressed(@NonNull InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi Native Ad impression recorded successfully");
          mNativeListener.onAdImpression(InMobiAdapter.this);
        }

        @Override
        public void onAdClicked(@NonNull InMobiNative inMobiNative) {
          Log.d(TAG, "InMobi Native Ad onAdClicked");
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
