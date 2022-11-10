package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_AD_DISPLAY_FAILED;
import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_INMOBI_NOT_INITIALIZED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.exceptions.SdkNotInitializedException;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.sdk.InMobiSdk;

import java.util.HashMap;
import java.util.Map;

public class InMobiInterstitialAd extends InterstitialAdEventListener implements MediationInterstitialAd {

    private InMobiInterstitial adInterstitial;
    private MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration;
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback;
    private MediationInterstitialAdCallback interstitialAdCallback;

    public InMobiInterstitialAd(@NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
                                @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
        this.mediationInterstitialAdConfiguration = mediationInterstitialAdConfiguration;
        this.mediationAdLoadCallback = mediationAdLoadCallback;
    }

    public void loadAd() {
        final Context context = mediationInterstitialAdConfiguration.getContext();
        Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();

        String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
        final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
        AdError error = InMobiAdapterUtils.validateInMobiAdLoadParams(accountID, placementId);
        if (error != null) {
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        InMobiInitializer.getInstance().init(context, accountID, new InMobiInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                createAndLoadInterstitialAd(context, placementId);
            }

            @Override
            public void onInitializeError(@NonNull AdError error) {
                Log.e(TAG, error.toString());
                if (mediationAdLoadCallback != null) {
                    mediationAdLoadCallback.onFailure(error);
                }
            }
        });
    }

    private void createAndLoadInterstitialAd(Context context, long placementId) {

        if (!InMobiSdk.isSDKInitialized()) {
            AdError error = InMobiConstants.createAdapterError(ERROR_INMOBI_NOT_INITIALIZED, "Please initialize the SDK before creating InMobiInterstitial.");
            Log.e(TAG, error.toString());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        adInterstitial = new InMobiInterstitial(context, placementId, InMobiInterstitialAd.this);

        if (mediationInterstitialAdConfiguration.getMediationExtras().keySet() != null) {
            adInterstitial.setKeywords(TextUtils.join(", ", mediationInterstitialAdConfiguration.getMediationExtras().keySet()));
        }

        //Update Age Restricted User
        InMobiAdapterUtils.updateAgeRestrictedUser(mediationInterstitialAdConfiguration);

        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(mediationInterstitialAdConfiguration);
        adInterstitial.setExtras(paramMap);

        InMobiAdapterUtils.configureGlobalTargeting(mediationInterstitialAdConfiguration
                .getMediationExtras());
        adInterstitial.load();
    }

    @Override
    public void showAd(@NonNull Context context) {
        if (!adInterstitial.isReady()) {
            AdError error = InMobiConstants.createAdapterError(ERROR_AD_NOT_READY,
                    "InMobi Rewarded ad is not yet ready to be shown.");
            Log.e(TAG, error.toString());
            return;
        }

        adInterstitial.show();
    }

    @Override
    public void onUserLeftApplication(@NonNull InMobiInterstitial inMobiInterstitial) {
        Log.d(TAG, "InMobi interstitial left application.");
        if (interstitialAdCallback != null) {
            interstitialAdCallback.onAdLeftApplication();
        }
    }

    @Override
    public void onRewardsUnlocked(@NonNull InMobiInterstitial inMobiInterstitial,
                                  Map<Object, Object> rewards) {
        // Google Mobile Ads SDK doesn't have a matching event.
    }

    @Override
    public void onAdDisplayFailed(@NonNull InMobiInterstitial inMobiInterstitial) {
        AdError error = InMobiConstants.createAdapterError(ERROR_AD_DISPLAY_FAILED, "InMobi SDK failed to display an interstitial ad.");
        Log.e(TAG, error.toString());
    }

    @Override
    public void onAdWillDisplay(@NonNull InMobiInterstitial inMobiInterstitial) {
        Log.d(TAG, "InMobi interstitial ad will be shown.");
        // Using onAdDisplayed to send the onAdOpened callback.
    }

    @Override
    public void onAdLoadSucceeded(@NonNull InMobiInterstitial inMobiInterstitial,
                                  @NonNull AdMetaInfo adMetaInfo) {
        Log.d(TAG, "InMobi interstitial ad has been loaded.");
        if (mediationAdLoadCallback != null) {
            interstitialAdCallback =
                    mediationAdLoadCallback.onSuccess(InMobiInterstitialAd.this);
        }
    }

    @Override
    public void onAdLoadFailed(@NonNull InMobiInterstitial inMobiInterstitial,
                               @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
        AdError error = InMobiConstants.createSdkError(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
                inMobiAdRequestStatus.getMessage());
        Log.e(TAG, error.toString());
        if (mediationAdLoadCallback != null) {
            mediationAdLoadCallback.onFailure(error);
        }
    }

    @Override
    public void onAdFetchSuccessful(@NonNull InMobiInterstitial inMobiInterstitial,
                                    @NonNull AdMetaInfo adMetaInfo) {
        Log.d(TAG, "InMobi interstitial ad fetched from server, "
                + "but ad contents still need to be loaded.");
    }

    @Override
    public void onAdDisplayed(@NonNull InMobiInterstitial inMobiInterstitial,
                              @NonNull AdMetaInfo adMetaInfo) {
        Log.d(TAG, "InMobi interstitial has been shown.");
        if (interstitialAdCallback != null) {
            interstitialAdCallback.onAdOpened();
        }
    }

    @Override
    public void onAdDismissed(@NonNull InMobiInterstitial inMobiInterstitial) {
        Log.d(TAG, "InMobi interstitial ad has been dismissed.");
        if (interstitialAdCallback != null) {
            interstitialAdCallback.onAdClosed();
        }
    }

    @Override
    public void onAdClicked(@NonNull InMobiInterstitial inMobiInterstitial,
                            Map<Object, Object> clickParameters) {
        Log.d(TAG, "InMobi interstitial ad has been clicked.");
        if (interstitialAdCallback != null) {
            interstitialAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onAdImpression(@NonNull InMobiInterstitial inMobiInterstitial) {
        Log.d(TAG, "InMobi interstitial ad has logged an impression.");
        if (interstitialAdCallback != null) {
            interstitialAdCallback.reportAdImpression();
        }
    }
}
