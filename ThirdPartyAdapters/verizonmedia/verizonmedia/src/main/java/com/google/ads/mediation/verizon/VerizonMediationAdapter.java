package com.google.ads.mediation.verizon;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import java.lang.ref.WeakReference;
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
     * Weak reference of context.
     */
    private WeakReference<Context> contextWeakRef;

    /**
     * The Verizon Media interstitial renderer.
     */
    private VerizonMediaInterstitialRenderer verizonMediaInterstitialRenderer;

    /**
     * The Verizon Media rewarded ad renderer.
     */
    private VerizonMediaRewardedRenderer verizonMediaRewardedRenderer;

    /**
     * The Verizon Media banner renderer.
     */
    private VerizonMediaBannerRenderer mBannerRenderer;

    /**
     * The Verizon Media native renderer.
     */
    private VerizonMediaNativeRenderer verizonMediaNativeRenderer;

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
                    mediationConfiguration.getServerParameters(), (Bundle)null);
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
        mBannerRenderer = new VerizonMediaBannerRenderer(this);
        mBannerRenderer.render(context, listener, serverParameters, adSize, mediationAdRequest,
                mediationExtras);
    }

    @Override
    public View getBannerView() {
        return mBannerRenderer.getBannerView();
    }

    @Override
    public void requestInterstitialAd(final Context context,
           final MediationInterstitialListener listener, final Bundle serverParameters,
           final MediationAdRequest mediationAdRequest, final Bundle mediationExtras) {
        setContext(context);
        verizonMediaInterstitialRenderer = new VerizonMediaInterstitialRenderer(this);
        verizonMediaInterstitialRenderer.render(context, listener, mediationAdRequest,
                serverParameters, mediationExtras);
    }

    @Override
    public void showInterstitial() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "Failed to show: context is null");
            return;
        }
        verizonMediaInterstitialRenderer.showInterstitial(context);
    }

    @Override
    public void loadRewardedAd(final MediationRewardedAdConfiguration
            mediationRewardedAdConfiguration, final MediationAdLoadCallback<MediationRewardedAd,
            MediationRewardedAdCallback> mediationAdLoadCallback) {
        verizonMediaRewardedRenderer =
                new VerizonMediaRewardedRenderer(mediationAdLoadCallback,
                        mediationRewardedAdConfiguration);
        verizonMediaRewardedRenderer.render();
    }

    @Override
    public void requestNativeAd(final Context context, final MediationNativeListener listener,
            final Bundle serverParameters, final NativeMediationAdRequest mediationAdRequest,
            final Bundle mediationExtras) {
        verizonMediaNativeRenderer = new VerizonMediaNativeRenderer( this);
        verizonMediaNativeRenderer.render(context, listener, serverParameters, mediationAdRequest,
                 mediationExtras);
    }

    @Override
    public void onDestroy() {

        Log.i(TAG, "Aborting.");

        if (verizonMediaInterstitialRenderer != null) {
            verizonMediaInterstitialRenderer.destroy();
        }

        if (mBannerRenderer != null) {
            mBannerRenderer.destroy();
        }

        if (verizonMediaNativeRenderer != null) {
            verizonMediaNativeRenderer.destroy();
        }

        if (verizonMediaRewardedRenderer != null) {
            verizonMediaRewardedRenderer.destroy();
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
    protected static boolean initializeSDK(final Context context, final String siteId) {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Log.e(TAG, "Verizon Ads SDK minimum supported API is 16");

            return false;
        }

        boolean success = true;
        if (!VASAds.isInitialized()) {

            if (!(context instanceof Activity)) {
                Log.e(TAG, "StandardEdition.initialize must be explicitly called with an Activity" +
                        " context.");

                return false;
            }
            if (TextUtils.isEmpty(siteId)) {
                Log.e(TAG, "Verizon Ads SDK Site ID must be set in mediation extras or "
                        + "server parameters");

                return false;
            }
            try {
                Application application = ((Activity) context).getApplication();
                Log.d(TAG, "Initializing using site ID: " + siteId);
                success = StandardEdition.initialize(application, siteId);
            } catch (Exception e) {
                Log.e(TAG, "Error occurred initializing Verizon Ads SDK, ", e);

                return false;
            }
        }

        VASAds.getActivityStateManager().setState((Activity) context,
                ActivityStateManager.ActivityState.RESUMED);
        VASAds.setPrivacyData(VerizonPrivacy.getInstance().getPrivacyData());

        return success;
    }

    private void setContext(@NonNull Context context) {
        contextWeakRef = new WeakReference<>(context);
    }

    private @Nullable Context getContext() {
        if (contextWeakRef == null) {
            return null;
        }
        return contextWeakRef.get();
    }
}
