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
public class IronSourceAdapter
        implements MediationInterstitialAdapter, ISDemandOnlyInterstitialListener {

    /**
     * Mediation interstitial ad listener used to forward interstitial events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mInterstitialListener;

    /**
     * This is the id of the instance to be shown.
     */
    private String mInstanceID;

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
            Log.e(IronSourceAdapterUtils.TAG, "IronSource SDK requires " +
                    "an Activity context to initialize");
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        try {
            String appKey = serverParameters.getString(IronSourceAdapterUtils.KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                Log.w(IronSourceAdapterUtils.TAG,
                        "Initialization Failed: Missing or Invalid App Key.");
                onISAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }

            this.mInstanceID = serverParameters.getString(
                    IronSourceAdapterUtils.KEY_INSTANCE_ID, "0");

            IronSource.setISDemandOnlyInterstitialListener(this);
            if (!IronSourceAdapterUtils.isIronSourceInitialized(IronSource.AD_UNIT.INTERSTITIAL)) {
                IronSourceAdapterUtils.initIronSourceSDK((Activity) context, appKey,
                        IronSource.AD_UNIT.INTERSTITIAL);
            }

            Log.d(IronSourceAdapterUtils.TAG, "Load IronSource interstitial ad for instance: " +
                    this.mInstanceID);
            IronSource.loadISDemandOnlyInterstitial(this.mInstanceID);
        } catch (Exception e) {
            Log.w(IronSourceAdapterUtils.TAG, "Initialization Failed.", e);
            onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void showInterstitial() {
        try {
            IronSource.showISDemandOnlyInterstitial(this.mInstanceID);
        } catch (Exception e) {
            Log.w(IronSourceAdapterUtils.TAG, "IronSource Interstitial failed to show.", e);
            if (mInterstitialListener != null) {
                mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                mInterstitialListener.onAdClosed(IronSourceAdapter.this);
            }
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
        Log.e(IronSourceAdapterUtils.TAG, String.format("IronSource Interstitial failed to load " +
                "for instance %s, Error: %d", this.mInstanceID, errorCode));

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdFailedToLoad(IronSourceAdapter.this, errorCode);
                }
            });
        }
    }

    //region ISDemandOnlyInterstitialListener implementation.
    @Override
    public void onInterstitialAdReady(String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, "IronSource Interstitial loaded successfully " +
                "for instance " + instanceId);

        // We only listen to a registered instance.
        if (!this.mInstanceID.equals(instanceId))
            return;

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdLoaded(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        Log.e(IronSourceAdapterUtils.TAG, String.format("IronSource Interstitial failed to load " +
                "for instance %s, Error: %s", this.mInstanceID, ironSourceError.getErrorMessage()));

        // We only listen to a registered instance.
        if (!this.mInstanceID.equals(instanceId))
            return;

        onISAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }

    @Override
    public void onInterstitialAdOpened(String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, "IronSource Interstitial opened ad for instance " +
                instanceId);

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdClosed(String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, "IronSource Interstitial closed ad for instance " +
                instanceId);

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
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
        Log.e(IronSourceAdapterUtils.TAG, String.format("IronSource Interstitial failed to show " +
                "for instance %s, Error: %s", this.mInstanceID, ironSourceError.getErrorMessage()));
        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                    mInterstitialListener.onAdClosed(IronSourceAdapter.this);
                }
            });
        }
    }

    @Override
    public void onInterstitialAdClicked(String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, "IronSource Interstitial clicked ad for instance " +
                instanceId);

        if (mInterstitialListener != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mInterstitialListener.onAdClicked(IronSourceAdapter.this);
                    mInterstitialListener.onAdLeftApplication(IronSourceAdapter.this);
                }
            });
        }
    }
    //endregion
}
