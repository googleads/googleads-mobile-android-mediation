package com.google.ads.mediation.pangle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.google.ads.mediation.pangle.rtb.PangleRtbBannerAdapter;
import com.google.ads.mediation.pangle.rtb.PangleRtbInterstitialAd;
import com.google.ads.mediation.pangle.rtb.PangleRtbNativeAdapter;
import com.google.ads.mediation.pangle.rtb.PangleRtbRewardAdapter;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;

import java.util.HashSet;
import java.util.List;

public class PangleMediationAdapter extends RtbAdapter {

    final static String TAG = PangleMediationAdapter.class.getSimpleName();

    @Override
    public void collectSignals(@NonNull RtbSignalData rtbSignalData,
                               @NonNull SignalCallbacks signalCallbacks) {
        String biddingToken = getPangleSdkManager().getBiddingToken();
        signalCallbacks.onSuccess(biddingToken);
    }

    @Override
    public void initialize(@NonNull Context context,
                           @NonNull final InitializationCompleteCallback initializationCompleteCallback,
                           @NonNull List<MediationConfiguration> list) {
        Log.d(TAG, "custom event  AdmobRewardVideoAdapter  initialize");

        HashSet<String> appIds = new HashSet<>();
        for (MediationConfiguration mediationConfiguration : list) {
            Bundle serverParameters = mediationConfiguration.getServerParameters();
            String appId = serverParameters.getString(PangleConstant.APP_ID);
            if (appId != null) {
                appIds.add(appId);
            }
        }

        int count = appIds.size();
        if (count <= 0) {
            initializationCompleteCallback.onInitializationFailed("Missing or Invalid AppID.");
            return;
        }

        String appId = appIds.iterator().next();

        if (count > 1) {
            String message = String.format("Find multiple appids in %s. " +
                    ", use %s to initialize Pangle SDK", appIds, appId);
            Log.w(TAG, message);
        }

        boolean hasWakeLockPermission = hasWakeLockPermission(context);
        if (!hasWakeLockPermission) {
            Log.w(TAG, "For video ads to work in Pangle Ad TextureView, " +
                    "declare the android.permission.WAKE_LOCK permission in your AndroidManifest.");
        }

        TTAdSdk.init(context, new TTAdConfig.Builder()
                .appId(appId)
                .useTextureView(hasWakeLockPermission)
                .build(), new TTAdSdk.InitCallback() {
            @Override
            public void success() {
                initializationCompleteCallback.onInitializationSucceeded();
            }

            @Override
            public void fail(int errorCode, String errorMessage) {
                initializationCompleteCallback.onInitializationFailed(errorMessage);
            }
        });
    }

    @NonNull
    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.ADAPTER_VERSION;
        String[] splits = versionString.split("\\.");

        if (splits.length >= 4) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String
                .format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
                        versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @NonNull
    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = TTAdSdk.getAdManager().getSDKVersion();
        String[] splits = versionString.split("\\.");

        if (splits.length >= 4) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String
                .format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
                        versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public void loadRtbBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration,
                                @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        PangleRtbBannerAdapter bannerRenderer = new PangleRtbBannerAdapter(adConfiguration, callback);
        bannerRenderer.render();
    }

    @Override
    public void loadRtbInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
                                      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        PangleRtbInterstitialAd interstitialRenderer = new PangleRtbInterstitialAd(adConfiguration, callback);
        interstitialRenderer.render();
    }

    @Override
    public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
                                @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
        PangleRtbNativeAdapter nativeRenderer = new PangleRtbNativeAdapter(adConfiguration, callback);
        nativeRenderer.render();
    }

    @Override
    public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
                                  @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        PangleRtbRewardAdapter rewardRenderer = new PangleRtbRewardAdapter(adConfiguration, callback);
        rewardRenderer.render();
    }

    private static boolean hasWakeLockPermission(Context context) {
        try {
            String packageName = context.getPackageName();
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            String[] requestedPermissions = packageInfo.requestedPermissions;
            String wakeLockPermission = Manifest.permission.WAKE_LOCK;
            if (requestedPermissions != null && requestedPermissions.length > 0) {
                for (String per : requestedPermissions) {
                    if (wakeLockPermission.equalsIgnoreCase(per)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    public static TTAdManager getPangleSdkManager() {
        return TTAdSdk.getAdManager();
    }

    public static void setCoppa(@NonNull MediationAdConfiguration mediationAdConfiguration) {
        if (mediationAdConfiguration.taggedForChildDirectedTreatment() == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
            TTAdSdk.setCoppa(1);
        } else if (mediationAdConfiguration.taggedForChildDirectedTreatment() == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
            TTAdSdk.setCoppa(0);
        } else {
            TTAdSdk.setCoppa(-1);
        }
    }
}
