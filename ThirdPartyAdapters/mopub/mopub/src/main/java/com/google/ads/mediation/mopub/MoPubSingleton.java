package com.google.ads.mediation.mopub;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideoManager;
import com.mopub.mobileads.MoPubRewardedVideos;
import com.mopub.mobileads.dfp.adapters.MoPubAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MoPubSingleton implements MoPubRewardedVideoListener {

    private static MoPubSingleton instance;
    private static boolean isInitializing;

    private ArrayList<SdkInitializationListener> mInitListeners = new ArrayList<>();
    private static HashMap<String, WeakReference<MoPubRewardedVideoListener>> mListeners =
            new HashMap<>();

    public static MoPubSingleton getInstance() {
        if (instance == null) {
            instance = new MoPubSingleton();
        }
        return instance;
    }

    private boolean hasListener(String adUnitID) {
        return (!TextUtils.isEmpty(adUnitID)
                && mListeners.containsKey(adUnitID)
                && mListeners.get(adUnitID).get() != null);
    }

    void adExpired(String adUnitID, MoPubRewardedVideoListener listener) {
        // Verify if the passed MoPubRewardedVideoListener instance matches the registered
        // instance for the given MoPub Ad Unit ID before removing from the list of listeners.
        if (hasListener(adUnitID)
                && listener != null
                && listener.equals(mListeners.get(adUnitID).get())) {
            mListeners.remove(adUnitID);
        }
    }

    boolean showRewardedAd(String adUnitID) {
        if (!TextUtils.isEmpty(adUnitID)
                && MoPubRewardedVideos.hasRewardedVideo(adUnitID)) {
            Log.d(MoPubMediationAdapter.TAG, "Showing a MoPub rewarded video.");
            MoPubRewardedVideos.showRewardedVideo(adUnitID);
            return true;
        } else {
            Log.e(MoPubMediationAdapter.TAG, "Failed to show a MoPub rewarded video. " +
                    "Either the video is not ready or the ad unit ID is empty.");
            mListeners.remove(adUnitID);
            return false;
        }
    }

    public void initializeMoPubSDK(Context context,
                                   SdkConfiguration configuration,
                                   SdkInitializationListener listener) {
        if (MoPub.isSdkInitialized()) {
            MoPubRewardedVideos.setRewardedVideoListener(MoPubSingleton.this);
            listener.onInitializationFinished();
            return;
        }

        mInitListeners.add(listener);
        if (!isInitializing) {
            isInitializing = true;

            MoPub.initializeSdk(context, configuration, new SdkInitializationListener() {
                @Override
                public void onInitializationFinished() {
                    MoPubLog.d("MoPub SDK initialized.");

                    MoPubRewardedVideos.setRewardedVideoListener(MoPubSingleton.this);
                    for (SdkInitializationListener initListener : mInitListeners) {
                        initListener.onInitializationFinished();
                    }
                    mInitListeners.clear();
                    isInitializing = false;
                }
            });
        }
    }

    public void loadRewardedAd(Context context,
                               final String adUnitID,
                               final MoPubRewardedVideoManager.RequestParameters requestParameters,
                               final MoPubRewardedVideoListener listener) {
        if (hasListener(adUnitID)) {
            Log.w(MoPubMediationAdapter.TAG, "An ad has already been requested "
                    + "for the MoPub Ad Unit ID: " + adUnitID);
            listener.onRewardedVideoLoadFailure(adUnitID, MoPubErrorCode.CANCELLED);
            return;
        }

        mListeners.put(adUnitID, new WeakReference<>(listener));

        SdkConfiguration configuration = new SdkConfiguration.Builder(adUnitID).build();
        initializeMoPubSDK(context, configuration, new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
                MoPubRewardedVideos.loadRewardedVideo(adUnitID, requestParameters);
            }
        });
    }

    static String getKeywords(MediationAdConfiguration mediationConfiguration,
                              boolean intendedForPII) {
        if (intendedForPII) {
            if (MoPub.canCollectPersonalInformation()) {
                return containsPII(mediationConfiguration) ?
                        MoPubAdapter.MOPUB_NATIVE_CEVENT_VERSION : "";
            } else {
                return "";
            }
        } else {
            return containsPII(mediationConfiguration) ? "" :
                    MoPubAdapter.MOPUB_NATIVE_CEVENT_VERSION;
        }
    }

    static boolean containsPII(MediationAdConfiguration configuration) {
        return configuration.getLocation() != null;
    }

    /**
     * {@link MoPubRewardedVideoListener} implementation
     */
    @Override
    public void onRewardedVideoLoadSuccess(@NonNull String adUnitId) {
        if (hasListener(adUnitId)) {
            mListeners.get(adUnitId).get().onRewardedVideoLoadSuccess(adUnitId);
        }
    }

    @Override
    public void onRewardedVideoLoadFailure(@NonNull String adUnitId,
                                           @NonNull MoPubErrorCode errorCode) {
        if (hasListener(adUnitId)) {
            mListeners.get(adUnitId).get().onRewardedVideoLoadFailure(adUnitId, errorCode);
        }
        mListeners.remove(adUnitId);
    }

    @Override
    public void onRewardedVideoStarted(@NonNull String adUnitId) {
        if (hasListener(adUnitId)) {
            mListeners.get(adUnitId).get().onRewardedVideoStarted(adUnitId);
        }
    }

    @Override
    public void onRewardedVideoPlaybackError(@NonNull String adUnitId,
                                             @NonNull MoPubErrorCode errorCode) {
        if (hasListener(adUnitId)) {
            mListeners.get(adUnitId).get().onRewardedVideoPlaybackError(adUnitId, errorCode);
        }
        mListeners.remove(adUnitId);
    }

    @Override
    public void onRewardedVideoClicked(@NonNull String adUnitId) {
        if (hasListener(adUnitId)) {
            mListeners.get(adUnitId).get().onRewardedVideoClicked(adUnitId);
        }
    }

    @Override
    public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds,
                                         @NonNull MoPubReward reward) {
        for (String adUnitId : adUnitIds) {
            if (hasListener(adUnitId)) {
                HashSet<String> set = new HashSet<>();
                set.add(adUnitId);
                mListeners.get(adUnitId).get().onRewardedVideoCompleted(set, reward);
            }
        }
    }

    @Override
    public void onRewardedVideoClosed(@NonNull String adUnitId) {
        if (hasListener(adUnitId)) {
            mListeners.get(adUnitId).get().onRewardedVideoClosed(adUnitId);
        }
        mListeners.remove(adUnitId);
    }
}
