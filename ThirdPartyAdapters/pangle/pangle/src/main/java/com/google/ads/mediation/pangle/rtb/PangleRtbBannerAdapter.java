package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstant.ERROR_BANNER_AD_SIZE_IS_NULL;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_PLACEMENT;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.google.ads.mediation.pangle.PangleConstant;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

import java.util.List;

public class PangleRtbBannerAdapter implements MediationBannerAd, TTNativeExpressAd.ExpressAdInteractionListener {
    private static final String TAG = "PangleRtbBannerAdapter";


    private MediationBannerAdConfiguration mAdConfiguration;
    private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mAdLoadCallback;
    private MediationBannerAdCallback mBannerAdCallback;
    private FrameLayout mWrappedAdView;

    public PangleRtbBannerAdapter(MediationBannerAdConfiguration mediationBannerAdConfiguration,
                                  MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
        this.mAdConfiguration = mediationBannerAdConfiguration;
        this.mAdLoadCallback = mediationAdLoadCallback;
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

        AdSize adSize = mAdConfiguration.getAdSize();
        if (adSize == null) {
            AdError error = PangleConstant.createAdapterError(ERROR_BANNER_AD_SIZE_IS_NULL,
                    "MediationBannerAdConfiguration getAdSize get null");
            mAdLoadCallback.onFailure(error);
            return;
        }

        Context context = mAdConfiguration.getContext();
        mWrappedAdView = new FrameLayout(context);

        String bidResponse = mAdConfiguration.getBidResponse();

        //(notice : make sure the Pangle sdk had been initialized) obtain Pangle ad manager
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
                if (mAdLoadCallback != null) {
                    mAdLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, errorMessage));
                }
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0) {
                    if (mAdLoadCallback != null) {
                        mAdLoadCallback.onFailure(PangleConstant.createSdkError(AdRequest.ERROR_CODE_NO_FILL,
                                "banner loaded success. but ad no fill "));
                    }
                    return;
                }
                TTNativeExpressAd mBannerExpressAd = ads.get(0);
                mBannerExpressAd.setExpressInteractionListener(PangleRtbBannerAdapter.this);
                mBannerExpressAd.render();
            }
        });
    }

    @NonNull
    @Override
    public View getView() {
        return mWrappedAdView;
    }


    /**
     * BannerInteractionListener onAdClicked
     */
    @Override
    public void onAdClicked(View view, int i) {
        if (mBannerAdCallback != null) {
            mBannerAdCallback.reportAdClicked();
        }
    }

    /**
     * BannerInteractionListener onAdShow
     */
    @Override
    public void onAdShow(View view, int i) {
        if (mBannerAdCallback != null) {
            mBannerAdCallback.reportAdImpression();
        }
    }

    /**
     * BannerInteractionListener onRenderFail
     */
    @Override
    public void onRenderFail(View view, String errorMessage, int errorCode) {
        mAdLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, errorMessage));
    }

    /**
     * BannerInteractionListener onRenderSuccess
     */
    @Override
    public void onRenderSuccess(View view, float v, float v1) {
        mBannerAdCallback = mAdLoadCallback.onSuccess(PangleRtbBannerAdapter.this);
        mWrappedAdView.addView(view);
    }
}
