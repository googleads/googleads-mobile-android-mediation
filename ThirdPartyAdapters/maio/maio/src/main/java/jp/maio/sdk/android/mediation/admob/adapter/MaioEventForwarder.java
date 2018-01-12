package jp.maio.sdk.android.mediation.admob.adapter;

import android.app.Activity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.MaioAdsListenerInterface;

/**
 * MaioEventForwarder is used to forward maio Rewarded and Interstitial events to the Admob SDK.
 */
public class MaioEventForwarder implements MaioAdsListenerInterface {

    private static final MaioEventForwarder sInstance = new MaioEventForwarder();
    private static boolean sInitialized;
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    private MediationRewardedVideoAdAdapter mRewardedAdapter;
    private MediationInterstitialAdapter mInterstitialAdapter;
    private MediationInterstitialListener mMediationInterstitialListener;
    private AdType mAdType;

    private enum AdType {VIDEO, INTERSTITIAL}

    private MaioEventForwarder() {
    }

    /*
     * A {@link RewardItem} used to map maio rewards to Google's rewarded video ads rewards.
     */
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

    public static void initialize(Activity activity, String mediaId) {
        sInstance._initialize(activity, mediaId);
    }

    private void _initialize(Activity activity, String mediaId) {
        MaioAds.init(activity, mediaId, this);
    }

    public static boolean isInitialized() {
        return sInitialized;
    }

    public static void showVideo(String zoneId,
                                 MediationRewardedVideoAdAdapter adapter,
                                 MediationRewardedVideoAdListener listener) {
        sInstance._showVideo(zoneId, adapter, listener);
    }

    private void _showVideo(String zoneId,
                            MediationRewardedVideoAdAdapter adapter,
                            MediationRewardedVideoAdListener listener) {
        MaioAds.show(zoneId);
        this.mRewardedAdapter = adapter;
        this.mMediationRewardedVideoAdListener = listener;
        mAdType = AdType.VIDEO;
    }

    public static void showInterstitial(String zoneId,
                                        MediationInterstitialAdapter adapter,
                                        MediationInterstitialListener listener) {
        sInstance._showInterstitial(zoneId, adapter, listener);
    }

    private void _showInterstitial(String zoneId,
                                   MediationInterstitialAdapter adapter,
                                   MediationInterstitialListener listener) {
        MaioAds.show(zoneId);
        this.mMediationInterstitialListener = listener;
        this.mInterstitialAdapter = adapter;
        mAdType = AdType.INTERSTITIAL;
    }

    private boolean _isVideo() {
        return (this.mMediationRewardedVideoAdListener != null && mAdType == AdType.VIDEO);
    }

    private boolean _isInterstitial() {
        return (this.mMediationInterstitialListener != null && mAdType == AdType.INTERSTITIAL);
    }

    @Override
    public void onInitialized() {
        this.sInitialized = true;
        if (_isVideo()) {
            this.mMediationRewardedVideoAdListener.onInitializationSucceeded(mRewardedAdapter);
        }
    }

    @Override
    public void onChangedCanShow(String zoneId, boolean newValue) {

    }

    @Override
    public void onOpenAd(String zoneId) {
        if (_isVideo()) {
            this.mMediationRewardedVideoAdListener.onAdOpened(mRewardedAdapter);
        } else if (_isInterstitial()) {
            this.mMediationInterstitialListener.onAdOpened(mInterstitialAdapter);

        }
    }

    @Override
    public void onStartedAd(String zoneId) {
        if (_isVideo()) {
            this.mMediationRewardedVideoAdListener.onVideoStarted(mRewardedAdapter);
        }
    }

    @Override
    public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
        if (!skipped) {
            if (_isVideo()) {
                this.mMediationRewardedVideoAdListener
                        .onRewarded(mRewardedAdapter, new MaioReward("", 1));
            }
        }
    }

    @Override
    public void onClosedAd(String zoneId) {
        if (_isVideo()) {
            this.mMediationRewardedVideoAdListener.onAdClosed(mRewardedAdapter);
        } else if (_isInterstitial()) {
            this.mMediationInterstitialListener.onAdClosed(mInterstitialAdapter);
        }
    }

    @Override
    public void onClickedAd(String zoneId) {
        if (_isVideo()) {
            this.mMediationRewardedVideoAdListener.onAdClicked(mRewardedAdapter);
            this.mMediationRewardedVideoAdListener.onAdLeftApplication(mRewardedAdapter);
        } else if (_isInterstitial()) {
            this.mMediationInterstitialListener.onAdClicked(mInterstitialAdapter);
            this.mMediationInterstitialListener.onAdLeftApplication(mInterstitialAdapter);
        }
    }

    @Override
    public void onFailed(FailNotificationReason reason, String zoneId) {
        if (_isVideo()) {
            this.mMediationInterstitialListener
                    .onAdFailedToLoad(mInterstitialAdapter, getAdRequestErrorType(reason));
        } else if (_isInterstitial()) {
            this.mMediationRewardedVideoAdListener
                    .onAdFailedToLoad(mRewardedAdapter, getAdRequestErrorType(reason));
        }
    }

    /**
     * This method will return an error type that can be read by Google Mobile Ads SDK.
     *
     * @param reason FailNotificationReason type to be translated to Google Mobile Ads SDK readable
     *               error code.
     * @return Ad request error code.
     */
    private static int getAdRequestErrorType(FailNotificationReason reason) {
        switch (reason) {
            case NETWORK:
            case RESPONSE:
            case NETWORK_NOT_READY:
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case AD_STOCK_OUT:
                return AdRequest.ERROR_CODE_NO_FILL;
            case VIDEO:
            case UNKNOWN:
            default:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }
}