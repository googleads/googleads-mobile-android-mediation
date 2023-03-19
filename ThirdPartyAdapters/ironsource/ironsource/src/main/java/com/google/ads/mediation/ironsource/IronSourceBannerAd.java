package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_APP_KEY;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.ISDemandOnlyBannerLayout;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyBannerListener;

public class IronSourceBannerAd implements MediationBannerAd, ISDemandOnlyBannerListener {

    /**
     * This is the id of the banner instance requested.
     */
    private String instanceID;

    /**
     * The view for the banner instance.
     */
    private  ISDemandOnlyBannerLayout ironSourceBannerLayout;
    private FrameLayout ironSourceAdView;
    private final MediationBannerAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
            adLoadCallback;
    private MediationBannerAdCallback bannerAdCallback;

    public IronSourceBannerAd(
                              @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
                              @NonNull
                                      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                                      mediationAdLoadCallback) {
        this.adConfiguration = mediationBannerAdConfiguration;
        this.adLoadCallback = mediationAdLoadCallback;
    }

    @NonNull
    @Override
    public View getView() {
        return ironSourceAdView;
    }

    public void render() {
        Bundle serverParameters = adConfiguration.getServerParameters();
        Context context = adConfiguration.getContext();
        String appKey = serverParameters.getString(KEY_APP_KEY);
        this.instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);

        ISBannerSize bannerSize = IronSourceAdapterUtils.getISBannerSize(context, adConfiguration.getAdSize());
        if (bannerSize == null) {
            AdError sizeError = new AdError(ERROR_BANNER_SIZE_MISMATCH, "There is no matching IronSource ad size for Google ad size: %s" +
                    "adSize",
                    ERROR_DOMAIN);
            Log.e(TAG, sizeError.toString());
            adLoadCallback.onFailure( sizeError);
            return;
        }

        ironSourceAdView = new FrameLayout(context);
        IronSourceManager.getInstance().initIronSourceSDK(context, appKey,
                new IronSourceManager.InitializationCallback() {
                    @Override
                    public void onInitializeSuccess() {
                        Log.d(TAG,
                                String.format("Loading IronSource banner ad with instance ID: %s", instanceID));
                        Activity activity = (Activity) context;
                        ironSourceBannerLayout = IronSource.createBannerForDemandOnly(activity, bannerSize);
                        ironSourceBannerLayout.setBannerDemandOnlyListener(IronSourceManager.getInstance());
                        IronSource.loadISDemandOnlyBanner(activity, ironSourceBannerLayout,instanceID);
                    }

                    @Override
                    public void onInitializeError(@NonNull AdError initializationError) {
                        Log.w(TAG, initializationError.toString());
                        adLoadCallback.onFailure(initializationError);
                    }
                });


    }

    @Override
    public void onBannerAdLoaded(@NonNull String instanceId) {
        Log.d(TAG, String.format("IronSource Banner ad loaded for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        ironSourceAdView.addView(ironSourceBannerLayout);
                        bannerAdCallback =  adLoadCallback.onSuccess(IronSourceBannerAd.this);
                    }
                });
    }

    @Override
    public void onBannerAdLoadFailed(@NonNull final String instanceId,@NonNull final IronSourceError ironSourceError) {
        final AdError loadError = new AdError(ironSourceError.getErrorCode(),
                ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
        Log.w(TAG, loadError.toString());

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        adLoadCallback.onFailure( loadError);
                    }
                });
    }

    public void onBannerAdShown(@NonNull String instanceId) {
        Log.d(TAG, String.format("IronSource Banner AdShown for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (bannerAdCallback != null) {
                            bannerAdCallback.reportAdImpression();
                        }
                    }
                });
    }

    @Override
    public void onBannerAdClicked(@NonNull String instanceId) {
        Log.d(TAG, String.format("IronSource Banner ad clicked for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (bannerAdCallback != null) {
                            bannerAdCallback.onAdOpened();
                            bannerAdCallback.reportAdClicked();
                        }
                    }
                });
    }

    @Override
    public void onBannerAdLeftApplication(@NonNull String instanceId) {
        Log.d(TAG, String.format("IronSource Banner ad onBanner Ad Left Application for instance ID: %s", instanceId));

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (bannerAdCallback != null) {
                            bannerAdCallback.onAdLeftApplication();
                        }
                    }
                });
    }

}
