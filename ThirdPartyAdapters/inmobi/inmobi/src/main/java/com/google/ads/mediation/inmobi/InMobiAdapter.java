
package com.google.ads.mediation.inmobi;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
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
import com.inmobi.ads.InMobiBanner.BannerAdListener;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.InMobiNative;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.InMobiSdk.LogLevel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * InMobi Adapter for AdMob Mediation. This class should not be used directly
 * by publishers.
 */
public final class InMobiAdapter
        implements
        MediationInterstitialAdapter,
        MediationBannerAdapter, MediationRewardedVideoAdAdapter, MediationNativeAdapter {

    // Callback listeners.
    private MediationBannerListener bannerListener;
    private MediationInterstitialListener interstitialListener;
    private MediationRewardedVideoAdListener rewardedVideoAdListener;
    private MediationNativeListener nativeListener;

    private InMobiInterstitial adInterstitial;
    private InMobiInterstitial adRewarded;
    private FrameLayout wrappedAdView;
    private static Boolean disableHardwareFlag = false;
    private static Boolean isAppInitialized = false;
    private String key = "";
    private String value = "";

    private final InMobiAdapter self = this;
    private NativeMediationAdRequest _nativeMedAdReq;

    private Boolean isOnlyUrl = false;


    @Override
    public void showInterstitial() {
        if (adInterstitial.isReady()) {
            Log.d("InMobiAdapter", "Ad is ready to show");
            adInterstitial.show();
        }
    }


    @Override
    public View getBannerView() {
        return wrappedAdView;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void requestBannerAd(Context context, MediationBannerListener listener,
                                Bundle serverParameters, AdSize mediationAdSize,
                                MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        if (!isAppInitialized && serverParameters != null) {
            Log.d("InMobiAdapter", serverParameters.getString("accountid"));
            Log.d("InMobiAdapter", serverParameters.getString("placementid"));

            InMobiSdk.init(context, serverParameters.getString("accountid"));
            isAppInitialized = true;
        }
        if (Build.VERSION.SDK_INT < 14) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        this.bannerListener = listener;

        FrameLayout.LayoutParams wrappedLayoutParams = new FrameLayout.LayoutParams(
                mediationAdSize.getWidthInPixels(context),
                mediationAdSize.getHeightInPixels(context));
        InMobiBanner adView;
        if (serverParameters != null) {
            if (context instanceof Activity) {

                adView = new InMobiBanner((Activity) context, Long.parseLong(serverParameters.getString
                        ("placementid")));
            } else {
                adView = new InMobiBanner(context, Long.parseLong(serverParameters.getString
                        ("placementid")));
            }
        } else {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        // Turn off automatic refresh
        //adView.setRefreshInterval(-1);
        adView.setEnableAutoRefresh(false);
        // Turn off the animation
        adView.setAnimationType(AnimationType.ANIMATION_OFF);
        if (mediationAdRequest.getKeywords() != null) {
            adView.setKeywords(TextUtils.join(", ",
                    mediationAdRequest.getKeywords()));
        }

        // request params
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("tp", "c_admob");

        isTaggedForChildDirectedTreatment(mediationAdRequest, paramMap);

        adView.setExtras(paramMap);

        if (mediationExtras == null) {
            mediationExtras = new Bundle();
        }
        adView.setListener(new BannerAdListener() {

            @Override
            public void onUserLeftApplication(InMobiBanner arg0) {
                Log.d("InMobiAdapter", "onUserLeftApplication");
                bannerListener.onAdLeftApplication(InMobiAdapter.this);
            }

            @Override
            public void onAdRewardActionCompleted(InMobiBanner arg0,
                                                  Map<Object, Object> rewards) {
                Log.d("InMobiAdapter", "InMobi Banner onRewardActionCompleted.");

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
            public void onAdLoadSucceeded(InMobiBanner arg0) {
                System.out.println("onLoadSucceeded");
                Log.d("InMobiAdapter", "onAdLoadSucceeded");

                bannerListener.onAdLoaded(InMobiAdapter.this);
            }

            @Override
            public void onAdLoadFailed(InMobiBanner arg0, InMobiAdRequestStatus arg1) {
                switch (arg1.getStatusCode()) {
                    case INTERNAL_ERROR:
                        bannerListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;

                    case REQUEST_INVALID:
                        bannerListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;

                    case NETWORK_UNREACHABLE:
                        bannerListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_NETWORK_ERROR);
                        break;

                    case NO_FILL:
                        bannerListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                        break;

                    default:
                        bannerListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;

                }
                Log.d("InMobiBanner", arg1.getMessage());
            }

            @Override
            public void onAdDisplayed(InMobiBanner arg0) {
                Log.d("InMobiAdapter", "onAdDismissed");
                bannerListener.onAdOpened(InMobiAdapter.this);
            }

            @Override
            public void onAdDismissed(InMobiBanner arg0) {
                Log.d("InMobiAdapter", "onAdDismissed");
                bannerListener.onAdClosed(InMobiAdapter.this);
            }

            @Override
            public void onAdInteraction(InMobiBanner arg0,
                                        Map<Object, Object> arg1) {
                Log.d("onBannerInteraction", "onBannerInteraction called");
                bannerListener.onAdClicked(InMobiAdapter.this);
            }
        });
        if (InMobiAdapter.disableHardwareFlag) {
            adView.disableHardwareAcceleration();
        }
        /*
         * We wrap the ad View in a FrameLayout to ensure that it's the right
		 * size. Without this the ad takes up the maximum width possible,
		 * causing artifacts on high density screens (like the Galaxy Nexus) or
		 * in landscape view. If the underlying library sets the appropriate
		 * size instead of match_parent, this wrapper can be removed.
		 */
        wrappedAdView = new FrameLayout(context);
        wrappedAdView.setLayoutParams(wrappedLayoutParams);
        adView.setLayoutParams(new LinearLayout.LayoutParams(mediationAdSize.getWidthInPixels
                (context), mediationAdSize.getHeightInPixels(context)));
        wrappedAdView.addView(adView);
        InMobiAdapterUtils.buildAdRequest(mediationAdRequest, mediationExtras);
        adView.load();
    }

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener, Bundle
                                              serverParameters,
                                      MediationAdRequest mediationAdRequest, Bundle
                                              mediationExtras) {

        final Activity activity;

        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            Log.w("InMobiAdapter", "Context not an Activity. Returning error!");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_NO_FILL);
            return;
        }

        if (!isAppInitialized) {
            InMobiSdk.init(context, serverParameters.getString("accountid"));
            isAppInitialized = true;
        }

        // We dont support android version below 4.0
        if (Build.VERSION.SDK_INT < 14) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        this.interstitialListener = listener;

        adInterstitial = new InMobiInterstitial(activity, Long.parseLong(serverParameters
                .getString("placementid")), new InMobiInterstitial.InterstitialAdListener2() {

            @Override
            public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
                Log.d("InMobiAdapter", "onUserLeftApplication");
                interstitialListener.onAdLeftApplication(InMobiAdapter.this);
            }

            @Override
            public void onAdRewardActionCompleted(InMobiInterstitial inMobiInterstitial,
                                                  Map<Object, Object> rewards) {
                Log.d("InMobiAdapter", "InMobi Interstitial onRewardActionCompleted.");

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
                Log.d("InMobiAdapter", "Ad Display failed.");
            }

            @Override
            public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
                Log.d("InMobiAdapter", "Ad Will Display.");
            }

            @Override
            public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial) {
                Log.d("InMobiAdapter", "onAdLoadSucceeded");
                interstitialListener.onAdLoaded(InMobiAdapter.this);
            }

            @Override
            public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                                       InMobiAdRequestStatus arg1) {
                switch (arg1.getStatusCode()) {

                    case INTERNAL_ERROR:
                        interstitialListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                    case REQUEST_INVALID:
                        interstitialListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;

                    case NETWORK_UNREACHABLE:
                        interstitialListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_NETWORK_ERROR);
                        break;

                    case NO_FILL:
                        interstitialListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                        break;


                    default:
                        interstitialListener.onAdFailedToLoad(InMobiAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                }
                Log.d("InMobiAdapter", "onAdLoadFailed");

            }

            @Override
            public void onAdReceived(InMobiInterstitial inMobiInterstitial) {
                Log.d("InMobiAdapter", "InMobi Ad server responded with an Ad.");
            }

            @Override
            public void onAdDisplayed(InMobiInterstitial inMobiInterstitial) {
                Log.d("InMobiAdapter", "onAdDisplayed");
                interstitialListener.onAdOpened(InMobiAdapter.this);

            }

            @Override
            public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
                Log.d("InMobiAdapter", "onAdDismissed");
                interstitialListener.onAdClosed(InMobiAdapter.this);
            }

            @Override
            public void onAdInteraction(InMobiInterstitial inMobiInterstitial,
                                        Map<Object, Object> objectObjectMap) {
                Log.d("InMobiAdapter",
                        "InterstitialInteraction");
                interstitialListener.onAdClicked(InMobiAdapter.this);
            }
        });

        if (mediationAdRequest.getKeywords() != null) {
            adInterstitial.setKeywords(TextUtils.join(", ",
                    mediationAdRequest.getKeywords()));
        }

        // request params
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("tp", "c_admob");

        isTaggedForChildDirectedTreatment(mediationAdRequest, paramMap);

        adInterstitial.setExtras(paramMap);

        if (InMobiAdapter.disableHardwareFlag) {
            adInterstitial.disableHardwareAcceleration();
        }
        InMobiAdapterUtils.buildAdRequest(mediationAdRequest, mediationExtras);
        adInterstitial.load();
    }

    private void isTaggedForChildDirectedTreatment(MediationAdRequest mediationAdRequest,
                                                   HashMap<String, String> paramMap) {
        if (mediationAdRequest.taggedForChildDirectedTreatment() == 1) {
            paramMap.put("coppa", "1");
        } else
            paramMap.put("coppa", "0");
    }

    @Override
    public void initialize(Context context, MediationAdRequest mediationAdRequest, String s,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters, Bundle networkExtras) {
        Log.d("InMobiAdapter", "initialize called from InMobiAdapter.");
        this.rewardedVideoAdListener = mediationRewardedVideoAdListener;
        String accountId = serverParameters.getString("accountid");

        Activity activity;

        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            Log.w("InMobiAdapter", "Context not an Activity. Returning error!");
            mediationRewardedVideoAdListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_NO_FILL);
            return;
        }

        if (!isAppInitialized) {
            InMobiSdk.init(context, accountId);
            isAppInitialized = true;
        }

        String placementId = serverParameters.getString("placementid");
        adRewarded = new InMobiInterstitial(activity, Long.parseLong(placementId), new
                InMobiInterstitial.InterstitialAdListener2() {
                    @Override
                    public void onAdRewardActionCompleted(InMobiInterstitial inMobiInterstitial,
                                                          Map<Object, Object> rewards) {
                        Log.d("InMobiAdapter", "InMobi RewardedVideo onRewardActionCompleted.");
                        if (null != rewards) {
                            for (Object reward : rewards.keySet()) {
                                key = reward.toString();
                                value = rewards.get(key).toString();
                                Log.d("Rewards: ", key + ":" + value);
                            }
                        }

                        rewardedVideoAdListener.onRewarded(InMobiAdapter.this, new RewardItem() {
                            @Override
                            public String getType() {
                                return key;
                            }

                            @Override
                            public int getAmount() {
                                if (null != value && !"".equalsIgnoreCase(value)) {
                                    try {
                                        return Integer.parseInt(value);
                                    } catch (NumberFormatException nfe) {
                                        Log.e("InMobiAdapter", "Reward value should be of type " +
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
                        Log.d("InMobiAdapter", "Ad Display failed.");
                    }

                    @Override
                    public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
                        Log.d("InMobiAdapter", "Ad Will Display.");
                    }

                    @Override
                    public void onAdDisplayed(InMobiInterstitial inMobiInterstitial) {
                        Log.d("InMobiAdapter", "onAdDisplayed");
                        rewardedVideoAdListener.onAdOpened(InMobiAdapter.this);
                        rewardedVideoAdListener.onVideoStarted(InMobiAdapter.this);
                    }

                    @Override
                    public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
                        Log.d("InMobiAdapter", "onAdDismissed");
                        rewardedVideoAdListener.onAdClosed(InMobiAdapter.this);
                    }

                    @Override
                    public void onAdInteraction(InMobiInterstitial inMobiInterstitial, Map<Object,
                            Object> map) {
                        Log.d("InMobiAdapter",
                                "onInterstitialInteraction called");
                        rewardedVideoAdListener.onAdClicked(InMobiAdapter.this);
                    }

                    @Override
                    public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial) {
                        Log.d("InMobiAdapter", "onAdLoadSucceeded");
                        rewardedVideoAdListener.onAdLoaded(InMobiAdapter.this);
                    }

                    @Override
                    public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                                               InMobiAdRequestStatus inMobiAdRequestStatus) {
                        switch (inMobiAdRequestStatus.getStatusCode()) {

                            case INTERNAL_ERROR:
                                rewardedVideoAdListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                break;
                            case REQUEST_INVALID:
                                rewardedVideoAdListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_INVALID_REQUEST);
                                break;

                            case NETWORK_UNREACHABLE:
                                rewardedVideoAdListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_NETWORK_ERROR);
                                break;

                            case NO_FILL:
                                rewardedVideoAdListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_NO_FILL);
                                break;

                            default:
                                rewardedVideoAdListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                break;
                        }
                        Log.d("InMobiAdapter", "onAdLoadFailed");
                    }

                    @Override
                    public void onAdReceived(InMobiInterstitial inMobiInterstitial) {
                        Log.d("InMobiAdapter", "InMobi Ad server responded with an Ad.");
                    }

                    @Override
                    public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
                        Log.d("InMobiAdapter", "onUserLeftApplication");
                        rewardedVideoAdListener.onAdLeftApplication(InMobiAdapter.this);
                    }
                });

        this.rewardedVideoAdListener.onInitializationSucceeded(this);

        if (mediationAdRequest.getKeywords() != null) {
            Log.d("InMobiAdapter", "keyword is present:" + mediationAdRequest.getKeywords()
                    .toString());
            adRewarded.setKeywords(TextUtils.join(", ",
                    mediationAdRequest.getKeywords()));
        }

        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("tp", "c_admob");

        isTaggedForChildDirectedTreatment(mediationAdRequest, paramMap);

        adRewarded.setExtras(paramMap);

        if (InMobiAdapter.disableHardwareFlag) {
            adRewarded.disableHardwareAcceleration();
        }
        InMobiAdapterUtils.buildAdRequest(mediationAdRequest, networkExtras);

    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle
            networkExtras) {
        if (adRewarded != null) {
            adRewarded.load();
        }
    }

    @Override
    public void showVideo() {
        if (adRewarded.isReady()) {
            adRewarded.show();
        }
    }

    @Override
    public boolean isInitialized() {
        return isAppInitialized;
    }


    @Override
    /**
     * Override method to Mediate Native Ads from InMobi
     */
    public void requestNativeAd(final Context context, MediationNativeListener listener, Bundle
            serverParameters,
                                NativeMediationAdRequest mediationAdRequest, Bundle
                                        mediationExtras) {

        this._nativeMedAdReq = mediationAdRequest;

		/* Logging few initial info */

        if (!isAppInitialized && serverParameters != null) {
            InMobiSdk.init(context, serverParameters.getString("accountid"));
            isAppInitialized = true;
        }

        InMobiSdk.setLogLevel(LogLevel.DEBUG);

        if (Build.VERSION.SDK_INT < 14) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        this.nativeListener = listener;

        final Boolean serveAnyAd = (mediationAdRequest.isAppInstallAdRequested() &&
                mediationAdRequest.isContentAdRequested());

        /**
         * InMobi Adapter will serve ad only if publisher requests for both AppInstall and Content
         * Ads else we will give No-Fill to Publisher
         */

        if (!serveAnyAd) {
            this.nativeListener.onAdFailedToLoad(InMobiAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
            return;
        }

        InMobiNative adNative = new InMobiNative(Long.parseLong(serverParameters.getString
                ("placementid")),
                new InMobiNative.NativeAdListener() {

                    @Override
                    public void onUserLeftApplication(InMobiNative arg0) {
                        Log.d("InMobiAdapter", "onUserLeftApplication");
                        nativeListener.onAdClicked(InMobiAdapter.this);
                        nativeListener.onAdOpened(InMobiAdapter.this);
                        nativeListener.onAdLeftApplication(InMobiAdapter.this);
                    }

                    @Override
                    public void onAdLoadSucceeded(final InMobiNative imNativeAd) {
                        System.out.println(" [ InMobi Native Ad ] : onAdLoadSucceeded ");
                        Log.d("InMobiAdapter", "onAdLoadSucceeded");

                        //If no add stop at this point
                        if (null == imNativeAd) {
                            return;
                        }

                        //This setting decides whether to download images or not

                        NativeAdOptions nativeAdOptions = self._nativeMedAdReq.getNativeAdOptions();


                        if (null != nativeAdOptions)
                            isOnlyUrl = nativeAdOptions.shouldReturnUrlsForImageAssets();

                        InMobiAppInstallNativeAdMapper inMobiAppInstallNativeAdMapper = new
                                InMobiAppInstallNativeAdMapper(InMobiAdapter.this,
                                imNativeAd, isOnlyUrl, nativeListener);
                        inMobiAppInstallNativeAdMapper.mapAppInstallAd();
                    }

                    @Override
                    public void onAdLoadFailed(InMobiNative arg0, InMobiAdRequestStatus arg1) {
                        switch (arg1.getStatusCode()) {
                            case INTERNAL_ERROR:
                                nativeListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                break;

                            case REQUEST_INVALID:
                                nativeListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_INVALID_REQUEST);
                                break;

                            case NETWORK_UNREACHABLE:
                                nativeListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_NETWORK_ERROR);
                                break;

                            case NO_FILL:
                                nativeListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_NO_FILL);
                                break;

                            default:
                                nativeListener.onAdFailedToLoad(InMobiAdapter.this,
                                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                break;

                        }
                        Log.d(" InMobiNativeAd ", arg1.getMessage());
                    }

                    @Override
                    public void onAdDisplayed(InMobiNative arg0) {
                        Log.d("InMobiAdapter", "onAdDisplayed");
                        nativeListener.onAdOpened(InMobiAdapter.this);
                    }

                    @Override
                    public void onAdDismissed(InMobiNative arg0) {
                        Log.d("InMobiAdapter", "onAdDismissed");
                        nativeListener.onAdClosed(InMobiAdapter.this);
                    }
                });

        adNative.setNativeAdEventListener(new InMobiNative.NativeAdEventsListener() {
            @Override
            public void onAdImpressed(InMobiNative inMobiNative) {
                Log.d("InMobiAdapter", "InMobi impression recorded successfully");
                nativeListener.onAdImpression(InMobiAdapter.this);
            }
        });

        //Setting mediation key words to native ad object
        Set<String> mediationKeyWords = mediationAdRequest.getKeywords();
        if (null != mediationKeyWords) {
            adNative.setKeywords(TextUtils.join(", ", mediationKeyWords));
        }

		/*
         *  extra request params : Add any other extra request params here
		 *  #1. Explicitly setting mediation supply parameter to AdMob
		 *  #2. Landing url
		 */
        HashMap<String, String> paramMap = new HashMap<>();

        paramMap.put("tp", "c_admob");

        isTaggedForChildDirectedTreatment(mediationAdRequest, paramMap);

        adNative.setExtras(paramMap);

        InMobiAdapterUtils.buildAdRequest(mediationAdRequest, mediationExtras);

        adNative.load();

    }
}