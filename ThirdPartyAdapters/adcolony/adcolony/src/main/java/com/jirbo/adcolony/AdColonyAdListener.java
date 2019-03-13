package com.jirbo.adcolony;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

/**
 * The {@link AdColonyAdListener} class is used to forward Interstitial ad
 * events from AdColony SDK to Google Mobile Ads SDK.
 */
class AdColonyAdListener extends AdColonyInterstitialListener {

    private MediationInterstitialListener _mediationInterstitialListener;
    private AdColonyAdapter _adapter;

    AdColonyAdListener(AdColonyAdapter adapter, MediationInterstitialListener listener) {
        _mediationInterstitialListener = listener;
        _adapter = adapter;
    }

    @Override
    public void onRequestFilled(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _adapter.setAd(ad);
            notifyAdLoaded();
        }
    }

    @Override
    public void onClicked(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _adapter.setAd(ad);
            _mediationInterstitialListener.onAdClicked(_adapter);
        }
    }

    @Override
    public void onClosed(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _adapter.setAd(ad);
            _mediationInterstitialListener.onAdClosed(_adapter);
        }
    }

    @Override
    public void onExpiring(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _adapter.setAd(ad);
            AdColony.requestInterstitial(ad.getZoneID(), this);
        }
    }

    @Override
    public void onIAPEvent(AdColonyInterstitial ad, String productId, int engagementType) {
        if (_adapter != null) {
            _adapter.setAd(ad);
        }
    }

    @Override
    public void onLeftApplication(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _adapter.setAd(ad);
            _mediationInterstitialListener.onAdLeftApplication(_adapter);
        }
    }

    @Override
    public void onOpened(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _adapter.setAd(ad);
            _mediationInterstitialListener.onAdOpened(_adapter);
        }
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        if (_adapter != null) {
            _adapter.setAd(null);
            _mediationInterstitialListener.onAdFailedToLoad(_adapter, AdRequest.ERROR_CODE_NO_FILL);
        }
    }

    void destroy() {
        _adapter = null;
        _mediationInterstitialListener = null;
    }

    void notifyAdLoaded() {
        _mediationInterstitialListener.onAdLoaded(_adapter);
    }
}
