package com.google.ads.mediation.snap;

import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.snap.SnapMediationAdapter.ERROR_SNAP_SDK_LOAD_FAILURE;
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

  private final MediationBannerAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;
  private MediationBannerAdCallback bannerAdCallback;

  public SnapBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.callback = callback;
  }

  public void loadAd() {
    SnapAdSize adSize = getBannerSize(adConfiguration.getContext(), adConfiguration.getAdSize());
    if (adSize == SnapAdSize.INVALID) {
      String errorMessage = String
          .format("Failed to load banner ad from Snap: %s is not a supported ad size.",
              adConfiguration.getAdSize());
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH, errorMessage, ERROR_DOMAIN);
      Log.e(TAG, error.toString());
      callback.onFailure(error);
      return;
    }

    bannerView = new BannerView(adConfiguration.getContext());
    bannerView.setAdSize(adSize);
    bannerView.setupListener(
        new SnapAdEventListener() {
          @Override
          public void onEvent(SnapAdKitEvent snapAdKitEvent, @Nullable String s) {
            handleEvent(snapAdKitEvent);
          }
        });

    Bundle serverParameters = adConfiguration.getServerParameters();
    String slotID = serverParameters.getString(SLOT_ID_KEY);
    if (TextUtils.isEmpty(slotID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load banner ad from Snap: Missing or invalid Ad Slot ID.", ERROR_DOMAIN);
      Log.e(TAG, error.toString());
      callback.onFailure(error);
      return;
    }

    String bid = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bid)) {
      AdError error = new AdError(ERROR_INVALID_BID_RESPONSE,
          "Failed to load banner ad from Snap: Missing or invalid bid response.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
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

  // TODO: This method is only called once and should be inlined.
  private void handleEvent(SnapAdKitEvent snapAdKitEvent) {
    if (snapAdKitEvent instanceof SnapAdLoadSucceeded) {
      bannerAdCallback = callback.onSuccess(SnapBannerAd.this);
      return;
    }

    if (snapAdKitEvent instanceof SnapAdLoadFailed) {
      SnapAdLoadFailed snapAdLoadFailedEvent = (SnapAdLoadFailed) snapAdKitEvent;
      String errorMessage = String
          .format("Snap SDK returned a banner ad load failed event with message: %s",
              snapAdLoadFailedEvent.getThrowable().getMessage());
      AdError error = new AdError(ERROR_SNAP_SDK_LOAD_FAILURE, errorMessage, ERROR_DOMAIN);
      Log.i(TAG, error.toString());
      callback.onFailure(error);
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

  @NonNull
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
