package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_PLACEMENT;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_SHOW_AD_NOT_LOADED;

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
        String placementId = adConfiguration.getServerParameters().getString(PangleConstant.PLACEMENT_ID);

        if (TextUtils.isEmpty(placementId)) {
            AdError error = PangleConstant.createAdapterError(ERROR_INVALID_PLACEMENT,
                    "Failed to request ad. PlacementID is null or empty.");
            Log.e(TAG, error.getMessage());
            adLoadCallback.onFailure(error);
            return;
        }

        String bidResponse = adConfiguration.getBidResponse();

        //(notice : make sure the Pangle sdk had been initialized) obtain Pangle ad manager
        TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
        TTAdNative mTTAdNative = mTTAdManager.createAdNative(adConfiguration.getContext().getApplicationContext());

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(placementId)
                .withBid(bidResponse)
                .build();

        mTTAdNative.loadFullScreenVideoAd(adSlot, new TTAdNative.FullScreenVideoAdListener() {
            @Override
            public void onError(int errorCode, String errorMessage) {
                adLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, errorMessage));
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
        if (ttFullVideoAd == null){
            interstitialAdCallback.onAdFailedToShow(PangleConstant.createAdapterError(ERROR_SHOW_AD_NOT_LOADED, "interstitial ad object is null"));
            return;
        }
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
        } else {
            ttFullVideoAd.showFullScreenVideoAd(null);
        }
    }


}
