package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_AD_DISPLAY_FAILED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_INMOBI_NOT_INITIALIZED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.INMOBI_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.inmobi.InMobiAdapter.TAG;

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

import java.util.HashMap;
import java.util.Map;

public class InMobiInterstitialAd implements MediationInterstitialAd {

    private InMobiInterstitial mAdInterstitial;
    private MediationInterstitialAdConfiguration mMediationInterstitialAdConfiguration;
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationAdLoadCallback;
    private MediationInterstitialAdCallback mInterstitialAdCallback;
    private static Boolean sDisableHardwareFlag = false;

    public InMobiInterstitialAd(MediationInterstitialAdConfiguration mMediationInterstitialAdConfiguration,
                                MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationAdLoadCallback) {
        this.mMediationInterstitialAdConfiguration = mMediationInterstitialAdConfiguration;
        this.mMediationAdLoadCallback = mMediationAdLoadCallback;
    }

    public void load() {
        final Context context = mMediationInterstitialAdConfiguration.getContext();
        Bundle serverParameters = mMediationInterstitialAdConfiguration.getServerParameters();

        String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
        if (TextUtils.isEmpty(accountID)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Account ID.",
                    ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationAdLoadCallback.onFailure(error);
            return;
        }

        final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
        InMobiInitializer.getInstance().init(context, accountID, new InMobiInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                createAndLoadInterstitialAd(context, placementId);
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

    private void createAndLoadInterstitialAd(Context context, long placementId) {

        if (placementId <= 0L) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or Invalid Placement ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationAdLoadCallback.onFailure(error);
            return;
        }

        try {
            mAdInterstitial = new InMobiInterstitial(context, placementId,
                    new InterstitialAdEventListener() {

                        @Override
                        public void onUserLeftApplication(@NonNull InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "InMobi interstitial left application.");
                            if (mInterstitialAdCallback != null) {
                                mInterstitialAdCallback.onAdLeftApplication();
                            }
                        }

                        @Override
                        public void onRewardsUnlocked(@NonNull InMobiInterstitial inMobiInterstitial,
                                                      Map<Object, Object> rewards) {
                            // No op.
                        }

                        @Override
                        public void onAdDisplayFailed(@NonNull InMobiInterstitial inMobiInterstitial) {
                            AdError error = new AdError(ERROR_AD_DISPLAY_FAILED, "InMobi ad failed to show.",
                                    ERROR_DOMAIN);
                            Log.w(TAG, error.getMessage());
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
                            if (mMediationAdLoadCallback != null) {
                                mInterstitialAdCallback =
                                        mMediationAdLoadCallback.onSuccess(InMobiInterstitialAd.this);
                            }
                        }

                        @Override
                        public void onAdLoadFailed(@NonNull InMobiInterstitial inMobiInterstitial,
                                                   @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
                            AdError error = new AdError(InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
                                    inMobiAdRequestStatus.getMessage(), INMOBI_SDK_ERROR_DOMAIN);
                            Log.w(TAG, error.getMessage());
                            if (mMediationAdLoadCallback != null) {
                                mMediationAdLoadCallback.onFailure(error);
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
                            if (mInterstitialAdCallback != null) {
                                mInterstitialAdCallback.onAdOpened();
                            }
                        }

                        @Override
                        public void onAdDismissed(@NonNull InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "InMobi interstitial ad has been dismissed.");
                            if (mInterstitialAdCallback != null) {
                                mInterstitialAdCallback.onAdClosed();
                            }
                        }

                        @Override
                        public void onAdClicked(@NonNull InMobiInterstitial inMobiInterstitial,
                                                Map<Object, Object> clickParameters) {
                            Log.d(TAG, "InMobi interstitial ad has been clicked.");
                            if (mInterstitialAdCallback != null) {
                                mInterstitialAdCallback.reportAdClicked();
                            }
                        }

                        @Override
                        public void onAdImpression(@NonNull InMobiInterstitial inMobiInterstitial) {
                            Log.d(TAG, "InMobi interstitial ad has logged an impression.");
                            if (mInterstitialAdCallback != null) {
                                mInterstitialAdCallback.reportAdImpression();
                            }
                        }
                    });
        } catch (SdkNotInitializedException exception) {
            AdError error = new AdError(ERROR_INMOBI_NOT_INITIALIZED, exception.getLocalizedMessage(),
                    ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationAdLoadCallback.onFailure(error);
            return;
        }

        if (mMediationInterstitialAdConfiguration.getMediationExtras().keySet() != null) {
            mAdInterstitial.setKeywords(TextUtils.join(", ", mMediationInterstitialAdConfiguration.getMediationExtras().keySet()));
        }

        //Update Age Restricted User
        InMobiAdapterUtils.updateAgeRestrictedUser(mMediationInterstitialAdConfiguration);

        Bundle extras = mMediationInterstitialAdConfiguration.getMediationExtras();
        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(mMediationInterstitialAdConfiguration);
        mAdInterstitial.setExtras(paramMap);

        if (sDisableHardwareFlag) {
            mAdInterstitial.disableHardwareAcceleration();
        }

        InMobiAdapterUtils.configureGlobalTargeting(extras);
        mAdInterstitial.load();
    }

    @Override
    public void showAd(@NonNull Context context) {
        if (!mAdInterstitial.isReady()) {
            AdError error = new AdError(ERROR_AD_NOT_READY,
                    "InMobi Rewarded ad is not yet ready to be shown.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            return;
        }

        mAdInterstitial.show();
    }
}
