package com.google.ads.mediation.nend;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

@SuppressWarnings("unused")
public class NendRewardedAdapter implements MediationRewardedVideoAdAdapter {
    public static final String KEY_USER_ID = "key_user_id";
    static final String TAG = "NendRewardedAdapter";

    private NendMediationRewardedVideoEventForwarder mRewardedVideoEventForwarder;

    @Override
    public void showVideo() {
        if (isInitialized()) mRewardedVideoEventForwarder.showAd();
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle mediationExtras) {
        if (isInitialized()) mRewardedVideoEventForwarder.loadAd(mediationExtras);
    }

    @Override
    public void initialize(Context context,
                           MediationAdRequest adRequest,
                           String unused,
                           MediationRewardedVideoAdListener adListener,
                           Bundle serverParameters,
                           Bundle mediationExtras) {
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Requires an Activity context to initialize!");
            adListener.onInitializationFailed(
                    NendRewardedAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        Activity activity = (Activity)context;

        mRewardedVideoEventForwarder = new NendMediationRewardedVideoEventForwarder(
                activity,
                NendRewardedAdapter.this,
                serverParameters,
                adListener
        );
        if (isInitialized()) {
            adListener.onInitializationSucceeded(this);
        } else {
            adListener.onInitializationFailed(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public boolean isInitialized() {
        return null != mRewardedVideoEventForwarder && mRewardedVideoEventForwarder.isInitialized();
    }

    @Override
    public void onDestroy() {
        if (mRewardedVideoEventForwarder != null) {
            mRewardedVideoEventForwarder.releaseAd();
            mRewardedVideoEventForwarder = null;
        }
    }

    @Override
    public void onPause() {
        if (mRewardedVideoEventForwarder != null) mRewardedVideoEventForwarder.onPause();
    }

    @Override
    public void onResume() {
        if (mRewardedVideoEventForwarder != null) mRewardedVideoEventForwarder.onResume();
    }
}
