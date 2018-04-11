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

/**
 * A {@link MediationInterstitialAdapter} to load and show IronSource interstitial ads using Google
 * Mobile Ads SDK mediation.
 */
public class IronSourceAdapter extends IronSourceBaseAdapter
        implements MediationInterstitialAdapter, ISDemandOnlyInterstitialListener {

    /**
     * Mediation interstitial ad listener used to forward interstitial events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mInterstitialListener;

    private static boolean mDidInitInterstitial = false;

    //region MediationInterstitialAdapter implementation.
    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {

        mInterstitialListener = listener;

        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            Log.d(TAG, "IronSource SDK requires an Activity context to initialize");
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        try {
            // Parse IronSource network-specific parameters.
            this.mIsLogEnabled = mediationAdRequest.isTesting();

            String appKey = serverParameters.getString(KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                onLog("IronSource initialization failed, make sure that 'applicationKey' server "
                        + "parameter is added");
                onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }

            this.mInstanceID = serverParameters.getString(KEY_INSTANCE_ID, "0");

            IronSource.setISDemandOnlyInterstitialListener(this);
            if (!mDidInitInterstitial) {
                initIronSourceSDK(context, appKey, IronSource.AD_UNIT.INTERSTITIAL);
                mDidInitInterstitial = true;
            }

            onLog("Load IronSource interstitial ad for instance: " + this.mInstanceID);
            IronSource.loadISDemandOnlyInterstitial(this.mInstanceID);

        } catch (Exception e) {
            onLog("IronSource initialization failed, error: " + e.getMessage());
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void showInterstitial() {
        try {
            IronSource.showISDemandOnlyInterstitial(this.mInstanceID);
        } catch (Exception e) {
            onLog(e.toString());
        }
    }
    //endregion

    @Override
    public void onDestroy() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    /**
     * Private IronSource methods
     */
    private void onISAdFailedToLoad(final int errorCode) {
        onLog("IronSource Interstitial failed to load for instance " + this.mInstanceID + " Error: "
                + errorCode);

        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdFailedToLoad(IronSourceAdapter.this, errorCode);
                }
            });
        }
    }

    //region ISDemandOnlyInterstitialListener implementation.
    @Override
    public void onInterstitialAdReady(String instanceId) {
        onLog("IronSource Interstitial loaded successfully for instance " + instanceId);

        // We only listen to a registered instance.
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
        onLog("IronSource Interstitial failed to load for instance " + instanceId + " Error: "
                + ironSourceError.getErrorMessage());

        // We only listen to a registered instance.
        if (!this.mInstanceID.equals(instanceId))
            return;

        onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }

    @Override
    public void onInterstitialAdOpened(String instanceId) {
        onLog("IronSource Interstitial opened ad for instance " + instanceId);

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
        onLog("IronSource Interstitial closed ad for instance " + instanceId);

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
        // No relevant delegate in AdMob interface.
    }

    @Override
    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        onLog("IronSource Interstitial failed to show for instance " + instanceId + ", error "
                + ironSourceError.getErrorMessage());
    }

    @Override
    public void onInterstitialAdClicked(String instanceId) {
        onLog("IronSource Interstitial clicked ad for instance " + instanceId);

        if (mInterstitialListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdClicked(IronSourceAdapter.this);
                    mInterstitialListener.onAdLeftApplication(IronSourceAdapter.this);
                }
            });
        }
    }
    //endregion
}
