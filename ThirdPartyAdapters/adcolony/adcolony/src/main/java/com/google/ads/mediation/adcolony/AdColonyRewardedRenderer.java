package com.google.ads.mediation.adcolony;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.jirbo.adcolony.AdColonyManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;

public class AdColonyRewardedRenderer implements MediationRewardedAd {

    private MediationRewardedAdCallback mRewardedAdCallback;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
    private MediationRewardedAdConfiguration adConfiguration;
    private AdColonyInterstitial mAdColonyInterstitial;
    private boolean isRtb = false;

    public AdColonyRewardedRenderer(
            MediationRewardedAdConfiguration adConfiguration,
            MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.mAdLoadCallback = callback;
    }

    public void render() {

        if (!adConfiguration.getBidResponse().equals("")) {
            isRtb = true;
        }
        boolean showPrePopup = false;
        boolean showPostPopup = false;
        Bundle serverParameters = adConfiguration.getServerParameters();
        Bundle networkExtras = adConfiguration.getMediationExtras();

        if (networkExtras != null) {
            showPrePopup = networkExtras.getBoolean("show_pre_popup", false);
            showPostPopup = networkExtras.getBoolean("show_post_popup", false);
        }

        AdColonyAdOptions adOptions = new AdColonyAdOptions()
                .enableConfirmationDialog(showPrePopup)
                .enableResultsDialog(showPostPopup);
        ArrayList<String> listFromServerParams =
                AdColonyManager.getInstance().parseZoneList(serverParameters);
        String requestedZone = AdColonyManager
                .getInstance().getZoneFromRequest(listFromServerParams, networkExtras);

        if (isRtb) {
            AdColonyRewardedEventForwarder.getInstance().addListener(requestedZone,
                    AdColonyRewardedRenderer.this);
            AdColony.requestInterstitial(requestedZone, AdColonyRewardedEventForwarder.getInstance()
                    , adOptions);
        } else {
            if (AdColonyRewardedEventForwarder.getInstance().isListenerAvailable(requestedZone)) {
                String logMessage = "Failed to load ad from AdColony: " +
                        "Only a maximum of one ad can be loaded per Zone ID.";
                Log.e(TAG, logMessage);
                mAdLoadCallback.onFailure(logMessage);
                return;
            }

            // Configures the AdColony SDK, which also initializes the SDK if it has not been yet.
            boolean adColonyConfigured =
                    AdColonyManager.getInstance().configureAdColony(adConfiguration);

            // Check if we have a valid zone and request the ad.
            if (adColonyConfigured && !TextUtils.isEmpty(requestedZone)) {

                AdColonyRewardedEventForwarder.getInstance().addListener(requestedZone,
                        AdColonyRewardedRenderer.this);

                AdColony.requestInterstitial(requestedZone,
                        AdColonyRewardedEventForwarder.getInstance(), adOptions);
            } else {
                // Cannot request an ad without a valid zone.
                adColonyConfigured = false;
            }
            if (!adColonyConfigured) {
                String logMessage = "Failed to request ad from AdColony: Internal Error";
                Log.w(TAG, logMessage);
                mAdLoadCallback.onFailure(logMessage);
            }
        }
    }

    //region AdColony Rewarded Events
    void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
        mAdColonyInterstitial = adColonyInterstitial;

        if (mAdLoadCallback != null) {
            mRewardedAdCallback = mAdLoadCallback.onSuccess(AdColonyRewardedRenderer.this);
        }
    }

    void onRequestNotFilled(AdColonyZone zone) {
        if (mAdLoadCallback != null) {
            mAdLoadCallback.onFailure("Failed to load ad from AdColony.");
        }
    }

    void onExpiring(AdColonyInterstitial ad) {
        // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
        mAdColonyInterstitial = null;
        AdColony.requestInterstitial(ad.getZoneID(), AdColonyRewardedEventForwarder.getInstance());
    }

    void onClicked(AdColonyInterstitial ad) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.reportAdClicked();
        }
    }

    void onOpened(AdColonyInterstitial ad) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdOpened();
            mRewardedAdCallback.onVideoStart();
            mRewardedAdCallback.reportAdImpression();
        }
    }

    void onLeftApplication(AdColonyInterstitial ad) {
        // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
    }

    void onClosed(AdColonyInterstitial ad) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
        }
    }

    void onIAPEvent(AdColonyInterstitial ad, String product_id, int engagement_type) {
        // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
    }

    void onReward(com.adcolony.sdk.AdColonyReward adColonyReward) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onVideoComplete();

            if (adColonyReward.success()) {
                AdColonyReward reward = new AdColonyReward(adColonyReward.getRewardName(),
                        adColonyReward.getRewardAmount());
                mRewardedAdCallback.onUserEarnedReward(reward);
            }
        }
    }

    @Override
    public void showAd(Context context) {
        if (mAdColonyInterstitial != null) {
            mAdColonyInterstitial.show();
        } else {
            mRewardedAdCallback.onAdFailedToShow("No ad to show.");
        }
    }
}
