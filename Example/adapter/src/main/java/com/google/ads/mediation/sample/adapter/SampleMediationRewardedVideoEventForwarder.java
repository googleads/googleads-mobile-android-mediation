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

package com.google.ads.mediation.sample.adapter;

import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleRewardedVideoAdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

/**
 * A {@link SampleRewardedVideoAdListener} that forwards events to AdMob mediation's
 * {@link MediationRewardedVideoAdListener}.
 */
public class SampleMediationRewardedVideoEventForwarder extends SampleRewardedVideoAdListener {
    private final MediationRewardedVideoAdListener mediationRewardedVideoAdListener;
    private final SampleAdapter sampleAdapter;
    private boolean isInitialized;

    /**
     * Creates a new {@link SampleMediationRewardedVideoEventForwarder}.
     *
     * @param listener      An AdMob Mediation {@link MediationRewardedVideoAdListener} that should
     *                      receive forwarded events.
     * @param sampleAdapter A {@link SampleAdapter} mediation adapter.
     */
    public SampleMediationRewardedVideoEventForwarder(MediationRewardedVideoAdListener listener,
                                                      SampleAdapter sampleAdapter) {
        this.mediationRewardedVideoAdListener = listener;
        this.sampleAdapter = sampleAdapter;
    }

    /**
     * @return whether or not the Sample SDK is initialized.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void onRewardedVideoInitialized() {
        super.onRewardedVideoInitialized();
        isInitialized = true;
        mediationRewardedVideoAdListener.onInitializationSucceeded(sampleAdapter);
    }

    @Override
    public void onRewardedVideoInitializationFailed(SampleErrorCode error) {
        super.onRewardedVideoInitializationFailed(error);
        isInitialized = false;
        mediationRewardedVideoAdListener.onInitializationFailed(
                sampleAdapter, getAdMobErrorCode(error));
    }

    @Override
    public void onAdRewarded(final String rewardType, final int amount) {
        super.onAdRewarded(rewardType, amount);

        /*
         * AdMob requires a reward item with a reward type and amount to be sent when sending the
         * rewarded callback. If you SDK does not have a reward amount you need to do the following:
         *
         * 1. AdMob provides an ability to override the reward value in the front end. Document
         * this asking the publisher to override the reward value on AdMob's front end.
         * 2. Send a reward item with default values for the type (an empty string "") and reward
         * amount (1).
         */
        mediationRewardedVideoAdListener.onRewarded(
                sampleAdapter, new SampleRewardItem(rewardType, amount));
    }

    @Override
    public void onAdClicked() {
        super.onAdClicked();
        mediationRewardedVideoAdListener.onAdClicked(sampleAdapter);
    }

    @Override
    public void onAdFullScreen() {
        super.onAdFullScreen();
        mediationRewardedVideoAdListener.onAdOpened(sampleAdapter);
        // Only send video started here if your SDK starts video immediately after the ad has been
        // opened/is fullscreen.
        mediationRewardedVideoAdListener.onVideoStarted(sampleAdapter);
    }

    @Override
    public void onAdClosed() {
        super.onAdClosed();
        mediationRewardedVideoAdListener.onAdClosed(sampleAdapter);
    }

    /**
     * Forwards the ad loaded event to AdMob SDK. The Sample SDK does not have an ad loaded
     * callback, the adapter calls this method if an ad is available when loadAd is called.
     */
    protected void onAdLoaded() {
        mediationRewardedVideoAdListener.onAdLoaded(sampleAdapter);
    }

    /**
     * Forwards the ad failed event to AdMob SDK. The Sample SDK does not have an ad failed to
     * load callback, the adapter calls this method to forward the failure callback.
     */
    protected void onAdFailedToLoad() {
        mediationRewardedVideoAdListener.onAdFailedToLoad(
                sampleAdapter, AdRequest.ERROR_CODE_NO_FILL);
    }

    /**
     * Converts {@link SampleErrorCode} into an AdMob SDK's {@link AdRequest} error code.
     *
     * @param errorCode a Sample SDK error code.
     * @return an AdMob SDK readable error code.
     */
    private int getAdMobErrorCode(SampleErrorCode errorCode) {
        switch (errorCode) {
            case BAD_REQUEST:
                return AdRequest.ERROR_CODE_INVALID_REQUEST;
            case NETWORK_ERROR:
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case NO_INVENTORY:
                return AdRequest.ERROR_CODE_NO_FILL;
            case UNKNOWN:
            default:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }
}
