package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

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

  /**
   * Vungle banner placement ID.
   */
  @NonNull
  private final String placementId;

  /**
   * Vungle ad configuration settings.
   */
  @NonNull
  private final AdConfig adConfig;

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
  private String adMarkup;

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
  private final VungleManager vungleManager;

  /**
   * Indicates whether a Vungle banner ad request is in progress.
   */
  private boolean pendingRequestBanner = false;

  /**
   * Indicates the Vungle banner ad's visibility.
   */
  private boolean visibility = true;

  VungleBannerAdapter(@NonNull String placementId, @NonNull String uniqueRequestId,
      @NonNull AdConfig adConfig, @NonNull MediationBannerAdapter mediationBannerAdapter) {
    vungleManager = VungleManager.getInstance();
    this.placementId = placementId;
    this.uniqueRequestId = uniqueRequestId;
    this.adConfig = adConfig;
    this.mediationAdapter = mediationBannerAdapter;
  }

  public VungleBannerAdapter(@NonNull String placementId, @NonNull String uniqueRequestId,
      @NonNull AdConfig adConfig, @NonNull MediationBannerAd mediationBannerAd) {
    vungleManager = VungleManager.getInstance();
    this.placementId = placementId;
    this.uniqueRequestId = uniqueRequestId;
    this.adConfig = adConfig;
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
    return pendingRequestBanner;
  }

  void requestBannerAd(@NonNull Context context, @NonNull String appId, @NonNull AdSize adSize,
      @NonNull MediationBannerListener mediationBannerListener) {
    mediationListener = mediationBannerListener;

    requestBannerAd(context, appId, adSize);
  }

  public void requestBannerAd(@NonNull Context context, @NonNull String appId,
      @NonNull AdSize adSize, @Nullable String adMarkup,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.adMarkup = adMarkup;

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
      adLayoutHeight = Math.round(adConfig.getAdSize().getHeight() * density);
    }
    RelativeLayout.LayoutParams adViewLayoutParams =
        new RelativeLayout.LayoutParams(adSize.getWidthInPixels(context), adLayoutHeight);
    adLayout.setLayoutParams(adViewLayoutParams);

    Log.d(TAG, "requestBannerAd: " + this);
    pendingRequestBanner = true;
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
                vungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
                if (!pendingRequestBanner) {
                  Log.w(TAG, "No banner request fired.");
                  return;
                }
                Log.w(TAG, error.toString());

                if (mediationAdapter != null && mediationListener != null) {
                  mediationListener.onAdFailedToLoad(mediationAdapter, error);
                } else if (mediationAdLoadCallback != null) {
                  mediationAdLoadCallback.onFailure(error);
                }
              }
            });
  }

  void destroy() {
    Log.d(TAG, "Vungle banner adapter destroy:" + this);
    visibility = false;
    vungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
    if (vungleBannerAd != null) {
      vungleBannerAd.detach();
      vungleBannerAd.destroyAd();
    }
    vungleBannerAd = null;
    pendingRequestBanner = false;
  }

  void preCache() {
    if (TextUtils.isEmpty(adMarkup)) {
      Banners.loadBanner(placementId, new BannerAdConfig(adConfig), null);
    }
  }

  void updateVisibility(boolean visible) {
    if (vungleBannerAd == null) {
      return;
    }

    this.visibility = visible;
    if (vungleBannerAd.getVungleBanner() != null) {
      vungleBannerAd.getVungleBanner().setAdVisibility(visible);
    }
  }

  private final LoadAdCallback adLoadCallback =
      new LoadAdCallback() {
        @Override
        public void onAdLoad(String id) {
          createBanner();
        }

        @Override
        public void onError(String id, VungleException exception) {
          vungleManager.removeActiveBannerAd(placementId, vungleBannerAd);

          if (!pendingRequestBanner) {
            Log.w(TAG, "No banner request fired.");
            return;
          }

          AdError error = VungleMediationAdapter.getAdError(exception);
          Log.w(TAG, error.toString());

          if (mediationAdapter != null && mediationListener != null) {
            mediationListener.onAdFailedToLoad(mediationAdapter, error);
          } else if (mediationAdLoadCallback != null) {
            mediationAdLoadCallback.onFailure(error);
          }
        }
      };

  private void loadBanner() {
    Log.d(TAG, "loadBanner: " + this);
    Banners.loadBanner(placementId, adMarkup, new BannerAdConfig(adConfig), adLoadCallback);
  }

  private void createBanner() {
    Log.d(TAG, "create banner: " + this);
    if (!pendingRequestBanner) {
      return;
    }

    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
    vungleBannerAd = vungleManager.getVungleBannerAd(placementId);
    VunglePlayAdCallback playAdCallback = new VunglePlayAdCallback(VungleBannerAdapter.this,
        VungleBannerAdapter.this, vungleBannerAd);

    if (AdConfig.AdSize.isBannerAdSize(adConfig.getAdSize())) {
      VungleBanner vungleBanner = Banners
          .getBanner(placementId, adMarkup, new BannerAdConfig(adConfig), playAdCallback);
      if (vungleBanner != null) {
        Log.d(TAG, "display banner:" + vungleBanner.hashCode() + this);
        if (vungleBannerAd != null) {
          vungleBannerAd.setVungleBanner(vungleBanner);
        }

        updateVisibility(visibility);
        vungleBanner.setLayoutParams(adParams);
        // Don't add to parent here.
        if (mediationAdapter != null && mediationListener != null) {
          mediationListener.onAdLoaded(mediationAdapter);
        } else if (mediationBannerAd != null && mediationAdLoadCallback != null) {
          mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(mediationBannerAd);
        }
      } else {
        AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
            "Vungle SDK returned a successful load callback, but Banners.getBanner() or "
                + "Vungle.getNativeAd() returned null.",
            ERROR_DOMAIN);
        Log.d(TAG, error.toString());
        if (mediationAdapter != null && mediationListener != null) {
          mediationListener.onAdFailedToLoad(mediationAdapter, error);
        } else if (mediationAdLoadCallback != null) {
          mediationAdLoadCallback.onFailure(error);
        }
      }
    } else {
      AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
          "Vungle SDK returned a successful load callback, but Banners.getBanner() or "
              + "Vungle.getNativeAd() returned null.",
          ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      if (mediationAdapter != null && mediationListener != null) {
        mediationListener.onAdFailedToLoad(mediationAdapter, error);
      } else if (mediationAdLoadCallback != null) {
        mediationAdLoadCallback.onFailure(error);
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
        + (TextUtils.isEmpty(adMarkup) ? "None" : "Yes")
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
    } else if (mediationBannerAdCallback != null) {
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
    } else if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onError(String placementID, VungleException exception) {
    AdError error = VungleMediationAdapter.getAdError(exception);
    Log.w(TAG, error.toString());
    if (mediationAdapter != null && mediationListener != null) {
      mediationListener.onAdFailedToLoad(mediationAdapter, error);
    } else if (mediationAdLoadCallback != null) {
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
