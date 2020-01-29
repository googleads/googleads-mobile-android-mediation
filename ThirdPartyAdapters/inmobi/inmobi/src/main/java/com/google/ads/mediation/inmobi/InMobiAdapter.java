package com.google.ads.mediation.inmobi;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

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
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.unification.sdk.InitializationStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * InMobi Adapter for AdMob Mediation used to load and show banner, interstitial and
 * native ads. This class should not be used directly by publishers.
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
    private static Boolean sIsAppInitialized = false;

    private NativeMediationAdRequest mNativeMedAdReq;

    private InMobiNative mAdNative;

    public static Boolean isAppInitialized() {
        return sIsAppInitialized;
    }

    /**
     * Converts a {@link com.inmobi.ads.InMobiAdRequestStatus.StatusCode} to Google Mobile
     * Ads SDK readable error code.
     *
     * @param statusCode the {@link com.inmobi.ads.InMobiAdRequestStatus.StatusCode} to be
     *                   converted.
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
    public void requestBannerAd(Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                AdSize mediationAdSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        mediationAdSize = getSupportedAdSize(context, mediationAdSize);
        if (mediationAdSize == null) {
            Log.w(TAG, "Failed to request ad, AdSize is null.");
            if (listener != null) {
                listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        if (!sIsAppInitialized && serverParameters != null) {
            Log.d(TAG, serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID));
            Log.d(TAG, serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID));

            @InitializationStatus String status = InMobiSdk.init(context, serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID),
                    InMobiConsent.getConsentObj());
            sIsAppInitialized = status.equals(InitializationStatus.SUCCESS);
            if (!sIsAppInitialized) {
                listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }
        }
        this.mBannerListener = listener;

        FrameLayout.LayoutParams wrappedLayoutParams = new FrameLayout.LayoutParams(
                mediationAdSize.getWidthInPixels(context),
                mediationAdSize.getHeightInPixels(context));
        InMobiBanner adView;
        if (serverParameters != null) {
            try {
                if (context instanceof Activity) {
                    adView = new InMobiBanner((Activity) context,
                            Long.parseLong(serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID)));
                } else {
                    adView = new InMobiBanner(context,
                            Long.parseLong(serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID)));
                }
            } catch (SdkNotInitializedException e) {
                listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }
        } else {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        // Turn off automatic refresh.
        adView.setEnableAutoRefresh(false);
        // Turn off the animation.
        adView.setAnimationType(AnimationType.ANIMATION_OFF);
        if (mediationAdRequest.getKeywords() != null) {
            adView.setKeywords(TextUtils.join(", ",
                    mediationAdRequest.getKeywords()));
        }

        // Create request params.
        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(mediationAdRequest);
        adView.setExtras(paramMap);

        if (mediationExtras == null) {
            mediationExtras = new Bundle();
        }
        adView.setListener(new BannerAdEventListener() {
            @Override
            public void onUserLeftApplication(InMobiBanner inMobiBanner) {
                Log.d(TAG, "onUserLeftApplication");
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
                System.out.println("onLoadSucceeded");
                Log.d(TAG, "onAdLoadSucceeded");

                mBannerListener.onAdLoaded(InMobiAdapter.this);
            }

            @Override
            public void onAdLoadFailed(InMobiBanner inMobiBanner,
                                       InMobiAdRequestStatus requestStatus) {
                mBannerListener.onAdFailedToLoad(
                        InMobiAdapter.this,
                        getAdRequestErrorCode(requestStatus.getStatusCode()));
                Log.d(TAG, "onAdLoadFailed: " + requestStatus.getMessage());
            }

            @Override
            public void onAdDisplayed(InMobiBanner inMobiBanner) {
                Log.d(TAG, "onAdDisplayed");
                mBannerListener.onAdOpened(InMobiAdapter.this);
            }

            @Override
            public void onAdDismissed(InMobiBanner inMobiBanner) {
                Log.d(TAG, "onAdDismissed");
                mBannerListener.onAdClosed(InMobiAdapter.this);
            }

            @Override
            public void onAdClicked(InMobiBanner inMobiBanner,
                                    Map<Object, Object> map) {
                Log.d("onBannerClicked", "onBannerClick called");
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

    private AdSize getSupportedAdSize(Context context, AdSize adSize) {
        /*
        Supported Sizes (ref: https://www.inmobi.com/ui/pdfs/ad-specs.pdf)
        320x50,
        300x250,
        728x90.
         */

        ArrayList<AdSize> potentials = new ArrayList<AdSize>(3);
        potentials.add(new AdSize(320, 50));
        potentials.add(new AdSize(300, 250));
        potentials.add(new AdSize(728, 90));
        Log.i(TAG, potentials.toString());
        return InMobiAdapterUtils.findClosestSize(context, adSize, potentials);
    }

    @Override
    public View getBannerView() {
        return mWrappedAdView;
    }
    //endregion

    //region MediationInterstitialAdapter implementation.
    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {

        if (!sIsAppInitialized) {
            @InitializationStatus String status = InMobiSdk.init(context, serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID),
                    InMobiConsent.getConsentObj());
            sIsAppInitialized = status.equals(InitializationStatus.SUCCESS);
            if (!sIsAppInitialized) {
                listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }
        }

        this.mInterstitialListener = listener;

        try {
            mAdInterstitial = new InMobiInterstitial(context, Long.parseLong(serverParameters
                    .getString(InMobiAdapterUtils.KEY_PLACEMENT_ID)),
                    new InterstitialAdEventListener() {

                        @Override
                        public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "onUserLeftApplication");
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
                            Log.d(TAG, "Ad Display failed.");
                        }

                        @Override
                        public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "Ad Will Display.");
                            // Using onAdDisplayed to send the onAdOpened callback.
                        }

                        @Override
                        public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "onAdLoadSucceeded");
                            mInterstitialListener.onAdLoaded(InMobiAdapter.this);
                        }

                        @Override
                        public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                                                   InMobiAdRequestStatus requestStatus) {
                            mInterstitialListener.onAdFailedToLoad(
                                    InMobiAdapter.this,
                                    getAdRequestErrorCode(requestStatus.getStatusCode()));
                            Log.d(TAG, "onAdLoadFailed: " + requestStatus.getMessage());

                        }

                        @Override
                        public void onAdReceived(InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "InMobi Ad server responded with an Ad.");
                        }

                        @Override
                        public void onAdDisplayed(InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "onAdDisplayed");
                            mInterstitialListener.onAdOpened(InMobiAdapter.this);

                        }

                        @Override
                        public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "onAdDismissed");
                            mInterstitialListener.onAdClosed(InMobiAdapter.this);
                        }

                        @Override
                        public void onAdClicked(InMobiInterstitial inMobiInterstitial,
                                                Map<Object, Object> objectObjectMap) {
                            Log.d(TAG,
                                    "InterstitialClicked");
                            mInterstitialListener.onAdClicked(InMobiAdapter.this);
                        }
                    });

            if (mediationAdRequest.getKeywords() != null) {
                mAdInterstitial.setKeywords(TextUtils.join(", ", mediationAdRequest.getKeywords()));
            }

            // request params
            HashMap<String, String> paramMap =
                    InMobiAdapterUtils.createInMobiParameterMap(mediationAdRequest);
            mAdInterstitial.setExtras(paramMap);

            if (InMobiAdapter.sDisableHardwareFlag) {
                mAdInterstitial.disableHardwareAcceleration();
            }
            InMobiAdapterUtils.setGlobalTargeting(mediationAdRequest, mediationExtras);
            mAdInterstitial.load();
        } catch (SdkNotInitializedException e) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
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
                                MediationNativeListener listener,
                                Bundle serverParameters,
                                final NativeMediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        this.mNativeMedAdReq = mediationAdRequest;

        /* Logging few initial info */
        if (!sIsAppInitialized && serverParameters != null) {
            @InitializationStatus String status = InMobiSdk.init(context, serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID),
                    InMobiConsent.getConsentObj());
            sIsAppInitialized = status.equals(InitializationStatus.SUCCESS);
            if (!sIsAppInitialized) {
                listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }
        }
        this.mNativeListener = listener;

        if (!mNativeMedAdReq.isUnifiedNativeAdRequested()
                && !mNativeMedAdReq.isAppInstallAdRequested()) {
            Log.e(TAG, "Failed to request native ad. Unified Native Ad or App install Ad should " +
                    "be requested");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        try {
            mAdNative = new InMobiNative(context,
                    Long.parseLong(serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID)),
                    new NativeAdEventListener() {
                        @Override
                        public void onAdLoadSucceeded(final InMobiNative imNativeAd) {
                            System.out.println(" [ InMobi Native Ad ] : onAdLoadSucceeded ");
                            Log.d(TAG, "onAdLoadSucceeded");

                            //If no add stop at this point
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

                            if (mediationAdRequest.isUnifiedNativeAdRequested()) {
                                InMobiUnifiedNativeAdMapper inMobiUnifiedNativeAdMapper =
                                        new InMobiUnifiedNativeAdMapper(InMobiAdapter.this,
                                                imNativeAd,
                                                mIsOnlyUrl,
                                                mNativeListener);
                                inMobiUnifiedNativeAdMapper.mapUnifiedNativeAd(context);
                            } else if (mediationAdRequest.isAppInstallAdRequested()) {

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
                            mNativeListener.onAdFailedToLoad(InMobiAdapter.this,
                                    getAdRequestErrorCode(requestStatus.getStatusCode()));
                            Log.d(TAG, "onAdLoadFailed: " + requestStatus.getMessage());
                        }

                        @Override
                        public void onAdFullScreenDismissed(InMobiNative inMobiNative) {
                            Log.d(TAG, "onAdDismissed");
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
                            Log.d("InMobiAdapter", "onUserLeftApplication");
                            mNativeListener.onAdLeftApplication(InMobiAdapter.this);
                        }

                        @Override
                        public void onAdImpressed(@NonNull InMobiNative inMobiNative) {
                            Log.d(TAG, "InMobi impression recorded successfully");
                            mNativeListener.onAdImpression(InMobiAdapter.this);
                        }

                        @Override
                        public void onAdClicked(@NonNull InMobiNative inMobiNative) {
                            mNativeListener.onAdClicked(InMobiAdapter.this);
                        }

                        @Override
                        public void onAdStatusChanged(@NonNull InMobiNative inMobiNative) {

                        }
                    });

            mAdNative.setVideoEventListener(new VideoEventListener() {
                @Override
                public void onVideoCompleted(final InMobiNative inMobiNative) {
                    super.onVideoCompleted(inMobiNative);
                    Log.d(TAG, "InMobi native video ad completed");
                    mNativeListener.onVideoEnd(InMobiAdapter.this);
                }


                @Override
                public void onVideoSkipped(final InMobiNative inMobiNative) {
                    super.onVideoSkipped(inMobiNative);
                    Log.d(TAG, "InMobi native video skipped");
                }
            });
            //Setting mediation key words to native ad object
            Set<String> mediationKeyWords = mediationAdRequest.getKeywords();
            if (null != mediationKeyWords) {
                mAdNative.setKeywords(TextUtils.join(", ", mediationKeyWords));
            }

            /*
             *  extra request params : Add any other extra request params here
             *  #1. Explicitly setting mediation supply parameter to AdMob
             *  #2. Landing url
             */
            HashMap<String, String> paramMap =
                    InMobiAdapterUtils.createInMobiParameterMap(mediationAdRequest);
            mAdNative.setExtras(paramMap);

            InMobiAdapterUtils.setGlobalTargeting(mediationAdRequest, mediationExtras);

            mAdNative.load();
        } catch (SdkNotInitializedException e) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
    }
    //endregion
}
