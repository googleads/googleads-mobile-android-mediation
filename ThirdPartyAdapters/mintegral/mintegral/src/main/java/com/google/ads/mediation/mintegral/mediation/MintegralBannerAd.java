package com.google.ads.mediation.mintegral.mediation;


import static com.google.ads.mediation.mintegral.MintegralConstants.ERROR_BANNER_SIZE_UNSUPPORTED;
import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
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

public abstract class MintegralBannerAd implements MediationBannerAd, BannerAdListener {

  protected final MediationBannerAdConfiguration adConfiguration;
  protected final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback;
  protected MBBannerView mbBannerView;
  protected MediationBannerAdCallback bannerAdCallback;
  protected ArrayList<AdSize> supportedSizes = new ArrayList<>(3);

  public MintegralBannerAd(
          @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
          @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                  mediationAdLoadCallback) {
    this.adConfiguration = mediationBannerAdConfiguration;
    this.adLoadCallback = mediationAdLoadCallback;
    supportedSizes.add(new AdSize(320, 50));
    supportedSizes.add(new AdSize(300, 250));
    supportedSizes.add(new AdSize(728, 90));
  }

  /**
   * Loads a Mintegral banner ad.
   */
  public abstract void loadAd();

  protected AdSize getAdSize() {
    AdSize closestSize = MediationUtils.findClosestSize(adConfiguration.getContext(),
            adConfiguration.getAdSize(), supportedSizes);
    if (closestSize == null) {
      AdError bannerSizeError = MintegralConstants.createAdapterError(
              ERROR_BANNER_SIZE_UNSUPPORTED, String.format(
                      "The requested banner size: %s is not supported by Mintegral SDK.",
                      adConfiguration.getAdSize()));
      Log.e(TAG, bannerSizeError.toString());
      adLoadCallback.onFailure(bannerSizeError);
      return null;
    }
    return closestSize;
  }

  public BannerSize validateMintegralBannerAdSizeForAdSize(AdSize closestSize) {
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
  public void onLoadFailed(MBridgeIds mBridgeIds, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorMessage);
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
