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

package com.google.ads.mediation.sample.sdk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ads.mediation.sample.sdk.R;
import com.google.ads.mediation.sample.sdk.SampleRewardedVideo;
import com.google.ads.mediation.sample.sdk.SampleRewardedVideoAd;
import com.google.ads.mediation.sample.sdk.SampleRewardedVideoAdListener;

import java.util.Locale;

/**
 * The {@link SampleSDKAdsActivity} is used to show sample rewarded video ads by the Sample SDK.
 */
public class SampleSDKAdsActivity extends AppCompatActivity {

    /**
     * Displays the amount of time remaining for the ad to complete.
     */
    private TextView countdownTimerView;

    /**
     * Closes the ad/activity.
     */
    private ImageButton closeAdButton;

    /**
     * Flag to determine whether or not it is ok to close this activity. The ad can be skipped
     * after 5 seconds; no reward is provided if the ad is closed before the countdown is finished.
     */
    private boolean isSkippable;

    /**
     * Flag to determine whether not the ad is clickable. The ad is not clickable when showing
     * the countdown (clickable after the video completed playing).
     */
    private boolean isClickable;

    /**
     * A simple countdown timer.
     */
    private CountDownTimer countDownTimer;

    /**
     * The Sample SDK's rewarded video ad object that needs to be shown to the user.
     */
    private SampleRewardedVideoAd sampleRewardedVideoAd;

    /**
     * Forwards rewarded video ad events.
     */
    private SampleRewardedVideoAdListener rewardedVideoAdListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_sdk_ads);

        // Get the Sample SDK rewarded video ad, which was added to the intent as extra.
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(SampleRewardedVideo.KEY_REWARDED_VIDEO_AD_EXTRA)) {
            sampleRewardedVideoAd =
                    intent.getParcelableExtra(SampleRewardedVideo.KEY_REWARDED_VIDEO_AD_EXTRA);
        } else {
            // Rewarded video ad not available, close ad.
            finish();
        }

        rewardedVideoAdListener = SampleRewardedVideo.getListener();
        if (rewardedVideoAdListener != null) {
            rewardedVideoAdListener.onAdFullScreen();
        }
        SampleRewardedVideo.setCurrentActivity(SampleSDKAdsActivity.this);

        ((TextView) findViewById(R.id.title_textView)).setText(sampleRewardedVideoAd.getAdName());
        findViewById(R.id.main_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rewardedVideoAdListener != null && isClickable) {
                    rewardedVideoAdListener.onAdClicked();
                }
            }
        });
        closeAdButton = (ImageButton) findViewById(R.id.close_button);
        closeAdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer = null;
                }
                finish();
            }
        });
        countdownTimerView = (TextView) findViewById(R.id.countdown_timer_textView);

        // Countdown timer for 10 seconds.
        countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished > 6000) {
                    isSkippable = false;
                    closeAdButton.setVisibility(View.GONE);
                } else {
                    // The ad is skippable after 5 seconds.
                    isSkippable = true;
                    closeAdButton.setVisibility(View.VISIBLE);
                }
                countdownTimerView.setText(String.format("%d", (millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
                int rewardAmount = sampleRewardedVideoAd.getRewardAmount();
                if (rewardedVideoAdListener != null) {
                    rewardedVideoAdListener.onAdRewarded("Reward", rewardAmount);
                }
                countdownTimerView.setText(String.format(
                        Locale.getDefault(), "Rewarded with reward amount %d", rewardAmount));
                isClickable = true;
            }
        }.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (rewardedVideoAdListener != null) {
            rewardedVideoAdListener.onAdClosed();
        }
    }

    @Override
    public void onBackPressed() {
        if (isSkippable) {
            super.onBackPressed();
        }

    }
}
