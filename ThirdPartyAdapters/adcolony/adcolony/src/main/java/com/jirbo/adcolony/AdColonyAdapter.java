package com.jirbo.adcolony;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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
    private AdColonyInterstitial adColonyInterstitial;

    // AdColony Ad Listeners
    private AdColonyAdListener adColonyInterstitialListener;

    // AdColony banner Ad Listeners
    private AdColonyBannerAdListener adColonyBannerAdListener;

    //AdColonyAdView ad view for banner.
    private AdColonyAdView adColonyAdView;

    //region MediationAdapter methods.
    @Override
    public void onDestroy() {
        if (adColonyInterstitial != null) {
            adColonyInterstitial.cancel();
            adColonyInterstitial.destroy();
        }
        if (adColonyInterstitialListener != null) {
            adColonyInterstitialListener.destroy();
        }
        if (adColonyAdView != null) {
            adColonyAdView.destroy();
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

        ArrayList<String> zoneList =
                AdColonyManager.getInstance().parseZoneList(serverParams);
        String requestedZone =
                AdColonyManager.getInstance().getZoneFromRequest(zoneList, mediationExtras);
        if (TextUtils.isEmpty(requestedZone)) {
            Log.e(TAG, "Failed to request ad: zone ID is null or empty");
            mediationInterstitialListener.onAdFailedToLoad(this,
                    AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        adColonyInterstitialListener = new AdColonyAdListener(this, mediationInterstitialListener);

        // Initialize AdColony.
        boolean success = AdColonyManager.getInstance()
                .configureAdColony(context, serverParams, mediationAdRequest, mediationExtras);

        if (!success) {
            Log.w(TAG, "Failed to configure AdColony SDK");
            mediationInterstitialListener
                    .onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        AdColony.requestInterstitial(requestedZone, adColonyInterstitialListener);
    }

    @Override
    public void showInterstitial() {
        showAdColonyInterstitial();
    }
    //endregion

    //region Shared private methods.
    private void showAdColonyInterstitial() {
        if (adColonyInterstitial != null) {
            adColonyInterstitial.show();
        }
    }

    void setAd(AdColonyInterstitial interstitialAd) {
        adColonyInterstitial = interstitialAd;
    }
    //endregion

    //region MediationBannerAdapter methods.
    @Override
    public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener,
                                Bundle serverParams, AdSize adSize,
                                MediationAdRequest mediationAdRequest, Bundle mediationExtras) {

        if (adSize == null) {
            Log.e(TAG, "Fail to request banner ad: adSize is null");
            mediationBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        AdColonyAdSize adColonyAdSize =
                AdColonyAdapterUtils.adColonyAdSizeFromAdMobAdSize(context, adSize);

        if (adColonyAdSize == null) {
            Log.e(TAG, "Failed to request banner with unsupported size: " + adSize.toString());
            mediationBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        ArrayList<String> zoneList =
                AdColonyManager.getInstance().parseZoneList(serverParams);
        String requestedZone =
                AdColonyManager.getInstance().getZoneFromRequest(zoneList, mediationExtras);

        if (TextUtils.isEmpty(requestedZone)) {
            Log.e(TAG, "Failed to request ad: zone ID is null or empty");
            mediationBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        adColonyBannerAdListener = new AdColonyBannerAdListener(this, mediationBannerListener);

        // Initialize AdColony.
        boolean success = AdColonyManager.getInstance()
                .configureAdColony(context, serverParams, mediationAdRequest, mediationExtras);

        if (!success) {
            Log.w(TAG, "Failed to configure AdColony SDK");
            mediationBannerListener
                    .onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        AdColony.requestAdView(requestedZone, adColonyBannerAdListener, adColonyAdSize);
    }

    @Override
    public View getBannerView() {
        return adColonyAdView;
    }

    void setAdView(AdColonyAdView ad) {
        this.adColonyAdView = ad;
    }
    //endregion
}
