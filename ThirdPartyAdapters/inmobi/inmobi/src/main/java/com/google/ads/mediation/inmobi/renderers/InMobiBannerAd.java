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

package com.google.ads.mediation.inmobi.renderers;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdViewHolder;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiBannerWrapper;
import com.google.ads.mediation.inmobi.InMobiConstants;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.listeners.BannerAdEventListener;
import java.util.Map;

public abstract class InMobiBannerAd extends BannerAdEventListener implements MediationBannerAd {

  protected final MediationBannerAdConfiguration mediationBannerAdConfiguration;
  protected final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      mediationAdLoadCallback;
  private MediationBannerAdCallback mediationBannerAdCallback;
  private InMobiAdViewHolder inMobiAdViewHolder;
  private InMobiInitializer inMobiInitializer;
  private InMobiAdFactory inMobiAdFactory;

  public InMobiBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    this.mediationBannerAdConfiguration = mediationBannerAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.inMobiInitializer = inMobiInitializer;
    this.inMobiAdFactory = inMobiAdFactory;
  }

  /** Invokes the InMobi SDK method for loading the ad. */
  protected abstract void internalLoadAd(InMobiBannerWrapper adView);

  public void loadAd() {
    final Context context = mediationBannerAdConfiguration.getContext();
    final AdSize closestBannerSize =
        InMobiAdapterUtils.findClosestBannerSize(
            context, mediationBannerAdConfiguration.getAdSize());
    if (closestBannerSize == null) {
      AdError bannerSizeError =
          InMobiConstants.createAdapterError(
              ERROR_BANNER_SIZE_MISMATCH,
              String.format(
                  "The requested banner size: %s is not supported by InMobi SDK.",
                  mediationBannerAdConfiguration.getAdSize()));
      Log.e(TAG, bannerSizeError.toString());
      mediationAdLoadCallback.onFailure(bannerSizeError);
      return;
    }

    final Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();
    final String accountId = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
    AdError error = InMobiAdapterUtils.validateInMobiAdLoadParams(accountId, placementId);
    if (error != null) {
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    inMobiInitializer.init(
        context,
        accountId,
        new InMobiInitializer.Listener() {
          @Override
          public void onInitializeSuccess() {
            createAndLoadBannerAd(context, placementId, closestBannerSize);
          }

          @Override
          public void onInitializeError(@NonNull AdError error) {
            Log.w(TAG, error.toString());
            mediationAdLoadCallback.onFailure(error);
          }
        });
  }

  private void createAndLoadBannerAd(final Context context, final long placementId,
      AdSize mediationBannerSize) {
    // Set the COPPA value in inMobi SDK
    InMobiAdapterUtils.setIsAgeRestricted();

    InMobiAdapterUtils.configureGlobalTargeting(
        mediationBannerAdConfiguration.getMediationExtras());

    InMobiBannerWrapper inMobiBannerWrapper =
        inMobiAdFactory.createInMobiBannerWrapper(context, placementId);
    // Turn off automatic refresh.
    inMobiBannerWrapper.setEnableAutoRefresh(false);
    // Turn off the animation.
    inMobiBannerWrapper.setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF);

    inMobiBannerWrapper.setListener(InMobiBannerAd.this);

    /*
     * Wrap InMobi's ad view to limit the dependency on its methods. For example, the method
     * that specifies the width and height for the ad view.
     */
    inMobiAdViewHolder = inMobiAdFactory.createInMobiAdViewHolder(context);
    inMobiAdViewHolder.setLayoutParams(
        new FrameLayout.LayoutParams(
            mediationBannerSize.getWidthInPixels(context),
            mediationBannerSize.getHeightInPixels(context)));

    inMobiBannerWrapper.setLayoutParams(
        new LinearLayout.LayoutParams(
            mediationBannerSize.getWidthInPixels(context),
            mediationBannerSize.getHeightInPixels(context)));
    inMobiAdViewHolder.addView(inMobiBannerWrapper);

    internalLoadAd(inMobiBannerWrapper);
  }

  @NonNull
  @Override
  public View getView() {
    return inMobiAdViewHolder.getFrameLayout();
  }

  @Override
  public void onUserLeftApplication(@NonNull InMobiBanner inMobiBanner) {
    mediationBannerAdCallback.onAdLeftApplication();
  }

  @Override
  public void onRewardsUnlocked(@NonNull InMobiBanner inMobiBanner, Map<Object, Object> rewards) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdLoadSucceeded(
      @NonNull InMobiBanner inMobiBanner, @NonNull AdMetaInfo adMetaInfo) {
    mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onAdLoadFailed(@NonNull InMobiBanner inMobiBanner,
      @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
    AdError error = InMobiConstants.createSdkError(
        InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
        inMobiAdRequestStatus.getMessage());
    Log.w(TAG, error.toString());
    mediationAdLoadCallback.onFailure(error);
  }

  @Override
  public void onAdDisplayed(@NonNull InMobiBanner inMobiBanner) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdDismissed(@NonNull InMobiBanner inMobiBanner) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.onAdClosed();
    }
  }

  @Override
  public void onAdClicked(@NonNull InMobiBanner inMobiBanner, Map<Object, Object> map) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdImpression(@NonNull InMobiBanner inMobiBanner) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdImpression();
    }
  }
}
