package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_AD_DISPLAY_FAILED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_INMOBI_NOT_INITIALIZED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.INMOBI_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.exceptions.SdkNotInitializedException;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import java.util.HashMap;
import java.util.Map;

public class InMobiRewardedAd implements MediationRewardedAd {

  private InMobiInterstitial mInMobiRewardedAd;

  private MediationRewardedAdConfiguration mRewardedAdConfiguration;
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mMediationAdLoadCallback;
  private MediationRewardedAdCallback mRewardedAdCallback;

  public InMobiRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback) {
    mRewardedAdConfiguration = mediationRewardedAdConfiguration;
    mMediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void load() {
    final Context context = mRewardedAdConfiguration.getContext();
    Bundle serverParameters = mRewardedAdConfiguration.getServerParameters();

    String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    if (TextUtils.isEmpty(accountID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Account ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadRewardAd(context, placementId, mMediationAdLoadCallback);
      }

      @Override
      public void onInitializeError(@NonNull AdError error) {
        Log.w(TAG, error.getMessage());
        if (mMediationAdLoadCallback != null) {
          mMediationAdLoadCallback.onFailure(error);
        }
      }
    });
  }

  // region MediationRewardedAd implementation.
  @Override
  public void showAd(Context context) {
    if (!mInMobiRewardedAd.isReady()) {
      AdError error = new AdError(ERROR_AD_NOT_READY,
          "InMobi Rewarded ad is not yet ready to be shown.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());

      if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onAdFailedToShow(error);
      }
      return;
    }

    mInMobiRewardedAd.show();
  }
  // endregion

  // region Rewarded adapter utility classes.
  private void createAndLoadRewardAd(Context context, long placementId,
      final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mMediationAdLoadCallback) {

    if (placementId <= 0L) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    try {
      mInMobiRewardedAd = new InMobiInterstitial(context, placementId,
          new InterstitialAdEventListener() {
            @Override
            public void onRewardsUnlocked(@NonNull InMobiInterstitial inMobiInterstitial,
                Map<Object, Object> rewards) {
              Log.d(TAG, "InMobi rewarded ad user earned a reward.");
              String rewardKey = "";
              String rewardStringValue = "";
              int rewardValue = 0;

              if (rewards != null) {
                for (Object reward : rewards.keySet()) {
                  rewardKey = reward.toString();
                  rewardStringValue = rewards.get(rewardKey).toString();
                  if (!TextUtils.isEmpty(rewardKey) &&
                      !TextUtils.isEmpty(rewardStringValue)) {
                    break;
                  }
                }
              }

              if (!TextUtils.isEmpty(rewardStringValue)) {
                try {
                  rewardValue = Integer.parseInt(rewardStringValue);
                } catch (NumberFormatException e) {
                  Log.w(TAG, "Expected an integer reward value. Got " +
                      rewardStringValue + " instead. Using reward value of 1.");
                  rewardValue = 1;
                }
              }

              if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onVideoComplete();
                mRewardedAdCallback.onUserEarnedReward(new InMobiReward(rewardKey, rewardValue));
              }
            }

            @Override
            public void onAdDisplayFailed(@NonNull InMobiInterstitial inMobiInterstitial) {
              AdError error = new AdError(ERROR_AD_DISPLAY_FAILED, "InMobi ad failed to show.",
                  ERROR_DOMAIN);
              Log.w(TAG, error.getMessage());
              if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdFailedToShow(error);
              }
            }

            @Override
            public void onAdWillDisplay(@NonNull InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi rewarded ad will be shown.");
            }

            @Override
            public void onAdDisplayed(@NonNull InMobiInterstitial inMobiInterstitial,
                @NonNull AdMetaInfo adMetaInfo) {
              Log.d(TAG, "InMobi rewarded ad has been shown.");
              if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdOpened();
                mRewardedAdCallback.onVideoStart();
                mRewardedAdCallback.reportAdImpression();
              }
            }

            @Override
            public void onAdDismissed(@NonNull InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi rewarded ad has been dismissed.");
              if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdClosed();
              }
            }

            @Override
            public void onAdClicked(@NonNull InMobiInterstitial inMobiInterstitial,
                Map<Object, Object> parameters) {
              Log.d(TAG, "InMobi rewarded ad has been clicked.");
              if (mRewardedAdCallback != null) {
                mRewardedAdCallback.reportAdClicked();
              }
            }

            @Override
            public void onAdLoadSucceeded(@NonNull InMobiInterstitial inMobiInterstitial,
                @NonNull AdMetaInfo adMetaInfo) {
              Log.d(TAG, "InMobi rewaded ad has been loaded.");
              if (mMediationAdLoadCallback != null) {
                mRewardedAdCallback =
                    mMediationAdLoadCallback.onSuccess(InMobiRewardedAd.this);
              }
            }

            @Override
            public void onAdLoadFailed(@NonNull InMobiInterstitial inMobiInterstitial,
                @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
              AdError error = new AdError(
                  InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
                  inMobiAdRequestStatus.getMessage(), INMOBI_SDK_ERROR_DOMAIN);
              Log.w(TAG, error.getMessage());
              if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(error);
              }
            }

            @Override
            public void onAdFetchSuccessful(@NonNull InMobiInterstitial inMobiInterstitial,
                @NonNull AdMetaInfo adMetaInfo) {
              Log.d(TAG, "InMobi rewarded ad fetched from server, "
                  + "but ad contents still need to be loaded.");
            }

            @Override
            public void onUserLeftApplication(@NonNull InMobiInterstitial inMobiInterstitial) {
              Log.d(TAG, "InMobi rewarded ad left application.");
            }

            @Override
            public void onRequestPayloadCreated(byte[] payload) {
              // No op.
            }

            @Override
            public void onRequestPayloadCreationFailed(@NonNull InMobiAdRequestStatus status) {
              // No op.
            }
          });
    } catch (SdkNotInitializedException exception) {
      AdError error = new AdError(ERROR_INMOBI_NOT_INITIALIZED, exception.getLocalizedMessage(),
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    Bundle extras = mRewardedAdConfiguration.getMediationExtras();
    HashMap<String, String> paramMap =
        InMobiAdapterUtils.createInMobiParameterMap(mRewardedAdConfiguration);
    mInMobiRewardedAd.setExtras(paramMap);
    InMobiAdapterUtils.setGlobalTargeting(mRewardedAdConfiguration, extras);
    mInMobiRewardedAd.load();
  }
  // endregion

}

class InMobiReward implements RewardItem {

  private String type;
  private int amount;

  InMobiReward(String type, int amount) {
    this.type = type;
    this.amount = amount;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public int getAmount() {
    return amount;
  }
}
