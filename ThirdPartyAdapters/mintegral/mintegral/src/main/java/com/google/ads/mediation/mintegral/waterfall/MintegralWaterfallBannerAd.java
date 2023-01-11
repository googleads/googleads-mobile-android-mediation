package com.google.ads.mediation.mintegral.waterfall;


import static com.google.ads.mediation.mintegral.MintegralConstants.ERROR_BANNER_SIZE_UNSUPPORTED;
import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralBannerAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.mbridge.msdk.out.BannerSize;
import com.mbridge.msdk.out.MBBannerView;

import java.util.ArrayList;

public class MintegralWaterfallBannerAd extends MintegralBannerAd {

  public MintegralWaterfallBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
                                    @NonNull MediationAdLoadCallback<MediationBannerAd,
                                            MediationBannerAdCallback> mediationAdLoadCallback) {
    super(mediationBannerAdConfiguration, mediationAdLoadCallback);
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
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId);
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
    mbBannerView.load();
  }
}
