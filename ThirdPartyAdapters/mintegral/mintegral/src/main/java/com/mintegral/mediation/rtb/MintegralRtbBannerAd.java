package com.mintegral.mediation.rtb;

import static com.mintegral.mediation.MintegralConstants.ERROR_BANNER_SIZE_MISMATCH;
import static com.mintegral.mediation.MintegralConstants.ERROR_INVALID_BID_RESPONSE;
import static com.mintegral.mediation.MintegralConstants.ERROR_SDK_INTER_ERROR;
import static com.mintegral.mediation.MintegralConstants.ERROR_SDK_LOAD_ERROR;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

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
import com.mintegral.mediation.MintegralConstants;

import java.util.ArrayList;

public class MintegralRtbBannerAd implements MediationBannerAd, BannerAdListener {

    private final MediationBannerAdConfiguration mAdConfiguration;
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mAdLoadCallback;
    private MediationBannerAdCallback mBannerAdCallback;
    private MBBannerView mBBannerView;
    private BannerSize mBannerSize;
    private static final int VALUE_800 = 800;
    private static final int VALUE_728 = 728;
    private static final int VALUE_600 = 600;
    private static final int VALUE_320 = 320;
    private static final int VALUE_300 = 300;
    private static final int VALUE_250 = 250;
    private static final int VALUE_90 = 90;
    private static final int VALUE_50 = 50;
    private static final int VALUE_0 = 0;
    RelativeLayout.LayoutParams mParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

    public MintegralRtbBannerAd(
            @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
            @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                    mediationAdLoadCallback) {
        this.mAdConfiguration = mediationBannerAdConfiguration;
        this.mAdLoadCallback = mediationAdLoadCallback;
        initBannerAd();
    }

    private void updateSupportBannerSize() {
        ArrayList<AdSize> supportedSizes = new ArrayList<>(5);
        supportedSizes.add(new AdSize(VALUE_320, VALUE_50));
        supportedSizes.add(new AdSize(VALUE_300, VALUE_250));
        supportedSizes.add(new AdSize(VALUE_728, VALUE_90));
        supportedSizes.add(new AdSize(VALUE_320, VALUE_90));
        supportedSizes.add(new AdSize(VALUE_800, VALUE_600));
        AdSize closestSize = MediationUtils.findClosestSize(mAdConfiguration.getContext(), mAdConfiguration.getAdSize(), supportedSizes);
        if (closestSize == null) {
            callBackFail(ERROR_BANNER_SIZE_MISMATCH, "Failed to request banner ad from Mintegral. Invalid banner size.");
            return;
        }
        if (closestSize.equals(AdSize.BANNER)) { // 320 * 50
            mBannerSize = new BannerSize(BannerSize.STANDARD_TYPE, VALUE_0, VALUE_0);
        }
        if (closestSize.equals(AdSize.MEDIUM_RECTANGLE)) { // 300 * 250
            mBannerSize = new BannerSize(BannerSize.MEDIUM_TYPE, VALUE_0, VALUE_0);
        }
        if (closestSize.equals(AdSize.LEADERBOARD)) { // 728 * 90
            mBannerSize = new BannerSize(BannerSize.SMART_TYPE, VALUE_728, VALUE_90);
        }
        if (mBannerSize == null && closestSize.getWidth() == VALUE_320) {
            mBannerSize = new BannerSize(BannerSize.LARGE_TYPE, VALUE_0, VALUE_0);
        }
        if (mBannerSize == null) {
            mBannerSize = new BannerSize(BannerSize.DEV_SET_TYPE, VALUE_800, VALUE_600);
        }
    }

    private void initBannerAd() {
        if (mAdConfiguration == null) {
            callBackFail(ERROR_SDK_INTER_ERROR, "Mintegral init Fail mAdConfiguration is null");
            return;
        }
        if (mAdConfiguration.getServerParameters() == null) {
            callBackFail(ERROR_SDK_INTER_ERROR, "Mintegral init Fail ServiceParameters is null");
            return;
        }
        if (mAdConfiguration.getContext() == null) {
            callBackFail(ERROR_SDK_INTER_ERROR, "Mintegral init Fail context is null");
            return;
        }
        String unitId = mAdConfiguration.getServerParameters().getString(MintegralConstants.AD_UNIT_ID);
        String placementId = mAdConfiguration.getServerParameters().getString(MintegralConstants.PLACEMENT_ID);
        mBBannerView = new MBBannerView(mAdConfiguration.getContext());
        updateSupportBannerSize();
        mBBannerView.init(mBannerSize, placementId, unitId);

        if (mBannerSize == null) {
            mBannerSize = new BannerSize(BannerSize.DEV_SET_TYPE, VALUE_800, VALUE_600);
        }
        mParams.width = dip2px(mAdConfiguration.getContext(), mBannerSize.getWidth());
        mParams.height = dip2px(mAdConfiguration.getContext(), mBannerSize.getHeight());
        mBBannerView.setLayoutParams(mParams);
    }

    public void load() {
        if (mAdConfiguration == null) {
            callBackFail(ERROR_SDK_LOAD_ERROR, "Mintegral Ad load Fail mAdConfiguration is null");
            return;
        }
        if (mBBannerView == null) {
            callBackFail(ERROR_SDK_LOAD_ERROR, "Mintegral Ad load Fail mBBannerView is null");
            return;
        }

        String token = mAdConfiguration.getBidResponse();
        if (TextUtils.isEmpty(token)) {
            callBackFail(ERROR_INVALID_BID_RESPONSE, "Failed to load Banner ad from MIntegral. Missing or invalid bid response.");
            return;
        }

        mBBannerView.setBannerAdListener(this);
        mBBannerView.loadFromBid(token);
    }

    @NonNull
    @Override
    public View getView() {
        return mBBannerView;
    }

    @Override
    public void onLoadFailed(MBridgeIds mBridgeIds, String s) {
        callBackFail(ERROR_SDK_INTER_ERROR, "Failed to load Banner ad from MIntegral.");
    }

    @Override
    public void onLoadSuccessed(MBridgeIds mBridgeIds) {
        if (mAdLoadCallback == null) {
            return;
        }
        mBannerAdCallback = mAdLoadCallback.onSuccess(this);
    }

    @Override
    public void onLogImpression(MBridgeIds mBridgeIds) {
        if (mBannerAdCallback != null) {
            mBannerAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onClick(MBridgeIds mBridgeIds) {
        if (mBannerAdCallback != null) {
            mBannerAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onLeaveApp(MBridgeIds mBridgeIds) {
        if (mBannerAdCallback != null) {
            mBannerAdCallback.onAdLeftApplication();
        }
    }

    @Override
    public void showFullScreen(MBridgeIds mBridgeIds) {

    }

    @Override
    public void closeFullScreen(MBridgeIds mBridgeIds) {

    }

    @Override
    public void onCloseBanner(MBridgeIds mBridgeIds) {
        if (mBannerAdCallback != null) {
            mBannerAdCallback.onAdClosed();
        }
    }

    public void onDestroy(){
        if(mBBannerView!= null){
            mBBannerView.release();
        }
    }


    private static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void callBackFail(int errorCode, String failMsg) {
        if (mAdLoadCallback != null) {
            AdError error = MintegralConstants.createAdapterError(errorCode, failMsg);
            mAdLoadCallback.onFailure(error);
        }
    }
}
