// Copyright 2022 Google LLC
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

package com.google.ads.mediation.mintegral;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static com.google.android.gms.ads.MobileAds.getRequestConfiguration;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.out.MBConfiguration;
import java.util.ArrayList;
import java.util.List;

public class MintegralUtils {

  public static final String TAG = MintegralUtils.class.getSimpleName();

  public static String getAdapterVersion() {
    return BuildConfig.ADAPTER_VERSION;
  }

  public static String getSdkVersion() {
    return MBConfiguration.SDK_VERSION;
  }

  /** Determines whether the ad should be muted based on the provided network extras. */
  public static boolean shouldMuteAudio(@NonNull Bundle networkExtras) {
    return networkExtras.getBoolean(MintegralExtras.Keys.MUTE_AUDIO);
  }

  @Nullable
  public static AdError validateMintegralAdLoadParams(
      @Nullable String adUnitId, @Nullable String placementId) {
    if (TextUtils.isEmpty(adUnitId)) {
      AdError parameterError =
          MintegralConstants.createAdapterError(
              MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid ad Unit ID configured for this ad source instance in the"
                  + " AdMob or Ad Manager UI.");
      Log.e(TAG, parameterError.toString());
      return parameterError;
    }
    if (TextUtils.isEmpty(placementId)) {
      AdError parameterError =
          MintegralConstants.createAdapterError(
              MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid Placement ID configured for this ad source instance in the"
                  + " AdMob or Ad Manager UI.");
      Log.e(TAG, parameterError.toString());
      return parameterError;
    }
    return null;
  }

  @Nullable
  public static AdError validateMintegralAdLoadParams(
      @Nullable String adUnitId, @Nullable String placementId, @Nullable String bidToken) {
    AdError parameterError = validateMintegralAdLoadParams(adUnitId, placementId);
    if (parameterError != null) {
      return parameterError;
    }
    if (TextUtils.isEmpty(bidToken)) {
      parameterError =
          MintegralConstants.createAdapterError(
              MintegralConstants.ERROR_INVALID_BID_RESPONSE,
              "Missing or invalid Mintegral bidding signal in this ad request.");
      Log.w(TAG, parameterError.toString());
      return parameterError;
    }
    return null;
  }

  public static int convertDipToPixel(@NonNull Context context, float dipValue) {
    Resources resources = context.getResources();
    if (resources == null) {
      return 0;
    }
    // Convert dip units to pixel units based on density scale, see Android developer documentation:
    // https://developer.android.com/training/multiscreen/screendensities#dips-pels.
    return (int)
        TypedValue.applyDimension(COMPLEX_UNIT_DIP, dipValue + 0.5f, resources.getDisplayMetrics());
  }

  protected static void configureMintegralPrivacy(Context context, MBridgeSDK mBridgeSDK) {
    int tagForChildDirected = getRequestConfiguration().getTagForChildDirectedTreatment();
    boolean isTaggedForChildDirected =
        tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
    mBridgeSDK.setCoppaStatus(context, isTaggedForChildDirected);
  }

  /** Get the Mintegral slot identifiers in RtbSignalData. */
  public static List<MintegralSlotIdentifier> getMintegralSlotIdentifiers(
      RtbSignalData rtbSignalData) {
    List<MintegralSlotIdentifier> mintegralSlotIdentifiers = new ArrayList<>();
    for (MediationConfiguration adConfiguration : rtbSignalData.getConfigurations()) {
      String adUnitId =
          adConfiguration.getServerParameters().getString(MintegralConstants.AD_UNIT_ID);
      String placementId =
          adConfiguration.getServerParameters().getString(MintegralConstants.PLACEMENT_ID);
      if (!TextUtils.isEmpty(adUnitId) && !TextUtils.isEmpty(placementId)) {
        mintegralSlotIdentifiers.add(new MintegralSlotIdentifier(adUnitId, placementId));
      }
    }
    return mintegralSlotIdentifiers;
  }
}
