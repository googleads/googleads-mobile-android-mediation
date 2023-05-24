package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN;

import android.util.Log;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyRewardedVideoListener;
import com.ironsource.mediationsdk.logger.IronSourceError;

public class IronSourceRewardedAdListener implements ISDemandOnlyRewardedVideoListener {

  @Override
  public void onRewardedVideoAdLoadSuccess(String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad loaded for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      if (ironSourceRewardedAd.getMediationAdLoadCallback() != null) {
        ironSourceRewardedAd.setRewardedAdCallback(
            ironSourceRewardedAd.getMediationAdLoadCallback().onSuccess(ironSourceRewardedAd));
      }
    }
  }

  @Override
  public void onRewardedVideoAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
    AdError loadError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage =
        String.format(
            "IronSource failed to load rewarded ad for instance ID: %s. Error: %s",
            instanceId, loadError.getMessage());
    Log.e(TAG, errorMessage);
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      if (ironSourceRewardedAd.getMediationAdLoadCallback() != null) {
        ironSourceRewardedAd.getMediationAdLoadCallback().onFailure(loadError);
      }
    }

    IronSourceRewardedAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onRewardedVideoAdOpened(final String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad opened for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.onAdOpened();
        adCallBack.onVideoStart();
        adCallBack.reportAdImpression();
      }
    }
  }

  @Override
  public void onRewardedVideoAdClosed(String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad closed for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.onAdClosed();
      }
    }

    IronSourceRewardedAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onRewardedVideoAdRewarded(String instanceId) {
    final IronSourceRewardItem ironSourceRewardItem = new IronSourceRewardItem();
    Log.d(
        TAG,
        String.format(
            "IronSource rewarded ad received reward: %d %s, for instance ID: %s",
            ironSourceRewardItem.getAmount(), ironSourceRewardItem.getType(), instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.onVideoComplete();
        adCallBack.onUserEarnedReward(ironSourceRewardItem);
      }
    }
  }

  @Override
  public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
    AdError showError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage =
        String.format(
            "IronSource failed to show rewarded ad for instance ID: %s. Error: %s",
            instanceId, showError.getMessage());
    Log.e(TAG, errorMessage);
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      // Check currently showing instance existence.
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.onAdFailedToShow(showError);
      }
    }

    IronSourceRewardedAd.removeFromAvailableInstances(instanceId);
  }

  @Override
  public void onRewardedVideoAdClicked(String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad clicked for instance ID: %s", instanceId));
    IronSourceRewardedAd ironSourceRewardedAd =
        IronSourceRewardedAd.getFromAvailableInstances(instanceId);

    if (ironSourceRewardedAd != null) {
      MediationRewardedAdCallback adCallBack = ironSourceRewardedAd.getRewardedAdCallback();
      if (adCallBack != null) {
        adCallBack.reportAdClicked();
      }
    }
  }
}
