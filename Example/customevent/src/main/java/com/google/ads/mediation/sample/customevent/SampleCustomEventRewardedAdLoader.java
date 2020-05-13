package com.google.ads.mediation.sample.customevent;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleRewardedAd;
import com.google.ads.mediation.sample.sdk.SampleRewardedAdListener;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

public class SampleCustomEventRewardedAdLoader extends SampleRewardedAdListener implements
    MediationRewardedAd {

  /**
   * Represents a {@link SampleRewardedAd}.
   */
  private SampleRewardedAd sampleRewardedAd;

  /*
   * Configuration used to load SampleRewardedAd.
   */
  private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

  /**
   * A {@link MediationAdLoadCallback} that handles any callback when a Sample rewarded ad finishes
   * loading.
   */
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallBack;

  /**
   * Used to forward rewarded video ad events to the Google Mobile Ads SDK.
   */
  private MediationRewardedAdCallback rewardedAdCallback;

  public SampleCustomEventRewardedAdLoader(MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          adLoadCallback
  ) {
    this.mediationRewardedAdConfiguration = adConfiguration;
    this.mediationAdLoadCallBack = adLoadCallback;
  }

  public void load() {
    String adUnitId = mediationRewardedAdConfiguration.getServerParameters().getString(
        "ad_unit");
    if (TextUtils.isEmpty(adUnitId)) {
      mediationAdLoadCallBack.onFailure("Ad Unit ID is empty.");
      return;
    }
    SampleAdRequest request = new SampleAdRequest();
    sampleRewardedAd = new SampleRewardedAd(adUnitId);
    sampleRewardedAd.loadAd(request);
  }

  @Override
  public void onRewardedAdLoaded() {
    rewardedAdCallback = mediationAdLoadCallBack.onSuccess(this);
  }

  @Override
  public void onRewardedAdFailedToLoad(SampleErrorCode error) {
    mediationAdLoadCallBack.onFailure(error.toString());
  }

  @Override
  public void showAd(Context context) {
    if (!(context instanceof Activity)) {
      rewardedAdCallback.onAdFailedToShow(
          "An activity context is required to show Sample rewarded ad.");
      return;
    }
    Activity activity = (Activity) context;

    if (!sampleRewardedAd.isAdAvailable()) {
      rewardedAdCallback.onAdFailedToShow("No ads to show.");
      return;
    }
    sampleRewardedAd.showAd(activity);
  }

  @Override
  public void onAdRewarded(final String rewardType, final int amount) {
    RewardItem rewardItem =
        new RewardItem() {
          @Override
          public String getType() {
            return rewardType;
          }

          @Override
          public int getAmount() {
            return amount;
          }
        };
    rewardedAdCallback.onUserEarnedReward(rewardItem);
  }

  @Override
  public void onAdClicked() {
    rewardedAdCallback.reportAdClicked();
  }

  @Override
  public void onAdFullScreen() {
    rewardedAdCallback.onAdOpened();
    rewardedAdCallback.onVideoStart();
    rewardedAdCallback.reportAdImpression();
  }

  @Override
  public void onAdClosed() {
    rewardedAdCallback.onAdClosed();
  }

  @Override
  public void onAdCompleted() {
    rewardedAdCallback.onVideoComplete();
  }
}
