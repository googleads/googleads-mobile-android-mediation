package com.mintegral.mediation.rtb;



import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.mbridge.msdk.out.BannerAdListener;
import com.mbridge.msdk.out.BannerSize;
import com.mbridge.msdk.out.MBBannerView;
import com.mbridge.msdk.out.MBridgeIds;

import java.util.ArrayList;

public class MintegralRtbBannerAd implements MediationBannerAd, BannerAdListener {

    private final MediationBannerAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback;
    private MediationBannerAdCallback bannerAdCallback;
    private MBBannerView mbBannerView;
    private BannerSize bannerSize;
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

    public MintegralRtbBannerAd(
            @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
            @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                    mediationAdLoadCallback) {
        this.adConfiguration = mediationBannerAdConfiguration;
        this.adLoadCallback = mediationAdLoadCallback;
    }

    private void updateSupportBannerSize() {
        ArrayList<AdSize> supportedSizes = new ArrayList<>(3);
        supportedSizes.add(new AdSize(320, 50));
        supportedSizes.add(new AdSize(300, 250));
        supportedSizes.add(new AdSize(728, 90));
        AdSize closestSize = MediationUtils.findClosestSize(adConfiguration.getContext(), adConfiguration.getAdSize(), supportedSizes);
        if (closestSize == null) {
            callFailureCallback(MintegralConstants.ERROR_BANNER_SIZE_MISMATCH, "Failed to request banner ad from Mintegral. Invalid banner size.");
            return;
        }
        if (closestSize.equals(AdSize.BANNER)) { // 320 * 50
            bannerSize = new BannerSize(BannerSize.STANDARD_TYPE, 0, 0);
        }
        if (closestSize.equals(AdSize.MEDIUM_RECTANGLE)) { // 300 * 250
            bannerSize = new BannerSize(BannerSize.MEDIUM_TYPE, 0, 0);
        }
        if (closestSize.equals(AdSize.LEADERBOARD)) { // 728 * 90
            bannerSize = new BannerSize(BannerSize.SMART_TYPE, closestSize.getWidth(),0);
        }
        if (bannerSize == null) {
            bannerSize = new BannerSize(BannerSize.DEV_SET_TYPE, closestSize.getWidth(), closestSize.getHeight());
        }
    }

    private void initBannerAd() {
        if (adConfiguration == null) {
            callFailureCallback(MintegralConstants.ERROR_SDK_ADAPTER_ERROR, "Mintegral init Fail mAdConfiguration is null");
            return;
        }
        if (adConfiguration.getServerParameters() == null) {
            callFailureCallback(MintegralConstants.ERROR_SDK_ADAPTER_ERROR, "Mintegral init Fail ServiceParameters is null");
            return;
        }
        if (adConfiguration.getContext() == null) {
            callFailureCallback(MintegralConstants.ERROR_SDK_ADAPTER_ERROR, "Mintegral init Fail context is null");
            return;
        }
        String unitId = adConfiguration.getServerParameters().getString(MintegralConstants.AD_UNIT_ID);
        String placementId = adConfiguration.getServerParameters().getString(MintegralConstants.PLACEMENT_ID);
        mbBannerView = new MBBannerView(adConfiguration.getContext());
        updateSupportBannerSize();
        mbBannerView.init(bannerSize, placementId, unitId);
        params.width = dipToPixels(adConfiguration.getContext(), bannerSize.getWidth());
        params.height = dipToPixels(adConfiguration.getContext(), bannerSize.getHeight());
        mbBannerView.setLayoutParams(params);
    }

    public void load() {
        initBannerAd();
        if (adConfiguration == null) {
            callFailureCallback(MintegralConstants.ERROR_SDK_ADAPTER_ERROR, "Mintegral Ad load Fail mAdConfiguration is null");
            return;
        }
        if (mbBannerView == null) {
            callFailureCallback(MintegralConstants.ERROR_SDK_ADAPTER_ERROR, "Mintegral Ad load Fail mBBannerView is null");
            return;
        }

        String token = adConfiguration.getBidResponse();
        if (TextUtils.isEmpty(token)) {
            callFailureCallback(MintegralConstants.ERROR_INVALID_BID_RESPONSE, "Failed to load Banner ad from MIntegral. Missing or invalid bid response.");
            return;
        }
        mbBannerView.setBannerAdListener(this);
        mbBannerView.loadFromBid(token);
    }

    @NonNull
    @Override
    public View getView() {
        return mbBannerView;
    }

    @Override
    public void onLoadFailed(MBridgeIds mBridgeIds, String errorMessage) {
        callFailureCallback(MintegralConstants.ERROR_SDK_INTER_ERROR, errorMessage);
    }

    @Override
    public void onLoadSuccessed(MBridgeIds mBridgeIds) {
        if (adLoadCallback == null) {
            return;
        }
        bannerAdCallback = adLoadCallback.onSuccess(this);
    }

    @Override
    public void onLogImpression(MBridgeIds mBridgeIds) {
        if (bannerAdCallback != null) {
            bannerAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onClick(MBridgeIds mBridgeIds) {
        if (bannerAdCallback != null) {
            bannerAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onLeaveApp(MBridgeIds mBridgeIds) {
        if (bannerAdCallback != null) {
            bannerAdCallback.onAdLeftApplication();
        }
    }

    @Override
    public void showFullScreen(MBridgeIds mBridgeIds) {
        //No-op, AdMob has no corresponding method
    }

    @Override
    public void closeFullScreen(MBridgeIds mBridgeIds) {
        //No-op, AdMob has no corresponding method
    }

    @Override
    public void onCloseBanner(MBridgeIds mBridgeIds) {
        if (bannerAdCallback != null) {
            bannerAdCallback.onAdClosed();
        }
    }

    public void onDestroy(){
        if(mbBannerView != null){
            mbBannerView.release();
        }
    }

    /**
     *  dip convert pixels
     * @param context
     * @param dpValue
     * @return
     */
    private static int dipToPixels(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void callFailureCallback(int errorCode, String errorMessage) {
        if (adLoadCallback != null) {
            AdError error = MintegralConstants.createAdapterError(errorCode, errorMessage);
            adLoadCallback.onFailure(error);
        }
    }
}
