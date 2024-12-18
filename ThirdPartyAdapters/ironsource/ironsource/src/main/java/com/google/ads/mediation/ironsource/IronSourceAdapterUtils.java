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

import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ADAPTER_ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.MobileAds;
import com.ironsource.mediationsdk.ISBannerSize;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
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

    // If none of the predefined sizes are matched, return a new IronSource size for the closest
    // size returned by Admob
    return new ISBannerSize(closestSize.getWidth(), closestSize.getHeight());
  }

  @NonNull
  public static com.unity3d.ironsourceads.AdSize getAdSizeFromGoogleAdSize(
      @NonNull Context context, @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials =
        new ArrayList<>(
            Arrays.asList(
                AdSize.BANNER, AdSize.MEDIUM_RECTANGLE, AdSize.LARGE_BANNER, AdSize.LEADERBOARD));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      return com.unity3d.ironsourceads.AdSize.banner();
    }

    if (AdSize.BANNER.equals(closestSize)) {
      return com.unity3d.ironsourceads.AdSize.banner();
    } else if (AdSize.MEDIUM_RECTANGLE.equals(closestSize)) {
      return com.unity3d.ironsourceads.AdSize.mediumRectangle();
    } else if (AdSize.LARGE_BANNER.equals(closestSize)) {
      return com.unity3d.ironsourceads.AdSize.large();
    } else if (AdSize.LEADERBOARD.equals(closestSize)) {
      return com.unity3d.ironsourceads.AdSize.leaderboard();
    }

    // If none of the predefined sizes are matched, return a banner size
    return com.unity3d.ironsourceads.AdSize.banner();
  }

  public static AdError buildAdErrorAdapterDomain(int code, @NonNull String message) {
    return new AdError(code, message, ADAPTER_ERROR_DOMAIN);
  }

  public static AdError buildAdErrorIronSourceDomain(int code, @NonNull String message) {
    return new AdError(code, message, IRONSOURCE_SDK_ERROR_DOMAIN);
  }

  public static AdError validateIronSourceAdLoadParams(
      @NonNull Context context, @NonNull String instanceID) {
    // Check that context is an Activity.
    if (!(context instanceof Activity)) {
      AdError contextError =
          new AdError(
              ERROR_REQUIRES_ACTIVITY_CONTEXT,
              "IronSource requires an Activity context to load ads.",
              ADAPTER_ERROR_DOMAIN);
      return contextError;
    }

    // Check validity of instance ID.
    if (TextUtils.isEmpty(instanceID)) {
      AdError loadError =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid instance ID.",
              ADAPTER_ERROR_DOMAIN);
      return loadError;
    }

    return null;
  }

  public static <T> boolean canLoadIronSourceAdInstance(
      @NonNull String instanceId,
      @NonNull ConcurrentHashMap<String, WeakReference<T>> instanceMap) {
    WeakReference<T> adRef = instanceMap.get(instanceId);
    return adRef == null || adRef.get() == null;
  }

  public static String getAdapterVersion() {
    return BuildConfig.ADAPTER_VERSION;
  }

  public static String prepareVersionToiAdsSdk(@NonNull String version) {
    return version.replace(".", "");
  }

  public static String getMediationType() {
    return IronSourceConstants.MEDIATION_NAME
        + prepareVersionToiAdsSdk(getAdapterVersion())
        + IronSourceConstants.SDK
        + prepareVersionToiAdsSdk(MobileAds.getVersion().toString())
        + IronSourceConstants.IADS
        + IronSourceConstants.IADS_ADAPTER_VERSION;
  }
}
