package com.applovin.mediation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK banner, interstitial, and rewarded video adapter for AdMob.
 * <p>
 * Created by Thomas So on 1/25/18.
 */
public class ApplovinAdapter
        implements MediationBannerAdapter, MediationInterstitialAdapter, MediationRewardedVideoAdAdapter, OnContextChangedListener
{
    private static final boolean LOGGING_ENABLED = false;
    private static final String  DEFAULT_ZONE    = "";

    // Interstitial globals
    private static final HashMap<String, Queue<AppLovinAd>> INTERSTITIAL_AD_QUEUES      = new HashMap<String, Queue<AppLovinAd>>();
    private static final Object                             INTERSTITIAL_AD_QUEUES_LOCK = new Object();

    // Rewarded video globals
    private static final HashMap<String, AppLovinIncentivizedInterstitial> INCENTIVIZED_ADS      = new HashMap<String, AppLovinIncentivizedInterstitial>();
    private static final Object                                            INCENTIVIZED_ADS_LOCK = new Object();

    // Parent objects
    private AppLovinSdk mSdk;
    private Context     mContext;
    private Bundle      mMediationExtras;

    private String mPlacement;
    private String mZoneId;

    // Interstitial objects
    private MediationInterstitialListener mMediationInterstitialListener;

    // Rewarded Video objects
    private AtomicBoolean mInitialized = new AtomicBoolean();
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    private AppLovinIncentivizedInterstitial mIncentivizedAd;

    // AppLovin Banner fields
    private AppLovinAdView mAdView;
    private AppLovinSdk    mAdViewSdk;

    //
    //  Interstitial Methods
    //

    @Override
    public void requestInterstitialAd(Context context, MediationInterstitialListener listener, Bundle serverParameters, MediationAdRequest mediationAdRequest, Bundle networkExtras)
    {
        synchronized ( INTERSTITIAL_AD_QUEUES_LOCK )
        {
            //
            // TODO: Make sure the same instance of adapter is not used for multiple ad requests, AND multiple sizes... We'd need interSDK, bannerSdk, etc if so... -_-
            //

            // Store parent objects
            mSdk = AppLovinUtils.retrieveSdk( serverParameters, context );
            mContext = context;
            mMediationExtras = networkExtras;
            mMediationInterstitialListener = listener;

            mPlacement = AppLovinUtils.retrievePlacement( serverParameters );
            mZoneId = AppLovinUtils.retrieveZoneId( networkExtras );

            log( DEBUG, "Requesting interstitial for zone: " + mZoneId + " and placement: " + mPlacement );

            //
            // Create Ad Load listener
            //

            final AppLovinAdLoadListener adLoadListener = new AppLovinAdLoadListener()
            {
                @Override
                public void adReceived(AppLovinAd ad)
                {
                    log( DEBUG, "Interstitial did load ad: " + ad.getAdIdNumber() + " for zone: " + mZoneId + " and placement: " + mPlacement );

                    synchronized ( INTERSTITIAL_AD_QUEUES_LOCK )
                    {
                        Queue<AppLovinAd> preloadedAds = INTERSTITIAL_AD_QUEUES.get( mZoneId );
                        if ( preloadedAds == null )
                        {
                            preloadedAds = new LinkedList<AppLovinAd>();
                            INTERSTITIAL_AD_QUEUES.put( mZoneId, preloadedAds );
                        }

                        preloadedAds.offer( ad );

                        //
                        // TODO: Make sure this doesn't have consequences switching context within syncrhonized block
                        //

                        AppLovinSdkUtils.runOnUiThread( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mMediationInterstitialListener.onAdLoaded( ApplovinAdapter.this );
                            }
                        } );
                    }
                }

                @Override
                public void failedToReceiveAd(int code)
                {
                    log( ERROR, "Interstitial failed to load with error: " + code );

                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mMediationInterstitialListener.onAdFailedToLoad( ApplovinAdapter.this, AppLovinUtils.toAdMobErrorCode( code ) );
                        }
                    } );
                }
            };


            final Queue<AppLovinAd> queue = INTERSTITIAL_AD_QUEUES.get( mZoneId );
            if ( queue != null && queue.isEmpty() )
            {
                // If we don't already have enqueued ads, fetch from SDK

                if ( !TextUtils.isEmpty( mZoneId ) )
                {
                    mSdk.getAdService().loadNextAdForZoneId( mZoneId, adLoadListener );
                }
                else
                {
                    mSdk.getAdService().loadNextAd( AppLovinAdSize.INTERSTITIAL, adLoadListener );
                }
            }
            else
            {
                log( DEBUG, "Enqueued interstitial found. Finishing load..." );
                mMediationInterstitialListener.onAdLoaded( this );
            }
        }
    }

    @Override
    public void showInterstitial()
    {
        synchronized ( INTERSTITIAL_AD_QUEUES_LOCK )
        {
            // Update mute state
            mSdk.getSettings().setMuted( shouldMuteAudio() );

            final Queue<AppLovinAd> queue = INTERSTITIAL_AD_QUEUES.get( mZoneId );
            final AppLovinAd dequeuedAd = ( queue != null ) ? queue.poll() : null;

            final AppLovinInterstitialAdDialog interstitialAd = AppLovinInterstitialAd.create( mSdk, mContext );
            final AppLovinInterstitialAdListener listener = new AppLovinInterstitialAdListener( this, mMediationInterstitialListener );
            interstitialAd.setAdDisplayListener( listener );
            interstitialAd.setAdClickListener( listener );
            interstitialAd.setAdVideoPlaybackListener( listener );

            if ( dequeuedAd != null )
            {
                log( DEBUG, "Showing interstitial for zone: " + mZoneId + " placement: " + mPlacement );
                interstitialAd.showAndRender( dequeuedAd, mPlacement );
            }
            else
            {
                log( DEBUG, "Attempting to show interstitial before one was loaded" );

                // Check if we have a default zone interstitial available
                if ( TextUtils.isEmpty( mZoneId ) && interstitialAd.isAdReadyToDisplay() )
                {
                    log( DEBUG, "Showing interstitial preloaded by SDK" );
                    interstitialAd.show( mPlacement );
                }
                // TODO: Show ad for zone identifier if exists
                else
                {
                    mMediationInterstitialListener.onAdOpened( this );
                    mMediationInterstitialListener.onAdClosed( this );
                }
            }
        }
    }

    //
    // Rewarded Video Methods
    //

    @Override
    public void initialize(Context context, MediationAdRequest adRequest, String userId, MediationRewardedVideoAdListener listener, Bundle serverParameters, Bundle networkExtras)
    {
        log( DEBUG, "Attempting to initialize SDK" );

        if ( !mInitialized.getAndSet( true ) )
        {
            // Store parent objects
            mSdk = AppLovinUtils.retrieveSdk( serverParameters, context );
            mContext = context;
            mMediationExtras = networkExtras;
            mMediationRewardedVideoAdListener = listener;
        }

        listener.onInitializationSucceeded( this );
    }

    @Override
    public boolean isInitialized()
    {
        return mInitialized.get();
    }

    @Override
    public void loadAd(MediationAdRequest adRequest, Bundle serverParameters, Bundle networkExtras)
    {
        synchronized ( INCENTIVIZED_ADS_LOCK )
        {
            mPlacement = AppLovinUtils.retrievePlacement( serverParameters );
            mZoneId = AppLovinUtils.retrieveZoneId( networkExtras );

            log( DEBUG, "Requesting interstitial for zone: " + mZoneId + " and placement: " + mPlacement );

            // Check if incentivized ad for zone already exists
            if ( INCENTIVIZED_ADS.containsKey( mZoneId ) )
            {
                mIncentivizedAd = INCENTIVIZED_ADS.get( mZoneId );
            }
            else
            {
                // If this is a default Zone, create the incentivized ad normally
                if ( DEFAULT_ZONE.equals( mZoneId ) )
                {
                    mIncentivizedAd = AppLovinIncentivizedInterstitial.create( mSdk );
                }
                // Otherwise, use the Zones API
                else
                {
                    mIncentivizedAd = AppLovinIncentivizedInterstitial.create( mZoneId, mSdk );
                }

                INCENTIVIZED_ADS.put( mZoneId, mIncentivizedAd );
            }
        }

        if ( mIncentivizedAd.isAdReadyToDisplay() )
        {
            mMediationRewardedVideoAdListener.onAdLoaded( this );
        }
        else
        {
            mIncentivizedAd.preload( new AppLovinAdLoadListener()
            {
                @Override
                public void adReceived(final AppLovinAd ad)
                {
                    log( DEBUG, "Rewarded video did load ad: " + ad.getAdIdNumber() + " for zoneIdentifier: " + mZoneId + " and placement: " + mPlacement );

                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mMediationRewardedVideoAdListener.onAdLoaded( ApplovinAdapter.this );
                        }
                    } );
                }

                @Override
                public void failedToReceiveAd(final int code)
                {
                    log( ERROR, "Rewarded video failed to load with error: " + code );

                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mMediationRewardedVideoAdListener.onAdFailedToLoad( ApplovinAdapter.this, AppLovinUtils.toAdMobErrorCode( code ) );
                        }
                    } );
                }
            } );
        }
    }

    @Override
    public void showVideo()
    {
        if ( mIncentivizedAd.isAdReadyToDisplay() )
        {
            // Update mute state
            mSdk.getSettings().setMuted( shouldMuteAudio() );

            log( DEBUG, "Showing rewarded video for zone: " + mZoneId + " placement: " + mPlacement );

            final AppLovinIncentivizedAdListener listener = new AppLovinIncentivizedAdListener( this, mMediationRewardedVideoAdListener );
            mIncentivizedAd.show( mContext, mPlacement, listener, listener, listener, listener );
        }
        else
        {
            log( DEBUG, "Attempting to show rewarded video before one was loaded" );

            // TODO: Add support for checking default SDK-preloaded ad
            mMediationRewardedVideoAdListener.onAdOpened( this );
            mMediationRewardedVideoAdListener.onAdClosed( this );
        }
    }

    //
    //  Banner Methods
    //

    // TODO: Smart banners please

    @Override
    public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener, Bundle serverParameters, AdSize adSize, MediationAdRequest mediationAdRequest, Bundle mediationExtras)
    {
        // TODO: More elegant solution for retrieving SDK
        mServerParameters = serverParameters;
        mAdViewSdk = sdkInstance( context );

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
                    public void adReceived(AppLovinAd ad)
                    {
                        log( DEBUG, "Successfully loaded banner ad" );

                        final String placement = ( serverParameters != null )
                                ? serverParameters.getString( PLACEMENT_PARAM ) : null;
                        mAdView.renderAd( ad, placement );

                        mediationBannerListener.onAdLoaded( ApplovinAdapter.this );
                    }

                    @Override
                    public void failedToReceiveAd(int errorCode)
                    {
                        log( ERROR, "Failed to load banner ad with code: " + errorCode );
                        mediationBannerListener.onAdFailedToLoad(
                                ApplovinAdapter.this, toAdMobErrorCode( errorCode ) );
                    }
                } );
                mAdView.setAdDisplayListener( new AppLovinAdDisplayListener()
                {
                    @Override
                    public void adDisplayed(AppLovinAd ad)
                    {
                        log( DEBUG, "Banner displayed" );
                    }

                    @Override
                    public void adHidden(AppLovinAd ad)
                    {
                        log( DEBUG, "Banner dismissed" );
                    }
                } );
                mAdView.setAdClickListener( new AppLovinAdClickListener()
                {
                    @Override
                    public void adClicked(AppLovinAd ad)
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
                        public void adOpenedFullscreen(AppLovinAd ad, AppLovinAdView adView)
                        {
                            log( DEBUG, "Banner opened fullscreen" );
                            mediationBannerListener.onAdOpened( ApplovinAdapter.this );
                        }

                        @Override
                        public void adClosedFullscreen(AppLovinAd ad, AppLovinAdView adView)
                        {
                            log( DEBUG, "Banner closed fullscreen" );
                            mediationBannerListener.onAdClosed( ApplovinAdapter.this );
                        }

                        @Override
                        public void adLeftApplication(AppLovinAd ad, AppLovinAdView adView)
                        {
                            // We will fire onAdLeftApplication() in the adClicked() callback
                            log( DEBUG, "Banner left application" );
                        }

                        @Override
                        public void adFailedToDisplay(AppLovinAd ad, AppLovinAdView adView, AppLovinAdViewDisplayErrorCode code)
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

    private AppLovinAdSize appLovinAdSizeFromAdMobAdSize(AdSize adSize)
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

    private AppLovinAdView createAdView(AppLovinAdSize size, Context parentContext, MediationBannerListener customEventBannerListener)
    {
        AppLovinAdView adView = null;

        try
        {
            // AppLovin SDK < 7.1.0 uses an Activity, as opposed to Context in >= 7.1.0
            final Class<?> contextClass =
                    ( AppLovinSdk.VERSION_CODE < 710 ) ? Activity.class : Context.class;
            final Constructor<?> constructor = AppLovinAdView.class
                    .getConstructor( AppLovinSdk.class, AppLovinAdSize.class, contextClass );

            adView = (AppLovinAdView) constructor.newInstance( mAdViewSdk, size, parentContext );
        }
        catch ( Throwable th )
        {
            log( ERROR, "Unable to get create AppLovinAdView." );
            customEventBannerListener.onAdFailedToLoad( this,
                                                        AdRequest.ERROR_CODE_INVALID_REQUEST );
        }

        return adView;
    }

    //
    // Base MediationAdapter Methods
    //

    @Override
    public void onPause() {}

    @Override
    public void onResume() {}

    @Override
    public void onDestroy() {}

    //
    // OnContextChangedListener Methods
    //

    @Override
    public void onContextChanged(Context context)
    {
        if ( context != null )
        {
            log( DEBUG, "Context changed: " + context );
            this.mContext = context;
        }
    }

    // TODO: Utils?

    private boolean shouldMuteAudio()
    {
        return mMediationExtras != null && mMediationExtras.getBoolean( AppLovinExtrasBundleBuilder.MUTE_AUDIO );
    }

    static void log(int priority, final String message)
    {
        if ( LOGGING_ENABLED )
        {
            Log.println( priority, "AppLovinAdapter", message );
        }
    }

}