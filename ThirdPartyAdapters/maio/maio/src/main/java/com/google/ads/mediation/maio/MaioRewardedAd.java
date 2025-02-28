package com.google.ads.mediation.maio;

import static com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.maio.MaioMediationAdapter.TAG;
import static com.google.ads.mediation.maio.MaioMediationAdapter.getAdError;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager;
import jp.maio.sdk.android.v2.request.MaioRequest;
import jp.maio.sdk.android.v2.rewarddata.RewardData;
import jp.maio.sdk.android.v2.rewarded.IRewardedLoadCallback;
import jp.maio.sdk.android.v2.rewarded.IRewardedShowCallback;
import jp.maio.sdk.android.v2.rewarded.Rewarded;

/**
 * The {@link MaioRewardedAd} is used to load maio ads and mediate the callbacks between Google
 * Mobile Ads SDK and the maio SDK.
 *
 * <p><b>Note:</b> This class is not thread-safe.
 */
public class MaioRewardedAd implements MediationRewardedAd {
  private Rewarded maioRewarded;
  private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;
  private MediationRewardedAdCallback rewardedAdCallback;
  protected String mediaID;
  protected String zoneID;

  MaioRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  public void loadAd() {
    Context context = mediationRewardedAdConfiguration.getContext();

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    mediaID = serverParameters.getString(MaioAdsManager.KEY_MEDIA_ID);
    if (TextUtils.isEmpty(mediaID)) {
      AdError error =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    zoneID = serverParameters.getString(MaioAdsManager.KEY_ZONE_ID);
    if (TextUtils.isEmpty(zoneID)) {
      AdError error =
          new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    maioRewarded =
        Rewarded.loadAd(
            new MaioRequest(
                zoneID, mediationRewardedAdConfiguration.isTestRequest(), /* bidData= */ ""),
            context,
            new IRewardedLoadCallback() {
              @Override
              public void loaded(@NonNull Rewarded rewarded) {
                if (adLoadCallback != null) {
                  rewardedAdCallback = adLoadCallback.onSuccess(MaioRewardedAd.this);
                }
              }

              @Override
              public void failed(@NonNull Rewarded rewarded, int errorCode) {
                AdError error = getAdError(errorCode);
                Log.w(TAG, error.getMessage());
                if (adLoadCallback != null) {
                  adLoadCallback.onFailure(error);
                }
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    maioRewarded.show(
        context,
        new IRewardedShowCallback() {
          @Override
          public void opened(@NonNull Rewarded rewarded) {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdOpened();
              rewardedAdCallback.onVideoStart();
              rewardedAdCallback.reportAdImpression();
            }
          }

          @Override
          public void closed(@NonNull Rewarded rewarded) {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdClosed();
              rewardedAdCallback.onVideoComplete();
            }
          }

          @Override
          public void clicked(@NonNull Rewarded rewarded) {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.reportAdClicked();
            }
          }

          @Override
          public void rewarded(@NonNull Rewarded rewarded, @NonNull RewardData rewardData) {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onUserEarnedReward();
            }
          }

          @Override
          public void failed(@NonNull Rewarded rewarded, int errorCode) {
            AdError error = getAdError(errorCode);
            Log.w(TAG, error.getMessage());
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdFailedToShow(error);
            }
          }
        });
  }
}
