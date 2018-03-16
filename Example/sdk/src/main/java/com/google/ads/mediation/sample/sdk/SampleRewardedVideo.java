/*
 * Copyright (C) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.sample.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import com.google.ads.mediation.sample.sdk.activities.SampleSDKAdsActivity;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;

/**
 * The {@link SampleRewardedVideo} class is used to load and show rewarded video ads for the
 * Sample SDK.
 */
public class SampleRewardedVideo {

    /**
     * Key to set and get rewarded video ad as an extra for an intent.
     */
    public static final String KEY_REWARDED_VIDEO_AD_EXTRA = "rewarded_video_ad_extra";

    /**
     * Flag to determine whether or not the Sample SDK's rewarded video code is initialized.
     */
    private static boolean isInitialized;

    /**
     * A queue of rewarded video ad objects.
     */
    private static Queue<SampleRewardedVideoAd> adsQueue;

    /**
     * A listener that SampleRewardedVideo will send rewarded video events to.
     */
    private static SampleRewardedVideoAdListener listener;

    /**
     * A weak reference to the current activity. Used to show rewarded video ads.
     */
    private static WeakReference<Activity> weakActivity;

    /**
     * Private constructor to restrict anyone from instantiating this class.
     */
    private SampleRewardedVideo() {
    }

    /**
     * Sets the rewarded video ad listener to which the rewarded video ad events will be forwarded.
     *
     * @param listener a {@code SampleRewardedVideoAdListener} to which to forward the rewarded
     *                 video ad events.
     */
    public static void setListener(SampleRewardedVideoAdListener listener) {
        SampleRewardedVideo.listener = listener;
    }

    /**
     * Returns the current rewarded video ad listener to which the Sample SDK is forwarding the
     * rewarded events.
     *
     * @return a {@code SampleRewardedVideoAdListener} that is currently registered to the Sample
     * SDK.
     */
    public static SampleRewardedVideoAdListener getListener() {
        return listener;
    }

    /**
     * Initializes the Sample SDK's rewarded video code and caches the ads if auto-caching is
     * enabled.
     *
     * @param activity an Android {@link Activity} needed to initialize the Sample SDK's rewarded
     *                 video.
     * @param adUnitId the Sample SDK ad unit.
     * @param listener the Sample SDK's rewarded video ad listener to receive rewarded video ad
     *                 events.
     */
    public static void initialize(Activity activity,
                                  String adUnitId,
                                  SampleRewardedVideoAdListener listener) {
        if (isInitialized) {
            Log.d("SampleSDK", "Sample rewarded video is already initialized.");
            return;
        }

        if (TextUtils.isEmpty(adUnitId)) {
            if (listener != null) {
                listener.onRewardedVideoInitializationFailed(SampleErrorCode.BAD_REQUEST);
            }
            Log.w("SampleSDK", "Rewarded video failed to initialize. Ad Unit ID is null");
            return;
        }

        setListener(listener);
        setCurrentActivity(activity);
        adsQueue = new ArrayDeque<>();

        loadAds();
    }

    /**
     * Checks whether or not a rewarded video ad is available. Loads ads if an ad is not available.
     *
     * @return {@code true} if the rewarded video ads queue is not null and has at least one ad,
     * {@code false} otherwise.
     */
    public static boolean isAdAvailable() {
        if (!isInitialized) {
            Log.w("SampleSDK", "Sample rewarded video not initialized. Call "
                    + "SampleRewardedVideo.initialize(...) before fetching ads.");
            return false;
        }

        if (!adsQueue.isEmpty()) {
            return true;
        } else {
            loadAds();
            return false;
        }
    }

    /**
     * Sets the current activity to be used to show rewarded video ads.
     *
     * @param activity an Android {@link Activity}.
     */
    public static void setCurrentActivity(Activity activity) {
        weakActivity = new WeakReference<>(activity);
    }

    /**
     * Loads rewarded video ads.
     */
    private static void loadAds() {
        Random random = new Random();
        int nextInt = random.nextInt(100);
        SampleErrorCode errorCode = null;
        if (nextInt < 80) {
            if (nextInt < 20) {
                createSampleRewardedVideoAds(2, 1);
            } else if (nextInt < 40) {
                createSampleRewardedVideoAds(4, 2);
            } else if (nextInt < 60) {
                createSampleRewardedVideoAds(6, 2);
            } else {
                createSampleRewardedVideoAds(8, 1);
            }
            if (listener != null && !isInitialized) {
                isInitialized = true;
                listener.onRewardedVideoInitialized();
            }
        } else if (nextInt < 85) {
            errorCode = SampleErrorCode.UNKNOWN;
        } else if (nextInt < 90) {
            errorCode = SampleErrorCode.BAD_REQUEST;
        } else if (nextInt < 95) {
            errorCode = SampleErrorCode.NETWORK_ERROR;
        } else if (nextInt < 100) {
            errorCode = SampleErrorCode.NO_INVENTORY;
        }
        if (errorCode != null && listener != null && !isInitialized) {
            listener.onRewardedVideoInitializationFailed(errorCode);
        }
    }

    /**
     * Shows a rewarded video ad if one is available. The publisher should check if
     * {@link #isAdAvailable()} is {@code true} before calling this method.
     */
    public static void showAd() {
        if (!(isAdAvailable())) {
            Log.w("SampleSDK", "No ads to show. Call SampleRewardedVideo.isAdAvailable() before "
                    + "calling showAd().");
            return;
        }


        if (weakActivity == null) {
            Log.d("SampleSDK", "Current activity is null. Make sure to call "
                    + "SampleRewardedVideo.setCurrentActivity(this) in your Activity's onResume()");
            return;
        }


        Context context = weakActivity.get();
        if (context == null) {
            Log.d("SampleSDK", "Current activity is null. Make sure to call "
                    + "SampleRewardedVideo.setCurrentActivity(this) in your Activity's onResume()");
            return;
        }

        Intent intent = new Intent(context, SampleSDKAdsActivity.class);
        intent.putExtra(KEY_REWARDED_VIDEO_AD_EXTRA, nextSampleRewardedVideoAd());
        context.startActivity(intent);
    }

    /**
     * Removes and gets the Sample SDK's rewarded video ad from the top of the queue. Check
     * {@link #isAdAvailable()} before calling this method.
     *
     * @return the {@link SampleRewardedVideoAd} on top of the queue.
     */
    private static SampleRewardedVideoAd nextSampleRewardedVideoAd() {
        SampleRewardedVideoAd sampleRewardedVideoAd = adsQueue.remove();
        if (adsQueue.isEmpty()) {
            loadAds();
        }
        return sampleRewardedVideoAd;
    }

    /**
     * Generates new rewarded video ads.
     *
     * @param numberOfAds number of ads to be created.
     * @param rewardValue the reward value associated with the ad created.
     */
    private static void createSampleRewardedVideoAds(int numberOfAds, int rewardValue) {
        for (int i = 0; i < numberOfAds; i++) {
            adsQueue.add(new SampleRewardedVideoAd(
                    String.format(Locale.getDefault(), "Ad %d of %d", (i + 1), numberOfAds),
                    rewardValue));
        }
    }

    public static void destroy() {
        weakActivity = null;
        adsQueue = null;
        listener = null;
        isInitialized = false;
    }
}
