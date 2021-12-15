package com.google.android.gms.ads.mediation;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Locale;

class ZucksBannerAdapter implements MediationBannerAd {

  private static final String TAG = "ZucksBannerAdapter";

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
                notifySdkLoadFailure(error);
              } else {
                adCallback = loadCallback.onSuccess(ZucksBannerAdapter.this);
                adCallback.reportAdImpression();
              }
            }

            @Override
            public void onFailure(AdBanner banner, Exception e) {
              notifySdkLoadFailure(e);
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
          @NonNull MediationBannerAdConfiguration configuration,
          @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback
  ) {
    this(
            configuration.getContext(),
            configuration.getAdSize(),
            configuration.getServerParameters(),
            mediationAdLoadCallback
    );
  }

  @VisibleForTesting
  ZucksBannerAdapter(
          @NonNull Context context,
          @Nullable AdSize adSize,
          @NonNull Bundle serverParams,
          @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> loadCallback
  ) {
    this.context = context;
    this.adSize = adSize;
    this.serverParams = serverParams;
    this.loadCallback = loadCallback;
  }

  public void loadBannerAd() {
    String adFrameId;

    if (!isSizeSupported(context, adSize)) {
      notifyAdapterLoadFailure(
              ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST,
              "It is not a supported size. size=" + adSize
      );
      return;
    }

    if ((adFrameId = AdMobUtil.getFrameId(serverParams)) == null) {
      notifyAdapterLoadFailure(
              ErrorMapper.ADAPTER_ERROR_INVALID_REQUEST,
              "FrameID not contained in serverParameters."
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

  @Nullable
  private AdError isValidAdSize(@NonNull AdBanner banner) {
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

  // region Notify and logging errors
  // @see <a href="https://github.com/googleads/googleads-mobile-android-mediation/pull/337#discussion_r764662057">GitHub review</a>
  private void notifyAdapterLoadFailure(@ErrorMapper.AdapterError int code, @NonNull String msg) {
    Log.w(TAG, String.format(Locale.ROOT, "%d: %s", code, msg));
    loadCallback.onFailure(ErrorMapper.createAdapterError(code, msg));
  }

  private void notifySdkLoadFailure(@NonNull Exception exception) {
    Log.w(TAG, exception);
    loadCallback.onFailure(ErrorMapper.convertSdkError(exception));
  }

  private void notifySdkLoadFailure(@NonNull AdError error) {
    Log.w(TAG, String.format(Locale.ROOT, "%d: %s", error.getCode(), error.getMessage()));
    loadCallback.onFailure(error);
  }
  // endregion

}
