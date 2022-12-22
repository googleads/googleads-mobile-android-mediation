package com.google.ads.mediation.mintegral.rtb;


import static com.google.ads.mediation.mintegral.MintegralConstants.ERROR_BANNER_SIZE_UNSUPPORTED;
import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.mbridge.msdk.out.BannerAdListener;
import com.mbridge.msdk.out.BannerSize;
import com.mbridge.msdk.out.MBBannerView;
import com.mbridge.msdk.out.MBridgeIds;

import java.util.ArrayList;

public class MintegralRtbBannerAd implements MediationBannerAd, BannerAdListener {

  private MBBannerView mbBannerView;
  private final MediationBannerAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback;
  private MediationBannerAdCallback bannerAdCallback;

  public MintegralRtbBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {
    this.adConfiguration = mediationBannerAdConfiguration;
    this.adLoadCallback = mediationAdLoadCallback;
  }

  public void loadAd() {
    BannerSize bannerSize = null;
    ArrayList<AdSize> supportedSizes = new ArrayList<>(3);
    supportedSizes.add(new AdSize(320, 50));
    supportedSizes.add(new AdSize(300, 250));
    supportedSizes.add(new AdSize(728, 90));
    AdSize closestSize = MediationUtils.findClosestSize(adConfiguration.getContext(),
        adConfiguration.getAdSize(), supportedSizes);
    if (closestSize == null) {
      AdError bannerSizeError = MintegralConstants.createAdapterError(
          ERROR_BANNER_SIZE_UNSUPPORTED, String.format(
              "The requested banner size: %s is not supported by Mintegral SDK.",
              adConfiguration.getAdSize()));
      Log.e(TAG, bannerSizeError.toString());
      adLoadCallback.onFailure(bannerSizeError);
      return;
    }

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

    String adUnitId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.PLACEMENT_ID);
    String bidToken = adConfiguration.getBidResponse();
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId, bidToken);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }
    mbBannerView = new MBBannerView(adConfiguration.getContext());
    mbBannerView.init(bannerSize, placementId, adUnitId);
    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
        closestSize.getWidthInPixels(adConfiguration.getContext()),
        closestSize.getHeightInPixels(adConfiguration.getContext()));
    mbBannerView.setLayoutParams(layoutParams);
    mbBannerView.setBannerAdListener(this);
    mbBannerView.loadFromBid(bidToken);
  }

  @NonNull
  @Override
  public View getView() {
    return mbBannerView;
  }

  @Override
  public void onLoadFailed(MBridgeIds mBridgeIds, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorMessage);
    Log.w(TAG, error.toString());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onLoadSuccessed(MBridgeIds mBridgeIds) {
    if (adLoadCallback == null) {
      return;
    }
    bannerAdCallback = adLoadCallback.onSuccess(this);
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
