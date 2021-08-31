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
    implements MediationBannerAdapter, MediationBannerAd {

  /** New adapter implementation */
  @VisibleForTesting
  static class ZucksMediationBannerAd implements MediationBannerAd {

    @NonNull private final ZucksBannerAdapter self;

    @Nullable
    private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> loadCallback;

    @Nullable private MediationBannerAdCallback adCallback = null;

    @VisibleForTesting @NonNull
    final AdBannerListener listener =
        new AdBannerListener() {

          @Override
          public void onReceiveAd(AdBanner banner) {
            AdError error = self.isValidAdSize(banner);
            if (error != null) {
              loadCallback.onFailure(error);
            } else {
              adCallback = loadCallback.onSuccess(self.root);
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

    @VisibleForTesting
    ZucksMediationBannerAd(@NonNull ZucksBannerAdapter self) {
      this.self = self;
    }

    @VisibleForTesting
    void loadBannerAd(
        @NonNull MediationBannerAdConfiguration configuration,
        @NonNull
            MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                mediationAdLoadCallback) {
      setLoadCallback(mediationAdLoadCallback);

      AdError error =
          self.configureBannerAd(
              configuration.getContext(),
              configuration.getAdSize(),
              configuration.getServerParameters(),
              listener);
      if (error != null) {
        loadCallback.onFailure(error);
      }
    }

    @VisibleForTesting
    void setLoadCallback(
        @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
      this.loadCallback = callback;
    }

    @NonNull
    @Override
    public View getView() {
      return self.zucksBanner;
    }
  }

  /** Legacy adapter implementation This class will be removed. */
  @VisibleForTesting
  @Deprecated
  static class ZucksMediationBannerAdapter implements MediationBannerAdapter {

    @NonNull private final ZucksBannerAdapter self;

    @Nullable private MediationBannerListener callback = null;

    @VisibleForTesting @NonNull
    final AdBannerListener listener =
        new AdBannerListener() {

          @Override
          public void onReceiveAd(AdBanner banner) {
            AdError error = self.isValidAdSize(banner);
            if (error != null) {
              callback.onAdFailedToLoad(self.root, error);
            } else {
              callback.onAdLoaded(self.root);
            }
          }

          @Override
          public void onFailure(AdBanner banner, Exception e) {
            callback.onAdFailedToLoad(self.root, ErrorMapper.convertSdkError(e));
          }

          @Override
          public void onTapAd(AdBanner banner) {
            callback.onAdClicked(self.root);
            callback.onAdOpened(self.root);
            callback.onAdLeftApplication(self.root);
          }

          @Override
          public void onBackApplication(AdBanner banner) {
            callback.onAdClosed(self.root);
          }
        };

    @VisibleForTesting
    ZucksMediationBannerAdapter(@NonNull ZucksBannerAdapter self) {
      this.self = self;
    }

    @Override
    public void requestBannerAd(
        Context context,
        MediationBannerListener callback,
        Bundle serverParameters,
        AdSize adSize,
        MediationAdRequest mediationAdRequest,
        Bundle mediationExtras) {
      AdError error = self.configureBannerAd(context, adSize, serverParameters, listener);
      if (error != null) {
        callback.onAdFailedToLoad(self.root, error);
      } else {
        setCallback(callback);
      }
    }

    @VisibleForTesting
    void setCallback(@NonNull MediationBannerListener callback) {
      this.callback = callback;
    }

    @Override
    public View getBannerView() {
      return self.zucksBanner;
    }

    @Override
    public void onDestroy() {
      // no-op
    }

    @Override
    public void onPause() {
      // no-op
    }

    @Override
    public void onResume() {
      // no-op
    }
  }

  @NonNull private final ZucksAdapter root;

  /** Banner instance of Zucks Ad Network SDK. */
  @Nullable private AdBanner zucksBanner = null;

  /** Size instance of Zucks Ad Network SDK. */
  @Nullable private AdSize adSize = null;

  // region New adapter
  @Nullable private ZucksMediationBannerAd mediationBannerAd = null;

  @NonNull
  private ZucksMediationBannerAd useBannerAd() {
    if (mediationBannerAd == null) {
      mediationBannerAd = new ZucksMediationBannerAd(this);
    }
    return mediationBannerAd;
  }
  // endregion

  // region Legacy adapter
  @Deprecated @Nullable private ZucksMediationBannerAdapter mediationBannerAdapter = null;

  @Deprecated
  @NonNull
  private ZucksMediationBannerAdapter useBannerAdapter() {
    if (mediationBannerAdapter == null) {
      mediationBannerAdapter = new ZucksMediationBannerAdapter(this);
    }
    return mediationBannerAdapter;
  }
  // endregion

  ZucksBannerAdapter(@NonNull ZucksAdapter root) {
    this.root = root;
  }

  @Override
  public void loadBannerAd(
      MediationBannerAdConfiguration mediationBannerAdConfiguration,
      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {
    useBannerAd().loadBannerAd(mediationBannerAdConfiguration, mediationAdLoadCallback);
  }

  @Deprecated
  @Override
  public void requestBannerAd(
      Context context,
      MediationBannerListener mediationBannerListener,
      Bundle serverParameters,
      AdSize adSize,
      MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    useBannerAdapter()
        .requestBannerAd(
            context,
            mediationBannerListener,
            serverParameters,
            adSize,
            mediationAdRequest,
            mediationExtras);
  }

  @VisibleForTesting
  @Nullable
  AdError configureBannerAd(
      Context context, AdSize adSize, Bundle serverParams, AdBannerListener listener) {
    if (!isSizeSupported(context, adSize)) {
      return ErrorMapper.createAdapterError(
          ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST, "It is not a supported size. size=" + adSize);
    }

    String adFrameId = AdMobUtil.getFrameId(serverParams);

    if (adFrameId == null) {
      return ErrorMapper.createAdapterError(
          ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST, "FrameID not contained in serverParameters.");
    } else {
      this.adSize = adSize;
      zucksBanner = new AdBanner(context, adFrameId, listener);
      AdMobUtil.configurePlatform(zucksBanner);
      zucksBanner.load();
      return null;
    }
  }

  @Deprecated
  @Override
  public View getBannerView() {
    return useBannerAdapter().getBannerView();
  }

  @NonNull
  @Override
  public View getView() {
    return useBannerAd().getView();
  }

  @Deprecated
  @Override
  public void onDestroy() {
    useBannerAdapter().onDestroy();
  }

  @Deprecated
  @Override
  public void onPause() {
    useBannerAdapter().onPause();
  }

  @Deprecated
  @Override
  public void onResume() {
    useBannerAdapter().onResume();
  }

  /** Validate passed size are supported in Zucks Ad Network SDK. */
  @VisibleForTesting
  static boolean isSizeSupported(@NonNull Context context, @NonNull AdSize adSize) {
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
  static AdError isValidAdSize(@NonNull AdSize adSize, @NonNull AdBanner banner) {
    if (adSize.getWidth() != banner.getWidthInDp()
        || adSize.getHeight() != banner.getHeightInDp()) {
      return ErrorMapper.createAdapterError(
          ErrorMapper.ADAPTER_ERROR_ILLEGAL_STATE, "It is not a supported size. size=" + adSize);
    } else {
      return null;
    }
  }
}
