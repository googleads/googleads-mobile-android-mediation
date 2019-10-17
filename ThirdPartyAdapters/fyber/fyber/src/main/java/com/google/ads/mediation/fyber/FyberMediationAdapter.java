package com.google.ads.mediation.fyber;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.fyber.inneractive.sdk.config.IAConfigManager;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Fyber's official AdMob 3rd party adapter class
 * Implements Banners and interstitials by implementing the {@link MediationBannerAdapter} and {@link MediationInterstitialAdapter} interfaces
 * Implements initialization and Rewarded video ads, by extending the {@link Adapter} class
 */
public class FyberMediationAdapter extends Adapter
        implements MediationBannerAdapter, MediationInterstitialAdapter {
    /**
     * Adapter class name for logging.
     */
    private static final String TAG = FyberMediationAdapter.class.getSimpleName();

    /**
     * Fyber requires to know the host mediation platform
     */
    private final static InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

    /**
     * Key to obtain App id, required for initializing Fyber's SDK.
     */
    static final String KEY_APP_ID = "applicationId";
    /**
     * Key to obtain a placement name or spot id. Required for creating a Fyber ad request
     */
    static final String KEY_SPOT_ID = "spotId";

    /**
     * Fyber's Spot object for the banner
     */
    private InneractiveAdSpot mBannerSpot;

    /** Holds the banner view which is created by Fyber, in order to return when AdMob calls getView */
    private ViewGroup mBannerWrapperView;

    // Interstitial related members
    /**
     * Admob's external interstitial listener
     */
    private MediationInterstitialListener mMediationInterstitialListener;
    /**
     * The context which was passed by AdMob to {@link #requestInterstitialAd}
    */
    private WeakReference<Context> mInterstitialContext;
    /**
     * Fyber's spot object for interstitial
     */
    private InneractiveAdSpot mInterstitialSpot;

    /**
     * Default C'tor
     */
    public FyberMediationAdapter() {
    }

    /**
     * Not supported. Use {@link MediationBannerAdapter#requestBannerAd} instead
     * @param configuration
     * @param callback
     */
    public void loadBannerAd(MediationBannerAdConfiguration configuration, MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        callback.onFailure(String.format("%s does not support MediationBannerAd", getClass().getSimpleName()));
    }

    /**
     * Not supported. Use {@link MediationInterstitialAdapter#requestInterstitialAd} instead
     * @param configuration
     * @param callback
     */
    public void loadInterstitialAd(MediationInterstitialAdConfiguration configuration, MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        callback.onFailure(String.format("%s does not support MediationInterstitialAd", getClass().getSimpleName()));
    }

    /**
     * Native ads mediaqtion is not supported by the FyberMediationAdapter
     * @param var1
     * @param callback
     */
    public void loadNativeAd(MediationNativeAdConfiguration var1, MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
        callback.onFailure(String.format("%s does not support native ads", getClass().getSimpleName()));
    }

    /**
     * Only rewarded ads are implemented using the new Adapter interface
     * @param configuration
     * @param callback
     */
    public void loadRewardedAd(MediationRewardedAdConfiguration configuration, MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        // Sometimes loadRewardedAd is called before initialize is called
        initializeFromBundle(configuration.getContext(), configuration.getServerParameters());

        FyberRewardedVideoRenderer rewardedVideoRenderer = new FyberRewardedVideoRenderer(configuration, callback);
        rewardedVideoRenderer.render();
    }

    @Override
    public void initialize(Context context, final InitializationCompleteCallback completionCallback, List<MediationConfiguration> mediationConfigurations) {
        // Initialize only once
        if (InneractiveAdManager.wasInitialized()) {
            waitForInitializationStatusAndReport(completionCallback);
            return;
        }

        String appId = null;

        List<String> configuredAppIds = new ArrayList<>();

        // Get AppId from configuration
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();

            // check for null
            if (serverParameters == null) {
                continue;
            }

            appId = serverParameters.getString(KEY_APP_ID);

            // Found an app id in server params
            if (!TextUtils.isEmpty(appId)) {
                configuredAppIds.add(appId);
            }
        }

        if (configuredAppIds.isEmpty()) {
            Log.w(TAG, "No appId received from AdMob. Cannot initialize Fyber Marketplace");
            if (completionCallback != null) {
                completionCallback.onInitializationFailed("Fyber SDK requires an appId to be configured on the AdMob UI");
            }

            return;
        }

        // We can only use one app id
        String appIdForInitialization = configuredAppIds.get(0);

        if (configuredAppIds.size() > 1) {
            String message = String.format("Multiple '%s' entries found: %s. " +
                "Using '%s' to initialize the Fyber Marketplace SDK",
                    KEY_APP_ID, appIdForInitialization, appIdForInitialization);
                Log.w(TAG, message);
        }

        InneractiveAdManager.initialize(context, appIdForInitialization);

        waitForInitializationStatusAndReport(completionCallback);
    }

    /**
     * A helper for checking out Fyber's initialization status
     * @param completionCallback Admob's initialization callback
     */
    private void waitForInitializationStatusAndReport(final InitializationCompleteCallback completionCallback) {
        if (completionCallback != null) {
            IAConfigManager.addListener(new IAConfigManager.OnConfigurationReadyAndValidListener() {
                @Override
                public void onConfigurationReadyAndValid(IAConfigManager iaConfigManager, boolean success, Exception e) {
                    // Can be called more than once
                    if (completionCallback != null) {
                        if (success) {
                            completionCallback.onInitializationSucceeded();
                        } else {
                            completionCallback.onInitializationFailed("Fyber SDK initialization failed");
                        }
                    }

                    IAConfigManager.removeListener(this);
                }
            });
        }
    }

    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
        return new VersionInfo(major, minor, micro);
    }

    public VersionInfo getSDKVersionInfo() {
        String sdkVersion = InneractiveAdManager.getVersion();
        String splits[] = sdkVersion.split("\\.");
        int major = 0;
        int minor = 0;
        int micro = 0;
        if (splits.length > 2) {
            major = Integer.parseInt(splits[0]);
            minor = Integer.parseInt(splits[1]);
            micro = Integer.parseInt(splits[2]);
        } else if (splits.length == 2) {
            major = Integer.parseInt(splits[0]);
            minor = Integer.parseInt(splits[1]);
        } else if (splits.length == 1) {
            major = Integer.parseInt(splits[0]);
        }
        return new VersionInfo(major, minor, micro);
    }

    /*****************************************************
    /** MediationBannerAdapter implementation starts here
    ******************************************************/

    @Override
    public void requestBannerAd(final Context context, final MediationBannerListener mediationBannerListener, Bundle bundle, AdSize adSize,
                                MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        initializeFromBundle(context, bundle);

        // Check that we got a valid spot id from the server
        String spotId = bundle.getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
            mediationBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            Log.w(TAG, "Cannot render banner ad. Please define a valid spot id on the AdMob UI");
            return;
        }

        mBannerSpot = InneractiveAdSpotManager.get().createSpot();
        mBannerSpot.setMediationName(MEDIATOR_NAME);

        InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
        mBannerSpot.addUnitController(controller);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);
        // Prepare wrapper view before making request
        mBannerWrapperView = new RelativeLayout(context);

        InneractiveAdSpot.RequestListener requestListener = createFyberBannerAdListener(mediationBannerListener);
        mBannerSpot.setRequestListener(requestListener);
        mBannerSpot.requestAd(request);
    }

    @Override
    public View getBannerView() {
        return mBannerWrapperView;
    }

    @Override
    public void onDestroy() {
        if (mBannerSpot != null) {
            mBannerSpot.destroy();
            mBannerSpot = null;
        }

        if (mInterstitialSpot != null) {
            mInterstitialSpot.destroy();;
            mInterstitialSpot = null;
        }

        if (mInterstitialContext != null) {
            mInterstitialContext.clear();
            mInterstitialContext = null;
        }
    }

    @Override
    public void onPause() {
        // No relevant action. Refresh is disabled for banners
    }

    @Override
    public void onResume() {
        // No relevant action. Refresh is disabled for banners
    }

    /**
     * Creates Fyber's banner ad request listener
     * @param mediationBannerListener Google's mediation banner listener
     * @return the created request listener
     */
    private InneractiveAdSpot.RequestListener createFyberBannerAdListener(final MediationBannerListener mediationBannerListener) {
        InneractiveAdSpot.RequestListener requestListener = new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot inneractiveAdSpot) {
                InneractiveAdViewEventsListener listener = createFyberAdViewListener(inneractiveAdSpot, mediationBannerListener);

                if (listener != null) {
                    mediationBannerListener.onAdLoaded(FyberMediationAdapter.this);
                } else {
                    mediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot inneractiveAdSpot, InneractiveErrorCode inneractiveErrorCode) {
                // Convert Fyber's Marketplace error code into AdMob's AdRequest error code
                int adMobErrorCode = AdRequest.ERROR_CODE_INTERNAL_ERROR;
                if (inneractiveErrorCode == InneractiveErrorCode.CONNECTION_ERROR
                        || inneractiveErrorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
                    adMobErrorCode = AdRequest.ERROR_CODE_NETWORK_ERROR;
                } else if (inneractiveErrorCode == InneractiveErrorCode.NO_FILL) {
                    adMobErrorCode = AdRequest.ERROR_CODE_NO_FILL;
                }

                mediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, adMobErrorCode);
            }
        };

        return requestListener;
    }

    /**
     * When an ad is fetched successfully, creates a listener for Fyber's AdView events
     * @param adSpot Fyber's ad view spot
     * @param mediationBannerListener Google's mediation banner listener
     * @return true if created succesfully, false otherwise
     */
    private InneractiveAdViewEventsListener createFyberAdViewListener(InneractiveAdSpot adSpot, final MediationBannerListener mediationBannerListener) {
        // Just a double check that we have the right type of selected controller
        if (adSpot == null || !(adSpot.getSelectedUnitController() instanceof InneractiveAdViewUnitController)) {
            return null;
        }

        InneractiveAdViewUnitController controller = (InneractiveAdViewUnitController)mBannerSpot.getSelectedUnitController();

        // Bind the wrapper view
        controller.bindView(mBannerWrapperView);

        InneractiveAdViewEventsListener adViewListener = new InneractiveAdViewEventsListenerAdapter() {
            @Override
            public void onAdImpression(InneractiveAdSpot adSpot) {
                // Nothing to report back here
            }

            @Override
            public void onAdClicked(InneractiveAdSpot adSpot) {
                mediationBannerListener.onAdClicked(FyberMediationAdapter.this);
                mediationBannerListener.onAdOpened(FyberMediationAdapter.this);
            }

            @Override
            public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
                mediationBannerListener.onAdClosed(FyberMediationAdapter.this);
            }

            @Override
            public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
                mediationBannerListener.onAdLeftApplication(FyberMediationAdapter.this);
            }
        };

        controller.setEventsListener(adViewListener);

        return adViewListener;
    }

    /** MediationInterstitialAdapter implementation */

    @Override
    public void requestInterstitialAd(Context context, final MediationInterstitialListener mediationInterstitialListener, Bundle bundle, MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        Log.d(TAG, "requestInterstitialAd called with bundle: " + bundle);

        initializeFromBundle(context, bundle);

        /* Cache the context for showInterstitial */
        mInterstitialContext = new WeakReference<>(context);

        // Check that we got a valid spot id from the server
        String spotId = bundle.getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
            mediationInterstitialListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            Log.w(TAG, "Cannot render interstitial ad. Please define a valid spot id on the AdMob UI");
            return;
        }

        mMediationInterstitialListener = mediationInterstitialListener;

        mInterstitialSpot = InneractiveAdSpotManager.get().createSpot();
        mInterstitialSpot.setMediationName(MEDIATOR_NAME);

        InneractiveFullscreenUnitController controller = new InneractiveFullscreenUnitController();
        mInterstitialSpot.addUnitController(controller);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);

        InneractiveAdSpot.RequestListener requestListener = createFyberInterstitialAdListener();
        mInterstitialSpot.setRequestListener(requestListener);
        mInterstitialSpot.requestAd(request);
    }

    @Override
    public void showInterstitial() {
        if (mInterstitialSpot != null && mInterstitialSpot.getSelectedUnitController() instanceof InneractiveFullscreenUnitController) {
            Context context = mInterstitialContext != null ? mInterstitialContext.get() : null;
            if (context != null) {
                ((InneractiveFullscreenUnitController) mInterstitialSpot.getSelectedUnitController()).show(context);
            } else {
                Log.w(TAG, "showInterstitial called, but context reference was lost");
                mMediationInterstitialListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        } else {
            Log.w(TAG, "showInterstitial called, but spot is not ready for show? Should never happen");
            mMediationInterstitialListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    private InneractiveAdSpot.RequestListener createFyberInterstitialAdListener() {
        InneractiveAdSpot.RequestListener requestListener = new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                InneractiveFullscreenAdEventsListener listener = createFyberInterstitialListener();

                if (listener != null) {
                    mMediationInterstitialListener.onAdLoaded(FyberMediationAdapter.this);
                } else {
                    mMediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                                     InneractiveErrorCode inneractiveErrorCode) {
                // Convert Fyber's Marketplace error code into AdMob's AdRequest error code
                int adMobErrorCode = AdRequest.ERROR_CODE_INTERNAL_ERROR;
                if (inneractiveErrorCode == InneractiveErrorCode.CONNECTION_ERROR
                        || inneractiveErrorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
                    adMobErrorCode = AdRequest.ERROR_CODE_NETWORK_ERROR;
                } else if (inneractiveErrorCode == InneractiveErrorCode.NO_FILL) {
                    adMobErrorCode = AdRequest.ERROR_CODE_NO_FILL;
                }

                mMediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, adMobErrorCode);
            }
        };

        return requestListener;
    }

    /**
     * When an ad is fetched successfully, creates a listener for Fyber's Interstitial events
     * @return the created event listener, or null otherwise
     */
    private InneractiveFullscreenAdEventsListener createFyberInterstitialListener() {
        // Just a double check that we have the right type of selected controller
        if (mInterstitialSpot == null || !(mInterstitialSpot.getSelectedUnitController() instanceof InneractiveFullscreenUnitController)) {
            return null;
        }

        InneractiveFullscreenUnitController controller = (InneractiveFullscreenUnitController)mInterstitialSpot.getSelectedUnitController();

        InneractiveFullscreenAdEventsListener interstitialListener = new InneractiveFullscreenAdEventsListenerAdapter() {
            @Override
            public void onAdImpression(InneractiveAdSpot adSpot) {
                mMediationInterstitialListener.onAdOpened(FyberMediationAdapter.this);
            }

            @Override
            public void onAdClicked(InneractiveAdSpot adSpot) {
                mMediationInterstitialListener.onAdClicked(FyberMediationAdapter.this);
            }

            @Override
            public void onAdDismissed(InneractiveAdSpot adSpot) {
                mMediationInterstitialListener.onAdClosed(FyberMediationAdapter.this);
            }

            @Override
            public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
                mMediationInterstitialListener.onAdLeftApplication(FyberMediationAdapter.this);
            }
        };

        controller.setEventsListener(interstitialListener);

        return interstitialListener;
    }

    /**
     *  Helper method for calling the initialization method, if it wasn't called by Admob
     * @param context
     * @param bundle
     */
    private void initializeFromBundle(Context context, Bundle bundle) {
        List<MediationConfiguration> configs = new ArrayList<>();

        // Bridge between the legacy API and the new Adapter API. The ad format parameter is not actually used in the initialize method
        configs.add(new MediationConfiguration(null, bundle));
        initialize(context, null, configs);
    }

}
