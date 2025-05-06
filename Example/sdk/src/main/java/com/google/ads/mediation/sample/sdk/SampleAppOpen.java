/*
 * Copyright 2025 Google LLC
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Random;

/**
 * A sample app open ad. This is an example of an app open class that most ad networks SDKs have.
 */
public class SampleAppOpen {

  private final Context context;
  private String adUnit;
  private SampleAdListener listener;

  /**
   * Create a new {@link SampleAppOpen}.
   *
   * @param context An Android {@link Context}.
   */
  public SampleAppOpen(@NonNull Context context) {
    this.context = context;
  }

  /**
   * Sets the sample ad unit.
   *
   * @param sampleAdUnit The sample ad unit.
   */
  public void setAdUnit(@Nullable String sampleAdUnit) {
    this.adUnit = sampleAdUnit;
  }

  /**
   * Sets a {@link SampleAdListener} to listen for ad events.
   *
   * @param listener The ad listener.
   */
  public void setAdListener(@NonNull SampleAdListener listener) {
    this.listener = listener;
  }

  /**
   * Fetch an ad. Instead of doing an actual ad fetch, we will randomly decide to succeed, or fail
   * with different error codes.
   *
   * @param request The ad request with targeting information.
   */
  public void fetchAd(@NonNull SampleAdRequest request) {
    if (listener == null) {
      return;
    }

    // If the publisher didn't set an ad unit, return a bad request.
    if (adUnit == null) {
      listener.onAdFetchFailed(SampleErrorCode.BAD_REQUEST);
    }

    Random random = new Random();
    int nextInt = random.nextInt(100);
    if (listener != null) {
      if (nextInt < 80) {
        listener.onAdFetchSucceeded();
      } else if (nextInt < 85) {
        listener.onAdFetchFailed(SampleErrorCode.UNKNOWN);
      } else if (nextInt < 90) {
        listener.onAdFetchFailed(SampleErrorCode.BAD_REQUEST);
      } else if (nextInt < 95) {
        listener.onAdFetchFailed(SampleErrorCode.NETWORK_ERROR);
      } else {
        listener.onAdFetchFailed(SampleErrorCode.NO_INVENTORY);
      }
    }
  }

  /**
   * Shows the app open ad.
   */
  public void show() {
    // Notify the developer that a full screen view will be presented.
    listener.onAdFullScreen();
    new AlertDialog.Builder(context)
        .setTitle("Sample App Open")
        .setMessage("You are viewing a sample app open ad.")
        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // Notify the developer that the interstitial was closed.
            listener.onAdClosed();
          }
        })
        .show();
  }

  /**
   * Destroy the app open ad.
   */
  public void destroy() {
    listener = null;
  }
}
