package com.vungle.mediation;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.vungle.warren.AdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleNativeAd;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;

class VungleBannerAdapter implements PlayAdCallback {

  private static final String TAG = VungleBannerAdapter.class.getSimpleName();

  /**
   * Vungle banner placement ID.
   */
  @NonNull
  private String mPlacementId;

  /**
   * Unique Vungle banner request ID.
   */
  @Nullable
  private String mUniquePubRequestId;

  /**
   * Mediation Banner Adapter instance to receive callbacks.
   */
  private MediationBannerAdapter bannerAdapter;

  /**
   * Vungle listener class to forward to the adapter.
   */
  private MediationBannerListener bannerListener;

  /**
   * Container for Vungle's banner ad view.
   */
  @NonNull
  private WeakReference<RelativeLayout> mAdLayout = new WeakReference<>(null);

  /**
   * Vungle ad configuration settings.
   */
  @NonNull
  private AdConfig mAdConfig;

  /**
   * Vungle ad object for non-MREC banner ads.
   */
  @Nullable
  private VungleBanner mVungleBannerAd;

  /**
   * Vungle ad object for MREC banner ads.
   */
  @Nullable
  private VungleNativeAd mVungleNativeAd;

  /**
   * Manager to handle Vungle banner ad requests.
   */
  @NonNull
  private VungleManager mVungleManager;

  /**
   * Indicates whether a Vungle banner ad request is in progress.
   */
  private boolean mPendingRequestBanner = false;

  /**
   * Indicates the Vungle banner ad's visibility.
   */
  private boolean mVisibility = true;

  VungleBannerAdapter(
      @NonNull String placementId,
      @Nullable String uniquePubRequestId,
      @NonNull AdConfig adConfig) {
    mVungleManager = VungleManager.getInstance();
    this.mPlacementId = placementId;
    this.mUniquePubRequestId = uniquePubRequestId;
    this.mAdConfig = adConfig;
  }

  @Nullable
  String getUniquePubRequestId() {
    return mUniquePubRequestId;
  }

  // Use weak references to allow VungleInterstitialAdapter to be garbage collected.
  // Banner view need to be added/removed on adLayout's onAttachedToWindow/onDetachedFromWindow
  // to break view's parent-child references chain to the leaked VungleBannerAdapter in
  // VungleManager.
  void setAdLayout(@NonNull RelativeLayout adLayout) {
    this.mAdLayout = new WeakReference<>(adLayout);
  }

  boolean isActive() {
    return mAdLayout.get() != null;
  }

  void requestBannerAd(@NonNull Context context, @NonNull String appId,
      @NonNull MediationBannerListener mediationBannerListener,
      @NonNull MediationBannerAdapter mediationBannerAdapter) {
    Log.d(TAG, "requestBannerAd: " + this);
    mPendingRequestBanner = true;

    bannerAdapter = mediationBannerAdapter;
    bannerListener = mediationBannerListener;

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
              public void onInitializeError(String errorMessage) {
                Log.d(TAG, "SDK init failed: " + VungleBannerAdapter.this);
                mVungleManager.removeActiveBannerAd(mPlacementId);
                if (mPendingRequestBanner && bannerListener != null && bannerAdapter != null) {
                  bannerListener
                      .onAdFailedToLoad(bannerAdapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
              }
            });
  }

  void destroy(@Nullable View adView) {
    Log.d(TAG, "Vungle banner adapter try to destroy: " + this);
    if (adView == mAdLayout.get()) {
      Log.d(TAG, "Vungle banner adapter destroy: " + this);
      mVisibility = false;
      mVungleManager.removeActiveBannerAd(mPlacementId);
      cleanUp();
      mPendingRequestBanner = false;
    }
  }

  /**
   * This method is a workaround for banner leak issue, and most callers should use {@link
   * VungleBannerAdapter#destroy(View)}.
   */
  void destroy() {
    destroy(mAdLayout.get());
  }

  void cleanUp() {
    Log.d(TAG, "Vungle banner adapter try to cleanUp: " + this);

    if (mVungleBannerAd != null) {
      Log.d(TAG, "Vungle banner adapter cleanUp: destroyAd # " + mVungleBannerAd.hashCode());
      mVungleBannerAd.destroyAd();
      detach();
      mVungleBannerAd = null;
    }

    if (mVungleNativeAd != null) {
      Log.d(
          TAG, "Vungle banner adapter cleanUp: finishDisplayingAd # " + mVungleNativeAd.hashCode());
      mVungleNativeAd.finishDisplayingAd();
      detach();
      mVungleNativeAd = null;
    }
  }

  void preCache() {
    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      Banners.loadBanner(mPlacementId, mAdConfig.getAdSize(), null);
    } else {
      Vungle.loadAd(mPlacementId, null);
    }
  }

  void updateVisibility(boolean visible) {
    this.mVisibility = visible;
    if (mVungleBannerAd != null) {
      mVungleBannerAd.setAdVisibility(visible);
    }
    if (mVungleNativeAd != null) {
      mVungleNativeAd.setAdVisibility(visible);
    }
  }

  private void loadBanner() {
    LoadAdCallback loadAdCallback = new LoadAdCallback() {
      @Override
      public void onAdLoad(String placementId) {
        createBanner();
      }

      @Override
      public void onError(String placementId, VungleException exception) {
        Log.d(TAG, "Ad load failed: " + VungleBannerAdapter.this);
        mVungleManager.removeActiveBannerAd(mPlacementId);
        if (mPendingRequestBanner && bannerListener != null && bannerAdapter != null) {
          bannerListener.onAdFailedToLoad(bannerAdapter, AdRequest.ERROR_CODE_NO_FILL);
        }
      }
    };

    Log.d(TAG, "loadBanner: " + this);
    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      Banners.loadBanner(mPlacementId, mAdConfig.getAdSize(), loadAdCallback);
    } else {
      Vungle.loadAd(mPlacementId, loadAdCallback);
    }
  }

  /**
   * {@link PlayAdCallback} implementation from Vungle.
   */
  @Override
  public void onAdStart(String placementId) {
    // Let's load it again to mimic auto-cache, don't care about errors.
    preCache();
  }

  @Override
  @Deprecated
  public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) {
    // No-op. Deprecated method from Vungle.
  }

  @Override
  public void onAdEnd(String placementId) {
    // No-op.
  }

  @Override
  public void onAdClick(String placementId) {
    if (mPendingRequestBanner && bannerListener != null && bannerAdapter != null) {
      bannerListener.onAdClicked(bannerAdapter);
      bannerListener.onAdOpened(bannerAdapter);
    }
  }

  @Override
  public void onAdRewarded(String placementId) {
    // No-op for banner ads.
  }

  @Override
  public void onAdLeftApplication(String placementId) {
    if (mPendingRequestBanner && bannerListener != null && bannerAdapter != null) {
      bannerListener.onAdLeftApplication(bannerAdapter);
    }
  }

  @Override
  public void onError(String placementId, VungleException exception) {
    Log.w(TAG, "Failed to play banner ad from Vungle: " + exception.getLocalizedMessage());
  }

  private void createBanner() {
    Log.d(TAG, "create banner:" + this);
    if (!mPendingRequestBanner) {
      return;
    }

    cleanUp();
    RelativeLayout.LayoutParams adParams =
        new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      mVungleBannerAd = Banners
          .getBanner(mPlacementId, mAdConfig.getAdSize(), VungleBannerAdapter.this);
      if (mVungleBannerAd != null) {
        Log.d(TAG, "display banner:" + mVungleBannerAd.hashCode() + this);
        mVungleManager.storeActiveBannerAd(mPlacementId, this);
        updateVisibility(mVisibility);
        mVungleBannerAd.setLayoutParams(adParams);
        // Don't add to parent here.
        if (bannerListener != null && bannerAdapter != null) {
          bannerListener.onAdLoaded(bannerAdapter);
        }
      } else {
        // Missing resources.
        if (bannerListener != null && bannerAdapter != null) {
          bannerListener
              .onAdFailedToLoad(bannerAdapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
      }
    } else {
      View adView = null;
      mVungleNativeAd = Vungle.getNativeAd(mPlacementId, mAdConfig, VungleBannerAdapter.this);
      if (mVungleNativeAd != null) {
        adView = mVungleNativeAd.renderNativeView();
        mVungleManager.storeActiveBannerAd(mPlacementId, this);
      }
      if (adView != null) {
        Log.d(TAG, "display MREC:" + mVungleNativeAd.hashCode() + this);
        updateVisibility(mVisibility);
        adView.setLayoutParams(adParams);
        // Don't add to parent here.
        if (bannerListener != null && bannerAdapter != null) {
          bannerListener.onAdLoaded(bannerAdapter);
        }
      } else {
        // Missing resources.
        if (bannerListener != null && bannerAdapter != null) {
          bannerListener
              .onAdFailedToLoad(bannerAdapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
      }
    }
  }

  @NonNull
  @Override
  public String toString() {
    return " [placementId="
        + mPlacementId
        + " # uniqueRequestId="
        + mUniquePubRequestId
        + " # hashcode="
        + hashCode()
        + "] ";
  }

  void attach() {
    RelativeLayout layout = mAdLayout.get();
    if (layout != null) {
      if (mVungleBannerAd != null && mVungleBannerAd.getParent() == null) {
        layout.addView(mVungleBannerAd);
      }
      if (mVungleNativeAd != null) {
        View adView = mVungleNativeAd.renderNativeView();
        if (adView != null && adView.getParent() == null) {
          layout.addView(adView);
        }
      }
    }
  }

  void detach() {
    if (mVungleBannerAd != null && mVungleBannerAd.getParent() != null) {
      ((ViewGroup) mVungleBannerAd.getParent()).removeView(mVungleBannerAd);
    }
    if (mVungleNativeAd != null) {
      View adView = mVungleNativeAd.renderNativeView();
      if (adView != null && adView.getParent() != null) {
        ((ViewGroup) adView.getParent()).removeView(adView);
      }
    }
  }
}
