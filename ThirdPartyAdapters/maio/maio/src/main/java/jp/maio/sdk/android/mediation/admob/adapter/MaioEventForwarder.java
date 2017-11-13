package jp.maio.sdk.android.mediation.admob.adapter;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.MaioAdsListenerInterface;

/**
 * MaioEventForwarder is used to forward maio Rewarded and Interstitial events to Admob SDK.
 */
public class MaioEventForwarder implements MaioAdsListenerInterface {

    private static final MaioEventForwarder instance = new MaioEventForwarder();
    private static boolean initialized;
    private MediationRewardedVideoAdListener mediationRewardedVideoAdListener;
    private MediationRewardedVideoAdAdapter rewardedAdapter;
    private MediationInterstitialAdapter interstitialAdapter;
    private MediationInterstitialListener mediationInterstitialListener;
    private AdType adType;

    private enum AdType { VIDEO, INTERSTITIAL }

    private MaioEventForwarder() {
    }

    private class MaioReward implements RewardItem {
        private final String type;
        private final int amount;

        public MaioReward(String type, int amount) {
            this.type = type;
            this.amount = amount;
        }

        @Override
        public int getAmount() {
            return this.amount;
        }


        @Override
        public String getType() {
            return this.type;
        }
    }

    public static void initialize(Context context, String mediaId){
        instance._initialize(context, mediaId);
    }

    private void _initialize(Context context, String mediaId){
        MaioAds.init((Activity)context, mediaId, this);
    }
    public static boolean isInitialized(){
        return initialized;
    }

    public static void showVideo(String zoneId, MediationRewardedVideoAdAdapter adapter, MediationRewardedVideoAdListener listener) {
        instance._showVideo(zoneId, adapter, listener);
    }

    private void _showVideo(String zoneId, MediationRewardedVideoAdAdapter adapter, MediationRewardedVideoAdListener listener) {
        MaioAds.show(zoneId);
        this.rewardedAdapter = adapter;
        this.mediationRewardedVideoAdListener = listener;
        adType = AdType.VIDEO;
    }

    public static void showInterstitial(String zoneId, MediationInterstitialAdapter adapter, MediationInterstitialListener listener) {
        instance._showInterstitial(zoneId, adapter, listener);
    }

    private void _showInterstitial(String zoneId, MediationInterstitialAdapter adapter, MediationInterstitialListener listener) {
        MaioAds.show(zoneId);
        this.mediationInterstitialListener = listener;
        this.interstitialAdapter = adapter;
        adType = AdType.INTERSTITIAL;
    }

    private boolean _isVideo(){
        return (this.mediationRewardedVideoAdListener != null && adType == AdType.VIDEO);
    }

    private boolean _isInterstitial(){
        return (this.mediationInterstitialListener != null && adType == AdType.INTERSTITIAL);
    }

    @Override
    public void onInitialized() {
        this.initialized = true;
        if (_isVideo()) {
            this.mediationRewardedVideoAdListener.onInitializationSucceeded(rewardedAdapter);
        }
    }

    @Override
    public void onChangedCanShow(String zoneId, boolean newValue) {

    }

    @Override
    public void onOpenAd(String zoneId) {
        if (_isVideo()) {
            this.mediationRewardedVideoAdListener.onAdOpened(rewardedAdapter);
        }
        else if (_isInterstitial()) {
            this.mediationInterstitialListener.onAdOpened(interstitialAdapter);

        }
    }

    @Override
    public void onStartedAd(String zoneId) {
        if(_isVideo()) {
            this.mediationRewardedVideoAdListener.onVideoStarted(rewardedAdapter);
        }
    }

    @Override
    public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
        if (!skipped) {
            if(_isVideo()) {
                this.mediationRewardedVideoAdListener.onRewarded(rewardedAdapter, new MaioReward("", 1));
            }
        }
    }

    @Override
    public void onClosedAd(String zoneId) {
        if(_isVideo()) {
            this.mediationRewardedVideoAdListener.onAdClosed(rewardedAdapter);
        }
        else if(_isInterstitial()) {
            this.mediationInterstitialListener.onAdClosed(interstitialAdapter);
        }
    }

    @Override
    public void onClickedAd(String zoneId) {
        if(_isVideo()) {
            this.mediationRewardedVideoAdListener.onAdClicked(rewardedAdapter);
            this.mediationRewardedVideoAdListener.onAdLeftApplication(rewardedAdapter);
        }
        else if (_isInterstitial()) {
            this.mediationInterstitialListener.onAdClicked(interstitialAdapter);
        }
    }


    @Override
    public void onFailed(FailNotificationReason reason, String zoneId) {

    }
}