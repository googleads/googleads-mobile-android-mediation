// Copyright 2023 Google LLC
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

package com.google.ads.mediation.pangle;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGGDPRConsentType;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest;
import com.bytedance.sdk.openadsdk.api.bidding.PAGBiddingRequest;
import com.bytedance.sdk.openadsdk.api.init.BiddingTokenCallback;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAd;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdLoadListener;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenRequest;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest;

/**
 * A wrapper for Pangle SDK's static methods that the adapter calls.
 *
 * <p>This wrapper exists to make it possible to mock Pangle SDK's static methods during unit
 * testing.
 */
public class PangleSdkWrapper {

  public void init(Context context, PAGConfig config, PAGSdk.PAGInitCallback callback) {
    PAGSdk.init(context, config, callback);
  }

  boolean isInitSuccess() {
    return PAGSdk.isInitSuccess();
  }


  void setGdprConsent(@PAGGDPRConsentType int gdpr) {
    PAGConfig.setGDPRConsent(gdpr);
  }


  void setUserData(String userData) {
    PAGConfig.setUserData(userData);
  }

  void getBiddingToken(Context context, PAGBiddingRequest biddingRequest,BiddingTokenCallback biddingTokenCallback) {
    PAGSdk.getBiddingToken(context,biddingRequest,biddingTokenCallback);
  }

  String getSdkVersion() {
    return PAGSdk.getSDKVersion();
  }

  public void loadBannerAd(
      String placementId, PAGBannerRequest request, PAGBannerAdLoadListener listener) {
    PAGBannerAd.loadAd(placementId, request, listener);
  }

  public void loadInterstitialAd(
      String placementId, PAGInterstitialRequest request, PAGInterstitialAdLoadListener listener) {
    PAGInterstitialAd.loadAd(placementId, request, listener);
  }

  public void loadNativeAd(
      String placementId, PAGNativeRequest pagNativeRequest, PAGNativeAdLoadListener listener) {
    PAGNativeAd.loadAd(placementId, pagNativeRequest, listener);
  }

  public void loadRewardedAd(
      String placementId, PAGRewardedRequest request, PAGRewardedAdLoadListener listener) {
    PAGRewardedAd.loadAd(placementId, request, listener);
  }

  public void loadAppOpenAd(
      @NonNull String unitId,
      @NonNull PAGAppOpenRequest request,
      @NonNull PAGAppOpenAdLoadListener listener) {
    PAGAppOpenAd.loadAd(unitId, request, listener);
  }
}
