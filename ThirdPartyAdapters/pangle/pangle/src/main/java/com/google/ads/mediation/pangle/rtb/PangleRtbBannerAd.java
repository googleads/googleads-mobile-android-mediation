package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstant.ERROR_BANNER_AD_SIZE_IS_INVALID;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_BID_RESPONSE_IS_INVALID;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_PLACEMENT;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_SDK_NOT_INIT;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.google.ads.mediation.pangle.PangleConstant;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

import java.util.List;

public class PangleRtbBannerAd implements MediationBannerAd, TTNativeExpressAd.ExpressAdInteractionListener {

    private static final String TAG = PangleRtbBannerAd.class.getSimpleName();
    private final MediationBannerAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback;
    private MediationBannerAdCallback bannerAdCallback;
    private FrameLayout wrappedAdView;

    public PangleRtbBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
                             @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
        this.adConfiguration = mediationBannerAdConfiguration;
        this.adLoadCallback = mediationAdLoadCallback;
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

        AdSize adSize = adConfiguration.getAdSize();
        if (adSize == null || adSize.getWidth() < 0 || adSize.getHeight() < 0) {
            AdError error = PangleConstant.createAdapterError(ERROR_BANNER_AD_SIZE_IS_INVALID,
                    "Failed to request ad from Pangle. Invalid banner size.");
            Log.w(TAG, error.getMessage());
            adLoadCallback.onFailure(error);
            return;
        }

        Context context = adConfiguration.getContext();
        wrappedAdView = new FrameLayout(context);

        TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
        TTAdNative mTTAdNative = mTTAdManager.createAdNative(context.getApplicationContext());

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(placementId)
                .setAdCount(1)
                .setExpressViewAcceptedSize(adSize.getWidth(), adSize.getHeight())
                .withBid(bidResponse)
                .build();
        mTTAdNative.loadBannerExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int errorCode, String errorMessage) {
                AdError error = PangleConstant.createSdkError(errorCode, errorMessage);
                Log.w(TAG, error.getMessage());
                adLoadCallback.onFailure(error);
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                TTNativeExpressAd mBannerExpressAd = ads.get(0);
                mBannerExpressAd.setExpressInteractionListener(PangleRtbBannerAd.this);
                mBannerExpressAd.render();
            }
        });
    }

    @NonNull
    @Override
    public View getView() {
        return wrappedAdView;
    }

    @Override
    public void onAdClicked(View view, int type) {
        if (bannerAdCallback != null) {
            bannerAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onAdShow(View view, int type) {
        if (bannerAdCallback != null) {
            bannerAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onRenderFail(View view, String errorMessage, int errorCode) {
        adLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, errorMessage));
    }

    @Override
    public void onRenderSuccess(View view, float width, float height) {
        wrappedAdView.addView(view);
        bannerAdCallback = adLoadCallback.onSuccess(PangleRtbBannerAd.this);
    }
}
