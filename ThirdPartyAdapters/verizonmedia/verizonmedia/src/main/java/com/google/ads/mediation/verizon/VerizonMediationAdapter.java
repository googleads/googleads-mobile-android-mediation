package com.google.ads.mediation.verizon;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.BuildConfig;
import com.verizon.ads.Configuration;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.inlineplacement.InlineAdFactory;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;
import com.verizon.ads.nativeplacement.NativeAdFactory;
import com.verizon.ads.utils.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class VerizonMediationAdapter extends Adapter
        implements MediationBannerAdapter, MediationInterstitialAdapter, MediationNativeAdapter {

    /**
     * The pixel-to-dpi scale for images downloaded Verizon Ads SDK
     */
    static final double VAS_IMAGE_SCALE = 1.0;

    public static final String TAG = VerizonMediationAdapter.class.getSimpleName();
    /**
     * The banner ad's parent view.
     */
    private LinearLayout bannerParentView;
    /**
     * Weak reference of context.
     */
    private WeakReference<Context> contextWeakRef;
    /**
     * Verizon Media inline ad factory.
     */
    private InlineAdFactory inlineAdFactory;
    /**
     * The adapter interstitial listener.
     */
    private AdapterInterstitialListener adapterInterstitialListener;
    /**
     * The adapter incentivized event listener.
     */
    private AdapterIncentivizedEventListener adapterIncentivizedEventListener;
    /**
     * The adapter inline listener.
     */
    private AdapterInlineListener adapterInlineListener;
    /**
     * The Verizon Media interstitial ad factory.
     */
    private InterstitialAdFactory interstitialAdFactory;
    /**
     * The Verizon Media native ad factory.
     */
    private NativeAdFactory nativeAdFactory;
    /**
     * The adapter native listener.
     */
    private AdapterNativeListener adapterNativeListener;

    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");

        if (splits.length >= 4) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String.format("Unexpected adapter version format: %s." +
                "Returning 0.0.0 for adapter version.", versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = Configuration.getString("com.verizon.ads",
                "editionVersion", null);

        if (TextUtils.isEmpty(versionString)) {
            versionString = VASAds.getSDKInfo().version;
        }
        String[] splits = versionString.split("\\.");
        if (splits.length >= 3) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String.format("Unexpected SDK version format: %s." +
                "Returning 0.0.0 for SDK version.", versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public void initialize(Context context,
            InitializationCompleteCallback initializationCompleteCallback,
            List<MediationConfiguration> mediationConfigurations) {

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed(
                    "Verizon Media SDK requires an Activity context to initialize");
            return;
        }

        HashSet<String> siteIDs = new HashSet<>();
        for (MediationConfiguration mediationConfiguration : mediationConfigurations) {
            String siteID = VerizonMediaAdapterUtils.getSiteId(
                    mediationConfiguration.getServerParameters(), null);
            if (!TextUtils.isEmpty(siteID)) {
                siteIDs.add(siteID);
            }
        }
        int count = siteIDs.size();
        if (count <= 0) {
            String logMessage = "Initialization failed: Missing or invalid Site ID";
            Log.e(TAG, logMessage);
            initializationCompleteCallback.onInitializationFailed(logMessage);
            return;
        }
        String siteID = siteIDs.iterator().next();
        if (count > 1) {
            String message = String.format("Multiple '%s' entries found: %s. " +
                    "Using '%s' to initialize Verizon SDK.", VerizonMediaAdapterUtils.SITE_KEY,
                    siteIDs, siteID);
            Log.w(TAG, message);
        }
        if (initializeSDK(context, siteID)) {
            if (VerizonConsent.getInstance().getConsentMap() != null) {
                VASAds.setConsentData(VerizonConsent.getInstance().getConsentMap(),
                        VerizonConsent.getInstance().isRestricted());
            }
            initializationCompleteCallback.onInitializationSucceeded();
        } else {
            initializationCompleteCallback.onInitializationFailed(
                    "Verizon SDK initialization failed");
        }
    }

    @Override
    public void requestBannerAd(final Context context, final MediationBannerListener listener,
            final Bundle serverParameters, com.google.android.gms.ads.AdSize adSize,
            final MediationAdRequest mediationAdRequest, final Bundle mediationExtras) {

        String placementId = VerizonMediaAdapterUtils.getPlacementId(serverParameters);
        String siteId = VerizonMediaAdapterUtils.getSiteId(serverParameters, mediationExtras);

        setContext(context);
        if (!initializeSDK(context, siteId)) {
            Log.e(TAG, "Unable to initialize Verizon Ads SDK");
            listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }
        VerizonMediaAdapterUtils.setCoppaValue(mediationAdRequest);
        if (adSize == null) {
            Log.w(TAG, "Fail to request banner ad, adSize is null");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        AdSize normalizedSize = VerizonMediaAdapterUtils.normalizeSize(context, adSize);
        if (normalizedSize == null) {
            Log.w(TAG,
                    "The input ad size " + adSize.toString() + " is not currently supported.");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        bannerParentView = new LinearLayout(context);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        bannerParentView.setLayoutParams(lp);
        VASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));

        adapterInlineListener = new AdapterInlineListener(this, listener, bannerParentView);
        inlineAdFactory = new InlineAdFactory(context, placementId,
                Collections.singletonList(
                        new com.verizon.ads.inlineplacement.AdSize(normalizedSize.getWidth(),
                                normalizedSize.getHeight())),
                adapterInlineListener);
        inlineAdFactory.setRequestMetaData(VerizonMediaAdapterUtils
                .getRequestMetadata(mediationAdRequest));
        try {
            inlineAdFactory.load(adapterInlineListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create InlineAd instance", e);

            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
            });
        }
    }

    @Override
    public View getBannerView() {
        return bannerParentView;
    }

    @Override
    public void requestInterstitialAd(final Context context,
           final MediationInterstitialListener listener, final Bundle serverParameters,
           final MediationAdRequest mediationAdRequest, final Bundle mediationExtras) {

        String placementId = VerizonMediaAdapterUtils.getPlacementId(serverParameters);
        String siteId = VerizonMediaAdapterUtils.getSiteId(serverParameters, mediationExtras);

        setContext(context);

        if (!initializeSDK(context, siteId)) {
            Log.e(TAG, "Unable to initialize Verizon Ads SDK");
            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            });

            return;
        }

        VerizonMediaAdapterUtils.setCoppaValue(mediationAdRequest);
        VASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));
        adapterInterstitialListener = new AdapterInterstitialListener(this, listener);
        interstitialAdFactory = new InterstitialAdFactory(context, placementId,
                adapterInterstitialListener);
        interstitialAdFactory.setRequestMetaData(VerizonMediaAdapterUtils
                .getRequestMetadata(mediationAdRequest));
        try {
            interstitialAdFactory.load(adapterInterstitialListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create InterstitialAd instance", e);
            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
            });
        }
    }

    @Override
    public void showInterstitial() {
        if (adapterInterstitialListener == null) {
            Log.e(TAG,"Failed to show: Adapter interstitial listener is null");
            return;
        }

        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "Failed to show: context is null");
            return;
        }
        adapterInterstitialListener.show(context);
    }

    @Override
    public void loadRewardedAd(final MediationRewardedAdConfiguration
            mediationRewardedAdConfiguration, final MediationAdLoadCallback<MediationRewardedAd,
            MediationRewardedAdCallback> mediationAdLoadCallback) {
        if (!VASAds.isInitialized()) {
            mediationAdLoadCallback.onFailure("Verizon Ads SDK not initialized");
            return;
        }

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        String placementId = VerizonMediaAdapterUtils.getPlacementId(serverParameters);
        setContext(mediationRewardedAdConfiguration.getContext());

        if (TextUtils.isEmpty(placementId)) {
            mediationAdLoadCallback.onFailure(
                    "Verizon Ads SDK placement ID must be set in mediationRewardedAdConfiguration" +
                            " server params");
            return;
        }
        VerizonMediaAdapterUtils.setCoppaValue(mediationRewardedAdConfiguration);
        VASAds.setLocationEnabled((mediationRewardedAdConfiguration.getLocation() != null));
        adapterIncentivizedEventListener =
                new AdapterIncentivizedEventListener(mediationAdLoadCallback);
        interstitialAdFactory =
                new InterstitialAdFactory(mediationRewardedAdConfiguration.getContext(),
                        placementId,
                        adapterIncentivizedEventListener);
        interstitialAdFactory.setRequestMetaData(VerizonMediaAdapterUtils
                .getRequestMetaData(mediationRewardedAdConfiguration));
        try {
            interstitialAdFactory.load(adapterIncentivizedEventListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load Verizon Ads SDK Incentivized Video Ad", e);
            if (mediationAdLoadCallback != null) {
                mediationAdLoadCallback.onFailure("Failed to load Incentivized Video Ad");
            }
        }
    }

    @Override
    public void requestNativeAd(final Context context, final MediationNativeListener listener,
            final Bundle serverParameters, final NativeMediationAdRequest mediationAdRequest,
            final Bundle mediationExtras) {

        String placementId = VerizonMediaAdapterUtils.getPlacementId(serverParameters);
        String siteId = VerizonMediaAdapterUtils.getSiteId(serverParameters, mediationExtras);
        String[] adTypes = new String[] {"inline"};

        setContext(context);

        if (!initializeSDK(context, siteId)) {
            Log.e(TAG, "Unable to initialize Verizon Ads SDK");
            listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }

        VerizonMediaAdapterUtils.setCoppaValue(mediationAdRequest);
        VASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));
        adapterNativeListener = new AdapterNativeListener(context, this, listener);
        nativeAdFactory = new NativeAdFactory(context, placementId, adTypes, adapterNativeListener);
        nativeAdFactory.setRequestMetaData(VerizonMediaAdapterUtils
                .getRequestMetadata(mediationAdRequest));
        NativeAdOptions options = mediationAdRequest.getNativeAdOptions();

        try {
            if ((options == null) || (!options.shouldReturnUrlsForImageAssets())) {
                nativeAdFactory.load(adapterNativeListener);
            } else {
                nativeAdFactory.loadWithoutAssets(adapterNativeListener);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to create Native Ad instance", e);
            listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                    AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    @Override
    public void onDestroy() {

        Log.i(TAG, "Aborting.");
        if (inlineAdFactory != null) {
            inlineAdFactory.abortLoad();
        }

        if (interstitialAdFactory != null) {
            interstitialAdFactory.abortLoad();
        }

        if (nativeAdFactory != null) {
            nativeAdFactory.abortLoad();
        }

        if (adapterInterstitialListener != null) {
            adapterInterstitialListener.destroy();
        }

        if (adapterInlineListener != null) {
            adapterInlineListener.destroy();
        }

        if (adapterNativeListener != null) {
            adapterNativeListener.destroy();
        }

        if (adapterIncentivizedEventListener != null) {
            adapterIncentivizedEventListener.destroy();
        }
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    /**
     * Checks whether Verizon Media SDK is initialized, if not initializes Verizon Media SDK.
     */
    private boolean initializeSDK(final Context context, final String siteId) {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Log.e(TAG, "Verizon Ads SDK minimum supported API is 16");

            return false;
        }

        boolean success = true;
        if (!VASAds.isInitialized()) {

            if (!(context instanceof Activity)) {
                Log.e(TAG, "StandardEdition.initialize must be explicitly called when " +
                        "instantiating the AdMob " +
                        "AdView or InterstitialAd without an Activity.");

                return false;
            }
            if (TextUtils.isEmpty(siteId)) {
                Log.e(TAG, "Verizon Ads SDK Site ID must be set in mediation extras or "
                        + "server params");

                return false;
            }
            try {
                Application application = ((Activity) context).getApplication();
                Log.d(TAG, "Using site ID: " + siteId);
                success = StandardEdition.initialize(application, siteId);
            } catch (Exception e) {
                Log.e(TAG, "Error occurred initializing Verizon Ads SDK, " + e.getMessage());

                return false;
            }
        }

        VASAds.getActivityStateManager().setState((Activity) context,
                ActivityStateManager.ActivityState.RESUMED);
        VASAds.setConsentData(VerizonConsent.getInstance().getConsentMap(),
                VerizonConsent.getInstance().isRestricted());

        return success;
    }

    private void setContext(final Context context) {
        contextWeakRef = new WeakReference<>(context);
    }

    private Context getContext() {
        if (contextWeakRef == null) {
            return null;
        }
        return contextWeakRef.get();
    }
}
