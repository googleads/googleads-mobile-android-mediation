// Copyright 2018 Google LLC
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

package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceConstants.ERROR_MISSING_ACTIVITY_CONTEXT;
import static com.google.ads.mediation.ironsource.IronSourceConstants.TAG;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.ironsource.mediationsdk.ISBannerSize;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link IronSourceAdapterUtils} class provides the publisher an ability to pass Activity to
 * IronSource SDK, as well as some helper methods for the IronSource adapters.
 */
public class IronSourceAdapterUtils {

  @Nullable
  public static ISBannerSize getISBannerSizeFromGoogleAdSize(
      @NonNull Context context, @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.MEDIUM_RECTANGLE);
    potentials.add(AdSize.LARGE_BANNER);

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      return null;
	}

	if (AdSize.BANNER.equals(closestSize)) {
	return ISBannerSize.BANNER;
	} else if (AdSize.MEDIUM_RECTANGLE.equals(closestSize)) {
	return ISBannerSize.RECTANGLE;
	} else if (AdSize.LARGE_BANNER.equals(closestSize)) {
	return ISBannerSize.LARGE;
	}

	return new ISBannerSize(closestSize.getWidth(), closestSize.getHeight());
  }

  public static AdError validateIronSourceAdLoadParams(Context context, String instanceID) {
    if (!(context instanceof Activity)) {
      String errorMessage =
          ERROR_MISSING_ACTIVITY_CONTEXT + "IronSource requires an Activity context to load ads.";
      Log.e(TAG, errorMessage);
      AdError contextError =
          new AdError(ERROR_MISSING_ACTIVITY_CONTEXT, errorMessage, ERROR_DOMAIN);
      return contextError;
    }

    if (TextUtils.isEmpty(instanceID)) {
      AdError loadError =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid instance ID.", ERROR_DOMAIN);
      return loadError;
    }

    return null;
  }

  public static <T> boolean canLoadIronSourceAdInstance(
      @NonNull String instanceId, @NonNull ConcurrentHashMap<String, T> instanceMap) {
    T adUnit = instanceMap.get(instanceId);
    return (adUnit == null);
  }
}
