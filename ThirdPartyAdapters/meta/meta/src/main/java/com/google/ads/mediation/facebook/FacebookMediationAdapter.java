// Copyright 2019 Google LLC
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

package com.google.ads.mediation.facebook;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.ads.AdSettings;
import com.facebook.ads.BidderTokenProvider;
import com.google.ads.mediation.facebook.rtb.FacebookRtbBannerAd;
import com.google.ads.mediation.facebook.rtb.FacebookRtbInterstitialAd;
import com.google.ads.mediation.facebook.rtb.FacebookRtbNativeAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class FacebookMediationAdapter extends RtbAdapter {

  public static final String TAG = FacebookMediationAdapter.class.getSimpleName();

  public static final String KEY_ID = "id";
  public static final String KEY_SOCIAL_CONTEXT_ASSET = "social_context";

  private FacebookRtbBannerAd banner;
  private FacebookRtbInterstitialAd interstitial;
  private FacebookRtbNativeAd nativeAd;
  private FacebookRewardedAd rewardedAd;
  private FacebookRewardedInterstitialAd rewardedInterstitialAd;

  public static final String PLACEMENT_PARAMETER = "pubid";
  public static final String RTB_PLACEMENT_PARAMETER = "placement_id";

  /**
   * Meta Audience Network adapter errors.
   */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {
      ERROR_INVALID_SERVER_PARAMETERS,
      ERROR_BANNER_SIZE_MISMATCH,
      ERROR_REQUIRES_ACTIVITY_CONTEXT,
      ERROR_FACEBOOK_INITIALIZATION,
      ERROR_REQUIRES_UNIFIED_NATIVE_ADS,
      ERROR_WRONG_NATIVE_TYPE,
      ERROR_NULL_CONTEXT,
      ERROR_MAPPING_NATIVE_ASSETS,
      ERROR_CREATE_NATIVE_AD_FROM_BID_PAYLOAD,
      ERROR_FAILED_TO_PRESENT_AD,
      ERROR_ADVIEW_CONSTRUCTOR_EXCEPTION
  })

  public @interface AdapterError {

  }

  /**
   * Server parameters (e.g. placement ID) are nil.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Meta Audience Network supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * The publisher must request ads with an activity context.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

  /**
   * The Meta Audience Network SDK failed to initialize.
   */
  public static final int ERROR_FACEBOOK_INITIALIZATION = 104;

  /**
   * The publisher did not request Unified native ads.
   */
  public static final int ERROR_REQUIRES_UNIFIED_NATIVE_ADS = 105;

  /**
   * The native ad loaded is a different object than the one expected.
   */
  public static final int ERROR_WRONG_NATIVE_TYPE = 106;

  /**
   * The context is null (unexpected, would be GMA SDK or adapter bug).
   */
  public static final int ERROR_NULL_CONTEXT = 107;

  /**
   * The loaded ad is missing the required native ad assets.
   */
  public static final int ERROR_MAPPING_NATIVE_ASSETS = 108;

  /**
   * Failed to create native ad from bid payload.
   */
  public static final int ERROR_CREATE_NATIVE_AD_FROM_BID_PAYLOAD = 109;

  /**
   * Meta Audience Network failed to present their interstitial/rewarded ad.
   */
  public static final int ERROR_FAILED_TO_PRESENT_AD = 110;

  /**
   * Exception thrown when creating a Meta Audience Network {@link com.facebook.ads.AdView} object.
   */
  public static final int ERROR_ADVIEW_CONSTRUCTOR_EXCEPTION = 111;

  // Meta Audience Network adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.facebook";

  // Meta Audience Network SDK error domain.
  public static final String FACEBOOK_SDK_ERROR_DOMAIN = "com.facebook.ads";

  private final MetaFactory metaFactory;

  public FacebookMediationAdapter() {
    metaFactory = new MetaFactory();
  }

  /** Converts Meta Audience Network SDK error codes to admob error codes {@link AdError}. */
  @NonNull
  public static AdError getAdError(com.facebook.ads.AdError error) {
    return new AdError(error.getErrorCode(), error.getErrorMessage(), FACEBOOK_SDK_ERROR_DOMAIN);
  }

  @Override
  @NonNull
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.ADAPTER_VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format("Unexpected adapter version format: %s." +
        "Returning 0.0.0 for adapter version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  @NonNull
  public VersionInfo getSDKVersionInfo() {
    String versionString = com.facebook.ads.BuildConfig.VERSION_NAME;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format("Unexpected SDK version format: %s." +
        "Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull final Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    ArrayList<String> placements = new ArrayList<>();
    for (MediationConfiguration adConfiguration : mediationConfigurations) {
      Bundle serverParameters = adConfiguration.getServerParameters();

      String placementID = getPlacementID(serverParameters);
      if (!TextUtils.isEmpty(placementID)) {
        placements.add(placementID);
      }
    }

    if (placements.isEmpty()) {
      initializationCompleteCallback.onInitializationFailed(
          "Initialization failed. No placement IDs found.");
      return;
    }

    FacebookInitializer.getInstance().initialize(context, placements,
        new FacebookInitializer.Listener() {
          @Override
          public void onInitializeSuccess() {
            initializationCompleteCallback.onInitializationSucceeded();
          }

          @Override
          public void onInitializeError(AdError error) {
            initializationCompleteCallback.onInitializationFailed(error.getMessage());
          }
        });
  }

  @Override
  public void collectSignals(RtbSignalData rtbSignalData, SignalCallbacks signalCallbacks) {
    String token = BidderTokenProvider.getBidderToken(rtbSignalData.getContext());
    signalCallbacks.onSuccess(token);
  }

  @Override
  public void loadRtbBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {
    banner = new FacebookRtbBannerAd(adConfiguration, mediationAdLoadCallback);
    banner.render();
  }

  @Override
  public void loadRtbInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback) {
    interstitial =
        new FacebookRtbInterstitialAd(adConfiguration, mediationAdLoadCallback, metaFactory);
    interstitial.render();
  }

  @Override
  public void loadRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    rewardedAd = new FacebookRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback);
    rewardedAd.render();
  }

  @Override
  public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
          mediationAdLoadCallback) {
    nativeAd = new FacebookRtbNativeAd(mediationNativeAdConfiguration, mediationAdLoadCallback);
    nativeAd.render();
  }

  @Override
  public void loadRtbRewardedInterstitialAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    rewardedInterstitialAd = new FacebookRewardedInterstitialAd(mediationRewardedAdConfiguration,
        mediationAdLoadCallback);
    rewardedInterstitialAd.render();
  }

  /**
   * Gets the Meta Audience Network placement ID.
   */
  public static @Nullable
  String getPlacementID(@NonNull Bundle serverParameters) {
    // Bidding uses a different key for Placement ID than waterfall mediation. Try the
    // bidding key first.
    String placementId = serverParameters.getString(RTB_PLACEMENT_PARAMETER);
    if (placementId == null) {
      // Fall back to checking the waterfall mediation key.
      placementId = serverParameters.getString(PLACEMENT_PARAMETER);
    }
    return placementId;
  }

  /**
   * Sets the Meta Audience Network mixed audience settings.
   */
  public static void setMixedAudience(@NonNull MediationAdConfiguration mediationAdConfiguration) {
    if (mediationAdConfiguration.taggedForChildDirectedTreatment()
        == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
      AdSettings.setMixedAudience(true);
    } else if (mediationAdConfiguration.taggedForChildDirectedTreatment()
        == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
      AdSettings.setMixedAudience(false);
    }
  }
}
