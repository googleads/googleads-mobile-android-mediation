package com.applovin.mediation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK interstitial and rewarded video ad adapter for AdMob.
 *
 * @version 7.4.1.1
 */
public class ApplovinAdapter implements MediationBannerAdapter, MediationRewardedVideoAdAdapter, MediationInterstitialAdapter, OnContextChangedListener {
    private static final boolean LOGGING_ENABLED = false;
    private static final String ADAPTER_VERSION = "7.4.1.1";
    private static final String SDK_KEY_PARAM = "sdkKey";
    private static final String PLACEMENT_PARAM = "placement";

    private static final AppLovinSdkSettings sSdkSettings = new AppLovinSdkSettings();

    private static Map<String, AppLovinAdListener> sPlacementListeners = new HashMap<>();
    private static List<AppLovinAdListener> sLoadList = new ArrayList<>();
    private static AppLovinSdk sInterstitialSdk;
    private static AppLovinAdListener sCurrentlyLoading;

    private boolean mInitialized;
    private AppLovinSdk mRewardedSdk;
    private AppLovinAdListener mAppLovinAdListener;
    private AppLovinIncentivizedInterstitial mRewardedAd;
    private Context mContext;
    private Bundle mServerParameters;
    private Bundle mMediationExtras;
    private MediationRewardedVideoAdListener mRewardedAdMobListener;
    private MediationInterstitialListener mInterstitialAdMobListener;

    AppLovinInterstitialAdDialog mInterstitialAd;

    // Banner fields
    private AppLovinAdView mAdView;
    private AppLovinSdk mAdViewSdk;

    //region AdMob rewarded video ad mediation methods
    @Override
    public void initialize(final Context context,
                           final MediationAdRequest adRequest,
                           final String userId,
                           final MediationRewardedVideoAdListener listener,
                           final Bundle serverParameters,
                           final Bundle networkExtras) {
        if (!validateContext(context)) {
            log(ERROR, "Unable to request AppLovin rewarded video. Invalid context provided.");
            listener.onInitializationFailed(this, AdRequest.ERROR_CODE_INVALID_REQUEST);

            return;
        }

        log(DEBUG, "Initializing AppLovin rewarded video...");

        this.mContext = context;
        mRewardedAdMobListener = listener;
        this.mServerParameters = serverParameters;

        if (!mInitialized) {
            mRewardedSdk = sdkInstance(context);

            mInitialized = true;
            mRewardedAd = AppLovinIncentivizedInterstitial.create(mRewardedSdk);
            mAppLovinAdListener = new AppLovinAdListener(this, mRewardedAdMobListener);
        }

        mRewardedAdMobListener.onInitializationSucceeded(this);
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    public void loadAd(final MediationAdRequest adRequest,
                       final Bundle serverParameters,
                       final Bundle networkExtras) {
        log(DEBUG, "Requesting AppLovin rewarded video");

        if (mRewardedAd.isAdReadyToDisplay()) {
            mRewardedAdMobListener.onAdLoaded(this);
        } else {
            mRewardedAd.preload(mAppLovinAdListener);
        }
    }

    @Override
    public void showVideo() {
        if (mRewardedAd.isAdReadyToDisplay()) {
            try {
                // AppLovin SDK < 7.2.0 uses an Activity, as opposed to Context in >= 7.2.0
                final Class<?> contextClass =
                        (AppLovinSdk.VERSION_CODE < 720) ? Activity.class : Context.class;
                final Method showMethod = AppLovinIncentivizedInterstitial.class.getMethod("show",
                        contextClass,
                        String.class,
                        AppLovinAdRewardListener.class,
                        AppLovinAdVideoPlaybackListener.class,
                        AppLovinAdDisplayListener.class,
                        AppLovinAdClickListener.class);

                try {
                    if (muteAudio()) {
                        mRewardedSdk.getSettings().setMuted(muteAudio());
                    }

                    if (placement() != null) {
                        showMethod.invoke(mRewardedAd,
                                this.mContext,
                                placement(),
                                mAppLovinAdListener,
                                mAppLovinAdListener,
                                mAppLovinAdListener,
                                mAppLovinAdListener);
                    } else {
                        showMethod.invoke(mRewardedAd,
                                this.mContext,
                                null,
                                mAppLovinAdListener,
                                mAppLovinAdListener,
                                mAppLovinAdListener,
                                mAppLovinAdListener);
                    }
                } catch (Throwable th) {
                    log(ERROR, "Unable to invoke show() method from"
                            + " AppLovinIncentivizedInterstitial.");
                    mRewardedAdMobListener.onAdOpened(this);
                    mRewardedAdMobListener.onAdClosed(this);
                }
            } catch (Throwable th) {
                log(ERROR, "Unable to get show() method from AppLovinIncentivizedInterstitial.");
                mRewardedAdMobListener.onAdOpened(this);
                mRewardedAdMobListener.onAdClosed(this);
            }
        } else {
            log(ERROR, "Failed to show an AppLovin rewarded video before one was loaded");
            mRewardedAdMobListener.onAdOpened(this);
            mRewardedAdMobListener.onAdClosed(this);
        }
    }
    //endregion

    //region Interstitial ad mediation methods
    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        log(DEBUG, "Requesting AppLovin interstitial...");

        // SDK versions BELOW 7.2.0 require a instance of an Activity to be passed in as the context
        if (!validateContext(context)) {
            log(ERROR, "Unable to request AppLovin interstitial. Invalid context provided.");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);

            return;
        }

        // Store parent objects
        this.mContext = context;
        this.mServerParameters = serverParameters;
        this.mMediationExtras = mediationExtras;
        mInterstitialAdMobListener = listener;

        if (!sPlacementListeners.containsKey(placement())) {
            mAppLovinAdListener = new AppLovinAdListener(this, mInterstitialAdMobListener);
            sPlacementListeners.put(placement(), mAppLovinAdListener);
        } else {
            mAppLovinAdListener = sPlacementListeners.get(placement());

            // Update to latest mInterstitialAdMobListener
            mAppLovinAdListener.updateAdMobListener(mInterstitialAdMobListener);
        }

        sInterstitialSdk = sdkInstance(context);

        log(DEBUG, "Adding interstitial request to load list for placement: " + placement());
        sLoadList.add(mAppLovinAdListener);
        loadNextAd();

        if (!AppLovinAdListener.ADS_QUEUE.isEmpty()) {
            adLoaded(mAppLovinAdListener);
            mInterstitialAdMobListener.onAdLoaded(ApplovinAdapter.this);
        }
    }

    @Override
    public void showInterstitial() {
        mAppLovinAdListener = sPlacementListeners.get(placement());

        if ((mAppLovinAdListener != null && mAppLovinAdListener.hasAdReady())
                || AppLovinInterstitialAd.isAdReadyToDisplay(
                        sInterstitialSdk.getApplicationContext())) {
            final AppLovinInterstitialAdDialog interstitialAd = createInterstitial();
            interstitialAd.setAdDisplayListener(mAppLovinAdListener);
            interstitialAd.setAdClickListener(mAppLovinAdListener);
            interstitialAd.setAdVideoPlaybackListener(mAppLovinAdListener);

            // mute audio if that option is set
            if (muteAudio()) {
                sInterstitialSdk.getSettings().setMuted(muteAudio());
            }

            // show with a placement if one is defined
            if (mAppLovinAdListener != null) {
                if (mAppLovinAdListener.hasAdReady()) {
                    interstitialAd.showAndRender(mAppLovinAdListener.dequeueAd(), placement());
                } else {
                    interstitialAd.show(placement());
                }
            } else {
                interstitialAd.show(placement());
            }
        } else {
            log(ERROR, "Failed to show an AppLovin interstitial before one was loaded."
                    + " Loading and showing...");
            mInterstitialAdMobListener.onAdOpened(this);
            mInterstitialAdMobListener.onAdClosed(this);
        }
    }

    private AppLovinInterstitialAdDialog createInterstitial() {
        AppLovinInterstitialAdDialog inter = null;

        try {
            // AppLovin SDK < 7.2.0 uses an Activity, as opposed to Context in >= 7.2.0
            final Class<?> contextClass =
                    (AppLovinSdk.VERSION_CODE < 720) ? Activity.class : Context.class;
            final Method method =
                    AppLovinInterstitialAd.class.getMethod(
                            "create", AppLovinSdk.class, contextClass);
            inter = (AppLovinInterstitialAdDialog) method.invoke(null, sInterstitialSdk, mContext);
        } catch (Throwable th) {
            log(ERROR, "Unable to create AppLovinInterstitialAd.");
            mInterstitialAdMobListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }

        return inter;
    }
    //endregion


    //region - Banner Methods
    @Override
    public void requestBannerAd(final Context context, final MediationBannerListener mediationBannerListener, final Bundle serverParameters, final AdSize adSize, final MediationAdRequest mediationAdRequest, final Bundle mediationExtras)
    {
        // TODO: More elegant solution for retrieving SDK
        mServerParameters = serverParameters;
        mAdViewSdk = sdkInstance(context);

        // SDK versions < 7.1.0 require a instance of an Activity to be passed in as the context
        if ( AppLovinSdk.VERSION_CODE < 710 && !( context instanceof Activity ) )
        {
            log( ERROR, "Unable to request AppLovin banner. Invalid context provided." );
            mediationBannerListener.onAdFailedToLoad( this, AdRequest.ERROR_CODE_INVALID_REQUEST );

            return;
        }

        log( DEBUG, "Requesting AppLovin banner of size: " + adSize );

        final AppLovinAdSize appLovinAdSize = appLovinAdSizeFromAdMobAdSize( adSize );
        if ( appLovinAdSize != null )
        {
            mAdView = createAdView( appLovinAdSize, context, mediationBannerListener );

            // Add paranoia null-check
            if ( mAdView != null )
            {
                mAdView.setAdLoadListener( new AppLovinAdLoadListener()
                {
                    @Override
                    public void adReceived(final AppLovinAd ad)
                    {
                        log( DEBUG, "Successfully loaded banner ad" );

                        final String placement = ( serverParameters != null ) ? serverParameters.getString( PLACEMENT_PARAM ) : null;
                        mAdView.renderAd( ad, placement );

                        mediationBannerListener.onAdLoaded( ApplovinAdapter.this );
                    }

                    @Override
                    public void failedToReceiveAd(final int errorCode)
                    {
                        log( ERROR, "Failed to load banner ad with code: " + errorCode );
                        mediationBannerListener.onAdFailedToLoad( ApplovinAdapter.this, toAdMobErrorCode( errorCode ) );
                    }
                } );
                mAdView.setAdDisplayListener( new AppLovinAdDisplayListener()
                {
                    @Override
                    public void adDisplayed(final AppLovinAd ad)
                    {
                        log( DEBUG, "Banner displayed" );
                    }

                    @Override
                    public void adHidden(final AppLovinAd ad)
                    {
                        log( DEBUG, "Banner dismissed" );
                    }
                } );
                mAdView.setAdClickListener( new AppLovinAdClickListener()
                {
                    @Override
                    public void adClicked(final AppLovinAd ad)
                    {
                        log( DEBUG, "Banner clicked" );

                        mediationBannerListener.onAdClicked( ApplovinAdapter.this );
                        mediationBannerListener.onAdOpened( ApplovinAdapter.this );
                        mediationBannerListener.onAdLeftApplication( ApplovinAdapter.this );
                    }
                } );

                // As of Android SDK >= 7.3.0, we added a listener for banner events
                if ( AppLovinSdk.VERSION_CODE >= 730 )
                {
                    mAdView.setAdViewEventListener( new AppLovinAdViewEventListener()
                    {
                        @Override
                        public void adOpenedFullscreen(final AppLovinAd ad, final AppLovinAdView adView)
                        {
                            log( DEBUG, "Banner opened fullscreen" );
                            mediationBannerListener.onAdOpened( ApplovinAdapter.this );
                        }

                        @Override
                        public void adClosedFullscreen(final AppLovinAd ad, final AppLovinAdView adView)
                        {
                            log( DEBUG, "Banner closed fullscreen" );
                            mediationBannerListener.onAdClosed( ApplovinAdapter.this );
                        }

                        @Override
                        public void adLeftApplication(final AppLovinAd ad, final AppLovinAdView adView)
                        {
                            // We will fire onAdLeftApplication() in the adClicked() callback
                            log( DEBUG, "Banner left application" );
                        }

                        @Override
                        public void adFailedToDisplay(final AppLovinAd ad, final AppLovinAdView adView, final AppLovinAdViewDisplayErrorCode code)
                        {
                            log( DEBUG, "Banner failed to display: " + code );
                        }
                    } );
                }

                mAdView.loadNextAd();
            }
            else
            {
                log( ERROR, "Unable to request AppLovin banner" );
                mediationBannerListener.onAdFailedToLoad( this, AdRequest.ERROR_CODE_INTERNAL_ERROR );
            }
        }
        else
        {
            log( ERROR, "Unable to request AppLovin banner" );
            mediationBannerListener.onAdFailedToLoad( this, AdRequest.ERROR_CODE_INVALID_REQUEST );
        }
    }

    @Override
    public View getBannerView()
    {
        return mAdView;
    }
    //endregion

    //region Banner utility methods
    private AppLovinAdSize appLovinAdSizeFromAdMobAdSize(final AdSize adSize)
    {
        if ( AdSize.BANNER.equals( adSize ) )
        {
            return AppLovinAdSize.BANNER;
        }
        else if ( AdSize.MEDIUM_RECTANGLE.equals( adSize ) )
        {
            return AppLovinAdSize.MREC;
        }
        else if ( AdSize.LEADERBOARD.equals( adSize ) )
        {
            return AppLovinAdSize.LEADER;
        }

        return null;
    }

    private AppLovinAdView createAdView(final AppLovinAdSize size, final Context parentContext, final MediationBannerListener customEventBannerListener)
    {
        AppLovinAdView adView = null;

        try
        {
            // AppLovin SDK < 7.1.0 uses an Activity, as opposed to Context in >= 7.1.0
            final Class<?> contextClass = ( AppLovinSdk.VERSION_CODE < 710 ) ? Activity.class : Context.class;
            final Constructor<?> constructor = AppLovinAdView.class.getConstructor( AppLovinSdk.class, AppLovinAdSize.class, contextClass );

            adView = (AppLovinAdView) constructor.newInstance( mAdViewSdk, size, parentContext );
        }
        catch ( Throwable th )
        {
            log( ERROR, "Unable to get create AppLovinAdView." );
            customEventBannerListener.onAdFailedToLoad( this, AdRequest.ERROR_CODE_INVALID_REQUEST );
        }

        return adView;
    }
    //endregion

    //region MediationAdapter methods.
    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onDestroy() {
    }
    //endregion

    @Override
    public void onContextChanged(final Context context) {
        if (context != null) {
            log(DEBUG, "Context changed: " + context);
            this.mContext = context;
        }
    }

    static synchronized void adLoaded(AppLovinAdListener appLovinAdListener) {
        sLoadList.remove(appLovinAdListener);
        sCurrentlyLoading = null;
        loadNextAd();
    }

    static synchronized void adLoadFailed(AppLovinAdListener appLovinAdListener) {
        sLoadList.remove(appLovinAdListener);
        sCurrentlyLoading = null;
        loadNextAd();
    }

    private static synchronized void loadNextAd() {
        if (sLoadList.size() > 0 && ((sCurrentlyLoading != null
                && sCurrentlyLoading != sLoadList.get(0)) || sCurrentlyLoading == null)) {
            sCurrentlyLoading = sLoadList.get(0);
            log(DEBUG, "Loading next interstitial ad from load list");
            sInterstitialSdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, sLoadList.get(0));
        } else if (sLoadList.size() == 0) {
            log(DEBUG, "No more interstitials to load");
        } else {
            log(DEBUG, "Another ad is currently loading");
        }
    }

    private String placement() {
        return mServerParameters.getString(PLACEMENT_PARAM);
    }

    private boolean muteAudio() {
        if (mMediationExtras != null) {
            return mMediationExtras.getBoolean(AppLovinExtrasBundleBuilder.MUTE_AUDIO, false);
        }

        return false;
    }

    private AppLovinSdk sdkInstance(final Context context) {
        String sdkKey = mServerParameters.getString(SDK_KEY_PARAM);
        AppLovinSdk sdk;

        if (sdkKey != null && !sdkKey.equals("")) {
            sdk = AppLovinSdk.getInstance(sdkKey, sSdkSettings, context);
        } else {
            sdk = AppLovinSdk.getInstance(context);
        }

        sdk.setPluginVersion(ADAPTER_VERSION);

        return sdk;
    }

    //region Utility Methods
    private static boolean validateContext(Context context) {
        // SDK versions BELOW 7.2.0 require a instance of an Activity to be passed in as the context
        if (AppLovinSdk.VERSION_CODE < 720 && !(context instanceof Activity)) {
            return false;
        }

        return true;
    }

    static void log(final int priority, final String message) {
        if (LOGGING_ENABLED) {
            Log.println(priority, "AppLovinAdapter", message);
        }
    }

    static int toAdMobErrorCode(final int applovinErrorCode) {
        if (applovinErrorCode == AppLovinErrorCodes.NO_FILL) {
            return AdRequest.ERROR_CODE_NO_FILL;
        } else if (applovinErrorCode == AppLovinErrorCodes.NO_NETWORK
                || applovinErrorCode == AppLovinErrorCodes.FETCH_AD_TIMEOUT) {
            return AdRequest.ERROR_CODE_NETWORK_ERROR;
        } else {
            return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }
    //endregion
}