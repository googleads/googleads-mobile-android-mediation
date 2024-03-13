/*
 * Copyright (C) 2017 Google, Inc.
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * SampleMediaView is a mocked media view returned by the Sample SDK. Normally, a media view would
 * contain an image or video asset. Since this one is just a mock, it displays a series of text
 * values instead.
 */
@SuppressLint("SetTextI18n")
public class SampleMediaView extends AppCompatTextView {

  SampleMediaViewListener listener;

  public SampleMediaView(Context context) {
    super(context);
    this.setBackgroundColor(0xFF00FF00);
    this.setGravity(Gravity.CENTER);
    this.setText("I'm a SampleMediaView.");
  }

  /**
   * Sets a {@link SampleNativeAdListener} to listen for ad events.
   *
   * @param listener The SampleMediaView listener.
   */
  public void setMediaViewListener(SampleMediaViewListener listener) {
    this.listener = listener;
  }

  /**
   * Show the text loading progress as if a video is playing.
   */
  public void beginPlaying() {
    this.setText("Playback has begun.");

    Handler handler = new Handler();
    final int runningTimeMillis = 10000;

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        SampleMediaView.this.setText("Playback is 25% finished.\nSome characters have been"
            + " introduced.");
      }
    }, runningTimeMillis / 4);

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        SampleMediaView.this.setText("Playback is 50% finished.\nThe characters have " +
            "encountered a problem.");
      }
    }, runningTimeMillis / 2);

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        SampleMediaView.this.setText("Playback is 75% finished.\nBut wait, a product " +
            "solves their problem!");
      }
    }, runningTimeMillis * 3 / 4);

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        SampleMediaView.this.setText("Playback is complete.\nWe've all learned about the " +
            "product.");

        if (listener != null) {
          listener.onVideoEnd();
        }
      }
    }, runningTimeMillis);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(widthMeasureSpec, (heightMeasureSpec * 5) / 4);
  }
}
