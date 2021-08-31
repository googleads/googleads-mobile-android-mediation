package com.google.android.gms.ads.mediation;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.ads.mediation.zucks.ZucksMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;

import com.google.ads.mediation.zucks.AdMobUtil;
import com.google.ads.mediation.zucks.ErrorMapper;
import net.zucks.listener.AdBannerListener;
import net.zucks.view.AdBanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Banner Ad Adapter implementation for calls from ZucksAdapter. Can **NOT** use this as a
 * standalone adapter implementation.
 *
 * @see com.google.android.gms.ads.mediation.ZucksAdapter ZucksAdapter
 */
class ZucksBannerAdapter extends ZucksMediationAdapter
    implements MediationBannerAd {

  @NonNull private final MediationBannerAd root;

  @NonNull private final Context context;

  @Nullable private final AdSize adSize;

  @NonNull private final Bundle serverParams;

  @NonNull
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> loadCallback;

  @Nullable private MediationBannerAdCallback adCallback = null;

  /** Banner instance of Zucks Ad Network SDK. */
  @Nullable private AdBanner zucksBanner = null;

  @VisibleForTesting @NonNull
  final AdBannerListener listener =
          new AdBannerListener() {

            @Override
            public void onReceiveAd(AdBanner banner) {
              AdError error = isValidAdSize(banner);
              if (error != null) {
                loadCallback.onFailure(error);
              } else {
                adCallback = loadCallback.onSuccess(root);
                adCallback.reportAdImpression();
              }
            }

            @Override
            public void onFailure(AdBanner banner, Exception e) {
              loadCallback.onFailure(ErrorMapper.convertSdkError(e));
            }

            @Override
            public void onTapAd(AdBanner banner) {
              adCallback.reportAdClicked();
              adCallback.onAdOpened();
              adCallback.onAdLeftApplication();
            }

            @Override
            public void onBackApplication(AdBanner banner) {
              adCallback.onAdClosed();
            }
          };

  ZucksBannerAdapter(
          @NonNull MediationBannerAd root,
          @NonNull MediationBannerAdConfiguration configuration,
          @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    this.root = root;
    this.context = configuration.getContext();
    this.adSize = configuration.getAdSize();
    this.serverParams = configuration.getServerParameters();
    this.loadCallback = mediationAdLoadCallback;
  }

  @VisibleForTesting
  void loadBannerAd() {
    String adFrameId;

    if (!isSizeSupported(context, adSize)) {
      loadCallback.onFailure(
              ErrorMapper.createAdapterError(
                      ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST,
                      "It is not a supported size. size=" + adSize)
      );
      return;
    }

    if ((adFrameId = AdMobUtil.getFrameId(serverParams)) == null) {
      loadCallback.onFailure(
              ErrorMapper.createAdapterError(
                      ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST,
                      "FrameID not contained in serverParameters.")
      );
      return;
    }

    zucksBanner = new AdBanner(context, adFrameId, listener);
    AdMobUtil.configurePlatform(zucksBanner);
    zucksBanner.load();
  }

  @NonNull
  @Override
  public View getView() {
    return zucksBanner;
  }

  /** Validate passed size are supported in Zucks Ad Network SDK. */
  @VisibleForTesting
  static boolean isSizeSupported(@NonNull Context context, @Nullable AdSize adSize) {
    if (adSize == null) {
      return false;
    }

    List<AdSize> supported = new ArrayList<>();

    supported.add(AdSize.BANNER);
    supported.add(new AdSize(320, 50));
    supported.add(AdSize.LARGE_BANNER);
    supported.add(AdSize.MEDIUM_RECTANGLE);

    return MediationUtils.findClosestSize(context, adSize, supported) != null;
  }

  @VisibleForTesting
  @Nullable
  AdError isValidAdSize(@NonNull AdBanner banner) {
    return isValidAdSize(adSize, banner);
  }

  /** For internal assertion. Validate passed size and actual size are equals. */
  @VisibleForTesting
  @Nullable
  static AdError isValidAdSize(@Nullable AdSize adSize, @NonNull AdBanner banner) {
    if (adSize == null || adSize.getWidth() != banner.getWidthInDp()
        || adSize.getHeight() != banner.getHeightInDp()) {
      return ErrorMapper.createAdapterError(
          ErrorMapper.ADAPTER_ERROR_ILLEGAL_STATE, "It is not a supported size. size=" + adSize);
    } else {
      return null;
    }
  }

}
