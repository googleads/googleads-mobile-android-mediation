package com.vungle.mediation;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VunglePlayAdCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.vungle.warren.AdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleNativeAd;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;

public class VungleBannerAdapter {

  private static final String TAG = VungleBannerAdapter.class.getSimpleName();

  /**
   * Vungle banner placement ID.
   */
  @NonNull
  private String placementId;

  /**
   * Vungle listener class to forward to the adapter.
   */
  @NonNull
  private WeakReference<VungleListener> mVungleListener = new WeakReference<>(null);

  /**
   * Vungle ad configuration settings.
   */
  @NonNull
  private AdConfig mAdConfig;

  /**
   * Unique Vungle banner request ID.
   */
  private String uniqueRequestId;

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
  private VungleManager mVungleManager;

  /**
   * Indicates whether a Vungle banner ad request is in progress.
   */
  private boolean mPendingRequestBanner = false;

  /**
   * Indicates the Vungle banner ad's visibility.
   */
  private boolean mVisibility = true;

  VungleBannerAdapter(@NonNull String placementId, @NonNull String uniqueRequestId,
      @NonNull AdConfig adConfig) {
    mVungleManager = VungleManager.getInstance();
    this.placementId = placementId;
    this.uniqueRequestId = uniqueRequestId;
    this.mAdConfig = adConfig;
  }

  void setVungleListener(@Nullable VungleListener vungleListener) {
    this.mVungleListener = new WeakReference<>(vungleListener);
  }

  @Nullable
  public String getUniqueRequestId() {
    return uniqueRequestId;
  }

  public RelativeLayout getAdLayout() {
    return adLayout;
  }

  @Nullable
  private VungleListener getVungleListener() {
    return mVungleListener.get();
  }

  public boolean isRequestPending() {
    return mPendingRequestBanner;
  }

  void requestBannerAd(@NonNull Context context, @NonNull String appId, @NonNull AdSize adSize) {
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
              public void onInitializeError(String errorMessage) {
                Log.d(TAG, "SDK init failed: " + VungleBannerAdapter.this);
                VungleListener listener = getVungleListener();
                mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
                if (mPendingRequestBanner && listener != null) {
                  listener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      Banners.loadBanner(placementId, mAdConfig.getAdSize(), null);
    } else {
      Vungle.loadAd(placementId, null);
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
    if (vungleBannerAd.getVungleMRECBanner() != null) {
      vungleBannerAd.getVungleMRECBanner().setAdVisibility(visible);
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
          Log.d(TAG, "Ad load failed:" + VungleBannerAdapter.this);
          VungleListener listener = getVungleListener();
          mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
          if (mPendingRequestBanner && listener != null) {
            listener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
          }
        }
      };

  private void loadBanner() {
    Log.d(TAG, "loadBanner: " + this);
    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      Banners.loadBanner(placementId, mAdConfig.getAdSize(), mAdLoadCallback);
    } else {
      Vungle.loadAd(placementId, mAdLoadCallback);
    }
  }

  private void createBanner() {
    Log.d(TAG, "create banner: " + this);
    if (!mPendingRequestBanner) {
      return;
    }

    VungleListener listener = getVungleListener();
    if (listener == null) {
      return;
    }

    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

    vungleBannerAd = mVungleManager.getVungleBannerAd(placementId);
    VunglePlayAdCallback playAdCallback = new VunglePlayAdCallback(listener,
        VungleBannerAdapter.this, vungleBannerAd);

    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      VungleBanner vungleBanner = Banners
          .getBanner(placementId, mAdConfig.getAdSize(), playAdCallback);
      if (vungleBanner != null) {
        Log.d(TAG, "display banner:" + vungleBanner.hashCode() + this);
        if (vungleBannerAd != null) {
          vungleBannerAd.setVungleBanner(vungleBanner);
        }

        updateVisibility(mVisibility);
        vungleBanner.setLayoutParams(adParams);
        // don't add to parent here
        listener.onAdAvailable();
      } else {
        // missing resources
        listener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
      }
    } else {
      View adView = null;
      VungleNativeAd vungleMRECBanner = Vungle.getNativeAd(placementId, mAdConfig, playAdCallback);
      if (vungleMRECBanner != null) {
        adView = vungleMRECBanner.renderNativeView();
      }

      if (adView != null) {
        Log.d(TAG, "display MREC:" + vungleMRECBanner.hashCode() + this);
        if (vungleBannerAd != null) {
          vungleBannerAd.setVungleMRECBanner(vungleMRECBanner);
        }

        updateVisibility(mVisibility);
        adView.setLayoutParams(adParams);
        // don't add to parent here
        listener.onAdAvailable();
      } else {
        // missing resources
        listener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
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

}
