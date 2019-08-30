package com.applovin.mediation;

import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.ads.mediation.applovin.AppLovinMediationAdapter;
import com.google.ads.mediation.applovin.AppLovinRewardItem;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

import java.util.Map;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/*
 * The {@link AppLovinIncentivizedAdListener} class is used to forward Rewarded ad events from
 * the AppLovin SDK to the Google Mobile Ads SDK.
 */
public class AppLovinIncentivizedAdListener
        implements AppLovinAdRewardListener, AppLovinAdDisplayListener,
        AppLovinAdClickListener, AppLovinAdVideoPlaybackListener {

    private MediationRewardedAdCallback mRewardedAdCallback;

    private boolean mFullyWatched;
    private AppLovinRewardItem mRewardItem;
    private String mZoneId;

    public AppLovinIncentivizedAdListener(MediationRewardedAdConfiguration adConfiguration,
                                          MediationRewardedAdCallback mRewardedAdCallback) {
        mZoneId = AppLovinUtils.retrieveZoneId(adConfiguration.getServerParameters());
        this.mRewardedAdCallback = mRewardedAdCallback;
    }

    // Ad Display Listener.
    @Override
    public void adDisplayed(AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, "Rewarded video displayed");
        mRewardedAdCallback.onAdOpened();
        mRewardedAdCallback.reportAdImpression();
    }

    @Override
    public void adHidden(AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, "Rewarded video dismissed");
        AppLovinMediationAdapter.INCENTIVIZED_ADS.remove(mZoneId);
        if (mFullyWatched) {
            mRewardedAdCallback.onUserEarnedReward(mRewardItem);
        }

        mRewardedAdCallback.onAdClosed();

    }

    // Ad Click Listener.
    @Override
    public void adClicked(AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, "Rewarded video clicked");
        mRewardedAdCallback.reportAdClicked();
    }

    // Video Playback Listener.
    @Override
    public void videoPlaybackBegan(AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, "Rewarded video playback began");
        mRewardedAdCallback.onVideoStart();
    }

    @Override
    public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched) {
        ApplovinAdapter.log(DEBUG, "Rewarded video playback ended at playback percent: "
                + percentViewed + "%");
        mFullyWatched = fullyWatched;
        if (fullyWatched) {
            mRewardedAdCallback.onVideoComplete();
        }
    }

    // Reward Listener.
    @Override
    public void userOverQuota(AppLovinAd ad, Map<String, String> response) {
        ApplovinAdapter.log(ERROR, "Rewarded video validation request for ad did exceed quota with"
                + " response: " + response);
    }

    @Override
    public void validationRequestFailed(AppLovinAd ad, int code) {
        ApplovinAdapter.log(ERROR, "Rewarded video validation request for ad failed with error"
                + " code: " + code);
    }

    @Override
    public void userRewardRejected(AppLovinAd ad, Map<String, String> response) {
        ApplovinAdapter.log(ERROR, "Rewarded video validation request was rejected with response: "
                + response);
    }

    @Override
    public void userDeclinedToViewAd(AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, "User declined to view rewarded video");
    }

    @Override
    public void userRewardVerified(AppLovinAd ad, Map<String, String> response) {
        final String currency = response.get("currency");
        final String amountStr = response.get("amount");

        // AppLovin returns amount as double.
        final int amount = (int) Double.parseDouble(amountStr);

        ApplovinAdapter.log(DEBUG, "Rewarded " + amount + " " + currency);
        mRewardItem = new AppLovinRewardItem(amount, currency);
    }
}
