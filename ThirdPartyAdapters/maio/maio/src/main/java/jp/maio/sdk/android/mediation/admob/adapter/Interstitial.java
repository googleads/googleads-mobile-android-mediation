package jp.maio.sdk.android.mediation.admob.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.maio.MaioMediationAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.MaioAdsListenerInterface;

/**
 * maio mediation adapter for AdMob Interstitial videos.
 */
public class Interstitial extends MaioMediationAdapter
        implements MediationInterstitialAdapter, MaioAdsListenerInterface {

    private MediationInterstitialListener mMediationInterstitialListener;

    //region MediationInterstitialAdapter implementation
    @Override
    public void onDestroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {

        if (!(context instanceof Activity)) {
            Log.w(TAG, "Failed to request ad from Maio: " +
                    "Maio SDK requires an Activity context to load ads.");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        this.mMediaID = serverParameters.getString(MaioAdsManager.KEY_MEDIA_ID);
        if (TextUtils.isEmpty(mMediaID)) {
            Log.w(TAG, "Failed to request ad from Maio: Missing or Invalid Media ID.");
            listener.onAdFailedToLoad(Interstitial.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        this.mZoneID = serverParameters.getString(MaioAdsManager.KEY_ZONE_ID);
        if (TextUtils.isEmpty(mMediaID)) {
            Log.w(TAG, "Failed to request ad from Maio: Missing or Invalid Zone ID.");
            listener.onAdFailedToLoad(Interstitial.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        this.mMediationInterstitialListener = listener;
        MaioAds.setAdTestMode(mediationAdRequest.isTesting());
        MaioAdsManager.getManager(mMediaID).initialize((Activity) context,
                new MaioAdsManager.InitializationListener() {
            @Override
            public void onMaioInitialized() {
                MaioAdsManager.getManager(mMediaID).loadAd(mZoneID, Interstitial.this);
            }
        });
    }

    @Override
    public void showInterstitial() {
        boolean didShow = MaioAdsManager.getManager(mMediaID).showAd(mZoneID);
        if (!didShow && this.mMediationInterstitialListener != null) {
            this.mMediationInterstitialListener.onAdOpened(Interstitial.this);
            this.mMediationInterstitialListener.onAdClosed(Interstitial.this);
        }
    }
    //endregion

    //region MaioAdsListenerInterface implementation
    @Override
    public void onInitialized() {
        // Not called.
        // MaioAdsManager calls MaioAdsManager.InitializationListener.onMaioInitialized() instead.
    }

    @Override
    public void onChangedCanShow(String zoneId, boolean isAvailable) {
        if (this.mMediationInterstitialListener != null && isAvailable) {
            this.mMediationInterstitialListener.onAdLoaded(Interstitial.this);
        }
    }

    @Override
    public void onFailed(FailNotificationReason reason, String zoneId) {
        Log.w(TAG, "Failed to request ad from Maio: " + reason.toString());
        if (this.mMediationInterstitialListener != null) {
            this.mMediationInterstitialListener.onAdFailedToLoad(Interstitial.this,
                    AdRequest.ERROR_CODE_NO_FILL);
        }
    }

    @Override
    public void onOpenAd(String zoneId) {
        if (this.mMediationInterstitialListener != null) {
            this.mMediationInterstitialListener.onAdOpened(Interstitial.this);
        }
    }

    @Override
    public void onStartedAd(String zoneId) {
        // No relevant Interstitial Ad event to forward to the Google Mobile Ads SDK.
    }

    @Override
    public void onClickedAd(String zoneId) {
        if (this.mMediationInterstitialListener != null) {
            this.mMediationInterstitialListener.onAdClicked(Interstitial.this);
            this.mMediationInterstitialListener.onAdLeftApplication(Interstitial.this);
        }
    }

    @Override
    public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
        // No relevant Interstitial Ad event to forward to the Google Mobile Ads SDK.
    }

    @Override
    public void onClosedAd(String zoneId) {
        if (this.mMediationInterstitialListener != null) {
            this.mMediationInterstitialListener.onAdClosed(Interstitial.this);
        }
    }
    //endregion

}