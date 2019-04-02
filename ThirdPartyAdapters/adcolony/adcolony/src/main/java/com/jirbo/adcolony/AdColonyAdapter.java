package com.jirbo.adcolony;

import android.content.Context;
import android.os.Bundle;

import com.google.ads.mediation.adcolony.AdColonyMediationAdapter;
import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import java.util.ArrayList;

/**
 * A {@link com.google.android.gms.ads.mediation.MediationAdapter} used to mediate interstitial ads
 * and rewarded video ads from AdColony.
 */
public class AdColonyAdapter extends AdColonyMediationAdapter
        implements MediationInterstitialAdapter {

    // AdColony Ad instance
    private AdColonyInterstitial _adColonyInterstitial;

    // AdColony Ad Listeners
    private AdColonyAdListener _adColonyInterstitialListener;

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
}
