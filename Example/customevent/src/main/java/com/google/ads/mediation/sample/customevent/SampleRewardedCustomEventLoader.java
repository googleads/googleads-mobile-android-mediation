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

/** Rewarded custom event loader for the SampleSDK. */
public class SampleRewardedCustomEventLoader extends SampleRewardedAdListener
    implements MediationRewardedAd {

  /**
   * Represents a {@link SampleRewardedAd}.
   */
  private SampleRewardedAd sampleRewardedAd;

  /** Configuration for requesting the rewarded ad from the third party network. */
  private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

  /**
   * A {@link MediationAdLoadCallback} that handles any callback when a Sample rewarded ad finishes
   * loading.
   */
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  /**
   * Used to forward rewarded video ad events to the Google Mobile Ads SDK.
   */
  private MediationRewardedAdCallback rewardedAdCallback;

  public SampleRewardedCustomEventLoader(
      MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
    this.mediationRewardedAdConfiguration = adConfiguration;
    this.mediationAdLoadCallback = adLoadCallback;
  }

  /** Loads the rewarded ad from the third party ad network. */
  public void loadAd() {
    // All custom events have a server parameter named "parameter" that returns back the parameter
    // entered into the AdMob UI when defining the custom event.
    String serverParameter =
        mediationRewardedAdConfiguration.getServerParameters().getString("parameter");
    if (TextUtils.isEmpty(serverParameter)) {
      mediationAdLoadCallback.onFailure(SampleCustomEventError.createCustomEventNoAdIdError());
      return;
    }

    SampleAdRequest request = new SampleAdRequest();
    sampleRewardedAd = new SampleRewardedAd(serverParameter);
    sampleRewardedAd.setListener(this);
    sampleRewardedAd.loadAd(request);
  }

  @Override
  public void onRewardedAdLoaded() {
    rewardedAdCallback = mediationAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onRewardedAdFailedToLoad(SampleErrorCode errorCode) {
    mediationAdLoadCallback.onFailure(SampleCustomEventError.createSampleSdkError(errorCode));
  }

  @Override
  public void showAd(Context context) {
    if (!(context instanceof Activity)) {
      rewardedAdCallback.onAdFailedToShow(
          SampleCustomEventError.createCustomEventNoActivityContextError());
      return;
    }
    Activity activity = (Activity) context;

    if (!sampleRewardedAd.isAdAvailable()) {
      rewardedAdCallback.onAdFailedToShow(
          SampleCustomEventError.createCustomEventAdNotAvailableError());
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
