package com.google.ads.mediation.snap;

import static com.google.ads.mediation.snap.SnapMediationAdapter.SLOT_ID_KEY;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
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

public class SnapRewardedAd implements MediationRewardedAd {
    private MediationRewardedAdConfiguration adConfiguration;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mMediationAdLoadCallback;
    private MediationRewardedAdCallback mRewardAdCallback;
    private String mSlotId;

    @Nullable
    private final AudienceNetworkAdsApi adsNetworkApi = AdKitAudienceAdsNetwork.getAdsNetwork();

    public SnapRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
                          @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
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
            mMediationAdLoadCallback.onFailure(new AdError(0, "Failed to load rewarded Ad from Snap. Invalid Ad Slot ID",
                    SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            return;
        }
        String bid = adConfiguration.getBidResponse();
        if (bid == null || bid.isEmpty()) {
            mMediationAdLoadCallback.onFailure(new AdError(0, "Failed to load rewarded ad from Snap. Invalid bid response",
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
        adsNetworkApi.loadRewarded(loadAdConfig);
    }

    @Override
    public void showAd(Context context) {
        adsNetworkApi.playAd(new SnapAdKitSlot(mSlotId, AdKitSlotType.REWARDED));
    }

    private void handleEvent(SnapAdKitEvent snapAdKitEvent) {
        if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
            if (mMediationAdLoadCallback != null) {
                mRewardAdCallback = mMediationAdLoadCallback.onSuccess(this);
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdLoadFailed) {
            if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(
                        new AdError(0,
                                "Failed to load rewarded ad from Snap." + ((SnapAdLoadFailed) snapAdKitEvent).getThrowable().getMessage(),
                                SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdVisible) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.onAdOpened();
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdClicked) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.reportAdClicked();
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdImpressionHappened) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.reportAdImpression();
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdDismissed) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.onAdClosed();
            }
            return;
        }
    }
}
