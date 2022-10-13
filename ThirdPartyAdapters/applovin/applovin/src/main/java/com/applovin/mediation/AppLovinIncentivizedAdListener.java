package com.applovin.mediation;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

import androidx.annotation.NonNull;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.google.ads.mediation.applovin.AppLovinMediationAdapter;
import com.google.ads.mediation.applovin.AppLovinRewardItem;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import java.util.Map;

/**
 * The {@link AppLovinIncentivizedAdListener} class is used to forward Rewarded ad events from the
 * AppLovin SDK to the Google Mobile Ads SDK.
 */
public class AppLovinIncentivizedAdListener
    implements AppLovinAdRewardListener,
    AppLovinAdDisplayListener,
    AppLovinAdClickListener,
    AppLovinAdVideoPlaybackListener {

  private final MediationRewardedAdCallback rewardedAdCallback;

  private boolean fullyWatched;
  private AppLovinRewardItem rewardItem;
  private final String zoneId;

  public AppLovinIncentivizedAdListener(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationRewardedAdCallback rewardedAdCallback) {
    zoneId = AppLovinUtils.retrieveZoneId(adConfiguration.getServerParameters());
    this.rewardedAdCallback = rewardedAdCallback;
  }

  // Ad Display Listener.
  @Override
  public void adDisplayed(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Rewarded video displayed.");
    rewardedAdCallback.onAdOpened();
    rewardedAdCallback.reportAdImpression();
  }

  @Override
  public void adHidden(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Rewarded video dismissed.");
    AppLovinMediationAdapter.INCENTIVIZED_ADS.remove(zoneId);
    if (fullyWatched) {
      rewardedAdCallback.onUserEarnedReward(rewardItem);
    }

    rewardedAdCallback.onAdClosed();
  }

  // Ad Click Listener.
  @Override
  public void adClicked(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Rewarded video clicked.");
    rewardedAdCallback.reportAdClicked();
  }

  // Video Playback Listener.
  @Override
  public void videoPlaybackBegan(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Rewarded video playback began.");
    rewardedAdCallback.onVideoStart();
  }

  @Override
  public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched) {
    ApplovinAdapter.log(
        DEBUG, "Rewarded video playback ended at playback percent: " + percentViewed + "%.");
    this.fullyWatched = fullyWatched;
    if (fullyWatched) {
      rewardedAdCallback.onVideoComplete();
    }
  }

  // Reward Listener.
  @Override
  public void userOverQuota(AppLovinAd ad, Map<String, String> response) {
    ApplovinAdapter.log(
        ERROR,
        "Rewarded video validation request for ad did exceed quota with response: " + response);
  }

  @Override
  public void validationRequestFailed(AppLovinAd ad, int code) {
    ApplovinAdapter.log(
        ERROR, "Rewarded video validation request for ad failed with error code: " + code);
  }

  @Override
  public void userRewardRejected(AppLovinAd ad, Map<String, String> response) {
    ApplovinAdapter.log(
        ERROR, "Rewarded video validation request was rejected with response: " + response);
  }

  @Override
  public void userRewardVerified(AppLovinAd ad, Map<String, String> response) {
    final String currency = response.get("currency");
    final String amountStr = response.get("amount");

    // AppLovin returns amount as double.
    final int amount = (int) Double.parseDouble(amountStr);

    ApplovinAdapter.log(DEBUG, "Rewarded " + amount + " " + currency);
    rewardItem = new AppLovinRewardItem(amount, currency);
  }
}
