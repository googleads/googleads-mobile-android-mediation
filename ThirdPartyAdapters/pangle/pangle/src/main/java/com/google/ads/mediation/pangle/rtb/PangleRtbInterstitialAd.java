package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_PLACEMENT;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_SHOW_FAIL;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.google.ads.mediation.pangle.PangleConstant;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class PangleRtbInterstitialAd implements MediationInterstitialAd {
    private static final String TAG = "PangleRtbInterstitialAd";

    private final MediationInterstitialAdConfiguration mAdConfiguration;
    private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mAdLoadCallback;
    private MediationInterstitialAdCallback mInterstitialAdCallback;
    private TTFullScreenVideoAd mTTFullVideoAd;

    public PangleRtbInterstitialAd(MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
                                   MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
        mAdConfiguration = mediationInterstitialAdConfiguration;
        mAdLoadCallback = mediationAdLoadCallback;
    }

    public void render() {
        PangleMediationAdapter.setCoppa(mAdConfiguration);
        String placementId = mAdConfiguration.getServerParameters().getString(PangleConstant.PLACEMENT_ID);

        if (TextUtils.isEmpty(placementId)) {
            AdError error = PangleConstant.createAdapterError(ERROR_INVALID_PLACEMENT,
                    "Failed to request ad. PlacementID is null or empty.");
            Log.e(TAG, error.getMessage());
            mAdLoadCallback.onFailure(error);
            return;
        }

        String bidResponse = mAdConfiguration.getBidResponse();

        //(notice : make sure the Pangle sdk had been initialized) obtain Pangle ad manager
        TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
        TTAdNative mTTAdNative = mTTAdManager.createAdNative(mAdConfiguration.getContext().getApplicationContext());

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(placementId)
                .setImageAcceptedSize(1080, 1920)
                .withBid(bidResponse)
                .build();

        mTTAdNative.loadFullScreenVideoAd(adSlot, new TTAdNative.FullScreenVideoAdListener() {
            @Override
            public void onError(int errorCode, String errorMessage) {
                mAdLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, errorMessage));
            }

            @Override
            public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ttFullScreenVideoAd) {
                mInterstitialAdCallback = mAdLoadCallback.onSuccess(PangleRtbInterstitialAd.this);
                mTTFullVideoAd = ttFullScreenVideoAd;
            }

            @Override
            public void onFullScreenVideoCached() {

            }
        });
    }

    @Override
    public void showAd(@NonNull Context context) {
        try {
            if (mTTFullVideoAd != null) {
                mTTFullVideoAd.setFullScreenVideoAdInteractionListener(new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
                    @Override
                    public void onAdShow() {
                        if (mInterstitialAdCallback != null) {
                            mInterstitialAdCallback.onAdOpened();
                            mInterstitialAdCallback.reportAdImpression();
                        }
                    }

                    @Override
                    public void onAdVideoBarClick() {
                        if (mInterstitialAdCallback != null) {
                            mInterstitialAdCallback.reportAdClicked();
                        }
                    }

                    @Override
                    public void onAdClose() {
                        if (mInterstitialAdCallback != null) {
                            mInterstitialAdCallback.onAdClosed();
                        }
                    }

                    @Override
                    public void onVideoComplete() {

                    }

                    @Override
                    public void onSkippedVideo() {

                    }
                });
                if (context instanceof Activity) {
                    mTTFullVideoAd.showFullScreenVideoAd((Activity) context);
                } else {
                    mTTFullVideoAd.showFullScreenVideoAd(null);
                }
            } else {
                if (mInterstitialAdCallback != null) {
                    mInterstitialAdCallback.onAdFailedToShow(PangleConstant.createAdapterError(ERROR_SHOW_FAIL, "interstitial ad object is null"));
                } else {
                    mAdLoadCallback.onFailure(PangleConstant.createAdapterError(ERROR_SHOW_FAIL, "interstitial ad object is null"));
                }
            }
        } catch (Throwable error) {
            Log.w(TAG, error.getMessage());
            if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.onAdFailedToShow(PangleConstant.createSdkError(ERROR_SHOW_FAIL, "interstitialAd failed to show"));
            } else {
                mAdLoadCallback.onFailure(PangleConstant.createSdkError(ERROR_SHOW_FAIL, "interstitialAd failed to show"));
            }
        }
    }


}
