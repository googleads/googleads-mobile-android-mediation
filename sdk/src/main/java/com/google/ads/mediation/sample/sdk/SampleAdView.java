/*
 * Copyright (C) 2014 Google, Inc.
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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

/**
 * An ad view for the sample ad network. This is an example of an ad view that most ad network SDKs
 * have.
 */
public class SampleAdView extends TextView {
    private SampleAdSize mAdSize;
    private String mAdUnit;
    private SampleAdListener mListener;

    /**
     * Create a new {@link SampleAdView}.
     * @param context An Android {@link Context}.
     */
    public SampleAdView(Context context) {
        super(context);
    }

    /**
     * Sets the size of the banner.
     * @param size The banner size.
     */
    public void setSize(SampleAdSize size) {
        this.mAdSize = size;
    }

    /**
     * Sets the sample ad unit.
     * @param sampleAdUnit The sample ad unit.
     */
    public void setAdUnit(String sampleAdUnit) {
        this.mAdUnit = sampleAdUnit;
    }

    /**
     * Sets a {@link SampleAdListener} to listen for ad events.
     * @param listener The ad listener.
     */
    public void setAdListener(SampleAdListener listener) {
        this.mListener = listener;
    }

    /**
     * Fetch an ad. Instead of doing an actual ad fetch, we will randomly decide to succeed, or
     * fail with different error codes.
     * @param request The ad request with targeting information.
     */
    public void fetchAd(SampleAdRequest request) {
        if (mListener == null) {
            return;
        }

        // If the publisher didn't set a size or ad unit, return a bad request.
        if (mAdSize == null || mAdUnit == null) {
            mListener.onAdFetchFailed(SampleErrorCode.BAD_REQUEST);
        }

        // Randomly decide whether to succeed or fail.
        Random random = new Random();
        int nextInt = random.nextInt(100);
        if (mListener != null) {
            if (nextInt < 85) {
                this.setText("Sample Text Ad");
                this.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Notify the developer that a full screen view will be presented.
                        mListener.onAdFullScreen();
                        Intent intent =
                                new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
                        SampleAdView.this.getContext().startActivity(intent);
                    }
                });
                mListener.onAdFetchSucceeded();
            } else if (nextInt < 90) {
                mListener.onAdFetchFailed(SampleErrorCode.UNKNOWN);
            } else if (nextInt < 95) {
                mListener.onAdFetchFailed(SampleErrorCode.NETWORK_ERROR);
            } else if (nextInt < 100) {
                mListener.onAdFetchFailed(SampleErrorCode.NO_INVENTORY);
            }
        }
    }

    /**
     * Destroy the banner.
     */
    public void destroy() {
        mListener = null;
    }
}
