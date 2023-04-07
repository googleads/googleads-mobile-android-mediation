// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * This class is intercepting the touch events to not allow touch events to be reached to InMobi's
 * primary view. As the Adapter wants AdMob to do the click tracking. If this class is not used then
 * the click will be consumed by InMobi's primary view and publisher will not get click callback
 * from AdMob.
 */
public class ClickInterceptorRelativeLayout extends RelativeLayout {

  public ClickInterceptorRelativeLayout(Context context) {
    super(context);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    return true;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return false;
  }
}
