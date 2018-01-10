package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;

import com.duapps.ad.video.DuVideoAd;
import com.duapps.ad.video.DuVideoAdsManager;
import com.google.ads.mediation.dap.forwarder.DapRewardedVideoEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

/**
 * Created by bushaopeng on 18/1/3.
 */
@Keep
public class DuRewardedAdAdapter implements MediationRewardedVideoAdAdapter {
    private static final String TAG = DuRewardedAdAdapter.class.getSimpleName();
    private int mRewardedVideoPid;
    private Context mRewardedVideoCtx;
    private DuVideoAd mDuRewardedVideoAd;
    private MediationRewardedVideoAdListener mRewardedVideoListener;
    private boolean mIsInitialized;

    /* ****************** MediationRewardedVideoAdAdapter ********************** */

    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String unUsed,
                           MediationRewardedVideoAdListener listener,
                           Bundle serverParameters,
                           Bundle mediationExtras) {
        if (context == null) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        int pid = getValidPid(serverParameters);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        mRewardedVideoListener = listener;
        mRewardedVideoPid = pid;
        mRewardedVideoCtx = context;
        mDuRewardedVideoAd = DuVideoAdsManager.getVideoAd(mRewardedVideoCtx, mRewardedVideoPid);
        mDuRewardedVideoAd.setListener(new DapRewardedVideoEventForwarder(this, mRewardedVideoListener));
        mIsInitialized = true;

        mRewardedVideoListener.onInitializationSucceeded(this);
        DuAdAdapter.d(TAG, "Dap Rewarded Video is initialized. mRewardedVideoPid = " + mRewardedVideoPid);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle mediationExtras) {
        if (mDuRewardedVideoAd == null) {
            mIsInitialized = false;
            if (mRewardedVideoListener != null) {
                mRewardedVideoListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
            return;
        }
        DuAdAdapter.d(TAG, "Dap Rewarded Video load....mRewardedVideoPid = " + mRewardedVideoPid);
        mDuRewardedVideoAd.load();
    }

    @Override
    public void showVideo() {
        if (mDuRewardedVideoAd != null && mDuRewardedVideoAd.isAdPlayable()) {
            DuAdAdapter.d(TAG, "Dap Rewarded Video is available. Showing...");
            mDuRewardedVideoAd.playAd(mRewardedVideoCtx);
        } else {
            DuAdAdapter.d(TAG, "Dap Rewarded Video is not available. Try re-requesting.");
        }
    }

    @Override
    public boolean isInitialized() {
        DuAdAdapter.d(TAG, "isInit = " + mIsInitialized);
        return mIsInitialized;
    }

    /* ****************** MediationAdapter ********************** */

    @Override
    public void onDestroy() {
        DuAdAdapter.d(TAG, "onDestroy ");
        if (mDuRewardedVideoAd != null) {
            mDuRewardedVideoAd.clearListener();
            mDuRewardedVideoAd = null;
        }
    }

    @Override
    public void onPause() {
        DuAdAdapter.d(TAG, "DuAdAdapter onPause");
    }

    @Override
    public void onResume() {
        DuAdAdapter.d(TAG, "DuAdAdapter onResume");
    }


    private int getValidPid(Bundle bundle) {
        if (bundle == null) {
            return -1;
        }
        String pidStr = bundle.getString(DuAdAdapter.KEY_DAP_PID);
        if (TextUtils.isEmpty(pidStr)) {
            return -1;
        }
        int pid = -1;
        try {
            pid = Integer.parseInt(pidStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
        if (pid < 0) {
            return -1;
        }
        return pid;
    }

}
