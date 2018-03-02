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

    enum RequestState {
        REQUESTED,
        FILLED,
        NOT_FILLED,
        CLOSED,
        EXPIRED,
        NONE
    }

    private MediationInterstitialListener _mediationInterstitialListener;
    private MediationRewardedVideoAdListener _mediationRewardedVideoAdListener;
    private AdColonyAdapter _adapter;
    private boolean _rewarded;
    private RequestState _state = RequestState.NONE;

    AdColonyAdListener(AdColonyAdapter adapter, MediationInterstitialListener listener) {
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
        if (_adapter != null) {
            _state = RequestState.FILLED;
            _adapter.setAd(ad);
            notifyAdLoaded();
        }
    }

    @Override
    public void onClicked(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _adapter.setAd(ad);
            if (_rewarded) {
                _mediationRewardedVideoAdListener.onAdClicked(_adapter);
            } else {
                _mediationInterstitialListener.onAdClicked(_adapter);
            }
        }
    }

    @Override
    public void onClosed(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _state = RequestState.CLOSED;
            _adapter.setAd(ad);
            if (_rewarded) {
                _mediationRewardedVideoAdListener.onAdClosed(_adapter);
            } else {
                _mediationInterstitialListener.onAdClosed(_adapter);
            }
        }
    }

    @Override
    public void onExpiring(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _state = RequestState.EXPIRED;
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
            if (_rewarded) {
                _mediationRewardedVideoAdListener.onAdLeftApplication(_adapter);
            } else {
                _mediationInterstitialListener.onAdLeftApplication(_adapter);
            }
        }
    }

    @Override
    public void onOpened(AdColonyInterstitial ad) {
        if (_adapter != null) {
            _adapter.setAd(ad);
            if (_rewarded) {
                _mediationRewardedVideoAdListener.onAdOpened(_adapter);
                _mediationRewardedVideoAdListener.onVideoStarted(_adapter);
            } else {
                _mediationInterstitialListener.onAdOpened(_adapter);
            }
        }
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        if (_adapter != null) {
            _state = RequestState.NOT_FILLED;
            _adapter.setAd(null);
            if (_rewarded) {
                AdColony.removeRewardListener();
                _mediationRewardedVideoAdListener
                        .onAdFailedToLoad(_adapter, AdRequest.ERROR_CODE_NO_FILL);
            } else {
                _mediationInterstitialListener
                        .onAdFailedToLoad(_adapter, AdRequest.ERROR_CODE_NO_FILL);
            }
        }
    }

    @Override
    public void onReward(com.adcolony.sdk.AdColonyReward reward) {
        if (_adapter != null && reward.success()) {
            AdColonyReward adReward =
                    new AdColonyReward(reward.getRewardName(), reward.getRewardAmount());
            _mediationRewardedVideoAdListener.onRewarded(_adapter, adReward);
        }
    }

    void destroy() {
        _adapter = null;
        _mediationInterstitialListener = null;
        _mediationRewardedVideoAdListener = null;
    }

    void onRequest() {
        _state = RequestState.REQUESTED;
    }

    RequestState getState() {
        return _state;
    }

    void notifyAdLoaded() {
        if (_rewarded) {
            _mediationRewardedVideoAdListener.onAdLoaded(_adapter);
        } else {
            _mediationInterstitialListener.onAdLoaded(_adapter);
        }
    }
}
