package com.vungle.mediation;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RelativeLayout;

import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.ads.mediation.vungle.VunglePlayAdCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.vungle.warren.AdConfig;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_VUNGLE_BANNER_NULL;

public class VungleBannerAdapter implements PlayAdCallback {

  private static final String TAG = VungleBannerAdapter.class.getSimpleName();

  /**
   * Vungle banner placement ID.
   */
  @NonNull
  private final String placementId;

  /**
   * Vungle ad configuration settings.
   */
  @NonNull
  private final AdConfig mAdConfig;

  /**
   * Unique Vungle banner request ID.
   */
  private final String uniqueRequestId;

  /**
   * Mediation Banner Adapter instance to receive callbacks.
   */
  private MediationBannerAdapter mediationAdapter;

  /**
   * Vungle listener class to forward to the adapter.
   */
  private MediationBannerListener mediationListener;

  /**
   * Mediation Banner Bidding Adapter instance.
   */
  private MediationBannerAd mediationBannerAd;

  /**
   * Vungle listener class to forward to the bidding adapter.
   */
  private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback;
  private MediationBannerAdCallback mediationBannerAdCallback;

  /**
   * Bid response of Bidding unit.
   */
  private String mAdMarkup;

  /**
   * Wrapper object for Vungle banner ads.
   */
  private VungleBannerAd vungleBannerAd;

  /**
   * Container for Vungle's banner ad view.
   */
  private RelativeLayout adLayout;

  /**
   * Manager to handle Vungle banner ad requests.
   */
  @NonNull
  private final VungleManager mVungleManager;

  /**
   * Indicates whether a Vungle banner ad request is in progress.
   */
  private boolean mPendingRequestBanner = false;

  /**
   * Indicates the Vungle banner ad's visibility.
   */
  private boolean mVisibility = true;

  VungleBannerAdapter(@NonNull String placementId, @NonNull String uniqueRequestId,
      @NonNull AdConfig adConfig, @NonNull MediationBannerAdapter mediationBannerAdapter) {
    mVungleManager = VungleManager.getInstance();
    this.placementId = placementId;
    this.uniqueRequestId = uniqueRequestId;
    this.mAdConfig = adConfig;
    this.mediationAdapter = mediationBannerAdapter;
  }

  public VungleBannerAdapter(@NonNull String placementId, @NonNull String uniqueRequestId,
      @NonNull AdConfig adConfig, @NonNull MediationBannerAd mediationBannerAd) {
    mVungleManager = VungleManager.getInstance();
    this.placementId = placementId;
    this.uniqueRequestId = uniqueRequestId;
    this.mAdConfig = adConfig;
    this.mediationBannerAd = mediationBannerAd;
  }

  @Nullable
  public String getUniqueRequestId() {
    return uniqueRequestId;
  }

  public RelativeLayout getAdLayout() {
    return adLayout;
  }

  public boolean isRequestPending() {
    return mPendingRequestBanner;
  }

  public void requestBannerAd(@NonNull Context context, @NonNull String appId,
      @NonNull AdSize adSize, @Nullable String adMarkup,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.mAdMarkup = adMarkup;

    requestBannerAd(context, appId, adSize);
  }

  void requestBannerAd(@NonNull Context context, @NonNull String appId, @NonNull AdSize adSize,
      @NonNull MediationBannerListener mediationBannerListener) {
    mediationListener = mediationBannerListener;

    requestBannerAd(context, appId, adSize);
  }

  private void requestBannerAd(Context context, String appId, AdSize adSize) {
    // Create the adLayout wrapper with the requested ad size, as Vungle's ad uses MATCH_PARENT for
    // its dimensions.
    adLayout =
        new RelativeLayout(context) {
          @Override
          protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attach();
          }

          @Override
          protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            detach();
          }
        };
    int adLayoutHeight = adSize.getHeightInPixels(context);
    // If the height is 0 (e.g. for inline adaptive banner requests), use the closest supported size
    // as the height of the adLayout wrapper.
    if (adLayoutHeight <= 0) {
      float density = context.getResources().getDisplayMetrics().density;
      adLayoutHeight = Math.round(mAdConfig.getAdSize().getHeight() * density);
    }
    RelativeLayout.LayoutParams adViewLayoutParams =
        new RelativeLayout.LayoutParams(adSize.getWidthInPixels(context), adLayoutHeight);
    adLayout.setLayoutParams(adViewLayoutParams);

    Log.d(TAG, "requestBannerAd: " + this);
    mPendingRequestBanner = true;
    VungleInitializer.getInstance()
        .initialize(
            appId,
            context.getApplicationContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                loadBanner();
              }

              @Override
              public void onInitializeError(AdError error) {
                mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
                if (mPendingRequestBanner && mediationAdapter != null
                    && mediationListener != null) {
                  Log.w(TAG, error.getMessage());
                  mediationListener.onAdFailedToLoad(mediationAdapter, error);
                } else if (mPendingRequestBanner && mediationAdLoadCallback != null) {
                  Log.w(TAG, error.getMessage());
                  mediationAdLoadCallback.onFailure(error);
                }
              }
            });
  }

  void destroy() {
    Log.d(TAG, "Vungle banner adapter destroy:" + this);
    mVisibility = false;
    mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
    if (vungleBannerAd != null) {
      vungleBannerAd.detach();
      vungleBannerAd.destroyAd();
    }
    vungleBannerAd = null;
    mPendingRequestBanner = false;
  }

  void preCache() {
    if (TextUtils.isEmpty(mAdMarkup)) {
      Banners.loadBanner(placementId, new BannerAdConfig(mAdConfig), null);
    }
  }

  void updateVisibility(boolean visible) {
    if (vungleBannerAd == null) {
      return;
    }

    this.mVisibility = visible;
    if (vungleBannerAd.getVungleBanner() != null) {
      vungleBannerAd.getVungleBanner().setAdVisibility(visible);
    }
  }

  private final LoadAdCallback mAdLoadCallback =
      new LoadAdCallback() {
        @Override
        public void onAdLoad(String id) {
          createBanner();
        }

        @Override
        public void onError(String id, VungleException exception) {
          mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);

          if (!mPendingRequestBanner) {
            Log.w(TAG, "No banner request fired.");
            return;
          }
          if (mediationAdapter != null && mediationListener != null) {
            AdError error = VungleMediationAdapter.getAdError(exception);
            Log.w(TAG, error.getMessage());
            mediationListener.onAdFailedToLoad(mediationAdapter, error);
            return;
          }
          if (mediationAdLoadCallback != null) {
            AdError error = VungleMediationAdapter.getAdError(exception);
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
          }
        }
      };

  private void loadBanner() {
    Log.d(TAG, "loadBanner: " + this);
    Banners.loadBanner(placementId, mAdMarkup, new BannerAdConfig(mAdConfig), mAdLoadCallback);
  }

  private void createBanner() {
    Log.d(TAG, "create banner: " + this);
    if (!mPendingRequestBanner) {
      return;
    }

    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
    vungleBannerAd = mVungleManager.getVungleBannerAd(placementId);
    VunglePlayAdCallback playAdCallback = new VunglePlayAdCallback(VungleBannerAdapter.this,
        VungleBannerAdapter.this, vungleBannerAd);

    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      VungleBanner vungleBanner = Banners
          .getBanner(placementId, mAdMarkup, new BannerAdConfig(mAdConfig), playAdCallback);
      if (vungleBanner != null) {
        Log.d(TAG, "display banner:" + vungleBanner.hashCode() + this);
        if (vungleBannerAd != null) {
          vungleBannerAd.setVungleBanner(vungleBanner);
        }

        updateVisibility(mVisibility);
        vungleBanner.setLayoutParams(adParams);
        // don't add to parent here
        if (mediationAdapter != null && mediationListener != null) {
          mediationListener.onAdLoaded(mediationAdapter);
          return;
        }
        if (mediationBannerAd != null && mediationAdLoadCallback != null) {
          mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(mediationBannerAd);
          return;
        }
      } else {
        AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
            "Vungle SDK returned a successful load callback, but Banners.getBanner() or "
                + "Vungle.getNativeAd() returned null.",
            ERROR_DOMAIN);
        Log.d(TAG, error.getMessage());
        if (mediationAdapter != null && mediationListener != null) {
          mediationListener.onAdFailedToLoad(mediationAdapter, error);
          return;
        }
        if (mediationAdLoadCallback != null) {
          mediationAdLoadCallback.onFailure(error);
          return;
        }
      }
    } else {
      AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
          "Vungle SDK returned a successful load callback, but Banners.getBanner() or "
              + "Vungle.getNativeAd() returned null.",
          ERROR_DOMAIN);
      Log.d(TAG, error.getMessage());
      if (mediationAdapter != null && mediationListener != null) {
        mediationListener.onAdFailedToLoad(mediationAdapter, error);
        return;
      }
      if (mediationAdLoadCallback != null) {
        mediationAdLoadCallback.onFailure(error);
        return;
      }
    }
  }

  @NonNull
  @Override
  public String toString() {
    return " [placementId="
        + placementId
        + " # uniqueRequestId="
        + uniqueRequestId
        + " # adMarkup="
        + (TextUtils.isEmpty(mAdMarkup) ? "None" : "Yes")
        + " # hashcode="
        + hashCode()
        + "] ";
  }

  void attach() {
    if (vungleBannerAd != null) {
      vungleBannerAd.attach();
    }
  }

  void detach() {
    if (vungleBannerAd != null) {
      vungleBannerAd.detach();
    }
  }

  @Override
  public void creativeId(String creativeId) {
    // no-op
  }

  /**
   * Vungle SDK's {@link PlayAdCallback} implementation.
   */
  @Override
  public void onAdStart(String placementID) {
    // Let's load it again to mimic auto-cache, don't care about errors.
    preCache();
  }

  @Override
  @Deprecated
  public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
    // Deprecated. No-op.
  }

  @Override
  public void onAdEnd(String placementID) {
    // No-op for banner ads.
  }

  @Override
  public void onAdClick(String placementID) {
    if (mediationAdapter != null && mediationListener != null) {
      mediationListener.onAdClicked(mediationAdapter);
      mediationListener.onAdOpened(mediationAdapter);
      return;
    }
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdClicked();
      mediationBannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdRewarded(String placementID) {
    // No-op for banner ads.
  }

  @Override
  public void onAdLeftApplication(String placementID) {
    if (mediationAdapter != null && mediationListener != null) {
      mediationListener.onAdLeftApplication(mediationAdapter);
      return;
    }
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onError(String placementID, VungleException exception) {
    AdError error = VungleMediationAdapter.getAdError(exception);
    Log.w(TAG, error.getMessage());
    if (mediationAdapter != null && mediationListener != null) {
      mediationListener.onAdFailedToLoad(mediationAdapter, error);
      return;
    }
    if (mediationAdLoadCallback != null) {
      mediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdViewed(String placementID) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdImpression();
    }
  }
}
