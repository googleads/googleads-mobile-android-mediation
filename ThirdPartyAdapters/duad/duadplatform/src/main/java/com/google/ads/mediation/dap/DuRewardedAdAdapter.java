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
 * Mediation adapter for Rewarded Video Ads for DU Ad Platform
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

    static void initializeVideoSDK(Context context, Bundle mediationExtras, int pid, String appId) {
        if (!isVideoInitialized) {
            boolean initIdsSuccess = false;
            boolean shouldInit = false;

            if (mediationExtras != null) {
                ArrayList<Integer> allPids = mediationExtras.getIntegerArrayList(DuAdMediation.KEY_ALL_VIDEO_PLACEMENT_ID);
                if (allPids != null) {
                    initializedVideoPlacementIds.addAll(allPids);
                    shouldInit = true;
                    initIdsSuccess = true;
                }
            }

            if (!initializedVideoPlacementIds.contains(pid)) {
                initializedVideoPlacementIds.add(pid);
                shouldInit = true;
            }

            if (shouldInit) {
                String initJsonConfig = DuAdMediation.buildJsonFromPidsNative(initializedVideoPlacementIds, "video");
                DuAdMediation.d(TAG, "init config json is : " + initJsonConfig);
                context = DuAdMediation.setAppIdInMeta(context, appId);
                DuAdNetwork.init(context, initJsonConfig);
                DuVideoAdSDK.init(context, initJsonConfig);
                if (initIdsSuccess) {
                    isVideoInitialized = true;
                } else {
                    String message = "Only the following video placementIds " + initializedVideoPlacementIds + " are initialized. "
                            + "It is strongly recommended to use DuAdExtrasBundleBuilder.addAllVideoPlacementId() to pass all "
                            + "your valid video placement IDs when making a Rewarded Video Ad Request, so that the DuAdNetwork "
                            + "can be properly initialized.";
                    Log.e(TAG, message);
                }
            }
        }
    }

    // region MediationRewardedVideoAdAdapter implementation
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
            String message = "Unable to find the Du Video Ad SDK. Please ensure you have integrated the latest " +
                    "version of the Du Video Ad SDK.";
            Log.e(TAG, message);
            Log.e(TAG, message);
            listener.onInitializationFailed(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }

        int pid = getValidPid(serverParameters);
        String appId = serverParameters.getString(DuAdMediation.KEY_APP_ID);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        initializeVideoSDK(context, mediationExtras, pid, appId);
        mRewardedVideoListener = listener;
        mRewardedVideoPid = pid;
        mRewardedVideoCtx = context;
        mDuRewardedVideoAd = DuVideoAdsManager.getVideoAd(mRewardedVideoCtx, mRewardedVideoPid);
        mDuRewardedVideoAd.setListener(new DapRewardedVideoEventForwarder(this, mRewardedVideoListener));
        mIsInitialized = true;

        mRewardedVideoListener.onInitializationSucceeded(this);
        DuAdMediation.d(TAG, "Dap Rewarded Video is initialized. mRewardedVideoPid = " +
                mRewardedVideoPid + ", adapter instance: " + this);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle mediationExtras) {
        if (!DuAdMediation.checkClassExist("com.duapps.ad.video.DuVideoAdsManager")) {
            String message = "Unable to find the Du Video Ad SDK. Please ensure you have integrated the latest " +
                    "version of the Du Video Ad SDK.";
            Log.e(TAG, message);
            Log.e(TAG, message);
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
            String message = "Unable to find the Du Video Ad SDK. Please ensure you have integrated the latest " +
                    "version of the Du Video Ad SDK.";
            Log.e(TAG, message);
            Log.e(TAG, message);
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
    // endregion

    // region MediationAdapter implementation
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
    // endregion

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
