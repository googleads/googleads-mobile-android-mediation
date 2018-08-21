package jp.maio.sdk.android.mediation.admob.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.MaioAdsInstance;

/**
 * maio mediation adapter for AdMob Rewarded videos.
 */
public class Rewarded implements MediationRewardedVideoAdAdapter, FirstLoadInterface {

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

        MaioAds.setAdTestMode(adRequest.isTesting());

        this.mMediationRewardedVideoAdListener = listener;
        mIsRewardedVideoInitialized = true;

        loadServerParameters(serverParameters);

        if (!MaioAdsInstanceRepository.isInitialized(this.mMediaId)) {
            //maio sdk initialization
            MaioEventForwarder.initialize((Activity) context, this.mMediaId, this);
            this.mMediationRewardedVideoAdListener.onInitializationSucceeded(this);

            return;
        }

        MaioAdsInstance maio = MaioAdsInstanceRepository.getMaioAdsInstance(this.mMediaId);

            if (maio.canShow(this.mRewardVideoZoneId)) {
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

    @Override
    public void adLoaded(String zoneId)
    {
        if (this.mMediationRewardedVideoAdListener != null && zoneId.equals(this.mRewardVideoZoneId)) {
            this.mMediationRewardedVideoAdListener.onAdLoaded(Rewarded.this);
        }
    }

    @Override
    //Load the next maio rewarded video ad
    public void loadAd(MediationAdRequest adRequest,
                       Bundle serverParameters,
                       Bundle networkExtras) {

        //Load new server parameters in case zone id has changed
        loadServerParameters(serverParameters);

        if(!MaioAdsInstanceRepository.isInitialized(this.mMediaId))
        {
            return;
        }

        MaioAdsInstance maio = MaioAdsInstanceRepository.getMaioAdsInstance(this.mMediaId);

        if (maio.canShow(this.mRewardVideoZoneId)) {
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
        MaioAdsInstance maio = MaioAdsInstanceRepository.getMaioAdsInstance(this.mMediaId);

        MaioEventForwarder.showVideo(this.mRewardVideoZoneId,
                Rewarded.this,
                mMediationRewardedVideoAdListener,
                maio);
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