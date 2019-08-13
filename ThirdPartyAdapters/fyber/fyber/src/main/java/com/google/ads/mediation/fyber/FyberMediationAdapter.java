package com.google.ads.mediation.fyber;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.fyber.inneractive.sdk.config.IAConfigManager;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.google.android.gms.ads.rewarded.RewardItem;

import java.util.ArrayList;
import java.util.List;

public class FyberMediationAdapter extends Adapter implements MediationBannerAdapter, MediationInterstitialAdapter, MediationRewardedVideoAdAdapter {
    /** Call initialize only once. TODO: Temporary for legacy adapter interface bridge solution */
    private boolean mInitializedCalled = false;
    /**
     * Adapter class name for logging.
     */
    static final String TAG = FyberMediationAdapter.class.getSimpleName();

    /**
     * Key to obtain App id, required for initializing Fyber's SDK.
     */
    static final String KEY_APP_ID = "applicationId";
    /**
     * Key to obtain a placement name or spot id. Required for creating a Fyber ad request
     */
    static final String KEY_SPOT_ID = "spotId";

    // Members
    // TODO: Shouldn't be a member
    private static String mAppId = null;

    // TODO: Is this really needed
    private boolean mReportedInitializationStatus = false;

    // TODO: If this instance die, why there is a isInitialized?
    private boolean mFyberSdkInitializedSucccesfully = false;

    public FyberMediationAdapter() {
        Log.d(TAG, "FyberMediationAdapter ctor");
    }

    public void loadBannerAd(MediationBannerAdConfiguration configuration, MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        Log.d(TAG, "loadBannerAd called");
        FyberBannerRenderer bannerRenderer = new FyberBannerRenderer(configuration, callback);
        bannerRenderer.render();
    }

    public void loadInterstitialAd(MediationInterstitialAdConfiguration configuration, MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        Log.d(TAG, "loadInterstitialAd called");

        FyberInterstitialRenderer interstitialRenderer = new FyberInterstitialRenderer(configuration, callback);
        interstitialRenderer.render();
    }

    public void loadRewardedAd(MediationRewardedAdConfiguration configuration, MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        Log.d(TAG, "loadRewardedAd called");
        FyberRewardedVideoRenderer rewardedVideoRenderer = new FyberRewardedVideoRenderer(configuration, callback);
        rewardedVideoRenderer.render();
    }

    public void loadNativeAd(MediationNativeAdConfiguration var1, MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> var2) {
        Log.d(TAG, "loadNativeAd called");
        var2.onFailure(String.valueOf(this.getClass().getSimpleName()).concat(" does not support native ads."));
    }

    @Override
    public void initialize(Context context, final InitializationCompleteCallback completionCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        Log.d(TAG, "FyberMediationAdapter initialize called with: " + mediationConfigurations);

        mInitializedCalled = true;
        // Get AppId from configuration
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            mAppId = serverParameters.getString(KEY_APP_ID);

            // Found an app id in server params
            if (!TextUtils.isEmpty(mAppId)) {
                break;
            }
        }

        if (TextUtils.isEmpty(mAppId)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            completionCallback.onInitializationFailed("Fyber SDK requires an appId to be configured on the AdMob console");

            // TODO: Doesn't the failed already logged
            Log.w(TAG, "No appId received from AdMob. Cannot initialize Fyber marketplace");
        } else {
            InneractiveAdManager.initialize(context, mAppId);
            IAConfigManager.addListener(new IAConfigManager.OnConfigurationReadyAndValidListener() {
                @Override
                public void onConfigurationReadyAndValid(IAConfigManager iaConfigManager, boolean success, Exception e) {
                    // Can be called more than once
                    if (!mReportedInitializationStatus) {
                        if (success) {
                            mFyberSdkInitializedSucccesfully = true;
                            completionCallback.onInitializationSucceeded();
                        } else {
                            completionCallback.onInitializationFailed("Fyber SDK initialization failed for appId = " + mAppId);
                            Log.d(TAG, "reporting initialization failed");
                        }

                        mReportedInitializationStatus = true;

                        IAConfigManager.removeListener(this);
                    }
                }
            });
        }

    }

    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
        return new VersionInfo(major, minor, micro);
    }

    public VersionInfo getSDKVersionInfo() {
        String sdkVersion = InneractiveAdManager.getVersion();
        String splits[] = sdkVersion.split("\\.");
        int major = 0;
        int minor = 0;
        int micro = 0;
        if (splits.length > 2) {
            major = Integer.parseInt(splits[0]);
            minor = Integer.parseInt(splits[1]);
            micro = Integer.parseInt(splits[2]);
        } else if (splits.length == 2) {
            major = Integer.parseInt(splits[0]);
            minor = Integer.parseInt(splits[1]);
        } else if (splits.length == 1) {
            major = Integer.parseInt(splits[0]);
        }
        return new VersionInfo(major, minor, micro);
    }

    /** ==============================================================
     * TEMPORARY code, until the new interface is working as expected
     * */

    /** MediationBannerAdapter implementation */

    @Override
    public void requestBannerAd(Context context, final MediationBannerListener mediationBannerListener, Bundle bundle, AdSize adSize, MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        Log.d(TAG, "legacy requestBannerAd called with bundle: " + bundle);
        initializeFromBundle(context, bundle, null);

        MediationBannerAdConfiguration dummyAdConfig = new MediationBannerAdConfiguration(context,
                "dummyResponse", // ?
                bundle, // Server extras
                mediationExtras, // Local extras, set by addNetworkExtrasBundle
                true, // is test ad?
                null, // Location - can be null
                0, // taggedForChildDirectedTreatment
                0, // taggedForUnderAgeTreatment
                null, // Ad content rating
                AdSize.BANNER, // Ad size
                "" // unknown
        );

        loadBannerAd(dummyAdConfig, new MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>() {
            @Override
            public MediationBannerAdCallback onSuccess(MediationBannerAd mediationBannerAd) {
                Log.d(TAG, "onSuccess callback called for banner");
                mTempBannerAd = mediationBannerAd;
                mediationBannerListener.onAdLoaded(FyberMediationAdapter.this);
                return new MediationBannerAdCallback() {
                    @Override
                    public void onAdLeftApplication() {
                        mediationBannerListener.onAdLeftApplication(FyberMediationAdapter.this);
                    }

                    @Override
                    public void reportAdClicked() {
                        mediationBannerListener.onAdClicked(FyberMediationAdapter.this);
                    }

                    @Override
                    public void reportAdImpression() {
                        // No separate callback on the old interface
                    }

                    @Override
                    public void onAdOpened() {
                        mediationBannerListener.onAdOpened(FyberMediationAdapter.this);
                    }

                    @Override
                    public void onAdClosed() {
                        mediationBannerListener.onAdClosed(FyberMediationAdapter.this);
                    }
                };
            }

            @Override
            public void onFailure(String s) {
                Log.d(TAG, "onFailure callback called for banner: " + s);
            }
        });
    }

    private MediationBannerAd mTempBannerAd;

    @Override
    public View getBannerView() {
        return mTempBannerAd.getView();
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

    /** MediationInterstitialAdapter implementation */

    @Override
    public void requestInterstitialAd(Context context, final MediationInterstitialListener mediationInterstitialListener, Bundle bundle, MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        Log.d(TAG, "legacy requestInterstitialAd called with bundle: " + bundle);
        initializeFromBundle(context, bundle, null);

        mInterstitialTempContext = context;

        MediationInterstitialAdConfiguration dummyAdConfig = new MediationInterstitialAdConfiguration(context,
                "dummyResponse", // ?
                bundle, // Server extras
                mediationExtras, // Local extras, set by addNetworkExtrasBundle
                true, // is test ad?
                null, // Location - can be null
                0, // taggedForChildDirectedTreatment
                0, // taggedForUnderAgeTreatment
                null, // Ad content rating
                "" // unknown
        );

        loadInterstitialAd(dummyAdConfig, new MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>() {
            @Override
            public MediationInterstitialAdCallback onSuccess(MediationInterstitialAd mediationInterstitialAd) {
                Log.d(TAG, "onSuccess callback called for interstitial");

                mTempInterstitialAd = mediationInterstitialAd;
                mediationInterstitialListener.onAdLoaded(FyberMediationAdapter.this);
                return new MediationInterstitialAdCallback() {
                    @Override
                    public void onAdLeftApplication() {
                        mediationInterstitialListener.onAdLeftApplication(FyberMediationAdapter.this);
                    }

                    @Override
                    public void reportAdClicked() {
                        mediationInterstitialListener.onAdClicked(FyberMediationAdapter.this);
                    }

                    @Override
                    public void reportAdImpression() {
                        // No separate callback on the old interface
                    }

                    @Override
                    public void onAdOpened() {
                        mediationInterstitialListener.onAdOpened(FyberMediationAdapter.this);
                    }

                    @Override
                    public void onAdClosed() {
                        mediationInterstitialListener.onAdClosed(FyberMediationAdapter.this);
                    }
                };
            }

            @Override
            public void onFailure(String s) {
                Log.d(TAG, "onFailure callback called for interstitial: " + s);
                mediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, 0);
            }
        });
    }

    Context mInterstitialTempContext;
    MediationInterstitialAd mTempInterstitialAd;

    @Override
    public void showInterstitial() {
        mTempInterstitialAd.showAd(mInterstitialTempContext);
    }

    /** MediationRewardedVideoAdAdapter implementation */

    Context mRewardedTempContext;
    MediationRewardedVideoAdListener mRewardedVideoAdListener;
    MediationRewardedAd mTempRewardedAd;

    @Override
    public void initialize(Context context, MediationAdRequest mediationAdRequest, String s, MediationRewardedVideoAdListener mediationRewardedVideoAdListener, Bundle bundle, Bundle bundle1) {
        Log.d(TAG, "rewarded initialize called with bundle: " + bundle);
        initializeFromBundle(context, bundle, mediationRewardedVideoAdListener);

        mRewardedTempContext = context;
        mRewardedVideoAdListener = mediationRewardedVideoAdListener;
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle bundle, Bundle mediationExtras) {
        Log.d(TAG, "legacy rewarded loadAd called with bundle: " + bundle);

        MediationRewardedAdConfiguration dummyAdConfig = new MediationRewardedAdConfiguration(mRewardedTempContext,
                "dummyResponse", // ?
                bundle, // Server extras
                mediationExtras, // Local extras, set by addNetworkExtrasBundle
                true, // is test ad?
                null, // Location - can be null
                0, // taggedForChildDirectedTreatment
                0, // taggedForUnderAgeTreatment
                null, // Ad content rating
                "" // unknown
        );

        loadRewardedAd(dummyAdConfig, new MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>() {
            @Override
            public MediationRewardedAdCallback onSuccess(MediationRewardedAd mediationRewardedAd) {
                Log.d(TAG, "rewarded onSuccess callback called");
                mTempRewardedAd = mediationRewardedAd;
                mRewardedVideoAdListener.onAdLoaded(FyberMediationAdapter.this);

                return new MediationRewardedAdCallback() {
                    @Override
                    public void onUserEarnedReward(RewardItem rewardItem) {
                        Log.d(TAG, "rewarded onUserEarnedReward callback called");
                        mRewardedVideoAdListener.onRewarded(FyberMediationAdapter.this, new com.google.android.gms.ads.reward.RewardItem() {
                            @Override
                            public String getType() {
                                return "";
                            }

                            @Override
                            public int getAmount() {
                                return 1;
                            }
                        });
                    }

                    @Override
                    public void onVideoStart() {
                        Log.d(TAG, "rewarded onVideoStart callback called");
                        mRewardedVideoAdListener.onVideoStarted(FyberMediationAdapter.this);
                    }

                    @Override
                    public void onVideoComplete() {
                        Log.d(TAG, "rewarded onVideoComplete callback called");
                        mRewardedVideoAdListener.onVideoCompleted(FyberMediationAdapter.this);
                    }

                    @Override
                    public void onAdFailedToShow(String s) {
                        Log.d(TAG, "rewarded onAdFailedToShow callback called");
                        mRewardedVideoAdListener.onAdFailedToLoad(FyberMediationAdapter.this, 0);
                    }

                    @Override
                    public void reportAdClicked() {
                        Log.d(TAG, "rewarded reportAdClicked callback called");
                        mRewardedVideoAdListener.onAdClicked(FyberMediationAdapter.this);
                    }

                    @Override
                    public void reportAdImpression() {
                        Log.d(TAG, "rewarded reportAdImpression callback called");
                        // No equivalent callback in old API
                    }

                    @Override
                    public void onAdOpened() {
                        Log.d(TAG, "rewarded onAdOpened callback called");
                        mRewardedVideoAdListener.onAdOpened(FyberMediationAdapter.this);
                    }

                    @Override
                    public void onAdClosed() {
                        Log.d(TAG, "rewarded onAdClosed callback called");
                        mRewardedVideoAdListener.onAdClosed(FyberMediationAdapter.this);
                    }
                };
            }

            @Override
            public void onFailure(String s) {
                Log.d(TAG, "rewarded onFailure callback called");
                mRewardedVideoAdListener.onAdFailedToLoad(FyberMediationAdapter.this, 0);
            }
        });
    }

    @Override
    public void showVideo() {
        mTempRewardedAd.showAd(mRewardedTempContext);
    }

    @Override
    public boolean isInitialized() {
        return mFyberSdkInitializedSucccesfully;
    }

    private class DummyInitializationCallback implements InitializationCompleteCallback {
        MediationRewardedVideoAdListener mTempInitializationCallback = null;
        public DummyInitializationCallback() {

        }

        public DummyInitializationCallback(MediationRewardedVideoAdListener externalListener) {
            mTempInitializationCallback = externalListener;
        }

        @Override
        public void onInitializationSucceeded() {
            if (mTempInitializationCallback != null) {
                Log.d(TAG, "onInitializationSucceeded callback - reporting legacy rewarded initialization callback");
                mTempInitializationCallback.onInitializationSucceeded(FyberMediationAdapter.this);
            }

            Log.d(TAG, "onInitializationSucceeded callback called");
        }

        @Override
        public void onInitializationFailed(String s) {
            if (mTempInitializationCallback != null) {
                mTempInitializationCallback.onInitializationFailed(FyberMediationAdapter.this, 0);
            }

            Log.d(TAG, "onInitializationFailed callback called");
        }
    }

    // Helper method for bridging the initialization method
    private void initializeFromBundle(Context context, Bundle bundle, MediationRewardedVideoAdListener oldInterfaceRewardedInitializtionCallback) {
        if (mInitializedCalled == false) {
            List<MediationConfiguration> configs = new ArrayList<>();
            configs.add(new MediationConfiguration(AdFormat.BANNER, bundle));

            initialize(context, new DummyInitializationCallback(oldInterfaceRewardedInitializtionCallback), configs);
        }
    }
}
