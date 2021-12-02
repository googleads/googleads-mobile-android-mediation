package com.google.ads.mediation.snap;

import static com.google.ads.mediation.snap.SnapMediationAdapter.SLOT_ID_KEY;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.snap.adkit.external.BannerView;
import com.snap.adkit.external.LoadAdConfig;
import com.snap.adkit.external.LoadAdConfigBuilder;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdSize;
import com.snap.adkit.external.SnapBannerAdImpressionRecorded;

public class SnapBannerAd implements MediationBannerAd {
    private BannerView mBannerView;

    private MediationBannerAdConfiguration adConfiguration;
    private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;
    private MediationBannerAdCallback mBannerAdCallback;
    private String mSlotId;

    public SnapBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration,
                        @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    public void loadAd() {
        mBannerView = new BannerView(adConfiguration.getContext());
        mBannerView.setAdSize(SnapAdSize.BANNER);
        mBannerView.setupListener(new SnapAdEventListener() {
            @Override
            public void onEvent(SnapAdKitEvent snapAdKitEvent, @Nullable @org.jetbrains.annotations.Nullable String s) {
                handleEvent(snapAdKitEvent);
            }
        });
        Bundle serverParameters = adConfiguration.getServerParameters();
        mSlotId = serverParameters.getString(SLOT_ID_KEY);
        if (mSlotId == null || mSlotId.isEmpty()) {
            callback.onFailure(new AdError(0, "Failed to load banner ad from Snap due to invalid slot id",
                    SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            return;
        }
        String bid = adConfiguration.getBidResponse();
        if (bid == null || bid.isEmpty()) {
            callback.onFailure(new AdError(0, "Failed to load banner ad from Snap due to invalid bid respnse",
                    SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            return;
        }
        LoadAdConfig loadAdConfig = new LoadAdConfigBuilder()
                .withPublisherSlotId(mSlotId).withBid(bid).build();
        mBannerView.loadAd(loadAdConfig);
    }

    @NonNull
    @Override
    public View getView() {
        return mBannerView.view();
    }

    private void handleEvent(SnapAdKitEvent snapAdKitEvent) {
        if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
            if (callback != null) {
                mBannerAdCallback = callback.onSuccess(this);
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdLoadFailed) {
            if (callback!= null) {
                callback.onFailure(
                        new AdError(0,
                                "Failed to load banner ad from Snap." + ((SnapAdLoadFailed) snapAdKitEvent).getThrowable().getMessage(),
                                SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN));
            }
            return;
        }
        if (snapAdKitEvent instanceof SnapAdClicked) {
            if (mBannerAdCallback != null) {
                mBannerAdCallback.onAdOpened();
                mBannerAdCallback.reportAdClicked();
                mBannerAdCallback.onAdLeftApplication();
            }
            return;
        }

        if (snapAdKitEvent instanceof SnapBannerAdImpressionRecorded) {
            if (mBannerAdCallback != null) {
                mBannerAdCallback.reportAdImpression();
            }
        }
    }
}
