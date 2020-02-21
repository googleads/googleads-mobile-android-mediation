package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.isSdkInitialized;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.exceptions.SdkNotInitializedException;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.unification.sdk.InitializationStatus;
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
        Context context = mRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)) {
            String logMessage = "Failed to load ad from InMobi: "
                + "InMobi SDK requires an Activity context to load ads.";
            Log.w(TAG, logMessage);
            mMediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        Bundle serverParameters = mRewardedAdConfiguration.getServerParameters();

        if (!isSdkInitialized.get()) {
            String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
            if (TextUtils.isEmpty(accountID)) {
                String logMessage =
                    "Failed to initialize InMobi SDK: Missing or invalid Account ID.";
                Log.w(TAG, logMessage);
                mMediationAdLoadCallback.onFailure(logMessage);
                return;
            }

            @InitializationStatus String status =
                InMobiSdk.init(context, accountID, InMobiConsent.getConsentObj());
            if (!status.equals(InitializationStatus.SUCCESS)) {
                Log.e(TAG, "Failed to initialize InMobi SDK: " + status);
                mMediationAdLoadCallback.onFailure(status);
                return;
            }

            isSdkInitialized.set(true);
        }

        final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
        if (placementId <= 0L) {
            String logMessage = "Failed to request InMobi rewarded ad.";
            Log.e(TAG, logMessage);
            mMediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        try {
            mInMobiRewardedAd = new InMobiInterstitial(context, placementId,
                new InterstitialAdEventListener() {
                    @Override
                    public void onRewardsUnlocked(InMobiInterstitial inMobiInterstitial,
                        Map<Object, Object> rewards) {
                        Log.d(TAG, "InMobi Rewarded Video onRewardsUnlocked.");
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
                                Log.w(TAG, "Expected an integer reward value. Got "  +
                                    rewardStringValue  + " instead. Using reward value of 1.");
                                rewardValue = 1;
                            }
                        }

                        if (mRewardedAdCallback != null) {
                            mRewardedAdCallback.onVideoComplete();
                            mRewardedAdCallback.onUserEarnedReward(
                                new com.google.ads.mediation.inmobi.InMobiReward(rewardKey,
                                    rewardValue));
                        }
                    }

                    @Override
                    public void onAdDisplayFailed(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdDisplayFailed");
                        if (mRewardedAdCallback != null) {
                            mRewardedAdCallback.onAdFailedToShow("Internal Error.");
                        }
                    }

                    @Override
                    public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdWillDisplay");
                    }

                    @Override
                    public void onAdDisplayed(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdDisplayed");
                        if (mRewardedAdCallback != null) {
                            mRewardedAdCallback.onAdOpened();
                            mRewardedAdCallback.onVideoStart();
                            mRewardedAdCallback.reportAdImpression();
                        }
                    }

                    @Override
                    public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdDismissed");
                        if (mRewardedAdCallback != null) {
                            mRewardedAdCallback.onAdClosed();
                        }
                    }

                    @Override
                    public void onAdClicked(InMobiInterstitial inMobiInterstitial,
                        Map<Object, Object> map) {
                        Log.d(TAG, "onAdClicked");
                        if (mRewardedAdCallback != null) {
                            mRewardedAdCallback.reportAdClicked();
                        }
                    }

                    @Override
                    public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdLoadSucceeded");
                        if (mMediationAdLoadCallback != null) {
                            mRewardedAdCallback =
                                mMediationAdLoadCallback.onSuccess(InMobiRewardedAd.this);
                        }
                    }

                    @Override
                    public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                        InMobiAdRequestStatus inMobiAdRequestStatus) {
                        String logMessage = "Failed to load ad from InMobi: "
                            + inMobiAdRequestStatus.getMessage();
                        Log.e(TAG, logMessage);
                        if (mMediationAdLoadCallback != null) {
                            mMediationAdLoadCallback.onFailure(logMessage);
                        }
                    }

                    @Override
                    public void onAdReceived(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "InMobi Ad server responded with an Ad.");
                    }

                    @Override
                    public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onUserLeftApplication");
                    }

                    @Override
                    public void onRequestPayloadCreated(byte[] payload) {
                        // No op.
                    }

                    @Override
                    public void onRequestPayloadCreationFailed(InMobiAdRequestStatus status) {
                        // No op.
                    }
                });
        } catch (SdkNotInitializedException exception) {
            String logMessage = "Failed to request InMobi rewarded ad.";
            Log.e(TAG, logMessage, exception);
            mMediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        Bundle extras = mRewardedAdConfiguration.getMediationExtras();
        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(mRewardedAdConfiguration);
        mInMobiRewardedAd.setExtras(paramMap);
        InMobiAdapterUtils.setGlobalTargeting(mRewardedAdConfiguration, extras);
        mInMobiRewardedAd.load();
    }

    // MediationRewardedAd implementation
    @Override
    public void showAd(Context context) {
        if (mInMobiRewardedAd.isReady()) {
            mInMobiRewardedAd.show();
        }
    }
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
