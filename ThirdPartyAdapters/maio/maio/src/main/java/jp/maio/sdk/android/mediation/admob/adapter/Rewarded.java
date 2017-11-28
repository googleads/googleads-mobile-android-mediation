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
    private MediationRewardedVideoAdListener mediationRewardedVideoAdListener;

    // maio Media Id
    private String mediaId;
    // maio Rewarded Zone Id
    private String rewardVideoZoneId;

    @Override
    public void initialize(Context context,
                           MediationAdRequest adRequest,
                           String userId,
                           MediationRewardedVideoAdListener listener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        if(!(context instanceof Activity)) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        this.mediationRewardedVideoAdListener = listener;

        loadServerParameters(serverParameters);

        if (!isInitialized()) {
            //maio sdk initialization
            MaioEventForwarder.initialize((Activity) context, this.mediaId);
        }
    }

    @Override
    //Load the next maio rewarded video ad
    public void loadAd(MediationAdRequest adRequest, Bundle serverParameters, Bundle networkExtras) {
        if (!isInitialized())
            return;

        //Load new server parameters in case zone id has changed
        loadServerParameters(serverParameters);

        if (MaioAds.canShow(this.rewardVideoZoneId)) {
            if (this.mediationRewardedVideoAdListener != null) {
                this.mediationRewardedVideoAdListener.onAdLoaded(Rewarded.this);
            }
        } else {
            if (this.mediationRewardedVideoAdListener != null) {
                this.mediationRewardedVideoAdListener.onAdFailedToLoad(Rewarded.this, 3);
            }
        }
    }

    private void loadServerParameters(Bundle serverParameters) {
        this.mediaId = serverParameters.getString("mediaId");
        this.rewardVideoZoneId = serverParameters.getString("zoneId");
    }

    @Override
    //Display maio rewarded video ad
    public void showVideo() {
        MaioEventForwarder.showVideo(this.rewardVideoZoneId, Rewarded.this, mediationRewardedVideoAdListener);
    }

    @Override
    public boolean isInitialized() {
        return MaioEventForwarder.isInitialized();
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