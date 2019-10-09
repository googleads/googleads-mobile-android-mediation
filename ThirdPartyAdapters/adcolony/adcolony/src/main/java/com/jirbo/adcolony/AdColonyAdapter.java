package com.jirbo.adcolony;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.google.ads.mediation.adcolony.AdColonyAdapterUtils;
import com.google.ads.mediation.adcolony.AdColonyMediationAdapter;
import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import java.util.ArrayList;

/**
 * A {@link com.google.android.gms.ads.mediation.MediationAdapter} used to mediate interstitial ads
 * and rewarded video ads from AdColony.
 */
public class AdColonyAdapter extends AdColonyMediationAdapter
        implements MediationInterstitialAdapter, MediationBannerAdapter {

    // AdColony Ad instance
    private AdColonyInterstitial _adColonyInterstitial;

    // AdColony Ad Listeners
    private AdColonyAdListener _adColonyInterstitialListener;

    // AdColony banner Ad Listeners
    private AdColonyBannerAdListener _adColonyBannerAdListener;

    //AdColonyAdView ad view for banner.
    private AdColonyAdView _adColonyAdView;

    //region MediationAdapter methods.
    @Override
    public void onDestroy() {
        if (_adColonyInterstitial != null) {
            _adColonyInterstitial.cancel();
            _adColonyInterstitial.destroy();
        }
        if (_adColonyInterstitialListener != null) {
            _adColonyInterstitialListener.destroy();
        }
        if(_adColonyAdView != null) {
            _adColonyAdView.destroy();
        }
        if (_adColonyBannerAdListener != null) {
            _adColonyBannerAdListener.destroy();
        }
    }

    @Override
    public void onPause() {
        // AdColony SDK will handle this here.
    }

    @Override
    public void onResume() {
        // AdColony SDK will handle this here.
    }
    //endregion

    //region MediationInterstitialAdapter methods.
    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParams,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        _adColonyInterstitialListener =
                new AdColonyAdListener(this, mediationInterstitialListener);
        // Initialize AdColony.
        boolean success = AdColonyManager.getInstance()
                .configureAdColony(context, serverParams, mediationAdRequest, mediationExtras);

        // If we were unable to configure, notify the listener.
        if (success) {
            // Configuration is successful; retrieve zones and request interstitial ad.
            ArrayList<String> newZoneList =
                    AdColonyManager.getInstance().parseZoneList(serverParams);
            String requestedZone =
                    AdColonyManager.getInstance().getZoneFromRequest(newZoneList, mediationExtras);

            if (requestedZone != null) {
                // We have a valid zoneId; request the ad.
                AdColony.requestInterstitial(requestedZone, _adColonyInterstitialListener);
            } else {
                // Zone ID couldn't be retrieved, so notify that this ad couldn't be loaded.
                success = false;
            }
        }

        if (!success) {
            mediationInterstitialListener
                    .onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    @Override
    public void showInterstitial() {
        showAdColonyInterstitial();
    }
    //endregion

    //region Shared private methods.
    private void showAdColonyInterstitial() {
        if (_adColonyInterstitial != null) {
            _adColonyInterstitial.show();
        }
    }

    void setAd(AdColonyInterstitial interstitialAd) {
        _adColonyInterstitial = interstitialAd;
    }
    //endregion

    //region MediationBannerAdapter methods.
    @Override
    public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener,
                                Bundle serverParams, AdSize adSize, MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        _adColonyBannerAdListener =
                new AdColonyBannerAdListener(this, mediationBannerListener);
        // Initialize AdColony.
        boolean success = AdColonyManager.getInstance()
                .configureAdColony(context, serverParams, mediationAdRequest, mediationExtras);

        // If we were unable to configure, notify the listener.
        if (success) {
            //convert the admob size into adcolony size.
            AdColonyAdSize adColonyAdSize = AdColonyAdapterUtils.adColonyAdSizeFromAdMobAdSize(context,adSize);
            if(adColonyAdSize !=null) {
                // Configuration is successful; retrieve zones and request interstitial ad.
                ArrayList<String> newZoneList =
                        AdColonyManager.getInstance().parseZoneList(serverParams);
                String requestedZone =
                        AdColonyManager.getInstance().getZoneFromRequest(newZoneList, mediationExtras);

                if (requestedZone != null) {
                    // We have a valid zoneId; request the ad.
                    AdColony.requestAdView(requestedZone, _adColonyBannerAdListener, adColonyAdSize);
                } else {
                    // Zone ID couldn't be retrieved, so notify that this ad couldn't be loaded.
                    success = false;
                }
            } else {
                success = false;
                Log.e(TAG,"Failed to request banner with unsupported size");
            }
        }
        if (!success) {
            mediationBannerListener
                    .onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    @Override
    public View getBannerView() {
        return _adColonyAdView;
    }

    void setAdView(AdColonyAdView ad) {
        this._adColonyAdView = ad;
    }
    //endregion
}
