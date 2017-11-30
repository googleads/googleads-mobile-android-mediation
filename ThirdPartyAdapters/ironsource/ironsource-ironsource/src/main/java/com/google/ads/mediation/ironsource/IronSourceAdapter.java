package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.google.android.gms.common.GoogleApiAvailability;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.config.ConfigFile;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.InterstitialListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoListener;

public class IronSourceAdapter implements MediationInterstitialAdapter, MediationRewardedVideoAdAdapter,
        RewardedVideoListener, InterstitialListener {

    /**
     * Adapter class name for logging
     */
    private static final String TAG = IronSourceAdapter.class.getSimpleName();

    /**
     * The current version of the adapter.
     */
    private static final String ADAPTER_VERSION_NAME = BuildConfig.VERSION_NAME;

    /**
     * Key to obtain App Key, required for initializing IronSource SDK.
     */
    private static final String KEY_APP_KEY = "appKey";

    /**
     * Key to obtain isTestEnabled flag, used to control console logs display
     */
    private static final String KEY_TEST_MODE = "isTestEnabled";

    /**
     * Key to obtain Rewarded Video placement name
     */
    private static final String KEY_RV_PLACEMENT = "rewardedVideoPlacement";

    /**
     * Key to obtain Interstitial placement name
     */
    private static final String KEY_IS_PLACEMENT = "interstitialPlacement";

    /**
     * Constant used for IronSource internal reporting
     */
    private static final String MEDIATION_NAME = "AdMob";

    /**
     * This is used for show logs inside the adapter
     */
    private boolean mIsTestEnabled;

    /**
     * This is the placement name used for Interstitial
     */
    private String mInterstitialPlacementName;

    /**
     * This is the placement name used for Rewarded Video
     */
    private String mRewardedVideoPlacementName;

    /**
     * Flag to keep track of whether or not this {@link IronSourceAdapter} is initialized.
     */
    private boolean mIsInitialized;

    /**
     * UI thread handler used to send callbacks with AdMob interface
     */
    private Handler mUIHandler;

    /**
     * Mediation rewarded video ad listener used to forward reward-based video ad events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;

    /**
     * Mediation interstitial ad listener used to forward interstitial events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mInterstitialListener;

    /**
     * Private IronSource methods
     */

    private synchronized void initIronSourceSDK(Context context, Bundle serverParameters, IronSource.AD_UNIT adUnit) {
        // 1 - We are not sending user ID from adapters anymore,
        //     the IronSource SDK will take care of this identifier

        // 2 - We assume the init is always successful (we will fail in load if needed)

        // SDK requires activity context to initialize, so check that the context
        // provided by the app is an activity context before initializing.
        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            Log.d(TAG, "IronSource SDK requires an Activity context to initialize");
            switch (adUnit) {
                case REWARDED_VIDEO:
                    mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                    break;
                case INTERSTITIAL:
                    onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                    break;
            }
            return;
        }

        try {
            mIsTestEnabled = serverParameters.getBoolean(KEY_TEST_MODE, false);

            String appKey = serverParameters.getString(KEY_APP_KEY);

            onLog("Server params | appKey: " + appKey + " | isTestEnabled: " + mIsTestEnabled + " | placementName: " + mRewardedVideoPlacementName);

            if (!TextUtils.isEmpty(appKey)) {
                // Everything is ok, continue with IronSource initialization
                ConfigFile.getConfigFile().setPluginData(MEDIATION_NAME, ADAPTER_VERSION_NAME,
                        String.valueOf(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE));
                IronSource.init((Activity) context, appKey, adUnit);
                mIsInitialized = true;

                switch (adUnit) {
                    case REWARDED_VIDEO:
                        // in case of Rewarded Video ad unit we report init success
                        onRewardedVideoInitSuccess();
                        break;
                    case INTERSTITIAL:
                        // in case of Interstitial ad unit we continue with loading the interstitial ad
                        break;
                }
            } else {
                onLog("onInitializationFailed, make sure that 'appKey' server parameter is added");
                switch (adUnit) {
                    case REWARDED_VIDEO:
                        mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    case INTERSTITIAL:
                        onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                }
            }
        } catch (Exception e) {
            onLog("onInitializationFailed, error: " + e.getMessage());
            switch (adUnit) {
                case REWARDED_VIDEO:
                    mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    break;
                case INTERSTITIAL:
                    onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    break;
            }
        }
    }

    private void onRewardedVideoInitSuccess() {
        if (mMediationRewardedVideoAdListener != null) {

            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onInitializationSucceeded");
                    mMediationRewardedVideoAdListener.onInitializationSucceeded(IronSourceAdapter.this);
                }
            });
        }
    }

    private void loadISIronSourceSDK() {
        onLog("loadInterstitial");
        if (IronSource.isInterstitialReady()) {
            onInterstitialAdReady();
        } else {
            IronSource.loadInterstitial();
        }
    }

    private void onLog(String message) {
        if (mIsTestEnabled) {
            Log.d(TAG, message);
        }
    }

    private void sendEventOnUIThread(Runnable runnable) {
        if (mUIHandler == null) {
            mUIHandler = new Handler(Looper.getMainLooper());
        }
        mUIHandler.post(runnable);
    }

    private void onISAdFailedToLoad(final int errorCode) {
        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onISAdFailedToLoad:" + errorCode);
                    mInterstitialListener.onAdFailedToLoad(IronSourceAdapter.this, errorCode);
                }
            });
        }
    }

    /**
     * MediationInterstitialAdapter implementation
     */

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        onLog("requestInterstitialAd");
        mInterstitialListener = listener;
        IronSource.setInterstitialListener(this);

        mInterstitialPlacementName = serverParameters.getString(KEY_IS_PLACEMENT, "");

        initIronSourceSDK(context, serverParameters, IronSource.AD_UNIT.INTERSTITIAL);
        loadISIronSourceSDK();
    }

    @Override
    public void showInterstitial() {
        onLog("showInterstitial");
        try {
            if (TextUtils.isEmpty(mInterstitialPlacementName)) {
                IronSource.showInterstitial();
            } else {
                IronSource.showInterstitial(mInterstitialPlacementName);
            }
        } catch (Exception e) {
            onLog(e.toString());
        }
    }

    /**
     * MediationRewardedVideoAdAdapter implementation
     */

    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String userId,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        onLog("initialize");
        mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;
        IronSource.setRewardedVideoListener(this);

        mRewardedVideoPlacementName = serverParameters.getString(KEY_RV_PLACEMENT, "");

        initIronSourceSDK(context, serverParameters, IronSource.AD_UNIT.REWARDED_VIDEO);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle networkExtras) {
        onLog("loadAd");

        if (IronSource.isRewardedVideoAvailable()) {
            onLog("onAdLoaded");
            mMediationRewardedVideoAdListener.onAdLoaded(IronSourceAdapter.this);
        }
    }

    @Override
    public void showVideo() {
        onLog("showVideo");

        if (TextUtils.isEmpty(mRewardedVideoPlacementName)) {
            IronSource.showRewardedVideo();
        } else {
            IronSource.showRewardedVideo(mRewardedVideoPlacementName);
        }
    }

    @Override
    public boolean isInitialized() {
        onLog("isInitialized: " + mIsInitialized);
        return mIsInitialized;
    }

    @Override
    public void onDestroy() {
        onLog("onDestroy");
    }

    @Override
    public void onPause() {
        onLog("onPause");
    }

    @Override
    public void onResume() {
        onLog("onResume");
    }

    /**
     * IronSource InterstitialListener implementation
     */

    @Override
    public void onInterstitialAdReady() {
        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdLoaded");
                    mInterstitialListener.onAdLoaded(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdLoadFailed(IronSourceError ironSourceError) {
        onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }

    @Override
    public void onInterstitialAdOpened() {
        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdOpened");
                    mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdClosed() {
        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdClosed");
                    mInterstitialListener.onAdClosed(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdShowSucceeded() {
        // No relevant delegate in AdMob interface
    }

    @Override
    public void onInterstitialAdShowFailed(IronSourceError ironSourceError) {
        onLog("onInterstitialAdShowFailed: " + ironSourceError.getErrorMessage());
    }

    @Override
    public void onInterstitialAdClicked() {
        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdClicked");
                    mInterstitialListener.onAdClicked(IronSourceAdapter.this);
                    onLog("onAdLeftApplication");
                    mInterstitialListener.onAdLeftApplication(IronSourceAdapter.this);
                }
            });
        }
    }

    /**
     * IronSource RewardedVideoListener implementation
     */

    @Override
    public void onRewardedVideoAvailabilityChanged(final boolean available) {
        onLog("onRewardedVideoAvailabilityChanged " + available);
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    if (available) {
                        onLog("onAdLoaded");
                        mMediationRewardedVideoAdListener.onAdLoaded(IronSourceAdapter.this);
                    } else {
                        onLog("onISAdFailedToLoad");
                        mMediationRewardedVideoAdListener.onAdFailedToLoad(IronSourceAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdOpened() {
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdOpened");
                    mMediationRewardedVideoAdListener.onAdOpened(IronSourceAdapter.this);
                    onLog("onVideoStarted");
                    mMediationRewardedVideoAdListener.onVideoStarted(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdClosed() {
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdClosed");
                    mMediationRewardedVideoAdListener.onAdClosed(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdStarted() {
        // Not called from IronSource SDK
    }

    @Override
    public void onRewardedVideoAdEnded() {
        // No relevant delegate in AdMob interface
    }

    @Override
    public void onRewardedVideoAdRewarded(final Placement placement) {
        if (placement == null) {
            onLog("IronSource Placement Error");
            return;
        }

        final IronSourceReward reward = new IronSourceReward(placement);

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {

                    onLog("onRewarded: " + reward.getType() + " " + reward.getAmount());
                    mMediationRewardedVideoAdListener.onRewarded(IronSourceAdapter.this, new IronSourceReward(placement));
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdShowFailed(IronSourceError ironsourceError) {
        // No relevant delegate in AdMob interface
        onLog("onRewardedVideoAdShowFailed: " + ironsourceError.getErrorMessage());
    }

    @Override
    public void onRewardedVideoAdClicked(Placement placement) {
        onLog("onRewardedVideoAdClicked, placement: " + placement.getPlacementName());

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdClicked");
                    mMediationRewardedVideoAdListener.onAdClicked(IronSourceAdapter.this);
                }
            });
        }
    }
}
