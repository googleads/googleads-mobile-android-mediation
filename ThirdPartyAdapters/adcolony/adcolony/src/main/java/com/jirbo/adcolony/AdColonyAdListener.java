package com.jirbo.adcolony;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyRewardListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

/**
 * The {@link AdColonyAdListener} class is used to forward Interstitial ad and Rewarded video ad
 * events from AdColony SDK to Google Mobile Ads SDK.
 */
class AdColonyAdListener extends AdColonyInterstitialListener implements AdColonyRewardListener {
    private MediationInterstitialListener _mediationInterstitialListener;
    private MediationRewardedVideoAdListener _mediationRewardedVideoAdListener;
    private AdColonyAdapter _adapter;
    private boolean _rewarded;

    AdColonyAdListener(AdColonyAdapter adapter, MediationInterstitialListener listener)
    {
        _mediationInterstitialListener = listener;
        _adapter = adapter;
    }

    AdColonyAdListener(AdColonyAdapter adapter, MediationRewardedVideoAdListener listener) {
        _mediationRewardedVideoAdListener = listener;
        _adapter = adapter;
        _rewarded = true;
    }

    @Override
    public void onRequestFilled(AdColonyInterstitial ad) {
        _adapter.setAd(ad);
        if (_rewarded) {
            _mediationRewardedVideoAdListener.onAdLoaded(_adapter);
        } else {
            _mediationInterstitialListener.onAdLoaded(_adapter);
        }
    }

    @Override
    public void onClicked(AdColonyInterstitial ad) {
        _adapter.setAd(ad);
        if (_rewarded) {
            _mediationRewardedVideoAdListener.onAdClicked(_adapter);
        } else {
            _mediationInterstitialListener.onAdClicked(_adapter);
        }
    }

    @Override
    public void onClosed(AdColonyInterstitial ad) {
        _adapter.setAd(ad);
        if (_rewarded) {
            _mediationRewardedVideoAdListener.onAdClosed(_adapter);
        } else {
            _mediationInterstitialListener.onAdClosed(_adapter);
        }
    }

    @Override
    public void onExpiring(AdColonyInterstitial ad) {
        _adapter.setAd(ad);
        AdColony.requestInterstitial(ad.getZoneID(), this);
    }

    @Override
    public void onIAPEvent(AdColonyInterstitial ad, String productId, int engagementType) {
        _adapter.setAd(ad);
    }

    @Override
    public void onLeftApplication(AdColonyInterstitial ad) {
        _adapter.setAd(ad);
        if (_rewarded) {
            _mediationRewardedVideoAdListener.onAdLeftApplication(_adapter);
        } else {
            _mediationInterstitialListener.onAdLeftApplication(_adapter);
        }
    }

    @Override
    public void onOpened(AdColonyInterstitial ad) {
        _adapter.setAd(ad);
        if (_rewarded) {
            _mediationRewardedVideoAdListener.onAdOpened(_adapter);
            _mediationRewardedVideoAdListener.onVideoStarted(_adapter);
        } else {
            _mediationInterstitialListener.onAdOpened(_adapter);
        }
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        _adapter.setAd(null);
        if (_rewarded) {
            AdColony.removeRewardListener();
            _mediationRewardedVideoAdListener.
                    onAdFailedToLoad(_adapter, AdRequest.ERROR_CODE_NO_FILL);
        } else {
            _mediationInterstitialListener.onAdFailedToLoad(_adapter, AdRequest.ERROR_CODE_NO_FILL);
        }
    }

    @Override
    public void onReward(com.adcolony.sdk.AdColonyReward reward) {
        AdColonyReward adReward =
                new AdColonyReward(reward.getRewardName(), reward.getRewardAmount());
        _mediationRewardedVideoAdListener.onRewarded(_adapter, adReward);
    }

    void destroy() {
        _adapter = null;
        _mediationInterstitialListener = null;
        _mediationRewardedVideoAdListener = null;
    }
}
