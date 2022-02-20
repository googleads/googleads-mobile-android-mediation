package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstant.ERROR_BID_RESPONSE_IS_INVALID;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_PLACEMENT;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_SDK_NOT_INIT;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.google.ads.mediation.pangle.PangleConstant;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class PangleRtbInterstitialAd implements MediationInterstitialAd {

    private static final String TAG = PangleRtbInterstitialAd.class.getSimpleName();
    private final MediationInterstitialAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> adLoadCallback;
    private MediationInterstitialAdCallback interstitialAdCallback;
    private TTFullScreenVideoAd ttFullVideoAd;

    public PangleRtbInterstitialAd(@NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
                                   @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
        adConfiguration = mediationInterstitialAdConfiguration;
        adLoadCallback = mediationAdLoadCallback;
    }

    public void render() {
        PangleMediationAdapter.setCoppa(adConfiguration);

        if (!TTAdSdk.isInitSuccess()) {
            AdError error = PangleConstant.createSdkError(ERROR_SDK_NOT_INIT,
                    "Pangle SDK not initialized, or initialization error.");
            Log.w(TAG, error.getMessage());
            adLoadCallback.onFailure(error);
            return;
        }

        String placementId = adConfiguration.getServerParameters().getString(PangleConstant.PLACEMENT_ID);
        if (TextUtils.isEmpty(placementId)) {
            AdError error = PangleConstant.createAdapterError(ERROR_INVALID_PLACEMENT,
                    "Failed to load ad from Pangle. Missing or invalid Placement ID.");
            Log.w(TAG, error.getMessage());
            adLoadCallback.onFailure(error);
            return;
        }

        String bidResponse = adConfiguration.getBidResponse();
        if (TextUtils.isEmpty(bidResponse)) {
            AdError error = PangleConstant.createAdapterError(ERROR_BID_RESPONSE_IS_INVALID,
                    "Failed to load ad from Pangle. Missing or invalid bidResponse");
            Log.w(TAG, error.getMessage());
            adLoadCallback.onFailure(error);
            return;
        }

        TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
        TTAdNative mTTAdNative = mTTAdManager.createAdNative(adConfiguration.getContext().getApplicationContext());

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(placementId)
                .withBid(bidResponse)
                .build();

        mTTAdNative.loadFullScreenVideoAd(adSlot, new TTAdNative.FullScreenVideoAdListener() {
            @Override
            public void onError(int errorCode, String errorMessage) {
                AdError error = PangleConstant.createSdkError(errorCode, errorMessage);
                Log.w(TAG, error.getMessage());
                adLoadCallback.onFailure(error);
            }

            @Override
            public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ttFullScreenVideoAd) {
                interstitialAdCallback = adLoadCallback.onSuccess(PangleRtbInterstitialAd.this);
                ttFullVideoAd = ttFullScreenVideoAd;
            }

            @Override
            public void onFullScreenVideoCached() {

            }
        });
    }

    @Override
    public void showAd(@NonNull Context context) {
        ttFullVideoAd.setFullScreenVideoAdInteractionListener(new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
            @Override
            public void onAdShow() {
                if (interstitialAdCallback != null) {
                    interstitialAdCallback.onAdOpened();
                    interstitialAdCallback.reportAdImpression();
                }
            }

            @Override
            public void onAdVideoBarClick() {
                if (interstitialAdCallback != null) {
                    interstitialAdCallback.reportAdClicked();
                }
            }

            @Override
            public void onAdClose() {
                if (interstitialAdCallback != null) {
                    interstitialAdCallback.onAdClosed();
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
            ttFullVideoAd.showFullScreenVideoAd((Activity) context);
            return;
        }
        ttFullVideoAd.showFullScreenVideoAd(null);
    }
}
