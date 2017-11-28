package jp.maio.sdk.android.mediation.admob.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import jp.maio.sdk.android.MaioAds;

/**
 * maio mediation adapter for AdMob Interstitial videos.
 */
public class Interstitial implements MediationInterstitialAdapter {

    //Admob Interstitial listener
    private MediationInterstitialListener mediationInterstitialListener;

    // maio Media Id
    private String mediaId;
    // maio Interstitial Zone Id
    private String interstitialZoneId;

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        if(!(context instanceof Activity)) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        this.mediationInterstitialListener = listener;
        loadServerParameters(serverParameters);

        if (!isInitialized()){
            //maio sdk initialization
            MaioEventForwarder.initialize((Activity) context, this.mediaId);
        }

        if (MaioAds.canShow(this.interstitialZoneId)) {
            if (this.mediationInterstitialListener != null) {
                this.mediationInterstitialListener.onAdLoaded(Interstitial.this);
            }
        } else {
            if (this.mediationInterstitialListener != null) {
                this.mediationInterstitialListener.onAdFailedToLoad(Interstitial.this, 3);
            }
        }
    }

    // Load media and zone id from the server
    private void loadServerParameters(Bundle serverParameters) {
        this.mediaId = serverParameters.getString("mediaId");
        this.interstitialZoneId = serverParameters.getString("zoneId");
    }

    @Override
    //Show maio Interstitial video ad
    public void showInterstitial() {
        MaioEventForwarder.showInterstitial(this.interstitialZoneId, Interstitial.this, mediationInterstitialListener );
    }

    //Checks if maio sdk has initialized
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