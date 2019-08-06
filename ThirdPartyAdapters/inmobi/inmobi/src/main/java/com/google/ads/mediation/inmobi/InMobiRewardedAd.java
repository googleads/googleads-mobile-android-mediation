package com.google.ads.mediation.inmobi;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.sdk.InMobiSdk;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.isSdkInitialized;

public class InMobiRewardedAd implements MediationRewardedAd {
    private InMobiInterstitial mInMobiRewardedAd;
    private SignalCallbacks mSignalCallbacks;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mMediationAdLoadCallback;
    private MediationRewardedAdCallback mRewardedAdCallback;
    private static HashMap<Long, WeakReference<InMobiRewardedAd>>
            mPlacementsInUse = new HashMap<>();
    public static final String TAG = InMobiRewardedAd.class.getName();

    public InMobiRewardedAd(Context context, final long placementId) {
        mInMobiRewardedAd = new InMobiInterstitial(context, placementId,
                new InterstitialAdEventListener() {

                    @Override
                    public void onRewardsUnlocked(InMobiInterstitial inMobiInterstitial,
                                                  Map<Object, Object> rewards) {
                        Log.d(TAG, "InMobi RewardedVideo onRewardsUnlocked.");
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
                        mPlacementsInUse.remove(placementId);
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
                        mPlacementsInUse.remove(placementId);
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
                        mRewardedAdCallback =
                                mMediationAdLoadCallback.onSuccess(InMobiRewardedAd.this);
                    }

                    @Override
                    public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                                               InMobiAdRequestStatus inMobiAdRequestStatus) {
                        String logMessage = "Failed to load ad from InMobi: "
                                + inMobiAdRequestStatus.getMessage();
                        Log.w(TAG, logMessage);
                        mMediationAdLoadCallback.onFailure(logMessage);
                        mPlacementsInUse.remove(placementId);
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
                    public void onRequestPayloadCreated(byte[] bytes) {
                        String payload = new String(bytes);
                        Log.d(TAG, "onRequestPayloadCreated: " + payload);
                        if (mSignalCallbacks != null) {
                            mSignalCallbacks.onSuccess(payload);
                        }
                    }

                    @Override
                    public void onRequestPayloadCreationFailed(InMobiAdRequestStatus status) {
                        String logMessage = status.getMessage();
                        Log.d(TAG, "onRequestPayloadCreationFailed: " + logMessage);
                        if (mSignalCallbacks != null) {
                            mSignalCallbacks.onFailure(logMessage);
                        }
                    }
                });
    }

    public void collectSignals(SignalCallbacks signalCallbacks) {
        mSignalCallbacks = signalCallbacks;
        mInMobiRewardedAd.getSignals();
    }

    public void load(MediationRewardedAdConfiguration adConfiguration,
                     MediationAdLoadCallback<MediationRewardedAd,
                             MediationRewardedAdCallback> callback) {
        Context context = adConfiguration.getContext();
        mMediationAdLoadCallback = callback;

        if (!(context instanceof Activity)) {
            String logMessage = "Failed to load ad from InMobi: "
                    + "InMobi SDK requires an Activity context to load ads.";
            Log.w(TAG, logMessage);
            callback.onFailure(logMessage);
            return;
        }

        Bundle serverParameters = adConfiguration.getServerParameters();
        Bundle extras = adConfiguration.getMediationExtras();

        if (!isSdkInitialized.get()) {
            String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
            if (TextUtils.isEmpty(accountID)) {
                String logMessage =
                        "Failed to load ad from InMobi: Missing or Invalid Account ID.";
                Log.w(TAG, logMessage);
                mMediationAdLoadCallback.onFailure(logMessage);
                return;
            }

            InMobiSdk.init(context, accountID, InMobiConsent.getConsentObj());
            isSdkInitialized.set(true);
        }

        String placementString =
                serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID);
        if (TextUtils.isEmpty(placementString)) {
            String logMessage = "Failed to load ad from InMobi: Missing or Invalid Placement ID.";
            Log.e(TAG, logMessage);
            mMediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        final long placementID;
        try {
            placementID = Long.parseLong(placementString.trim());
        } catch (NumberFormatException ex) {
            String logMessage = "Failed to load ad from InMobi: Invalid Placement ID.";
            Log.w(TAG, logMessage, ex);
            mMediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        if (mPlacementsInUse.containsKey(placementID)
                && mPlacementsInUse.get(placementID).get() != null) {
            String logMessage = "Failed to load ad from InMobi: "
                    + "An ad has already been requested for placement ID: " + placementID;
            Log.w(TAG, logMessage);
            mMediationAdLoadCallback.onFailure(logMessage);
            return;
        }
        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(adConfiguration);
        mInMobiRewardedAd.setExtras(paramMap);
        InMobiAdapterUtils.setGlobalTargeting(adConfiguration, extras);
        String bidResponse = adConfiguration.getBidResponse();
        if (TextUtils.isEmpty(bidResponse)) {
            mInMobiRewardedAd.load();
        } else {
            mInMobiRewardedAd.load(bidResponse.getBytes());
        }
    }

    //MediationRewardedAd implementation
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
