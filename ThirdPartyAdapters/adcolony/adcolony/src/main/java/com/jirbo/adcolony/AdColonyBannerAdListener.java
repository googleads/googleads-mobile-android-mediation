package com.jirbo.adcolony;

import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationBannerListener;

/**
 * The {@link AdColonyBannerAdListener} class is used to forward Banner ad
 * events from AdColony SDK to Google Mobile Ads SDK.
 */
class AdColonyBannerAdListener extends AdColonyAdViewListener {

    private MediationBannerListener _mediationBannerListener;
    private AdColonyAdapter _adapter;

    AdColonyBannerAdListener(AdColonyAdapter adapter, MediationBannerListener listener) {
        _mediationBannerListener = listener;
        _adapter = adapter;
    }

    @Override
    public void onClicked(AdColonyAdView ad) {
        if (_adapter != null) {
            _adapter.setAdView(ad);
            _mediationBannerListener.onAdClicked(_adapter);
        }
    }

    @Override
    public void onOpened(AdColonyAdView ad) {
        if (_adapter != null) {
            _adapter.setAdView(ad);
            _mediationBannerListener.onAdOpened(_adapter);
        }
    }

    @Override
    public void onClosed(AdColonyAdView ad) {
        if (_adapter != null) {
            _adapter.setAdView(ad);
            _mediationBannerListener.onAdClosed(_adapter);
        }
    }

    @Override
    public void onLeftApplication(AdColonyAdView ad) {
        if (_adapter != null) {
            _adapter.setAdView(ad);
            _mediationBannerListener.onAdLeftApplication(_adapter);
        }
    }

    @Override
    public void onRequestFilled(AdColonyAdView adColonyAdView) {
        if (_adapter != null) {
            _adapter.setAdView(adColonyAdView);
            notifyAdLoaded();
        }
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        if (_adapter != null) {
            _adapter.setAdView(null);
            _mediationBannerListener.onAdFailedToLoad(_adapter, AdRequest.ERROR_CODE_NO_FILL);
        }
    }

    void destroy() {
        _adapter = null;
        _mediationBannerListener = null;
    }

    void notifyAdLoaded() {
        _mediationBannerListener.onAdLoaded(_adapter);
    }
}
