package com.applovin.mediation;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
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
    private static final boolean LOGGING_ENABLED = true;
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
    private Bundle      mNetworkExtras;

    // Interstitial objects
    private MediationInterstitialListener mMediationInterstitialListener;

    // Rewarded Video objects
    private final AtomicBoolean mInitialized = new AtomicBoolean();
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    private AppLovinIncentivizedInterstitial mIncentivizedInterstitial;

    // Banner objects
    private AppLovinAdView mAdView;

    // Controlled fields
    private String mPlacement;
    private String mZoneId;


    //
    //  Interstitial Methods
    //

    @Override
    public void requestInterstitialAd(Context context, MediationInterstitialListener interstitialListener, Bundle serverParameters, MediationAdRequest mediationAdRequest, Bundle networkExtras)
    {
        // Store parent objects
        mSdk = AppLovinUtils.retrieveSdk( serverParameters, context );
        mContext = context;
        mNetworkExtras = networkExtras;
        mMediationInterstitialListener = interstitialListener;

        mPlacement = AppLovinUtils.retrievePlacement( serverParameters );
        mZoneId = AppLovinUtils.retrieveZoneId( serverParameters );

        log( DEBUG, "Requesting interstitial for zone: " + mZoneId + " and placement: " + mPlacement );

        //
        // Create Ad Load listener
        //

        final AppLovinAdLoadListener adLoadListener = new AppLovinAdLoadListener()
        {
            @Override
            public void adReceived(final AppLovinAd ad)
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
            public void failedToReceiveAd(final int code)
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

        synchronized ( INTERSTITIAL_AD_QUEUES_LOCK )
        {
            final Queue<AppLovinAd> queue = INTERSTITIAL_AD_QUEUES.get( mZoneId );
            if ( queue == null || ( queue != null && queue.isEmpty() ) )
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
    }

    @Override
    public void showInterstitial()
    {
        synchronized ( INTERSTITIAL_AD_QUEUES_LOCK )
        {
            // Update mute state
            mSdk.getSettings().setMuted( AppLovinUtils.shouldMuteAudio( mNetworkExtras ) );

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
    public void initialize(Context context, MediationAdRequest adRequest, String userId, MediationRewardedVideoAdListener rewardedVideoAdListener, Bundle serverParameters, Bundle networkExtras)
    {
        log( DEBUG, "Attempting to initialize SDK" );

        if ( !mInitialized.getAndSet( true ) )
        {
            // Store parent objects
            mSdk = AppLovinUtils.retrieveSdk( serverParameters, context );
            mContext = context;
            mNetworkExtras = networkExtras;
            mMediationRewardedVideoAdListener = rewardedVideoAdListener;
        }

        rewardedVideoAdListener.onInitializationSucceeded( this );
    }

    @Override
    public boolean isInitialized()
    {
        // Please note: AdMob re-uses the adapter for rewarded videos, so be cognizant of states.
        return mInitialized.get();
    }

    @Override
    public void loadAd(MediationAdRequest adRequest, Bundle serverParameters, Bundle networkExtras)
    {
        synchronized ( INCENTIVIZED_ADS_LOCK )
        {
            mPlacement = AppLovinUtils.retrievePlacement( serverParameters );
            mZoneId = AppLovinUtils.retrieveZoneId( serverParameters );

            log( DEBUG, "Requesting rewarded video for zone: " + mZoneId + " and placement: " + mPlacement );

            // Check if incentivized ad for zone already exists
            if ( INCENTIVIZED_ADS.containsKey( mZoneId ) )
            {
                mIncentivizedInterstitial = INCENTIVIZED_ADS.get( mZoneId );
            }
            else
            {
                // If this is a default Zone, create the incentivized ad normally
                if ( DEFAULT_ZONE.equals( mZoneId ) )
                {
                    mIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create( mSdk );
                }
                // Otherwise, use the Zones API
                else
                {
                    mIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create( mZoneId, mSdk );
                }

                INCENTIVIZED_ADS.put( mZoneId, mIncentivizedInterstitial );
            }
        }

        if ( mIncentivizedInterstitial.isAdReadyToDisplay() )
        {
            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    mMediationRewardedVideoAdListener.onAdLoaded( ApplovinAdapter.this );
                }
            } );
        }
        else
        {
            // Save placement and zone id, in case the adapter is re-used with new values but the callbacks are for the old values
            final String placement = mPlacement;
            final String zoneId = mZoneId;

            mIncentivizedInterstitial.preload( new AppLovinAdLoadListener()
            {
                @Override
                public void adReceived(final AppLovinAd ad)
                {
                    log( DEBUG, "Rewarded video did load ad: " + ad.getAdIdNumber() + " for zone: " + zoneId + " and placement: " + placement );

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
        if ( mIncentivizedInterstitial.isAdReadyToDisplay() )
        {
            // Update mute state
            mSdk.getSettings().setMuted( AppLovinUtils.shouldMuteAudio( mNetworkExtras ) );

            log( DEBUG, "Showing rewarded video for zone: " + mZoneId + " placement: " + mPlacement );

            final AppLovinIncentivizedAdListener listener = new AppLovinIncentivizedAdListener( this, mMediationRewardedVideoAdListener );
            mIncentivizedInterstitial.show( mContext, mPlacement, listener, listener, listener, listener );
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

    @Override
    public void requestBannerAd(Context context, final MediationBannerListener mediationBannerListener, Bundle serverParameters, AdSize adSize, MediationAdRequest mediationAdRequest, Bundle networkExtras)
    {
        // Store parent objects
        mSdk = AppLovinUtils.retrieveSdk( serverParameters, context );

        mPlacement = AppLovinUtils.retrievePlacement( serverParameters );
        mZoneId = AppLovinUtils.retrieveZoneId( serverParameters );

        log( DEBUG, "Requesting banner of size " + adSize + " for zone: " + mZoneId + " and placement: " + mPlacement );

        // Convert requested size to AppLovin Ad Size
        final AppLovinAdSize appLovinAdSize = appLovinAdSizeFromAdMobAdSize( adSize );
        if ( appLovinAdSize != null )
        {
            mAdView = new AppLovinAdView( mSdk, appLovinAdSize, context );

            final AppLovinBannerAdListener listener = new AppLovinBannerAdListener( mZoneId, mPlacement, mAdView, this, mediationBannerListener );
            mAdView.setAdDisplayListener( listener );
            mAdView.setAdClickListener( listener );
            mAdView.setAdViewEventListener( listener );

            if ( !TextUtils.isEmpty( mZoneId ) )
            {
                mSdk.getAdService().loadNextAdForZoneId( mZoneId, listener );
            }
            else
            {
                mSdk.getAdService().loadNextAd( appLovinAdSize, listener );
            }
        }
        else
        {
            log( ERROR, "Failed to request banner with unsupported size" );

            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    mediationBannerListener.onAdFailedToLoad( ApplovinAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST );
                }
            } );
        }
    }

    @Override
    public View getBannerView()
    {
        return mAdView;
    }

    private AppLovinAdSize appLovinAdSizeFromAdMobAdSize(AdSize adSize)
    {
        final boolean isSmartBanner = ( adSize.getWidth() == AdSize.FULL_WIDTH ) && ( adSize.getHeight() == AdSize.AUTO_HEIGHT );

        if ( AdSize.BANNER.equals( adSize ) || AdSize.LARGE_BANNER.equals( adSize ) || isSmartBanner )
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
            mContext = context;
        }
    }

    //
    // Logging
    //

    static void log(int priority, final String message)
    {
        if ( LOGGING_ENABLED )
        {
            Log.println( priority, "AppLovinAdapter", message );
        }
    }
}
