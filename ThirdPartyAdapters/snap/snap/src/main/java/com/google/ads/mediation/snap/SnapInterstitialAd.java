package com.google.ads.mediation.snap;

import static com.google.ads.mediation.snap.SnapMediationAdapter.SLOT_ID_KEY;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.snap.adkit.external.AdKitAudienceAdsNetwork;
import com.snap.adkit.external.AdKitSlotType;
import com.snap.adkit.external.AudienceNetworkAdsApi;
import com.snap.adkit.external.LoadAdConfig;
import com.snap.adkit.external.LoadAdConfigBuilder;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdDismissed;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdImpressionHappened;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdKitSlot;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdVisible;

public class SnapInterstitialAd implements MediationInterstitialAd {
    private MediationInterstitialAdConfiguration adConfiguration;
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationAdLoadCallback;
    private MediationInterstitialAdCallback mInterstitialAdCallback;
    private String mSlotId;

    @Nullable
    private final AudienceNetworkAdsApi adsNetworkApi = AdKitAudienceAdsNetwork.getAdsNetwork();

    public SnapInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
                              @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.mMediationAdLoadCallback = callback;
    }

    public void loadAd() {
        if (adsNetworkApi == null) {
            mMediationAdLoadCallback.onFailure(new AdError(0, "Snap Audience Network failed to initialize.", SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            return;
        }
        Bundle serverParameters = adConfiguration.getServerParameters();
        mSlotId = serverParameters.getString(SLOT_ID_KEY);
        if (mSlotId == null || mSlotId.isEmpty()) {
            mMediationAdLoadCallback.onFailure(new AdError(0, "Failed to load interstitial Ad from Snap. Invalid Ad Slot ID.",
                    SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            return;
        }
        String bid = adConfiguration.getBidResponse();
        if (bid == null || bid.isEmpty()) {
            mMediationAdLoadCallback.onFailure(new AdError(0, "Failed to load interstitial Ad from Snap. Invalid bid response.",
                    SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            return;
        }
        adsNetworkApi.setupListener(new SnapAdEventListener() {
            @Override
            public void onEvent(SnapAdKitEvent snapAdKitEvent, String slotId) {
                handleEvent(snapAdKitEvent);
            }
        });
        LoadAdConfig loadAdConfig = new LoadAdConfigBuilder()
                .withPublisherSlotId(mSlotId).withBid(bid).build();
        adsNetworkApi.loadInterstitial(loadAdConfig);
    }

    @Override
    public void showAd(Context context) {
        adsNetworkApi.playAd(new SnapAdKitSlot(mSlotId, AdKitSlotType.INTERSTITIAL));
    }

    private void handleEvent(SnapAdKitEvent snapAdKitEvent) {
        if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
            if (mMediationAdLoadCallback != null) {
                mInterstitialAdCallback = mMediationAdLoadCallback.onSuccess(this);
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdLoadFailed) {
            if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(
                        new AdError(0,
                                "Failed to load interstitial ad from Snap." + ((SnapAdLoadFailed) snapAdKitEvent).getThrowable().getMessage(),
                                SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdVisible) {
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.onAdOpened();
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdClicked) {
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.reportAdClicked();
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdImpressionHappened) {
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.reportAdImpression();
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdDismissed) {
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.onAdClosed();
            }
            return;
        }
    }
}
