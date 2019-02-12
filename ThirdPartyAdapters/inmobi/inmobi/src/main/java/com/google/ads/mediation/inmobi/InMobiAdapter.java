
package com.google.ads.mediation.inmobi;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

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
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.InMobiBanner.AnimationType;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.listeners.BannerAdEventListener;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.ads.listeners.NativeAdEventListener;
import com.inmobi.sdk.InMobiSdk;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * InMobi Adapter for AdMob Mediation used to load and show banner, interstitial, rewarded video and
 * native ads. This class should not be used directly by publishers.
 */
public final class InMobiAdapter implements MediationBannerAdapter, MediationInterstitialAdapter,
        MediationRewardedVideoAdAdapter, MediationNativeAdapter {
    private static final String TAG = InMobiAdapter.class.getSimpleName();

    // Callback listeners.
    private MediationBannerListener mBannerListener;
    private MediationInterstitialListener mInterstitialListener;
    private MediationRewardedVideoAdListener mRewardedVideoAdListener;
    private MediationNativeListener mNativeListener;

    private InMobiInterstitial mAdInterstitial;
    private InMobiInterstitial mAdRewarded;
    private FrameLayout mWrappedAdView;

    private static Boolean sDisableHardwareFlag = false;
    private static Boolean sIsAppInitialized = false;

    private String mKey = "";
    private String mValue = "";

    private NativeMediationAdRequest mNativeMedAdReq;

    private Boolean mIsOnlyUrl = false;

    private InMobiNative mAdNative;

    /**
     * Flag to keep track of whether or not the InMobi rewarded video ad adapter has been
     * initialized.
     */
    private boolean mIsRewardedVideoAdAdapterInitialized;

    public static Boolean IsAppInitialized() {
        return sIsAppInitialized;
    }

    private void isTaggedForChildDirectedTreatment(MediationAdRequest mediationAdRequest,
                                                   HashMap<String, String> paramMap) {
        if (mediationAdRequest.taggedForChildDirectedTreatment()
                == MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
            paramMap.put("coppa", "1");
        } else
            paramMap.put("coppa", "0");
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
        if (!sIsAppInitialized && serverParameters != null) {
            Log.d(TAG, serverParameters.getString("accountid"));
            Log.d(TAG, serverParameters.getString("placementid"));

            InMobiSdk.init(context, serverParameters.getString("accountid"), InMobiConsent.getConsentObj());
            sIsAppInitialized = true;
        }
        this.mBannerListener = listener;

        FrameLayout.LayoutParams wrappedLayoutParams = new FrameLayout.LayoutParams(
                mediationAdSize.getWidthInPixels(context),
                mediationAdSize.getHeightInPixels(context));
        InMobiBanner adView;
        if (serverParameters != null) {
            if (context instanceof Activity) {
                adView = new InMobiBanner((Activity) context,
                        Long.parseLong(serverParameters.getString("placementid")));
            } else {
                adView = new InMobiBanner(context,
                        Long.parseLong(serverParameters.getString("placementid")));
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
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("tp", "c_admob");

        isTaggedForChildDirectedTreatment(mediationAdRequest, paramMap);

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
                        InMobiAdapter.this, getAdRequestErrorCode(requestStatus.getStatusCode()));
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
        InMobiAdapterUtils.buildAdRequest(mediationAdRequest, mediationExtras);
        adView.load();
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
            InMobiSdk.init(context, serverParameters.getString("accountid"), InMobiConsent.getConsentObj());
            sIsAppInitialized = true;
        }

        this.mInterstitialListener = listener;

        mAdInterstitial = new InMobiInterstitial(context, Long.parseLong(serverParameters
                .getString("placementid")), new  InterstitialAdEventListener() {

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
                        Log.d("Rewards: ", key + ":" + value);
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
                        InMobiAdapter.this, getAdRequestErrorCode(requestStatus.getStatusCode()));
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
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("tp", "c_admob");

        isTaggedForChildDirectedTreatment(mediationAdRequest, paramMap);

        mAdInterstitial.setExtras(paramMap);

        if (InMobiAdapter.sDisableHardwareFlag) {
            mAdInterstitial.disableHardwareAcceleration();
        }
        InMobiAdapterUtils.buildAdRequest(mediationAdRequest, mediationExtras);
        mAdInterstitial.load();
    }

    @Override
    public void showInterstitial() {
        if (mAdInterstitial.isReady()) {
            Log.d(TAG, "Ad is ready to show");
            mAdInterstitial.show();
        }
    }
    //endregion

    //region MediationRewardedVideoAdAdapter implementation.
    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String unused,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        Log.d(TAG, "initialize called from InMobiAdapter.");
        this.mRewardedVideoAdListener = mediationRewardedVideoAdListener;
        String accountId = serverParameters.getString("accountid");

        if (!sIsAppInitialized) {
            InMobiSdk.init(context, accountId, InMobiConsent.getConsentObj());
            sIsAppInitialized = true;
        }

        String placementId = serverParameters.getString("placementid");
        mAdRewarded = new InMobiInterstitial(context, Long.parseLong(placementId), new  InterstitialAdEventListener(){
            @Override
            public void onRewardsUnlocked(InMobiInterstitial inMobiInterstitial,
                                          Map<Object, Object> rewards) {
                Log.d(TAG, "InMobi RewardedVideo onRewardsUnlocked.");
                if (null != rewards) {
                    for (Object reward : rewards.keySet()) {
                        mKey = reward.toString();
                        mValue = rewards.get(mKey).toString();
                        Log.d("Rewards: ", mKey + ":" + mValue);
                    }
                }
                mRewardedVideoAdListener.onVideoCompleted(InMobiAdapter.this);
                mRewardedVideoAdListener.onRewarded(InMobiAdapter.this, new RewardItem() {
                    @Override
                    public String getType() {
                        return mKey;
                    }
                    @Override
                    public int getAmount() {
                        if (null != mValue && !"".equalsIgnoreCase(mValue)) {
                            try {
                                return Integer.parseInt(mValue);
                            } catch (NumberFormatException nfe) {
                                Log.e(TAG, "Reward value should be of type " +
                                        "integer:" + nfe.getMessage());
                                nfe.printStackTrace();
                                return 0;
                            }
                        } else
                            return 0;
                    }
                });
            }

            @Override
            public void onAdDisplayFailed(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "Ad Display failed.");
            }

            @Override
            public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "Ad Will Display.");
            }

            @Override
            public void onAdDisplayed(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onAdDisplayed");
                mRewardedVideoAdListener.onAdOpened(InMobiAdapter.this);
                mRewardedVideoAdListener.onVideoStarted(InMobiAdapter.this);
            }

            @Override
            public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onAdDismissed");
                mRewardedVideoAdListener.onAdClosed(InMobiAdapter.this);
            }

            @Override
            public void onAdClicked(InMobiInterstitial inMobiInterstitial, Map<Object,
                    Object> map) {
                Log.d(TAG,
                        "onInterstitialClicked called");
                mRewardedVideoAdListener.onAdClicked(InMobiAdapter.this);
            }

            @Override
            public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onAdLoadSucceeded");
                mRewardedVideoAdListener.onAdLoaded(InMobiAdapter.this);
            }

            @Override
            public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                                       InMobiAdRequestStatus inMobiAdRequestStatus) {
                mRewardedVideoAdListener.onAdFailedToLoad(InMobiAdapter.this,
                        getAdRequestErrorCode(inMobiAdRequestStatus.getStatusCode()));
                Log.d(TAG, "onAdLoadFailed: " + inMobiAdRequestStatus.getMessage());
            }

            @Override
            public void onAdReceived(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "InMobi Ad server responded with an Ad.");
            }

            @Override
            public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onUserLeftApplication");
                mRewardedVideoAdListener.onAdLeftApplication(InMobiAdapter.this);
            }
        });

        mIsRewardedVideoAdAdapterInitialized = true;
        this.mRewardedVideoAdListener.onInitializationSucceeded(this);

        if (mediationAdRequest.getKeywords() != null) {
            Log.d(TAG, "keyword is present:" + mediationAdRequest.getKeywords()
                    .toString());
            mAdRewarded.setKeywords(TextUtils.join(", ",
                    mediationAdRequest.getKeywords()));
        }

        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("tp", "c_admob");

        isTaggedForChildDirectedTreatment(mediationAdRequest, paramMap);

        mAdRewarded.setExtras(paramMap);

        if (InMobiAdapter.sDisableHardwareFlag) {
            mAdRewarded.disableHardwareAcceleration();
        }
        InMobiAdapterUtils.buildAdRequest(mediationAdRequest, networkExtras);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle networkExtras) {
        if (mAdRewarded != null) {
            mAdRewarded.load();
        }
    }

    @Override
    public void showVideo() {
        if (mAdRewarded.isReady()) {
            mAdRewarded.show();
        }
    }

    @Override
    public boolean isInitialized() {
        return mIsRewardedVideoAdAdapterInitialized && sIsAppInitialized;
    }
    //endregion

    //region MediationNativeAdapter implementation.
    @Override
    public void requestNativeAd(final Context context,
                                MediationNativeListener listener,
                                Bundle serverParameters,
                                NativeMediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        this.mNativeMedAdReq = mediationAdRequest;

       /* Logging few initial info */
        if (!sIsAppInitialized && serverParameters != null) {
            InMobiSdk.init(context, serverParameters.getString("accountid"), InMobiConsent.getConsentObj());
            sIsAppInitialized = true;
        }
        this.mNativeListener = listener;

        final Boolean serveAnyAd = (mediationAdRequest.isAppInstallAdRequested()
                && mediationAdRequest.isContentAdRequested()) || mediationAdRequest.isUnifiedNativeAdRequested();;

       /*
        * InMobi Adapter will serve ad only if publisher requests for both AppInstall and Content
        * Ads else we will give No-Fill to Publisher
        */
        if (!serveAnyAd) {
            this.mNativeListener
                    .onAdFailedToLoad(InMobiAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdNative = new InMobiNative(context,
                Long.parseLong(serverParameters.getString("placementid")),
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


                        if (null != nativeAdOptions)
                            mIsOnlyUrl = nativeAdOptions.shouldReturnUrlsForImageAssets();

                        InMobiAppInstallNativeAdMapper inMobiAppInstallNativeAdMapper =
                                new InMobiAppInstallNativeAdMapper(
                                        InMobiAdapter.this,
                                        imNativeAd,
                                        mIsOnlyUrl,
                                        mNativeListener);
                        inMobiAppInstallNativeAdMapper.mapAppInstallAd(context);
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
                    public void onAdStatusChanged(@NonNull InMobiNative inMobiNative){

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
        HashMap<String, String> paramMap = new HashMap<>();

        paramMap.put("tp", "c_admob");

        isTaggedForChildDirectedTreatment(mediationAdRequest, paramMap);

        mAdNative.setExtras(paramMap);

        InMobiAdapterUtils.buildAdRequest(mediationAdRequest, mediationExtras);

        mAdNative.load();
    }
    //endregion
}

