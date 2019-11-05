package com.google.ads.mediation.verizon;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;
import com.verizon.ads.utils.ThreadUtils;

import java.util.Map;


public class AdapterIncentivizedEventListener
    implements InterstitialAd.InterstitialAdListener, InterstitialAdFactory.InterstitialAdFactoryListener, MediationRewardedAd {

    private static final String TAG = AdapterIncentivizedEventListener.class.getSimpleName();

    private static final String VIDEO_COMPLETE_EVENT_ID = "onVideoComplete";
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback;
    private InterstitialAd interstitialAd;
    private boolean completionEventCalled = false;
    private MediationRewardedAdCallback mediationRewardedAdCallback;


    AdapterIncentivizedEventListener(MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {

        this.mediationAdLoadCallback = mediationAdLoadCallback;
    }


    @Override
    public void onLoaded(final InterstitialAdFactory interstitialAdFactory, final InterstitialAd interstitialAd) {

        this.interstitialAd = interstitialAd;

        // reset the completion event with each new interstitial ad load
        completionEventCalled = false;

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mediationAdLoadCallback != null) {
                    mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(AdapterIncentivizedEventListener.this);
                }
            }
        });

        Log.i(TAG, "Verizon Ads SDK incentivized video interstitial loaded.");
    }


    @Override
    public void onCacheLoaded(final InterstitialAdFactory interstitialAdFactory, final int numRequested,
        final int numReceived) {
        // no op.  caching not supported in adapter
    }


    @Override
    public void onCacheUpdated(final InterstitialAdFactory interstitialAdFactory, final int cacheSize) {
        // no op.  caching not supported in adapter
    }


    @Override
    public void onError(final InterstitialAdFactory interstitialAdFactory, final ErrorInfo errorInfo) {

        switch (errorInfo.getErrorCode()) {
            case VASAds.ERROR_AD_REQUEST_FAILED:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mediationAdLoadCallback != null) {
                            mediationAdLoadCallback.onFailure(
                                "Verizon Ads SDK incentivized video interstitial ad request failed");
                        }
                    }
                });
                break;
            case VASAds.ERROR_AD_REQUEST_TIMED_OUT:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mediationAdLoadCallback != null) {
                            mediationAdLoadCallback.onFailure(
                                "Verizon Ads SDK incentivized video interstitial ad request timed out");
                        }
                    }
                });
                break;

            default:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mediationAdLoadCallback != null) {
                            mediationAdLoadCallback.onFailure(
                                "Verizon Ads SDK incentivized video interstitial error");
                        }
                    }
                });
                break;
        }
        Log.w(TAG, "Verizon Ads SDK incentivized video interstitial request failed (" + errorInfo.getErrorCode() + "): " +
            errorInfo.getDescription());
    }


    @Override
    public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {

        // This error callback is used if the interstitial ad is loaded successfully, but an error occurs while trying to display
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mediationRewardedAdCallback != null) {
                    mediationRewardedAdCallback.onAdFailedToShow(errorInfo.getDescription());
                }
            }
        });
        Log.e(TAG, "Verizon Ads SDK incentivized video interstitial error: " + errorInfo);
    }


    @Override
    public void onShown(final InterstitialAd interstitialAd) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mediationRewardedAdCallback != null) {
                    mediationRewardedAdCallback.onAdOpened();
                    mediationRewardedAdCallback.onVideoStart();
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK incentivized video interstitial shown.");
    }


    @Override
    public void onClosed(final InterstitialAd interstitialAd) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mediationRewardedAdCallback != null) {
                    mediationRewardedAdCallback.onAdClosed();
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK ad closed");
    }


    @Override
    public void onClicked(final InterstitialAd interstitialAd) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mediationRewardedAdCallback != null) {
                    mediationRewardedAdCallback.reportAdClicked();
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK incentivized video interstitial clicked.");
    }


    @Override
    public void onAdLeftApplication(final InterstitialAd interstitialAd) {

        Log.i(TAG, "Verizon Ads SDK incentivized video interstitial left application.");
    }


    @Override
    public void onEvent(final InterstitialAd interstitialAd, final String source, final String eventId,
        final Map<String, Object> arguments) {

        if (VIDEO_COMPLETE_EVENT_ID.equals(eventId) && !completionEventCalled) {
            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (mediationRewardedAdCallback != null) {
                        mediationRewardedAdCallback.onVideoComplete();
                        mediationRewardedAdCallback.onUserEarnedReward(new RewardItem() {
                            @Override
                            public String getType() {

                                return "";
                            }


                            @Override
                            public int getAmount() {

                                return 1;
                            }
                        });

                        completionEventCalled = true;
                    }
                }
            });
        }
    }


    @Override
    public void showAd(Context context) {

        if ((interstitialAd == null) || (context == null)) {
            if (mediationAdLoadCallback != null) {
                mediationAdLoadCallback.onFailure("Verizon Ads SDK incentivized video interstitial failed to load and cannot be shown");
            }
            return;
        }

        interstitialAd.show(context);
    }


    void destroy() {

        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
    }
}
