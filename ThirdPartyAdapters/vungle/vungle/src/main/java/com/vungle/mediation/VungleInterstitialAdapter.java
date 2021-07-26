package com.vungle.mediation;

import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.util.ArrayList;

/**
 * A {@link RtbAdapter} used to load and show Vungle interstitial/banner ads using Google Mobile Ads
 * SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter extends VungleMediationAdapter
    implements MediationInterstitialAd, MediationBannerAd {

  private static final String TAG = VungleInterstitialAdapter.class.getSimpleName();

  private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationAdLoadCallback;
  private MediationInterstitialAdCallback mediationInterstitialAdCallback;
  private VungleManager mVungleManager;
  private AdConfig mAdConfig;
  private String mPlacementForPlay;
  private String mAdMarkup;

  // banner/MREC
  private VungleBannerAdapter vungleBannerAdapter;

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
    mMediationAdLoadCallback = mediationAdLoadCallback;

    Bundle mediationExtras = mediationInterstitialAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException e) {
      String message = "Failed to load ad from Vungle: " + e.getLocalizedMessage();
      Log.w(TAG, message, e);
      if (mMediationAdLoadCallback != null) {
        AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message, TAG);
        mMediationAdLoadCallback.onFailure(error);
      }
      return;
    }

    mVungleManager = VungleManager.getInstance();

    mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacementForPlay)) {
      String logMessage = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
      Log.w(TAG, logMessage);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, logMessage, TAG);
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    mAdMarkup = mediationInterstitialAdConfiguration.getBidResponse();
    Log.d(TAG, "Render interstitial mAdMarkup=" + mAdMarkup);
    if (TextUtils.isEmpty(mAdMarkup)) {
      mAdMarkup = null;
    }

    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
    VungleInitializer.getInstance()
        .initialize(
            config.getAppId(),
            mediationInterstitialAdConfiguration.getContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                loadAd();
              }

              @Override
              public void onInitializeError(String errorMessage) {
                String logMessage = "SDK init failed: : " + errorMessage;
                Log.w(TAG, logMessage);
                if (mMediationAdLoadCallback != null) {
                  AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR, logMessage, TAG);
                  mMediationAdLoadCallback.onFailure(error);
                }
              }
            });
  }

  private void loadAd() {
    if (Vungle.canPlayAd(mPlacementForPlay, mAdMarkup)) {
      if (mMediationAdLoadCallback != null) {
        mediationInterstitialAdCallback = mMediationAdLoadCallback
            .onSuccess(VungleInterstitialAdapter.this);
      }
      return;
    }

    // Placement ID is not what Vungle's SDK gets back after init/config.
    if (!mVungleManager.isValidPlacement(mPlacementForPlay)) {
      if (mMediationAdLoadCallback != null) {
        AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST,
            "Invalid placement to play", TAG);
        mMediationAdLoadCallback.onFailure(error);
      }
      return;
    }

    Vungle.loadAd(mPlacementForPlay, mAdMarkup, mAdConfig, new LoadAdCallback() {
      @Override
      public void onAdLoad(String placementID) {
        if (mMediationAdLoadCallback != null) {
          mediationInterstitialAdCallback = mMediationAdLoadCallback
              .onSuccess(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        Log.w(TAG, "Failed to load ad from Vungle: " + exception.getLocalizedMessage());
        if (mMediationAdLoadCallback != null) {
          AdError error = VungleManager.mapErrorCode(exception, TAG);
          mMediationAdLoadCallback.onFailure(error);
        }
      }
    });
  }

  @Override
  public void showAd(@NonNull Context context) {
    Vungle.playAd(mPlacementForPlay, mAdMarkup, mAdConfig, new PlayAdCallback() {

      @Override
      public void creativeId(String creativeId) {
        // no-op
      }

      @Override
      public void onAdStart(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdOpened();
        }
      }

      @Override
      public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
        // Deprecated, no-op.
      }

      @Override
      public void onAdEnd(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdClosed();
        }
      }

      @Override
      public void onAdClick(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.reportAdClicked();
        }
      }

      @Override
      public void onAdRewarded(String placementID) {
        // No-op for interstitial ads.
      }

      @Override
      public void onAdLeftApplication(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdLeftApplication();
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        Log.w(TAG, "Failed to play ad from Vungle: " + exception.getLocalizedMessage());
        if (mediationInterstitialAdCallback != null) {
          // AdError error = VungleManager.mapErrorCode(exception, TAG);
          mediationInterstitialAdCallback.onAdClosed();
        }
      }

      @Override
      public void onAdViewed(String id) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.reportAdImpression();
        }
      }
    });
  }

//  @Override
//  public void onDestroy() {
//    Log.d(TAG, "onDestroy: " + hashCode());
//    if (vungleBannerAdapter != null) {
//      vungleBannerAdapter.destroy();
//      vungleBannerAdapter = null;
//    }
//  }
//
//  // banner
//  @Override
//  public void onPause() {
//    Log.d(TAG, "onPause");
//    if (vungleBannerAdapter != null) {
//      vungleBannerAdapter.updateVisibility(false);
//    }
//  }
//
//  @Override
//  public void onResume() {
//    Log.d(TAG, "onResume");
//    if (vungleBannerAdapter != null) {
//      vungleBannerAdapter.updateVisibility(true);
//    }
//  }

  @Override
  public void loadBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {

    Bundle mediationExtras = mediationBannerAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException e) {
      String message = "Failed to load ad from Vungle: " + e.getLocalizedMessage();
      Log.w(TAG, message, e);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message, TAG);
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    mVungleManager = VungleManager.getInstance();

    String placementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    Log.d(TAG, "requestBannerAd for Placement: " + placementForPlay
        + " ### Adapter instance: " + this.hashCode());

    if (TextUtils.isEmpty(placementForPlay)) {
      String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
      Log.w(TAG, message);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message, TAG);
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Context context = mediationBannerAdConfiguration.getContext();
    AdSize adSize = mediationBannerAdConfiguration.getAdSize();

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!hasBannerSizeAd(context, adSize, adConfig)) {
      String message = "Failed to load ad from Vungle: Invalid banner size.";
      Log.w(TAG, message);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message, TAG);
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    // Adapter does not support multiple Banner instances playing for same placement except for
    // refresh.
    String uniqueRequestId = config.getRequestUniqueId();
    AdError adError = VungleManager.getInstance()
        .canRequestBannerAd(placementForPlay, uniqueRequestId);
    if (adError != null) {
      mediationAdLoadCallback.onFailure(adError);
      return;
    }

    vungleBannerAdapter = new VungleBannerAdapter(placementForPlay, uniqueRequestId, adConfig,
        VungleInterstitialAdapter.this);
    Log.d(TAG, "New banner adapter: " + vungleBannerAdapter + "; size: " + adConfig.getAdSize());

    VungleBannerAd vungleBanner = new VungleBannerAd(placementForPlay, vungleBannerAdapter);
    mVungleManager.registerBannerAd(placementForPlay, vungleBanner);

    Log.d(TAG, "Requesting banner with ad size: " + adConfig.getAdSize());
    vungleBannerAdapter
        .requestBannerAd(config.getAppId(), mediationBannerAdConfiguration,
            mediationAdLoadCallback);
  }

  @NonNull
  @Override
  public View getView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return vungleBannerAdapter.getAdLayout();
  }

  private boolean hasBannerSizeAd(Context context, AdSize adSize, AdConfig adConfig) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(BANNER_SHORT.getWidth(), BANNER_SHORT.getHeight()));
    potentials.add(new AdSize(BANNER.getWidth(), BANNER.getHeight()));
    potentials.add(new AdSize(BANNER_LEADERBOARD.getWidth(), BANNER_LEADERBOARD.getHeight()));
    potentials.add(new AdSize(VUNGLE_MREC.getWidth(), VUNGLE_MREC.getHeight()));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      Log.i(TAG, "Not found closest ad size: " + adSize);
      return false;
    }
    Log.i(
        TAG,
        "Found closest ad size: " + closestSize.toString() + " for requested ad size: " + adSize);

    if (closestSize.getWidth() == BANNER_SHORT.getWidth()
        && closestSize.getHeight() == BANNER_SHORT.getHeight()) {
      adConfig.setAdSize(BANNER_SHORT);
    } else if (closestSize.getWidth() == BANNER.getWidth()
        && closestSize.getHeight() == BANNER.getHeight()) {
      adConfig.setAdSize(BANNER);
    } else if (closestSize.getWidth() == BANNER_LEADERBOARD.getWidth()
        && closestSize.getHeight() == BANNER_LEADERBOARD.getHeight()) {
      adConfig.setAdSize(BANNER_LEADERBOARD);
    } else if (closestSize.getWidth() == VUNGLE_MREC.getWidth()
        && closestSize.getHeight() == VUNGLE_MREC.getHeight()) {
      adConfig.setAdSize(VUNGLE_MREC);
    }

    return true;
  }
}
