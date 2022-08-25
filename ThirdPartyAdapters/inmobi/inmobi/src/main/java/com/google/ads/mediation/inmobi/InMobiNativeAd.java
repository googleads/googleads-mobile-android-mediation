package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_INMOBI_NOT_INITIALIZED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_NON_UNIFIED_NATIVE_REQUEST;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.INMOBI_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.exceptions.SdkNotInitializedException;
import com.inmobi.ads.listeners.NativeAdEventListener;
import com.inmobi.ads.listeners.VideoEventListener;

import java.util.HashMap;
import java.util.Set;

public class InMobiNativeAd extends UnifiedNativeAdMapper {

    MediationNativeAdConfiguration mMediationNativeAdConfiguration;
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mMediationAdLoadCallback;
    private InMobiNative mAdNative;
    public MediationNativeAdCallback mMediationNativeAdCallback;

    public InMobiNativeAd(MediationNativeAdConfiguration mMediationNativeAdConfiguration,
                          MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mMediationAdLoadCallback) {
            this.mMediationNativeAdConfiguration = mMediationNativeAdConfiguration;
            this.mMediationAdLoadCallback = mMediationAdLoadCallback;
    }

    public void load() {
        final Context context = mMediationNativeAdConfiguration.getContext();
        Bundle serverParameters = mMediationNativeAdConfiguration.getServerParameters();

        //todo
//        if (!mMediationNativeAdConfiguration.) {
//            AdError error = new AdError(ERROR_NON_UNIFIED_NATIVE_REQUEST,
//                    "Unified Native Ad should be requested.", ERROR_DOMAIN);
//            Log.w(TAG, error.getMessage());
//            mMediationAdLoadCallback.onFailure(error);
//            return;
//        }

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
                createAndLoadNativeAd(context, placementId);
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

    private void createAndLoadNativeAd(Context context, long placementId) {

        if (placementId <= 0L) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or Invalid Placement ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationAdLoadCallback.onFailure(error);
            return;
        }

        try {
            mAdNative = new InMobiNative(context, placementId, new NativeAdEventListener() {
                @Override
                public void onAdLoadSucceeded(@NonNull final InMobiNative imNativeAd,
                                              @NonNull AdMetaInfo adMetaInfo) {
                    Log.d(TAG, "InMobi native ad has been loaded.");

                    // This setting decides whether to download images or not.
                    NativeAdOptions nativeAdOptions = mMediationNativeAdConfiguration.getNativeAdOptions();
                    boolean mIsOnlyUrl = false;

                    if (null != nativeAdOptions) {
                        mIsOnlyUrl = nativeAdOptions.shouldReturnUrlsForImageAssets();
                    }

                    InMobiUnifiedNativeAdMapper inMobiUnifiedNativeAdMapper =
                            new InMobiUnifiedNativeAdMapper(imNativeAd, mIsOnlyUrl, mMediationAdLoadCallback, InMobiNativeAd.this);
                    inMobiUnifiedNativeAdMapper.mapUnifiedNativeAd(context);
                }

                @Override
                public void onAdLoadFailed(@NonNull InMobiNative inMobiNative,
                                           @NonNull InMobiAdRequestStatus requestStatus) {
                    AdError error = new AdError(InMobiAdapterUtils.getMediationErrorCode(requestStatus),
                            requestStatus.getMessage(), INMOBI_SDK_ERROR_DOMAIN);
                    Log.w(TAG, error.getMessage());
                    mMediationAdLoadCallback.onFailure(error);
                }

                @Override
                public void onAdFullScreenDismissed(@NonNull InMobiNative inMobiNative) {
                    Log.d(TAG, "InMobi native ad has been dismissed.");
                    if (mMediationNativeAdCallback != null) {
                        mMediationNativeAdCallback.onAdClosed();
                    }
                }

                @Override
                public void onAdFullScreenWillDisplay(@NonNull InMobiNative inMobiNative) {
                    // No op.
                }

                @Override
                public void onAdFullScreenDisplayed(@NonNull InMobiNative inMobiNative) {
                    Log.d(TAG, "InMobi native ad opened.");
                    if (mMediationNativeAdCallback != null) {
                        mMediationNativeAdCallback.onAdOpened();
                    }
                }

                @Override
                public void onUserWillLeaveApplication(@NonNull InMobiNative inMobiNative) {
                    Log.d(TAG, "InMobi native ad left application.");
                    if (mMediationNativeAdCallback != null) {
                        mMediationNativeAdCallback.onAdLeftApplication();
                    }
                }

                @Override
                public void onAdClicked(@NonNull InMobiNative inMobiNative) {
                    Log.d(TAG, "InMobi native ad has been clicked.");
                    if (mMediationNativeAdCallback != null) {
                        mMediationNativeAdCallback.reportAdClicked();
                    }
                }

                @Override
                public void onAdImpression(@NonNull InMobiNative inMobiNative) {
                    Log.d(TAG, "InMobi native ad has logged an impression.");
                }

                @Override
                public void onAdStatusChanged(@NonNull InMobiNative inMobiNative) {
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

        mAdNative.setVideoEventListener(new VideoEventListener() {
            @Override
            public void onVideoCompleted(final InMobiNative inMobiNative) {
                super.onVideoCompleted(inMobiNative);
                Log.d(TAG, "InMobi native video ad completed.");
                mMediationNativeAdCallback.onVideoComplete();
            }

            @Override
            public void onVideoSkipped(final InMobiNative inMobiNative) {
                super.onVideoSkipped(inMobiNative);
                Log.d(TAG, "InMobi native video ad skipped.");
            }
        });

        //todo
//        // Setting mediation key words to native ad object
//        Set<String> mediationKeyWords = mMediationNativeAdConfiguration.getKeywords();
//        if (null != mediationKeyWords) {
//            mAdNative.setKeywords(TextUtils.join(", ", mediationKeyWords));
//        }

        //Update Age Restricted User
        InMobiAdapterUtils.updateAgeRestrictedUser(mMediationNativeAdConfiguration);

        /*
         *  Extra request params : Add any other extra request params here
         *  #1. Explicitly setting mediation supply parameter to AdMob
         *  #2. Landing url
         */
        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(mMediationNativeAdConfiguration);
        mAdNative.setExtras(paramMap);

        InMobiAdapterUtils.setGlobalTargeting(mMediationNativeAdConfiguration, mMediationNativeAdConfiguration.getMediationExtras());
        mAdNative.load();
    }
}
