package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_APP_KEY;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.ironsource.IronSourceManager.InitializationCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.ISDemandOnlyBannerLayout;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;

/**
 * A {@link MediationInterstitialAdapter} to load and show IronSource interstitial ads using Google
 * Mobile Ads SDK mediation.
 */
public class IronSourceAdapter implements MediationInterstitialAdapter, MediationBannerAdapter, IronSourceAdapterListener {

    /**
     * Mediation interstitial ad listener used to forward interstitial events from IronSource SDK to
     * Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mInterstitialListener;

    /**
     * This is the id of the instance to be shown.
     */
    private String mInstanceID;

    /**
     * The view for the banner instance.
     */
    private ISDemandOnlyBannerLayout mIronSourceBannerLayout;

    /**
     * Callback object for Google's Banner Lifecycle.
     */
    private MediationBannerListener mediationBannerListener;


    // region MediationInterstitialAdapter implementation.
    @Override
    public void requestInterstitialAd(@NonNull Context context,
                                      @NonNull final MediationInterstitialListener listener, @NonNull final Bundle serverParameters,
                                      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {

        String appKey = serverParameters.getString(KEY_APP_KEY);
        IronSourceManager.getInstance().initIronSourceSDK(context, appKey,
                new InitializationCallback() {
                    @Override
                    public void onInitializeSuccess() {
                        mInstanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
                        mInterstitialListener = listener;
                        Log.d(TAG,
                                String.format("Loading IronSource interstitial ad with instance ID: %s",
                                        mInstanceID));
                        IronSourceManager.getInstance().loadInterstitial(context, mInstanceID, IronSourceAdapter.this);
                    }

                    @Override
                    public void onInitializeError(@NonNull AdError initializationError) {
                        Log.e(TAG, initializationError.getMessage());
                        listener.onAdFailedToLoad(IronSourceAdapter.this, initializationError);
                    }
                });
    }

    @Override
    public void showInterstitial() {
        Log.d(TAG,
                String.format("Showing IronSource interstitial ad for instance ID: %s", this.mInstanceID));
        IronSourceManager.getInstance().showInterstitial(mInstanceID);
    }
    // endregion

    @Override
    public void onDestroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {
    }

    // region ISDemandOnlyInterstitialListener implementation.
    public void onInterstitialAdReady(String instanceId) {
        Log.d(TAG, String.format("IronSource Interstitial ad loaded for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mInterstitialListener != null) {
                            mInterstitialListener.onAdLoaded(IronSourceAdapter.this);
                        }
                    }
                });
    }

    public void onInterstitialAdLoadFailed(String instanceId, final IronSourceError ironSourceError) {
        AdError loadError = new AdError(ironSourceError.getErrorCode(),
                ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
        String errorMessage = String
                .format("IronSource failed to load interstitial ad for instance ID: %s. Error: %s",
                        instanceId, loadError.getMessage());
        Log.e(TAG, errorMessage);

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mInterstitialListener != null) {
                            mInterstitialListener.onAdFailedToLoad(IronSourceAdapter.this, loadError);
                        }
                    }
                });
    }

    public void onInterstitialAdOpened(String instanceId) {
        Log.d(TAG, String.format("IronSource Interstitial ad opened for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mInterstitialListener != null) {
                            mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                        }
                    }
                });
    }

    public void onInterstitialAdClosed(String instanceId) {
        Log.d(TAG, String.format("IronSource Interstitial ad closed for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mInterstitialListener != null) {
                            mInterstitialListener.onAdClosed(IronSourceAdapter.this);
                        }
                    }
                });
    }

    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        AdError showError = new AdError(ironSourceError.getErrorCode(),
                ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
        String errorMessage = String
                .format("IronSource failed to show interstitial ad for instance ID: %s. Error: %s",
                        instanceId, showError.getMessage());
        Log.e(TAG, errorMessage);

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mInterstitialListener != null) {
                            mInterstitialListener.onAdOpened(IronSourceAdapter.this);
                            mInterstitialListener.onAdClosed(IronSourceAdapter.this);
                        }
                    }
                });
    }

    public void onInterstitialAdClicked(String instanceId) {
        Log.d(TAG, String.format("IronSource Interstitial ad clicked for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mInterstitialListener != null) {
                            mInterstitialListener.onAdClicked(IronSourceAdapter.this);
                            mInterstitialListener.onAdLeftApplication(IronSourceAdapter.this);
                        }
                    }
                });
    }
    // endregion

    // region IronSourceAdapterListener Interstitial implementation.
    @Override
    public void onAdFailedToLoad(@NonNull AdError loadError) {
        Log.e(TAG, loadError.getMessage());
        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mInterstitialListener != null) {
                            mInterstitialListener.onAdFailedToLoad(IronSourceAdapter.this, loadError);
                        }
                    }
                });
    }

    @Override
    public void onAdFailedToShow(@NonNull AdError showError) {
        Log.e(TAG, showError.getMessage());
    }





    // region IronSourceAdapterListener Banner implementation.
    public void onBannerAdLoaded(String instanceId) {
        Log.d(TAG, String.format("IronSource Banner ad loaded for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationBannerListener != null) {
                            mediationBannerListener.onAdLoaded(IronSourceAdapter.this);
                        }
                    }
                });
    }


    public void onBannerAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        AdError loadError = new AdError(ironSourceError.getErrorCode(),
                ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
        String errorMessage = String
                .format("IronSource failed to load Banner ad for instance ID: %s. Error: %s",
                        instanceId, loadError.getMessage());
        Log.e(TAG, errorMessage);

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationBannerListener != null) {
                            mediationBannerListener.onAdFailedToLoad(IronSourceAdapter.this, loadError);
                        }
                    }
                });
    }


    public void onBannerAdClicked(String instanceId) {
        Log.d(TAG, String.format("IronSource Banner ad clicked for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationBannerListener != null) {
                            mediationBannerListener.onAdClicked(IronSourceAdapter.this);
                        }
                    }
                });
    }


    public void onBannerAdShown(String instanceId) {
        Log.d(TAG, String.format("IronSource Banner ad onBannerAdScreenPresented for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationBannerListener != null) {
                            mediationBannerListener.onAdOpened(IronSourceAdapter.this);
                        }
                    }
                });
    }

    public void onBannerAdLeftApplication(String instanceId) {
        Log.d(TAG, String.format("IronSource Banner ad onBannerAdLeftApplication for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationBannerListener != null) {
                            mediationBannerListener.onAdLeftApplication(IronSourceAdapter.this);
                        }
                    }
                });

    }

    // endregion

    @NonNull
    @Override
    public View getBannerView() {
        return mIronSourceBannerLayout;
    }


    @Override
    public void requestBannerAd(@NonNull Context context,
                                @NonNull MediationBannerListener listener,
                                @NonNull Bundle serverParameters, @NonNull AdSize adSize,
                                @NonNull MediationAdRequest mediationAdRequest,
                                @Nullable Bundle bundle1) {
        ISBannerSize bannerSize = IronSourceAdapterUtils.getISBannerSize(context, adSize);

        if (!(context instanceof Activity)) {
            AdError errorMessage = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, "IronSource requires an Activity context to load ads." +
                    "adSize",
                    ERROR_DOMAIN);
            mediationBannerListener.onAdFailedToLoad(IronSourceAdapter.this, errorMessage);
            return;
        }
        Activity activity = (Activity) context;


        if (bannerSize == null) {
            AdError errorMessage = new AdError(ERROR_BANNER_SIZE_MISMATCH, "There is no matching IronSource ad size for Google ad size: %s" +
                    "adSize",
                    ERROR_DOMAIN);
            mediationBannerListener.onAdFailedToLoad(IronSourceAdapter.this, errorMessage);
            return;
        }


        String appKey = serverParameters.getString(KEY_APP_KEY);
        IronSourceManager.getInstance().initIronSourceSDK(context, appKey,
                new InitializationCallback() {
                    @Override
                    public void onInitializeSuccess() {
                        mInstanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
                        mediationBannerListener = listener;
                        Log.d(TAG,
                                String.format("Loading IronSource Banner ad with instance ID: %s",
                                        mInstanceID));
                        mIronSourceBannerLayout = IronSource.createBannerForDemandOnly(activity, bannerSize);
                        IronSourceManager.getInstance().loadBanner(mIronSourceBannerLayout, context, mInstanceID, IronSourceAdapter.this);
                    }

                    @Override
                    public void onInitializeError(@NonNull AdError initializationError) {
                        Log.e(TAG, initializationError.getMessage());
                        listener.onAdFailedToLoad(IronSourceAdapter.this, initializationError);
                    }
                });


    }





}
