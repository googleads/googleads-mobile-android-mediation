package com.jirbo.adcolony;

import android.content.Context;
import android.os.Bundle;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyRewardListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import java.util.ArrayList;

/**
 * A {@link com.google.android.gms.ads.mediation.MediationAdapter} used to mediate interstitial ads
 * and rewarded video ads from AdColony.
 */
public class AdColonyAdapter implements MediationInterstitialAdapter,
        MediationRewardedVideoAdAdapter {
    // Google-AdMob listeners
    private MediationRewardedVideoAdListener _mediationRewardedVideoAdListener;

    // AdColony Ad instance
    private AdColonyInterstitial _adcAd;

    // AdColony Ad Listeners
    private AdColonyAdListener _adColonyInterstitialListener;
    private AdColonyAdListener _adColonyRewardedInterstitialListener;

    /**************
     * MediationAdapter Methods
     *************/

    @Override
    public void onDestroy() {
        AdColonyManager.getInstance().onDestroy();
        if (_adcAd != null) {
            _adcAd.cancel();
            _adcAd.destroy();
        }
        if (_adColonyInterstitialListener != null) {
            _adColonyInterstitialListener.destroy();
        }
        if (_adColonyRewardedInterstitialListener != null) {
            _adColonyRewardedInterstitialListener.destroy();
            AdColony.removeRewardListener();
        }
    }

    @Override
    public void onPause() {
        // AdColony SDK will handle this here
    }

    @Override
    public void onResume() {
        // AdColony SDK will handle this here
    }

    /***************
     * MediationInterstitialAdapter methods
     ***************/

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParams,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        _adColonyInterstitialListener =
                new AdColonyAdListener(this, mediationInterstitialListener);
        // initialize AdColony
        boolean success = AdColonyManager.getInstance()
                .configureAdColony(context, serverParams, mediationAdRequest, mediationExtras);

        // if we were unable to configure, notify the listener
        if (success) {
            // configuration is successful
            // retrieve zones and request interstitial ad
            ArrayList<String> newZoneList =
                    AdColonyManager.getInstance().parseZoneList(serverParams);
            String requestedZone =
                    AdColonyManager.getInstance().getZoneFromRequest(newZoneList, mediationExtras);

            if (requestedZone != null) {
                // we have a valid zoneId -- request the ad
                AdColony.requestInterstitial(requestedZone, _adColonyInterstitialListener);
            } else {
                // a zone ID couldn't be retrieved, so notify that this ad couldn't be loaded.
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

    /***************
     * MediationRewardedVideoAdapter methods
     ***************/
    @Override
    public void initialize(Context context, MediationAdRequest mediationAdRequest, String userId,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParams, Bundle networkExtras) {
        this._mediationRewardedVideoAdListener = mediationRewardedVideoAdListener;
        _adColonyRewardedInterstitialListener =
                new AdColonyAdListener(this, mediationRewardedVideoAdListener);
        boolean success = AdColonyManager.getInstance()
                .configureAdColony(context, serverParams, mediationAdRequest, networkExtras);
        if (success) {
            AdColonyManager.getInstance().rewardedAdsConfigured = true;
            this._mediationRewardedVideoAdListener.onInitializationSucceeded(this);
        } else {
            this._mediationRewardedVideoAdListener
                    .onInitializationFailed(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParams,
                       Bundle networkExtras) {
        boolean showPrePopup = false;
        boolean showPostPopup = false;
        boolean success = AdColonyManager.getInstance()
                .configureAdColony(null, serverParams, mediationAdRequest, networkExtras);

        // retrieve the appropriate zone for this ad-request
        ArrayList<String> listFromServerParams =
                AdColonyManager.getInstance().parseZoneList(serverParams);
        String requestedZone = AdColonyManager
                .getInstance().getZoneFromRequest(listFromServerParams, networkExtras);

        if (networkExtras != null) {
            showPrePopup = networkExtras.getBoolean("show_pre_popup", false);
            showPostPopup = networkExtras.getBoolean("show_post_popup", false);
        }
        // update the reward listener if it had not been set before
        AdColonyRewardListener currentRewardListener = AdColony.getRewardListener();
        if (currentRewardListener == null) {
            // one hasn't been added yet.
            AdColony.setRewardListener(_adColonyRewardedInterstitialListener);
        }

        if (requestedZone != null) {
            // we have a valid zone, so request the ad
            AdColonyAdOptions adOptions = new AdColonyAdOptions();
            adOptions.enableConfirmationDialog(showPrePopup);
            adOptions.enableResultsDialog(showPostPopup);
            AdColony.requestInterstitial(requestedZone,
                    _adColonyRewardedInterstitialListener,
                    adOptions);
        } else {
            // cannot request an ad without a valid zone.
            success = false;
        }

        if (!success) {
            _mediationRewardedVideoAdListener
                    .onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    @Override
    public void showVideo() {
        showAdColonyInterstitial();
    }

    @Override
    public boolean isInitialized() {
        return AdColonyManager.getInstance().rewardedAdsConfigured;
    }

    /***************
     * shared private methods
     ***************/

    private void showAdColonyInterstitial() {
        if (_adcAd != null) {
            _adcAd.show();
        }
    }

    void setAd(AdColonyInterstitial ad) {
        _adcAd = ad;
    }
}
