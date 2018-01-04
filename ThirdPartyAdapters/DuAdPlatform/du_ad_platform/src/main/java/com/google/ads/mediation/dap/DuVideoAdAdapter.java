package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.duapps.ad.video.DuVideoAd;
import com.duapps.ad.video.DuVideoAdsManager;
import com.google.ads.mediation.dap.forwarder.DapCustomRewardedVideoEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

/**
 * Created by bushaopeng on 18/1/3.
 */

public class DuVideoAdAdapter implements MediationRewardedVideoAdAdapter {
    private static final String TAG = "DAP Mediation";
    /**
     * This key should be configured at AdMob server side or AdMob front-end.
     */
    private static final String DAP_PID_KEY = "placementId";
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
        mDuRewardedVideoAd.setListener(new DapCustomRewardedVideoEventForwarder(this, mRewardedVideoListener));
        mIsInitialized = true;

        mRewardedVideoListener.onInitializationSucceeded(this);
        Log.d(TAG, "Dap Rewarded Video is initialized. mRewardedVideoPid = " + mRewardedVideoPid);
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
        Log.d(TAG, "Dap Rewarded Video load....mRewardedVideoPid = " + mRewardedVideoPid);
        mDuRewardedVideoAd.load();
    }

    @Override
    public void showVideo() {
        if (mDuRewardedVideoAd != null && mDuRewardedVideoAd.isAdPlayable()) {
            Log.d(TAG, "Dap Rewarded Video is available. Showing...");
            mDuRewardedVideoAd.playAd(mRewardedVideoCtx);
        } else {
            Log.d(TAG, "Dap Rewarded Video is not available. Try re-requesting.");
        }
    }

    @Override
    public boolean isInitialized() {
        Log.d(TAG, "isInit = " + mIsInitialized);
        return mIsInitialized;
    }

    /* ****************** MediationAdapter ********************** */

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy ");
        if (mDuRewardedVideoAd != null) {
            mDuRewardedVideoAd.clearListener();
            mDuRewardedVideoAd = null;
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "DuAdAdapter onPause");
    }

    @Override
    public void onResume() {
        Log.d(TAG, "DuAdAdapter onResume");
    }


    private int getValidPid(Bundle bundle) {
        if (bundle == null) {
            return -1;
        }
        String pidStr = bundle.getString(DAP_PID_KEY);
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
