package com.applovin.mediation;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.google.android.gms.ads.mediation.MediationBannerListener;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * Created by thomasso on 1/26/18.
 */

class AppLovinBannerAdListener
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdViewEventListener
{
    private final ApplovinAdapter         mAdapter;
    private final MediationBannerListener mMediationBannerListener;
    private final AppLovinAdView          mAdView;
    private final String                  mZoneId;
    private final String                  mPlacement;

    AppLovinBannerAdListener(String zoneId, String placement, AppLovinAdView adView, ApplovinAdapter adapter, MediationBannerListener mediationBannerListener)
    {
        mAdapter = adapter;
        mMediationBannerListener = mediationBannerListener;
        mAdView = adView;
        mZoneId = zoneId;
        mPlacement = placement;
    }

    //
    // Ad Load Listener
    //

    @Override
    public void adReceived(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "Banner did load ad: " + ad.getAdIdNumber() + " for zone: " + mZoneId + " and placement: " + mPlacement );

        mAdView.renderAd( ad, mPlacement );
        mMediationBannerListener.onAdLoaded( mAdapter );
    }

    @Override
    public void failedToReceiveAd(int code)
    {
        ApplovinAdapter.log( ERROR, "Failed to load banner ad with error: " + code );
        mMediationBannerListener.onAdFailedToLoad( mAdapter, AppLovinUtils.toAdMobErrorCode( code ) );
    }

    //
    // Ad Display Listener
    //
    @Override
    public void adDisplayed(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "Banner displayed" );
    }

    @Override
    public void adHidden(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "Banner dismissed" );
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "Banner clicked" );
        mMediationBannerListener.onAdClicked( mAdapter );
    }

    //
    // Ad View Event Listener
    //

    @Override
    public void adOpenedFullscreen(AppLovinAd ad, AppLovinAdView adView)
    {
        ApplovinAdapter.log( DEBUG, "Banner opened fullscreen" );
        mMediationBannerListener.onAdOpened( mAdapter );
    }

    @Override
    public void adClosedFullscreen(AppLovinAd ad, AppLovinAdView adView)
    {
        ApplovinAdapter.log( DEBUG, "Banner closed fullscreen" );
        mMediationBannerListener.onAdClosed( mAdapter );
    }

    @Override
    public void adLeftApplication(AppLovinAd ad, AppLovinAdView adView)
    {
        ApplovinAdapter.log( DEBUG, "Banner left application" );
        mMediationBannerListener.onAdLeftApplication( mAdapter );
    }

    @Override
    public void adFailedToDisplay(AppLovinAd ad, AppLovinAdView adView, AppLovinAdViewDisplayErrorCode code)
    {
        ApplovinAdapter.log( DEBUG, "Banner failed to display: " + code );
    }
}
