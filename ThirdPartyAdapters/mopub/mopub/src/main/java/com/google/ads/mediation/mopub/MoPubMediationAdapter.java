package com.google.ads.mediation.mopub;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideoManager;
import com.mopub.mobileads.MoPubRewardedVideos;
import com.mopub.mobileads.dfp.adapters.MoPubAdapter;

import java.util.Set;

/**
 * A {@link com.google.ads.mediation.mopub.MoPubMediationAdapter} used to mediate rewarded video
 * ads from MoPub.
 */
public class MoPubMediationAdapter implements MediationRewardedVideoAdAdapter {

    private static final String TAG = MoPubMediationAdapter.class.getSimpleName();
    private static final String MOPUB_AD_UNIT_KEY = "adUnitId";

    private mMediationRewardedVideoListener mMediationRewardedVideoListener;
    private MediationRewardedVideoAdListener mediationRewardedVideoAdListener;
    private boolean isRewardedVideoInitialized = false;
    private String adUnitId;
    private boolean adExpired;

    @Override
    public void onDestroy() {
        MoPubRewardedVideos.setRewardedVideoListener(null);
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void initialize(Context context, MediationAdRequest mediationAdRequest,
                           String s, MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle bundle, Bundle bundle1) {

        adUnitId = bundle.getString(MOPUB_AD_UNIT_KEY);
        adExpired = false;

        if (TextUtils.isEmpty(adUnitId)) {
            Log.d(TAG, "Failed to initialize MoPub rewarded video. The ad unit ID is empty.");
            mediationRewardedVideoAdListener.onAdFailedToLoad(
                    MoPubMediationAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        /* Persist the listener in case the adapter needs it later (for example, if the ad request
        has to be delayed for the MoPub SDK to finish initializing */
        setRewardedVideoListener(mediationRewardedVideoAdListener);

        mMediationRewardedVideoListener = new mMediationRewardedVideoListener(mediationRewardedVideoAdListener);

        if (!MoPub.isSdkInitialized()) {
            initializeMoPub(context, adUnitId);
        } else {
            isRewardedVideoInitialized = true;
            mediationRewardedVideoAdListener.onInitializationSucceeded(MoPubMediationAdapter.this);
            MoPubRewardedVideos.setRewardedVideoListener(mMediationRewardedVideoListener);
        }
    }

    private void setRewardedVideoListener(MediationRewardedVideoAdListener listener) {
        mediationRewardedVideoAdListener = listener;
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle bundle, Bundle bundle1) {

        if (!TextUtils.isEmpty(adUnitId)) {
            if (MoPubRewardedVideos.hasRewardedVideo(adUnitId)) {
                mediationRewardedVideoAdListener.onAdLoaded(MoPubMediationAdapter.this);
            } else {
                MoPubRewardedVideoManager.RequestParameters rewardedRequestParameters =
                        new MoPubRewardedVideoManager.RequestParameters(
                                MoPubAdapter.getKeywords(mediationAdRequest, false),
                                MoPubAdapter.getKeywords(mediationAdRequest, true),
                                mediationAdRequest.getLocation()
                        );
                MoPubRewardedVideos.loadRewardedVideo(adUnitId, rewardedRequestParameters);
            }
        } else {
            Log.d(TAG, "Failed to request a MoPub rewarded video. The ad unit ID is empty.");
            mediationRewardedVideoAdListener.onAdFailedToLoad(MoPubMediationAdapter.this,
                    AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    @Override
    public void showVideo() {
        if (!adExpired && !TextUtils.isEmpty(adUnitId) && MoPubRewardedVideos.hasRewardedVideo(adUnitId)) {
            Log.d(TAG, "Showing a MoPub rewarded video.");
            MoPubRewardedVideos.showRewardedVideo(adUnitId);
        } else {
            Log.d(TAG, "Failed to show a MoPub rewarded video. Either the video is not ready " +
                    "or the ad unit ID is empty.");
        }
    }

    @Override
    public boolean isInitialized() {
        return isRewardedVideoInitialized;
    }

    private class mMediationRewardedVideoListener implements
            MoPubRewardedVideoListener {

        MediationRewardedVideoAdListener listener;

        public mMediationRewardedVideoListener(MediationRewardedVideoAdListener listener) {
            this.listener = listener;
        }

        @Override
        public void onRewardedVideoLoadSuccess(@NonNull String adUnitId) {
            if (listener != null) {
                listener.onAdLoaded(MoPubMediationAdapter.this);
            }
        }

        @Override
        public void onRewardedVideoLoadFailure(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
            if (listener != null) {
                switch (errorCode) {
                    case NO_FILL:
                        listener.onAdFailedToLoad(MoPubMediationAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                        break;
                    case NETWORK_TIMEOUT:
                        listener.onAdFailedToLoad(MoPubMediationAdapter.this,
                                AdRequest.ERROR_CODE_NETWORK_ERROR);
                        break;
                    case SERVER_ERROR:
                        listener.onAdFailedToLoad(MoPubMediationAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    case EXPIRED:
                        adExpired = true;
                        listener.onAdFailedToLoad(MoPubMediationAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;

                    default:
                        listener.onAdFailedToLoad(MoPubMediationAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                }
            }
        }

        @Override
        public void onRewardedVideoStarted(@NonNull String adUnitId) {
            if (listener != null) {
                listener.onAdOpened(MoPubMediationAdapter.this);
            }
        }

        @Override
        public void onRewardedVideoPlaybackError(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
            // AdMob does not have a playback failure callback to notify
        }

        @Override
        public void onRewardedVideoClicked(@NonNull String adUnitId) {
            if (listener != null) {
                listener.onAdClicked(MoPubMediationAdapter.this);
                listener.onAdLeftApplication(MoPubMediationAdapter.this);
            }
        }

        @Override
        public void onRewardedVideoClosed(@NonNull String adUnitId) {
            if (listener != null) {
                listener.onAdClosed(MoPubMediationAdapter.this);
            }
        }

        @Override
        public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds, @NonNull final MoPubReward reward) {
            Preconditions.checkNotNull(reward);

            class AdMobRewardItem implements RewardItem {

                @Override
                public String getType() {
                    return reward.getLabel();
                }

                @Override
                public int getAmount() {
                    return reward.getAmount();
                }
            }

            if (listener != null) {
                listener.onRewarded(MoPubMediationAdapter.this, new AdMobRewardItem());
                listener.onVideoCompleted(MoPubMediationAdapter.this);
            }
        }
    }

    // Initializing the MoPub SDK. Required as of 5.0.0
    private void initializeMoPub(Context context, String adUnitId) {

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnitId)
                .build();

        if (!(context instanceof Activity)) {
            Log.d(TAG, "Failed to initialize MoPub rewarded video. An Activity Context is needed.");
            mediationRewardedVideoAdListener.onInitializationFailed(MoPubMediationAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        Activity activity = (Activity) context;
        MoPub.initializeSdk(activity, sdkConfiguration, initSdkListener());
    }

    private SdkInitializationListener initSdkListener() {
        return new SdkInitializationListener() {

            @Override
            public void onInitializationFinished() {
                MoPubLog.d("MoPub SDK initialized.");

                if (mediationRewardedVideoAdListener != null) {
                    mediationRewardedVideoAdListener.onInitializationSucceeded(MoPubMediationAdapter.this);
                }
                isRewardedVideoInitialized = true;
                MoPubRewardedVideos.setRewardedVideoListener(mMediationRewardedVideoListener);
            }
        };
    }
}