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
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;

public class IronSourceAdapter extends IronSourceBaseAdapter implements MediationInterstitialAdapter, ISDemandOnlyInterstitialListener  {

    /**
     * Mediation interstitial ad listener used to forward interstitial events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mInterstitialListener;

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

        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            Log.d(TAG, "IronSource SDK requires an Activity context to initialize");
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        try {

            // Parse IronSource network-specific parameters
            if (mediationExtras != null) {
                if (mediationExtras.containsKey(KEY_TEST_MODE)) {
                    this.mIsTestEnabled = mediationExtras.getBoolean(KEY_TEST_MODE, false);
                }
            }

            String appKey = serverParameters.getString(KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                onLog("onInitializationFailed, make sure that 'appKey' server parameter is added");
                onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }

            this.mInstanceID = serverParameters.getString(KEY_INTANCE_ID, "0");

            onLog("Server params for IS | appKey: " + appKey + " | isTestEnabled: " + this.mIsTestEnabled + " | InstanceID: " + this.mInstanceID );

            IronSource.setISDemandOnlyInterstitialListener(this);
            initIronSourceSDK(context, appKey, IronSource.AD_UNIT.INTERSTITIAL);
            loadISIronSourceSDK();

        } catch (Exception e) {
            onLog("onInitializationFailed, error: " + e.getMessage());
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void showInterstitial() {
        onLog("showInterstitial for instance: " + this.mInstanceID);
        try {
            IronSource.showISDemandOnlyInterstitial(this.mInstanceID);
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
     * Private IronSource methods
     */

    private void loadISIronSourceSDK() {
        if (IronSource.isISDemandOnlyInterstitialReady(this.mInstanceID)) {
            onInterstitialAdReady(this.mInstanceID);
        } else {
            onLog("loadInterstitial for instance: " + this.mInstanceID);
            IronSource.loadISDemandOnlyInterstitial(this.mInstanceID);
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
     * IronSource ISDemandOnlyInterstitialListener implementation
     */

    @Override
    public void onInterstitialAdReady(String instanceId) {
        onLog("onInterstitialAdReady for instance: " + instanceId);

        // We only listen to a registered instance
        if (!this.mInstanceID.equals(instanceId))
            return;

        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdLoaded(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        onLog("onInterstitialAdLoadFailed: " + ironSourceError.getErrorMessage() + " for instance: " + instanceId);

        // We only listen to a registered instance
        if (!this.mInstanceID.equals(instanceId))
            return;

        onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }

    @Override
    public void onInterstitialAdOpened(String instanceId) {
        onLog("onInterstitialAdOpened for instance: " + instanceId);

        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdClosed(String instanceId) {
        onLog("onInterstitialAdClosed for instance: " + instanceId);

        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdClosed(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdShowSucceeded(String instanceId) {
        // No relevant delegate in AdMob interface
    }

    @Override
    public void onInterstitialAdShowFailed(String instanceId,IronSourceError ironSourceError) {
        onLog("onInterstitialAdShowFailed: " + ironSourceError.getErrorMessage() + " for instance: " + instanceId);
    }

    @Override
    public void onInterstitialAdClicked(String instanceId) {
        onLog("onInterstitialAdClicked for instance: " + instanceId);

        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdClicked(IronSourceAdapter.this);
                    mInterstitialListener.onAdLeftApplication(IronSourceAdapter.this);
                }
            });
        }
    }
}
