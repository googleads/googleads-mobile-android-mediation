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
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
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
  private FrameLayout wrappedAdView;
  private MediationBannerAdCallback mediationBannerAdCallback;

  public InMobiBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    this.mediationBannerAdConfiguration = mediationBannerAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  /** Invokes the InMobi SDK method for loading the ad. */
  protected abstract void internalLoadAd(InMobiBanner adView);

  public void loadAd() {
    final Context context = mediationBannerAdConfiguration.getContext();
    final Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();

    final AdSize inMobiMediationAdSize = InMobiAdapterUtils.findClosestBannerSize(context,
        mediationBannerAdConfiguration.getAdSize());
    if (inMobiMediationAdSize == null) {
      AdError mismatchError = InMobiConstants.createAdapterError(ERROR_BANNER_SIZE_MISMATCH,
          String.format("The requested banner size: %s is not supported by InMobi SDK.",
              mediationBannerAdConfiguration.getAdSize()));
      Log.e(TAG, mismatchError.toString());
      mediationAdLoadCallback.onFailure(mismatchError);
      return;
    }

    final String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
    AdError error = InMobiAdapterUtils.validateInMobiAdLoadParams(accountID, placementId);
    if (error != null) {
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    InMobiInitializer.getInstance().init(context, accountID, new InMobiInitializer.Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadBannerAd(context, placementId);
      }

      @Override
      public void onInitializeError(@NonNull AdError error) {
        Log.w(TAG, error.toString());
        if (mediationAdLoadCallback != null) {
          mediationAdLoadCallback.onFailure(error);
        }
      }
    });
  }

  private void createAndLoadBannerAd(Context context, long placementId) {
    FrameLayout.LayoutParams wrappedLayoutParams = new FrameLayout.LayoutParams(
        mediationBannerAdConfiguration.getAdSize().getWidthInPixels(context),
        mediationBannerAdConfiguration.getAdSize().getHeightInPixels(context));

    InMobiBanner adView = new InMobiBanner(context, placementId);

    // Turn off automatic refresh.
    adView.setEnableAutoRefresh(false);
    // Turn off the animation.
    adView.setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF);

    // Set the COPPA value in InMobi SDK
    InMobiAdapterUtils.setIsAgeRestricted(mediationBannerAdConfiguration);

    Bundle mediationExtras = mediationBannerAdConfiguration.getMediationExtras();

    adView.setListener(InMobiBannerAd.this);

    /*
     * Wrap InMobi's ad view to limit the dependency on its methods. For example, the method
     * that specifies the width and height for the ad view.
     */
    wrappedAdView = new FrameLayout(context);
    wrappedAdView.setLayoutParams(wrappedLayoutParams);
    adView.setLayoutParams(new LinearLayout.LayoutParams(
        mediationBannerAdConfiguration.getAdSize().getWidthInPixels(context),
        mediationBannerAdConfiguration.getAdSize().getHeightInPixels(context)));
    wrappedAdView.addView(adView);
    InMobiAdapterUtils.configureGlobalTargeting(mediationExtras);
    internalLoadAd(adView);
  }

  @NonNull
  @Override
  public View getView() {
    return wrappedAdView;
  }

  @Override
  public void onUserLeftApplication(@NonNull InMobiBanner inMobiBanner) {
    Log.d(TAG, "InMobi banner ad has caused the user to leave the application.");
    mediationBannerAdCallback.onAdLeftApplication();
  }

  @Override
  public void onRewardsUnlocked(@NonNull InMobiBanner inMobiBanner, Map<Object, Object> rewards) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdLoadSucceeded(@NonNull InMobiBanner inMobiBanner,
      @NonNull AdMetaInfo adMetaInfo) {
    Log.d(TAG, "InMobi banner ad has been loaded.");
    if (mediationAdLoadCallback != null) {
      mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(this);
    }
  }

  @Override
  public void onAdLoadFailed(@NonNull InMobiBanner inMobiBanner,
      @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
    AdError error = InMobiConstants.createSdkError(
        InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
        inMobiAdRequestStatus.getMessage());
    Log.e(TAG, error.toString());
    mediationAdLoadCallback.onFailure(error);
  }

  @Override
  public void onAdDisplayed(@NonNull InMobiBanner inMobiBanner) {
    Log.d(TAG, "InMobi banner ad opened a full screen view.");
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdDismissed(@NonNull InMobiBanner inMobiBanner) {
    Log.d(TAG, "InMobi banner ad has been dismissed.");
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.onAdClosed();
    }
  }

  @Override
  public void onAdClicked(@NonNull InMobiBanner inMobiBanner, Map<Object, Object> map) {
    Log.d(TAG, "InMobi banner ad has been clicked.");
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdImpression(@NonNull InMobiBanner inMobiBanner) {
    Log.d(TAG, "InMobi banner ad has logged an impression.");
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdImpression();
    }
  }
}
