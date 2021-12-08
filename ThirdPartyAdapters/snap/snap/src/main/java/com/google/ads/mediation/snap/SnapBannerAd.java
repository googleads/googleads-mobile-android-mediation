package com.google.ads.mediation.snap;

import static com.google.ads.mediation.snap.SnapMediationAdapter.SLOT_ID_KEY;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.snap.adkit.external.BannerView;
import com.snap.adkit.external.LoadAdConfig;
import com.snap.adkit.external.LoadAdConfigBuilder;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdSize;
import com.snap.adkit.external.SnapBannerAdImpressionRecorded;

import java.util.ArrayList;

public class SnapBannerAd implements MediationBannerAd {

  private static final String TAG = SnapBannerAd.class.getSimpleName();

  private BannerView bannerView;

  private MediationBannerAdConfiguration adConfiguration;
  private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;
  private MediationBannerAdCallback bannerAdCallback;
  private String slotID;

  public SnapBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.callback = callback;
  }

  public void loadAd() {
    SnapAdSize adSize = getBannerSize(adConfiguration.getContext(), adConfiguration.getAdSize());
    if (adSize == SnapAdSize.INVALID) {
      AdError error =
          new AdError(
              0,
              "Failed to load banner ad from Snap. Invalid Ad Size.",
              SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    bannerView = new BannerView(adConfiguration.getContext());
    bannerView.setAdSize(adSize);
    bannerView.setupListener(
        new SnapAdEventListener() {
          @Override
          public void onEvent(
              SnapAdKitEvent snapAdKitEvent,
              @Nullable String s) {
            handleEvent(snapAdKitEvent);
          }
        });

    Bundle serverParameters = adConfiguration.getServerParameters();
    slotID = serverParameters.getString(SLOT_ID_KEY);
    if (TextUtils.isEmpty(slotID)) {
      AdError error =
          new AdError(
              0,
              "Failed to load banner ad from Snap. Missing or invalid Ad Slot ID.",
              SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    String bid = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bid)) {
      AdError error =
          new AdError(
              0,
              "Failed to load banner ad from Snap. Missing or invalid bid response.",
              SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    LoadAdConfig loadAdConfig =
        new LoadAdConfigBuilder().withPublisherSlotId(slotID).withBid(bid).build();
    bannerView.loadAd(loadAdConfig);
  }

  @Override
  @NonNull
  public View getView() {
    return bannerView.view();
  }

  private void handleEvent(SnapAdKitEvent snapAdKitEvent) {
    if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
      if (callback != null) {
        bannerAdCallback = callback.onSuccess(this);
      }
      return;
    }

    if (snapAdKitEvent instanceof SnapAdLoadFailed) {
      if (callback != null) {
        AdError error =
            new AdError(
                0,
                "Failed to load banner ad from Snap."
                    + ((SnapAdLoadFailed) snapAdKitEvent).getThrowable().getMessage(),
                SnapMediationAdapter.SNAP_AD_SDK_ERROR_DOMAIN);
        Log.w(TAG, error.getMessage());
        callback.onFailure(error);
      }
      return;
    }

    if (snapAdKitEvent instanceof SnapAdClicked) {
      if (bannerAdCallback != null) {
        bannerAdCallback.onAdOpened();
        bannerAdCallback.reportAdClicked();
        bannerAdCallback.onAdLeftApplication();
      }
      return;
    }

    if (snapAdKitEvent instanceof SnapBannerAdImpressionRecorded) {
      if (bannerAdCallback != null) {
        bannerAdCallback.reportAdImpression();
      }
    }
  }

  private SnapAdSize getBannerSize(@NonNull Context context, @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.MEDIUM_RECTANGLE);
    AdSize matchedAdSize = MediationUtils.findClosestSize(context, adSize, potentials);

    if (matchedAdSize.equals(AdSize.BANNER)) {
      return SnapAdSize.BANNER;
    }

    if (matchedAdSize.equals(AdSize.MEDIUM_RECTANGLE)) {
      return SnapAdSize.MEDIUM_RECTANGLE;
    }

    return SnapAdSize.INVALID;
  }
}
