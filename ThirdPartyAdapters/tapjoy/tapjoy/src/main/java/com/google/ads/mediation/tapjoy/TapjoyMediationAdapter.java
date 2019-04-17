package com.google.ads.mediation.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.Tapjoy;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class TapjoyMediationAdapter extends Adapter
        implements MediationRewardedAd, TJPlacementVideoListener {

    static final String TAG = TapjoyMediationAdapter.class.getSimpleName();

    static final String SDK_KEY_SERVER_PARAMETER_KEY = "sdkKey";
    static final String PLACEMENT_NAME_SERVER_PARAMETER_KEY = "placementName";
    static final String MEDIATION_AGENT = "admob";
    static final String TAPJOY_INTERNAL_ADAPTER_VERSION =
            "1.0.0"; // only used internally for Tapjoy SDK

    private static final String TAPJOY_DEBUG_FLAG_KEY = "enable_debug";

    private TJPlacement videoPlacement;

    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mAdLoadCallback;
    private MediationRewardedAdCallback mMediationRewardedAdCallback;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static HashMap<String, WeakReference<TapjoyMediationAdapter>> mPlacementsInUse =
            new HashMap<>();

    /**
     * {@link Adapter} implementation
     */
    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = Tapjoy.getVersion();
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public void initialize(Context context,
                           final InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed("Initialization Failed: "
                    + "Tapjoy SDK requires an Activity context to initialize");
            return;
        }

        HashSet<String> sdkKeys = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            String sdkKeyFromServer = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);

            if (!TextUtils.isEmpty(sdkKeyFromServer)) {
                sdkKeys.add(sdkKeyFromServer);
            }
        }

        String sdkKey;
        int count = sdkKeys.size();
        if (count > 0) {
            sdkKey = sdkKeys.iterator().next();

            if (count > 1) {
                String message = String.format("Multiple '%s' entries found: %s. " +
                                "Using '%s' to initialize the IronSource SDK.",
                        SDK_KEY_SERVER_PARAMETER_KEY, sdkKeys.toString(), sdkKey);
                Log.w(TAG, message);
            }
        } else {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization failed: Missing or Invalid SDK key.");
            return;
        }

        Tapjoy.setActivity((Activity) context);

        Hashtable<String, Object> connectFlags = new Hashtable<>();
        // TODO: Get Debug flag from publisher at init time. Currently not possible.
        // connectFlags.put("TJC_OPTION_ENABLE_LOGGING", true);

        TapjoyInitializer.getInstance().initialize((Activity) context, sdkKey, connectFlags,
                new TapjoyInitializer.Listener() {
            @Override
            public void onInitializeSucceeded() {
                initializationCompleteCallback.onInitializationSucceeded();
            }

            @Override
            public void onInitializeFailed(String message) {
                initializationCompleteCallback.onInitializationFailed("Initialization failed: "
                        + message);
            }
        });
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        Log.i(TAG, "Loading ad for Tapjoy-AdMob adapter");

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        Bundle networkExtras = mediationRewardedAdConfiguration.getMediationExtras();

        Context context = mediationRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)) {
            String logMessage = "Tapjoy SDK requires an Activity context to request ads";
            Log.e(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }
        Activity activity = (Activity) context;

        final String placementName =
                serverParameters.getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);
        if (TextUtils.isEmpty(placementName)) {
            String logMessage = "No placement name given for Tapjoy-AdMob adapter";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        String sdkKey = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);
        if (TextUtils.isEmpty(sdkKey)) {
            String logMessage = "Failed to request ad from Tapjoy: Missing or Invalid SDK Key.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        Tapjoy.setActivity(activity);

        Hashtable<String, Object> connectFlags = new Hashtable<>();
        if (networkExtras != null && networkExtras.containsKey(TAPJOY_DEBUG_FLAG_KEY)) {
            connectFlags.put("TJC_OPTION_ENABLE_LOGGING",
                    networkExtras.getBoolean(TAPJOY_DEBUG_FLAG_KEY, false));
        }

        mAdLoadCallback = mediationAdLoadCallback;
        TapjoyInitializer.getInstance().initialize(activity, sdkKey, connectFlags,
                new TapjoyInitializer.Listener() {
            @Override
            public void onInitializeSucceeded() {
                if (mPlacementsInUse.containsKey(placementName) &&
                        mPlacementsInUse.get(placementName).get() != null) {
                    String logMessage =
                            "An ad has already been requested for placement: " + placementName;
                    Log.w(TAG, logMessage);
                    mAdLoadCallback.onFailure(logMessage);
                    return;
                }

                mPlacementsInUse.put(placementName,
                        new WeakReference<>(TapjoyMediationAdapter.this));
                createVideoPlacementAndRequestContent(placementName);
            }

            @Override
            public void onInitializeFailed(String message) {
                String logMessage = "Failed to request ad from Tapjoy: " + message;
                Log.w(TAG, logMessage);
                mAdLoadCallback.onFailure(logMessage);
            }
        });
    }

    @Override
    public void showAd(Context context) {
        Log.i(TAG, "Show video content for Tapjoy-AdMob adapter");
        if (videoPlacement != null && videoPlacement.isContentAvailable()) {
            videoPlacement.showContent();
        } else if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback.onAdFailedToShow("Tapjoy Rewarded Ad is not ready.");
        }
    }

    private void createVideoPlacementAndRequestContent(final String placementName) {
        Log.i(TAG, "Creating video placement for AdMob adapter");

        videoPlacement = Tapjoy.getPlacement(placementName, new TJPlacementListener() {
            // Placement Callbacks
            @Override
            public void onRequestSuccess(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!videoPlacement.isContentAvailable()) {
                            mPlacementsInUse.remove(placementName);

                            String logMessage =
                                    "Failed to request rewarded ad from Tapjoy: No Fill.";
                            Log.w(TAG, logMessage);
                            if (mAdLoadCallback != null) {
                                mAdLoadCallback.onFailure(logMessage);
                            }
                        }
                    }
                });
            }

            @Override
            public void onRequestFailure(TJPlacement tjPlacement, final TJError tjError) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPlacementsInUse.remove(placementName);

                        String logMessage =
                                "Failed to request rewarded ad from Tapjoy: " + tjError.message;
                        Log.w(TAG, logMessage);
                        if (mAdLoadCallback != null) {
                            mAdLoadCallback.onFailure(logMessage);
                        }
                    }
                });
            }

            @Override
            public void onContentReady(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Tapjoy Rewarded Ad is available.");
                        if (mAdLoadCallback != null) {
                            mMediationRewardedAdCallback =
                                    mAdLoadCallback.onSuccess(TapjoyMediationAdapter.this);
                        }
                    }
                });
            }

            @Override
            public void onContentShow(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Tapjoy Rewarded Ad has been opened.");
                        if (mMediationRewardedAdCallback != null) {
                            mMediationRewardedAdCallback.onAdOpened();
                        }
                    }
                });
            }

            @Override
            public void onContentDismiss(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Tapjoy Rewarded Ad has been closed.");
                        if (mMediationRewardedAdCallback != null) {
                            mMediationRewardedAdCallback.onAdClosed();
                        }
                        mPlacementsInUse.remove(placementName);
                    }
                });
            }

            @Override
            public void onPurchaseRequest(TJPlacement tjPlacement,
                                          TJActionRequest tjActionRequest,
                                          String s) {
                // no-op
            }

            @Override
            public void onRewardRequest(TJPlacement tjPlacement,
                                        TJActionRequest tjActionRequest,
                                        String s,
                                        int i) {
                // no-op
            }
        });
        videoPlacement.setMediationName(MEDIATION_AGENT);
        videoPlacement.setAdapterVersion(TAPJOY_INTERNAL_ADAPTER_VERSION);
        videoPlacement.setVideoListener(TapjoyMediationAdapter.this);

        videoPlacement.requestContent();
    }

    /**
     * {@link TJPlacementVideoListener} implementation.
     */

    @Override
    public void onVideoStart(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Tapjoy Rewarded Ad has started playing.");
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onVideoStart();
                    mMediationRewardedAdCallback.reportAdImpression();
                }
            }
        });
    }

    @Override
    public void onVideoError(final TJPlacement tjPlacement, final String errorMessage) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                mPlacementsInUse.remove(tjPlacement.getName());
                Log.w(TAG, "Tapjoy Rewarded Ad has failed to play: " + errorMessage);
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onAdFailedToShow(errorMessage);
                }
            }
        });
    }

    @Override
    public void onVideoComplete(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Tapjoy Rewarded Ad has finished playing.");
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onVideoComplete();
                    mMediationRewardedAdCallback.onUserEarnedReward(new TapjoyReward());
                }
            }
        });
    }

    /**
     * A {@link RewardItem} used to map Tapjoy reward to Google's reward.
     */
    public class TapjoyReward implements RewardItem {
        @Override
        public String getType() {
            // Tapjoy only supports fixed rewards and doesn't provide a reward type.
            return "";
        }

        @Override
        public int getAmount() {
            // Tapjoy only supports fixed rewards and doesn't provide a reward amount.
            return 1;
        }
    }

}
