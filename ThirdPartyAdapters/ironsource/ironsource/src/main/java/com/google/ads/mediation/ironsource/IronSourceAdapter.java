package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialListener;

public class IronSourceAdapter extends IronSourceBaseAdapter implements MediationInterstitialAdapter, InterstitialListener {

    /**
     * Mediation interstitial ad listener used to forward interstitial events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mInterstitialListener;

    /**
     * Key to obtain Interstitial placement name
     */
    private static final String KEY_IS_PLACEMENT = "interstitialPlacement";

    /**
     * This is the placement name used for Interstitial
     */
    private String mInterstitialPlacementName;

    /**
     * Private IronSource methods
     */

    private void loadISIronSourceSDK() {
        onLog("loadInterstitial");
        if (IronSource.isInterstitialReady()) {
            onInterstitialAdReady();
        } else {
            IronSource.loadInterstitial();
        }
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

        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            Log.d(TAG, "IronSource SDK requires an Activity context to initialize");
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        try {

            String appKey = serverParameters.getString(KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                onLog("onInitializationFailed, make sure that 'appKey' server parameter is added");
                onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }

            onLog("Server params | appKey: " + appKey + " | isTestEnabled: " + mIsTestEnabled + " | placementName: " + mInterstitialPlacementName);

            initIronSourceSDK(context, serverParameters, IronSource.AD_UNIT.INTERSTITIAL);
            loadISIronSourceSDK();
        } catch (Exception e) {
            onLog("onInitializationFailed, error: " + e.getMessage());
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
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
        onLog("onInterstitialAdLoadFailed" + ironSourceError.getErrorMessage());
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

}
