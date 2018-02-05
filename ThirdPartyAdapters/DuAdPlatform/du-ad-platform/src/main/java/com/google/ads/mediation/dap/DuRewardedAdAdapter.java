package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;

import com.duapps.ad.base.DuAdNetwork;
import com.duapps.ad.video.DuVideoAd;
import com.duapps.ad.video.DuVideoAdSDK;
import com.duapps.ad.video.DuVideoAdsManager;
import com.google.ads.mediation.dap.forwarder.DapRewardedVideoEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import java.util.ArrayList;
import java.util.HashSet;

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
    static boolean isVideoInitialized = false;
    public static HashSet<Integer> initializedVideoPlacementIds = new HashSet<>();


    static void initializeVideoSDK(Context context, Bundle mediationExtras, int pid) {
        if (!isVideoInitialized) {
            boolean initIdsSucc = false;
            boolean shouldInit = false;
            if (mediationExtras != null) {
                ArrayList<Integer> allPids = mediationExtras.getIntegerArrayList(DuAdMediation.KEY_ALL_VIDEO_PLACEMENT_ID);
                if (allPids != null) {
                    initializedVideoPlacementIds.addAll(allPids);
                    shouldInit = true;
                    initIdsSucc = true;
                }
            }
            if (!initializedVideoPlacementIds.contains(pid)) {
                initializedVideoPlacementIds.add(pid);
                shouldInit = true;
            }
            if (shouldInit) {
                String initJsonConfig = DuAdMediation.buildJsonFromPidsNative(initializedVideoPlacementIds, "video");
                DuAdMediation.d(TAG, "init config json is : " + initJsonConfig);
                DuAdNetwork.init(context, initJsonConfig);
                DuVideoAdSDK.init(context, initJsonConfig);
                if (initIdsSucc) {
                    isVideoInitialized = true;
                } else {
                    String msg = "Only the following video placementIds " + initializedVideoPlacementIds + " is " +
                            "initialized. "
                            + "It is Strongly recommended to use DuAdExtrasBundleBuilder.addAllVideoPlacementId() to pass all "
                            + "your valid video placement id when " +
                            "requests video ads, "
                            + "so that the DuVideoAdSDK could be normally initialized.";
                    Log.e(TAG, msg);
                }
            }
        }
    }
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
        if (!DuAdMediation.checkClassExist("com.duapps.ad.video.DuVideoAdsManager")) {
            String msg = "No Du Video Ad SDK can be found, please make sure you have integrated latest version of Du " +
                    "Video Ad SDK";
            Log.e(TAG, msg);
            Log.e(TAG, msg);
            listener.onInitializationFailed(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }

        int pid = getValidPid(serverParameters);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        initializeVideoSDK(context, mediationExtras, pid);
        mRewardedVideoListener = listener;
        mRewardedVideoPid = pid;
        mRewardedVideoCtx = context;
        mDuRewardedVideoAd = DuVideoAdsManager.getVideoAd(mRewardedVideoCtx, mRewardedVideoPid);
        mDuRewardedVideoAd.setListener(new DapRewardedVideoEventForwarder(this, mRewardedVideoListener));
        mIsInitialized = true;

        mRewardedVideoListener.onInitializationSucceeded(this);
        DuAdMediation.d(TAG, "Dap Rewarded Video is initialized. mRewardedVideoPid = " + mRewardedVideoPid +
                ", adapter instance: " + this);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle mediationExtras) {
        if (!DuAdMediation.checkClassExist("com.duapps.ad.video.DuVideoAdsManager")) {
            String msg = "No Du Video Ad SDK is found, please make sure you have integrated latest version of Du "
                    + "Video Ad SDK";
            Log.e(TAG, msg);
            Log.e(TAG, msg);
            return;
        }
        if (mDuRewardedVideoAd == null) {
            mIsInitialized = false;
            if (mRewardedVideoListener != null) {
                mRewardedVideoListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
            return;
        }
        DuAdMediation.d(TAG, "Dap Rewarded Video load....mRewardedVideoPid = " + mRewardedVideoPid);
        mDuRewardedVideoAd.load();
    }

    @Override
    public void showVideo() {
        if (!DuAdMediation.checkClassExist("com.duapps.ad.video.DuVideoAdsManager")) {
            String msg = "No Du Video Ad SDK is found, please make sure you have integrated latest version of Du "
                    + "Video Ad SDK";
            Log.e(TAG, msg);
            Log.e(TAG, msg);
            return;
        }
        if (mDuRewardedVideoAd != null && mDuRewardedVideoAd.isAdPlayable()) {
            DuAdMediation.d(TAG, "Dap Rewarded Video is available. Showing...");
            mDuRewardedVideoAd.playAd(mRewardedVideoCtx);
        } else {
            DuAdMediation.d(TAG, "Dap Rewarded Video is not available. Try re-requesting.");
        }
    }

    @Override
    public boolean isInitialized() {
        DuAdMediation.d(TAG, "isInit = " + mIsInitialized + ", adapter instance: " + this);
        return mIsInitialized;
    }

    /* ****************** MediationAdapter ********************** */

    @Override
    public void onDestroy() {
        DuAdMediation.d(TAG, "onDestroy ");
        if (mDuRewardedVideoAd != null) {
            mDuRewardedVideoAd.clearListener();
            mDuRewardedVideoAd = null;
        }
    }

    @Override
    public void onPause() {
        DuAdMediation.d(TAG, "DuAdAdapter onPause, adapter instance: " + this);
    }

    @Override
    public void onResume() {
        DuAdMediation.d(TAG, "DuAdAdapter onResume, adapter instance: " + this);
    }


    private int getValidPid(Bundle bundle) {
        if (bundle == null) {
            return -1;
        }
        String pidStr = bundle.getString(DuAdMediation.KEY_DAP_PID);
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
