// Copyright 2016 Google Inc.
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

package com.google.ads.mediation.chartboost;

import androidx.annotation.NonNull;
import com.chartboost.sdk.ChartboostDelegate;
import com.google.ads.mediation.chartboost.ChartboostMediationAdapter.AdapterError;
import com.google.android.gms.ads.AdError;

/**
 * The {@link AbstractChartboostAdapterDelegate} class will be used to mediate callbacks between
 * {@link ChartboostSingleton} and Chartboost adapters.
 */
public abstract class AbstractChartboostAdapterDelegate extends ChartboostDelegate {

  /**
   * This method should return the {@link ChartboostParams} used by the adapter.
   *
   * @return Chartboost params containing ad request parameters.
   */
  public abstract ChartboostParams getChartboostParams();

  /**
   * Called when the adapter fails to load an ad.
   *
   * @param loadError the {@link AdError} object.
   */
  public abstract void onAdFailedToLoad(@NonNull AdError loadError);
}
