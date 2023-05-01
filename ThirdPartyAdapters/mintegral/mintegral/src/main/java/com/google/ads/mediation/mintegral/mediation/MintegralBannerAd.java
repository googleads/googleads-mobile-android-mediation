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

package com.google.ads.mediation.mintegral.mediation;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.mbridge.msdk.out.BannerAdWithCodeListener;
import com.mbridge.msdk.out.BannerSize;
import com.mbridge.msdk.out.MBBannerView;
import com.mbridge.msdk.out.MBridgeIds;

import java.util.ArrayList;

public abstract class MintegralBannerAd extends BannerAdWithCodeListener implements
    MediationBannerAd {

  protected MediationBannerAdConfiguration adConfiguration;
  protected final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      adLoadCallback;
  protected MBBannerView mbBannerView;
  protected MediationBannerAdCallback bannerAdCallback;

  public MintegralBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {
    this.adConfiguration = mediationBannerAdConfiguration;
    this.adLoadCallback = mediationAdLoadCallback;
  }

  /**
   * Loads a Mintegral banner ad.
   */
  public abstract void loadAd();

  @Nullable
  public static BannerSize getMintegralBannerSizeFromAdMobAdSize(@NonNull AdSize adSize,
      @NonNull Context context) {
    ArrayList<AdSize> supportedAdSizes = new ArrayList<>();
    supportedAdSizes.add(new AdSize(320, 50));
    supportedAdSizes.add(new AdSize(300, 250));
    supportedAdSizes.add(new AdSize(728, 90));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, supportedAdSizes);
    if (closestSize == null) {
      return null;
    }

    BannerSize bannerSize = null;
    if (closestSize.equals(AdSize.BANNER)) { // 320 * 50
      bannerSize = new BannerSize(BannerSize.STANDARD_TYPE, 0, 0);
    }
    if (closestSize.equals(AdSize.MEDIUM_RECTANGLE)) { // 300 * 250
      bannerSize = new BannerSize(BannerSize.MEDIUM_TYPE, 0, 0);
    }
    if (closestSize.equals(AdSize.LEADERBOARD)) { // 728 * 90
      bannerSize = new BannerSize(BannerSize.SMART_TYPE, closestSize.getWidth(), 0);
    }
    if (bannerSize == null) {
      bannerSize = new BannerSize(BannerSize.DEV_SET_TYPE, closestSize.getWidth(),
          closestSize.getHeight());
    }
    return bannerSize;
  }

  @NonNull
  @Override
  public View getView() {
    return mbBannerView;
  }

  @Override
  public void onLoadFailedWithCode(MBridgeIds mBridgeIds, int errorCode, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorCode, errorMessage);
    Log.w(TAG, error.toString());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onLoadSuccessed(MBridgeIds mBridgeIds) {
    if (adLoadCallback != null) {
      bannerAdCallback = adLoadCallback.onSuccess(this);
    }
  }

  @Override
  public void onLogImpression(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onClick(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onLeaveApp(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void showFullScreen(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void closeFullScreen(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdClosed();
    }
  }

  @Override
  public void onCloseBanner(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdClosed();
    }
  }
}
