package com.google.ads.mediation.snap;

import android.content.Context;
import android.os.Bundle;

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
    private static final String SLOT_ID_KEY = "adSlotId";

    private MediationRewardedAdConfiguration adConfiguration;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mMediationAdLoadCallback;
    private MediationRewardedAdCallback mRewardAdCallback;
    private String mSlotId;

    @Nullable
    private final AudienceNetworkAdsApi adsNetworkApi = AdKitAudienceAdsNetwork.getAdsNetwork();

    public SnapRewardedAd(MediationRewardedAdConfiguration adConfiguration,
                          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.mMediationAdLoadCallback = callback;
    }

    public void loadAd() {
        if (adsNetworkApi == null) {
            mMediationAdLoadCallback.onFailure(new AdError(0, "snap ad network not properly initialized", SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
        }
        Bundle serverParameters = adConfiguration.getServerParameters();
        mSlotId = serverParameters.getString(SLOT_ID_KEY);
        adsNetworkApi.setupListener(new SnapAdEventListener() {
            @Override
            public void onEvent(SnapAdKitEvent snapAdKitEvent, String slotId) {
                handleEvent(snapAdKitEvent);
            }
        });

        String bid = adConfiguration.getBidResponse();
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
        } else if (snapAdKitEvent instanceof SnapAdLoadFailed) {
            if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(
                        new AdError(0, "ad load fail", SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            }
        } else if (snapAdKitEvent instanceof SnapAdVisible) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.onAdOpened();
            }
        } else if (snapAdKitEvent instanceof SnapAdClicked) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.reportAdClicked();
            }
        } else if (snapAdKitEvent instanceof SnapAdImpressionHappened) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.reportAdImpression();
            }
        } else if (snapAdKitEvent instanceof SnapAdDismissed) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.onAdClosed();
            }
        }
    }
}
