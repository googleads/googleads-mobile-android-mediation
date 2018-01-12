package jp.maio.sdk.android.mediation.admob.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import jp.maio.sdk.android.MaioAds;

/**
 * maio mediation adapter for AdMob Rewarded videos.
 */
public class Rewarded implements MediationRewardedVideoAdAdapter {

    //Admob Rewarded listener
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;

    // maio Media Id
    private String mMediaId;
    // maio Rewarded Zone Id
    private String mRewardVideoZoneId;
    // Flag to keep track of whether or not the maio rewarded video ad adapter has been initialized.
    private boolean mIsRewardedVideoInitialized;

    @Override
    public void initialize(Context context,
                           MediationAdRequest adRequest,
                           String userId,
                           MediationRewardedVideoAdListener listener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        if (!(context instanceof Activity)) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        this.mMediationRewardedVideoAdListener = listener;
        mIsRewardedVideoInitialized = true;

        loadServerParameters(serverParameters);

        if (!isInitialized()) {
            //maio sdk initialization
            MaioEventForwarder.initialize((Activity) context, this.mMediaId);
        }
    }

    @Override
    //Load the next maio rewarded video ad
    public void loadAd(MediationAdRequest adRequest,
                       Bundle serverParameters,
                       Bundle networkExtras) {
        if (!isInitialized())
            return;

        //Load new server parameters in case zone id has changed
        loadServerParameters(serverParameters);

        if (MaioAds.canShow(this.mRewardVideoZoneId)) {
            if (this.mMediationRewardedVideoAdListener != null) {
                this.mMediationRewardedVideoAdListener.onAdLoaded(Rewarded.this);
            }
        } else {
            if (this.mMediationRewardedVideoAdListener != null) {
                this.mMediationRewardedVideoAdListener
                        .onAdFailedToLoad(Rewarded.this, AdRequest.ERROR_CODE_NO_FILL);
            }
        }
    }

    private void loadServerParameters(Bundle serverParameters) {
        this.mMediaId = serverParameters.getString("mediaId");
        this.mRewardVideoZoneId = serverParameters.getString("zoneId");
    }

    @Override
    //Display maio rewarded video ad
    public void showVideo() {
        MaioEventForwarder.showVideo(this.mRewardVideoZoneId,
                                     Rewarded.this,
                                     mMediationRewardedVideoAdListener);
    }

    @Override
    public boolean isInitialized() {
        return MaioEventForwarder.isInitialized() && mIsRewardedVideoInitialized;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }
}