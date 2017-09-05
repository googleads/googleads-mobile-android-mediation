package com.applovin.mediation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdkSettings;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK interstitials and rewarded video adapter for AdMob.
 * @version 7.3.2.0
 */

public class ApplovinAdapter
        implements MediationRewardedVideoAdAdapter, MediationInterstitialAdapter, OnContextChangedListener
{
    private static final boolean LOGGING_ENABLED = false;
    private static final String  ADAPTER_VERSION = "7.3.2.0";
    private static final String  SDK_KEY_PARAM   = "sdkKey";
    private static final String  PLACEMENT_PARAM = "placement";

    private static final AppLovinSdkSettings sdkSettings = new AppLovinSdkSettings();

    private static Map<String, AppLovinAdListener> placementListeners = new HashMap<>();
    private static List<AppLovinAdListener>        loadList           = new ArrayList<>();
    private static AppLovinSdk                     interstitialSdk;
    private static AppLovinAdListener              currentlyLoading;

    private boolean                          initialized;
    private AppLovinSdk                      rewardedSdk;
    private AppLovinAdListener               appLovinAdListener;
    private AppLovinIncentivizedInterstitial rewardedAd;
    private Context                          context;
    private Bundle                           serverParameters;
    private Bundle                           mediationExtras;
    private MediationRewardedVideoAdListener rewardedAdMobListener;
    private MediationInterstitialListener    interstitialAdMobListener;

    AppLovinInterstitialAdDialog interstitialAd;

    //
    // AdMob Mediation Methods
    //

    @Override
    public void initialize(final Context context, final MediationAdRequest adRequest, final String userId, final MediationRewardedVideoAdListener listener, final Bundle serverParameters, final Bundle networkExtras)
    {
        if ( !validateContext( context ))
        {
            log( ERROR, "Unable to request AppLovin rewarded video. Invalid context provided." );
            listener.onInitializationFailed( this, AdRequest.ERROR_CODE_INVALID_REQUEST );

            return;
        }

        log( DEBUG, "Initializing AppLovin rewarded video..." );

        this.context          = context;
        rewardedAdMobListener = listener;
        this.serverParameters = serverParameters;

        if ( !initialized )
        {
            rewardedSdk = sdkInstance();

            initialized = true;
            rewardedAd = AppLovinIncentivizedInterstitial.create( rewardedSdk );
            appLovinAdListener = new AppLovinAdListener( this, rewardedAdMobListener );
        }

        rewardedAdMobListener.onInitializationSucceeded( this );
    }

    @Override
    public boolean isInitialized()
    {
        return initialized;
    }

    @Override
    public void loadAd(final MediationAdRequest adRequest, final Bundle serverParameters, final Bundle networkExtras)
    {
        log( DEBUG, "Requesting AppLovin rewarded video" );

        if ( rewardedAd.isAdReadyToDisplay() )
        {
            rewardedAdMobListener.onAdLoaded( this );
        }
        else
        {
            rewardedAd.preload( appLovinAdListener );
        }
    }

    @Override
    public void showVideo()
    {
        if ( rewardedAd.isAdReadyToDisplay() )
        {
            try
            {
                // AppLovin SDK < 7.2.0 uses an Activity, as opposed to Context in >= 7.2.0
                final Class<?> contextClass = ( AppLovinSdk.VERSION_CODE < 720 ) ? Activity.class : Context.class;
                final Method showMethod = AppLovinIncentivizedInterstitial.class.getMethod( "show",
                                                                                            contextClass,
                                                                                            String.class,
                                                                                            AppLovinAdRewardListener.class,
                                                                                            AppLovinAdVideoPlaybackListener.class,
                                                                                            AppLovinAdDisplayListener.class,
                                                                                            AppLovinAdClickListener.class );

                try
                {
                    if ( muteAudio() )
                    {
                        rewardedSdk.getSettings().setMuted( muteAudio() );
                    }

                    if ( placement() != null )
                    {
                        showMethod.invoke( rewardedAd, this.context, placement(), appLovinAdListener, appLovinAdListener, appLovinAdListener, appLovinAdListener );
                    }
                    else
                    {
                        showMethod.invoke( rewardedAd, this.context, null, appLovinAdListener, appLovinAdListener, appLovinAdListener, appLovinAdListener );
                    }
                }
                catch ( Throwable th )
                {
                    log( ERROR, "Unable to invoke show() method from AppLovinIncentivizedInterstitial." );
                    rewardedAdMobListener.onAdOpened( this );
                    rewardedAdMobListener.onAdClosed( this );
                }
            }
            catch ( Throwable th )
            {
                log( ERROR, "Unable to get show() method from AppLovinIncentivizedInterstitial." );
                rewardedAdMobListener.onAdOpened( this );
                rewardedAdMobListener.onAdClosed( this );
            }
        }
        else
        {
            log( ERROR, "Failed to show an AppLovin rewarded video before one was loaded" );
            rewardedAdMobListener.onAdOpened( this );
            rewardedAdMobListener.onAdClosed( this );
        }
    }

    //
    // Interstitial Mediation Methods
    //

    @Override
    public void requestInterstitialAd( Context context, MediationInterstitialListener listener, Bundle serverParameters, MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        log( DEBUG, "Requesting AppLovin interstitial..." );

        // SDK versions BELOW 7.2.0 require a instance of an Activity to be passed in as the context
        if ( !validateContext( context ) )
        {
            log( ERROR, "Unable to request AppLovin interstitial. Invalid context provided." );
            listener.onAdFailedToLoad( this, AdRequest.ERROR_CODE_INVALID_REQUEST );

            return;
        }

        // Store parent objects
        this.context              = context;
        this.serverParameters     = serverParameters;
        this.mediationExtras      = mediationExtras;
        interstitialAdMobListener = listener;

        if ( !placementListeners.containsKey( placement() ) )
        {
            appLovinAdListener = new AppLovinAdListener( this, interstitialAdMobListener );
            placementListeners.put( placement(), appLovinAdListener );
        }
        else
        {
            appLovinAdListener = placementListeners.get( placement() );

            // Update to latest interstitialAdMobListener
            appLovinAdListener.updateAdMobListener( interstitialAdMobListener );
        }

        interstitialSdk = sdkInstance();

        log(DEBUG, "Adding interstitial request to load list for placement: " + placement() );
        loadList.add( appLovinAdListener );
        loadNextAd();

        if ( !AppLovinAdListener.ADS_QUEUE.isEmpty()  )
        {
            adLoaded( appLovinAdListener );
            interstitialAdMobListener.onAdLoaded( ApplovinAdapter.this );
        }
    }

    @Override
    public void showInterstitial() {
        appLovinAdListener = placementListeners.get( placement() );

        if ( (appLovinAdListener != null && appLovinAdListener.hasAdReady()) || AppLovinInterstitialAd.isAdReadyToDisplay( interstitialSdk.getApplicationContext() ) )
        {
            final AppLovinInterstitialAdDialog interstitialAd = createInterstitial();
            interstitialAd.setAdDisplayListener( appLovinAdListener );
            interstitialAd.setAdClickListener( appLovinAdListener );
            interstitialAd.setAdVideoPlaybackListener( appLovinAdListener );

            // mute audio if that option is set
            if ( muteAudio() )
            {
                interstitialSdk.getSettings().setMuted( muteAudio() );
            }

            // show with a placement if one is defined
            if ( appLovinAdListener != null )
            {
                if ( appLovinAdListener.hasAdReady() )
                {
                    interstitialAd.showAndRender( appLovinAdListener.dequeueAd(), placement() );
                }
                else
                {
                    interstitialAd.show( placement() );
                }
            }
            else
            {
                interstitialAd.show( placement() );
            }
        }
        else
        {
                log( ERROR, "Failed to show an AppLovin interstitial before one was loaded. Loading and showing..." );
                interstitialAdMobListener.onAdOpened( this );
                interstitialAdMobListener.onAdClosed( this );
        }
    }

    private AppLovinInterstitialAdDialog createInterstitial()
    {
        AppLovinInterstitialAdDialog inter = null;

        try
        {
            // AppLovin SDK < 7.2.0 uses an Activity, as opposed to Context in >= 7.2.0
            final Class<?> contextClass = ( AppLovinSdk.VERSION_CODE < 720 ) ? Activity.class : Context.class;
            final Method method = AppLovinInterstitialAd.class.getMethod( "create", AppLovinSdk.class, contextClass );
            inter = (AppLovinInterstitialAdDialog) method.invoke(null, interstitialSdk, context);
        }
        catch ( Throwable th )
        {
            log( ERROR, "Unable to create AppLovinInterstitialAd." );
            interstitialAdMobListener.onAdFailedToLoad( this, AdRequest.ERROR_CODE_INTERNAL_ERROR );
        }

        return inter;
    }

    @Override
    public void onPause() {}

    @Override
    public void onResume() {}

    @Override
    public void onDestroy() {

    }

    @Override
    public void onContextChanged(final Context context)
    {
        if ( context != null )
        {
            log( DEBUG, "Context changed: " + context );
            this.context = context;
        }
    }

    static synchronized void adLoaded( AppLovinAdListener appLovinAdListener )
    {
        loadList.remove( appLovinAdListener );
        currentlyLoading = null;
        loadNextAd();
    }

    static synchronized void adLoadFailed( AppLovinAdListener appLovinAdListener )
    {
        loadList.remove( appLovinAdListener );
        currentlyLoading = null;
        loadNextAd();
    }

    private static synchronized void loadNextAd()
    {
        if ( loadList.size() > 0 && ((currentlyLoading != null && currentlyLoading != loadList.get( 0 ) ) || currentlyLoading == null ) )
        {
            currentlyLoading = loadList.get( 0 );
            log( DEBUG, "Loading next interstitial ad from load list" );
            interstitialSdk.getAdService().loadNextAd( AppLovinAdSize.INTERSTITIAL, loadList.get( 0 ) );
        }
        else if ( loadList.size() == 0 )
        {
            log( DEBUG, "No more interstitials to load");
        }
        else
        {
            log( DEBUG, "Another ad is currently loading" );
        }
    }

    private String placement()
    {
        return serverParameters.getString( PLACEMENT_PARAM );
    }

    private boolean muteAudio ()
    {
        if ( mediationExtras != null )
        {
            return mediationExtras.getBoolean(AppLovinExtrasBundleBuilder.MUTE_AUDIO, false);
        }

        return false;
    }

    private AppLovinSdk sdkInstance()
    {
        String sdkKey = serverParameters.getString( SDK_KEY_PARAM );
        AppLovinSdk sdk;

        if ( sdkKey != null && !sdkKey.equals("") )
        {
            sdk = AppLovinSdk.getInstance(sdkKey, sdkSettings, context);
        }
        else
        {
            sdk = AppLovinSdk.getInstance( context );
        }

        sdk.setPluginVersion( ADAPTER_VERSION );

        return sdk;
    }

    //
    // Utility Methods
    //

    private static boolean validateContext(Context context)
    {
        // SDK versions BELOW 7.2.0 require a instance of an Activity to be passed in as the context
        if ( AppLovinSdk.VERSION_CODE < 720 && !( context instanceof Activity ) )
        {
            return false;
        }

        return true;
    }

    static void log(final int priority, final String message)
    {
        if ( LOGGING_ENABLED )
        {
            Log.println( priority, "AppLovinAdapter", message );
        }
    }

    static int toAdMobErrorCode(final int applovinErrorCode)
    {
        if ( applovinErrorCode == AppLovinErrorCodes.NO_FILL )
        {
            return AdRequest.ERROR_CODE_NO_FILL;
        }
        else if ( applovinErrorCode == AppLovinErrorCodes.NO_NETWORK || applovinErrorCode == AppLovinErrorCodes.FETCH_AD_TIMEOUT )
        {
            return AdRequest.ERROR_CODE_NETWORK_ERROR;
        }
        else
        {
            return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }
}