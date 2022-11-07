package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_INMOBI_NOT_INITIALIZED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.exceptions.SdkNotInitializedException;
import com.inmobi.ads.listeners.NativeAdEventListener;
import com.inmobi.ads.listeners.VideoEventListener;
import com.inmobi.sdk.InMobiSdk;

import java.util.HashMap;

public class InMobiNativeAd extends NativeAdEventListener {

    MediationNativeAdConfiguration mediationNativeAdConfiguration;
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback;
    private InMobiNative adNative;
    public MediationNativeAdCallback mediationNativeAdCallback;

    public InMobiNativeAd(MediationNativeAdConfiguration mediationNativeAdConfiguration,
                          MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback) {
        this.mediationNativeAdConfiguration = mediationNativeAdConfiguration;
        this.mediationAdLoadCallback = mediationAdLoadCallback;
    }

    public void loadAd() {
        final Context context = mediationNativeAdConfiguration.getContext();
        Bundle serverParameters = mediationNativeAdConfiguration.getServerParameters();

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
                createAndLoadNativeAd(context, placementId);
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

    private void createAndLoadNativeAd(Context context, long placementId) {

        if(!InMobiSdk.isSDKInitialized()){
            AdError error = InMobiConstants.createAdapterError(ERROR_INMOBI_NOT_INITIALIZED, "Please initialize the SDK before creating InMobiNative.");
            Log.e(TAG, error.toString());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        adNative = new InMobiNative(context, placementId, InMobiNativeAd.this);

        adNative.setVideoEventListener(new VideoEventListener() {
            @Override
            public void onVideoCompleted(final InMobiNative inMobiNative) {
                super.onVideoCompleted(inMobiNative);
                Log.d(TAG, "InMobi native video ad completed.");
                if (mediationNativeAdCallback != null) {
                    mediationNativeAdCallback.onVideoComplete();
                }
            }

            @Override
            public void onVideoSkipped(final InMobiNative inMobiNative) {
                super.onVideoSkipped(inMobiNative);
                Log.d(TAG, "InMobi native video ad skipped.");
            }
        });

        // Setting mediation key words to native ad object
        if (null != mediationNativeAdConfiguration.getMediationExtras().keySet()) {
            adNative.setKeywords(TextUtils.join(", ", mediationNativeAdConfiguration.getMediationExtras().keySet()));
        }

        //Update Age Restricted User
        InMobiAdapterUtils.updateAgeRestrictedUser(mediationNativeAdConfiguration);

        /*
         *  Extra request params : Add any other extra request params here
         *  #1. Explicitly setting mediation supply parameter to AdMob
         *  #2. Landing url
         */
        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(mediationNativeAdConfiguration);
        adNative.setExtras(paramMap);

        InMobiAdapterUtils.configureGlobalTargeting(mediationNativeAdConfiguration.getMediationExtras());
        adNative.load();
    }

    @Override
    public void onAdLoadSucceeded(@NonNull final InMobiNative imNativeAd,
                                  @NonNull AdMetaInfo adMetaInfo) {
        Log.d(TAG, "InMobi native ad has been loaded.");

        // This setting decides whether to download images or not.
        NativeAdOptions nativeAdOptions = mediationNativeAdConfiguration.getNativeAdOptions();
        boolean mIsOnlyUrl = false;

        if (null != nativeAdOptions) {
            mIsOnlyUrl = nativeAdOptions.shouldReturnUrlsForImageAssets();
        }

        InMobiUnifiedNativeAdMapper inMobiUnifiedNativeAdMapper =
                new InMobiUnifiedNativeAdMapper(imNativeAd, mIsOnlyUrl, mediationAdLoadCallback, InMobiNativeAd.this);
        inMobiUnifiedNativeAdMapper.mapUnifiedNativeAd(mediationNativeAdConfiguration.getContext());
    }

    @Override
    public void onAdLoadFailed(@NonNull InMobiNative inMobiNative,
                               @NonNull InMobiAdRequestStatus requestStatus) {
        AdError error = InMobiConstants.createSdkError(InMobiAdapterUtils.getMediationErrorCode(requestStatus),
                requestStatus.getMessage());
        Log.e(TAG, error.toString());
        if (mediationNativeAdCallback != null) {
            mediationAdLoadCallback.onFailure(error);
        }
    }

    @Override
    public void onAdFullScreenDismissed(@NonNull InMobiNative inMobiNative) {
        Log.d(TAG, "InMobi native ad has been dismissed.");
        if (mediationNativeAdCallback != null) {
            mediationNativeAdCallback.onAdClosed();
        }
    }

    @Override
    public void onAdFullScreenWillDisplay(@NonNull InMobiNative inMobiNative) {
        // Google Mobile Ads SDK doesn't have a matching event.
    }

    @Override
    public void onAdFullScreenDisplayed(@NonNull InMobiNative inMobiNative) {
        Log.d(TAG, "InMobi native ad opened.");
        if (mediationNativeAdCallback != null) {
            mediationNativeAdCallback.onAdOpened();
        }
    }

    @Override
    public void onUserWillLeaveApplication(@NonNull InMobiNative inMobiNative) {
        Log.d(TAG, "InMobi native ad left application.");
        if (mediationNativeAdCallback != null) {
            mediationNativeAdCallback.onAdLeftApplication();
        }
    }

    @Override
    public void onAdClicked(@NonNull InMobiNative inMobiNative) {
        Log.d(TAG, "InMobi native ad has been clicked.");
        if (mediationNativeAdCallback != null) {
            mediationNativeAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onAdImpression(@NonNull InMobiNative inMobiNative) {
        Log.d(TAG, "InMobi native ad has logged an impression.");
        if (mediationNativeAdCallback != null) {
            mediationNativeAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onAdStatusChanged(@NonNull InMobiNative inMobiNative) {
        // Google Mobile Ads SDK doesn't have a matching event.
    }
}
