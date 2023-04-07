// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.fyber;

import static com.google.ads.mediation.fyber.FyberAdapterUtils.getAdError;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DT Exchange's official AdMob 3rd party adapter class. Implements Banners and Interstitials by
 * implementing the {@link MediationBannerAdapter} and {@link MediationInterstitialAdapter}
 * interfaces. Implements initialization and Rewarded video ads, by extending the {@link Adapter}
 * class.
 */
public class FyberMediationAdapter extends Adapter
    implements MediationBannerAdapter, MediationInterstitialAdapter {

  /**
   * Adapter class name for logging.
   */
  static final String TAG = FyberMediationAdapter.class.getSimpleName();

  /**
   * DT Exchange requires to know the host mediation platform.
   */
  private final static InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

  /**
   * Key to obtain App id, required for initializing DT Exchange's SDK.
   */
  private static final String KEY_APP_ID = "applicationId";

  /**
   * Key to obtain a placement name or spot id. Required for creating a DT Exchange ad request.
   */
  static final String KEY_SPOT_ID = "spotId";

  /**
   * Key to obtain the mute video state, which enables the publisher to mute interstitial ads
   */
  static final String KEY_MUTE_VIDEO = "muteVideo";

  /**
   * Requested banner ad size.
   */
  private AdSize requestedAdSize;

  /**
   * DT Exchange's Spot object for the banner.
   */
  private InneractiveAdSpot bannerSpot;

  /**
   * A wrapper view for the DT Exchange banner view.
   */
  private ViewGroup bannerWrapperView;

  /**
   * AdMob's external Banner listener.
   */
  private MediationBannerListener mediationBannerListener;

  /**
   * AdMob's external Interstitial listener.
   */
  private MediationInterstitialListener mediationInterstitialListener;

  /**
   * The Activity which was passed by AdMob to {@link #requestInterstitialAd}.
   */
  private WeakReference<Activity> interstitialActivityRef;

  /**
   * DT Exchange's spot object for interstitial.
   */
  private InneractiveAdSpot interstitialSpot;

  /**
   * DT Exchange rewarded ad video renderer.
   */
  private FyberRewardedVideoRenderer rewardedRenderer;

  /**
   * Default Constructor.
   */
  public FyberMediationAdapter() {
  }

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_WRONG_CONTROLLER_TYPE,
          ERROR_AD_NOT_READY
      })

  public @interface AdapterError {

  }

  /**
   * DT Exchange adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.dtexchange";

  /**
   * Server parameters, such as app ID or spot ID, are invalid.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a DT Exchange supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 103;

  /**
   * DT Exchange SDK loaded an ad but returned an unexpected controller.
   */
  public static final int ERROR_WRONG_CONTROLLER_TYPE = 105;

  /**
   * Ad not ready.
   */
  public static final int ERROR_AD_NOT_READY = 106;

  /**
   * Context is not an activity instance.
   */
  public static final int ERROR_CONTEXT_NOT_ACTIVITY_INSTANCE = 107;

  /**
   * Only rewarded ads are implemented using the new Adapter interface.
   */
  public void loadRewardedAd(@NonNull final MediationRewardedAdConfiguration configuration,
      @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          callback) {
    // Sometimes loadRewardedAd is called before initialize.
    String keyAppID = configuration.getServerParameters().getString(KEY_APP_ID);
    if (TextUtils.isEmpty(keyAppID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "App ID is null or empty.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    InneractiveAdManager.initialize(configuration.getContext(), keyAppID,
        new OnFyberMarketplaceInitializedListener() {
          @Override
          public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
            if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
              AdError error = getAdError(fyberInitStatus);
              Log.w(TAG, error.getMessage());
              callback.onFailure(error);
              return;
            }
            rewardedRenderer = new FyberRewardedVideoRenderer(configuration, callback);
            rewardedRenderer.render();
          }
        });
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback completionCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {
    // Initialize only once.
    if (InneractiveAdManager.wasInitialized()) {
      completionCallback.onInitializationSucceeded();
      return;
    }

    Set<String> configuredAppIds = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String appId = serverParameters.getString(KEY_APP_ID);
      if (!TextUtils.isEmpty(appId)) {
        configuredAppIds.add(appId);
      }
    }

    if (configuredAppIds.isEmpty()) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "DT Exchange SDK requires an appId to be configured on the AdMob UI.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      completionCallback.onInitializationFailed(error.getMessage());
      return;
    }

    // We can only use one app id to initialize the DT Exchange SDK.
    String appIdForInitialization = configuredAppIds.iterator().next();
    if (configuredAppIds.size() > 1) {
      String logMessage = String.format("Multiple '%s' entries found: %s. "
              + "Using '%s' to initialize the DT Exchange SDK.", KEY_APP_ID, configuredAppIds,
          appIdForInitialization);
      Log.w(TAG, logMessage);
    }

    InneractiveAdManager.initialize(context, appIdForInitialization,
        new OnFyberMarketplaceInitializedListener() {
          @Override
          public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
            if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
              AdError error = getAdError(fyberInitStatus);
              Log.w(TAG, error.getMessage());
              completionCallback.onInitializationFailed(error.getMessage());
              return;
            }
            completionCallback.onInitializationSucceeded();
          }
        });
  }

  @NonNull
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.ADAPTER_VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format("Unexpected adapter version format: %s. " +
        "Returning 0.0.0 for adapter version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  public VersionInfo getSDKVersionInfo() {
    String sdkVersion = InneractiveAdManager.getVersion();
    String[] splits = sdkVersion.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format("Unexpected SDK version format: %s. " +
        "Returning 0.0.0 for SDK version.", sdkVersion);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  /**
   * {@link MediationBannerAdapter} implementation.
   */
  @Override
  public void requestBannerAd(@NonNull final Context context,
      @NonNull final MediationBannerListener mediationBannerListener,
      @NonNull final Bundle serverParameters, @NonNull final AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable final Bundle mediationExtras) {
    this.mediationBannerListener = mediationBannerListener;
    String keyAppId = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(keyAppId)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "App ID is null or empty.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      this.mediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
      return;
    }

    InneractiveAdManager.initialize(context, keyAppId, new OnFyberMarketplaceInitializedListener() {
      @Override
      public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
        if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
          AdError error = getAdError(fyberInitStatus);
          Log.w(TAG, error.getMessage());
          FyberMediationAdapter.this.mediationBannerListener.onAdFailedToLoad(
              FyberMediationAdapter.this, error);
          return;
        }

        // Check that we got a valid Spot ID from the server.
        String spotId = serverParameters.getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
          AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
              "Cannot render banner ad. Please define a valid spot id on the AdMob UI.",
              ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          FyberMediationAdapter.this.mediationBannerListener.onAdFailedToLoad(
              FyberMediationAdapter.this, error);
          return;
        }

        bannerSpot = InneractiveAdSpotManager.get().createSpot();
        bannerSpot.setMediationName(MEDIATOR_NAME);

        InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
        bannerSpot.addUnitController(controller);

        // Prepare wrapper view before making request.
        bannerWrapperView = new RelativeLayout(context);

        InneractiveAdSpot.RequestListener requestListener = createFyberBannerAdListener();
        bannerSpot.setRequestListener(requestListener);

        requestedAdSize = adSize;

        FyberAdapterUtils.updateFyberExtraParams(mediationExtras);
        InneractiveAdRequest request = new InneractiveAdRequest(spotId);
        bannerSpot.requestAd(request);
      }
    });
  }

  @NonNull
  @Override
  public View getBannerView() {
    return bannerWrapperView;
  }

  @Override
  public void onDestroy() {
    if (bannerSpot != null) {
      bannerSpot.destroy();
      bannerSpot = null;
    }

    if (interstitialSpot != null) {
      interstitialSpot.destroy();
      interstitialSpot = null;
    }

    if (interstitialActivityRef != null) {
      interstitialActivityRef.clear();
      interstitialActivityRef = null;
    }
  }

  @Override
  public void onPause() {
    // No relevant action. Refresh is disabled for banners.
  }

  @Override
  public void onResume() {
    // No relevant action. Refresh is disabled for banners.
  }

  /**
   * Creates DT Exchange's banner ad request listener.
   *
   * @return the created request listener.
   */
  @NonNull
  private InneractiveAdSpot.RequestListener createFyberBannerAdListener() {
    return new InneractiveAdSpot.RequestListener() {
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        // Just a double check that we have the right type of selected controller.
        if (!(bannerSpot.getSelectedUnitController() instanceof
            InneractiveAdViewUnitController)) {
          String message = String.format("Unexpected controller type. Expected: %s. Actual: %s",
              InneractiveUnitController.class.getName(),
              bannerSpot.getSelectedUnitController().getClass().getName());
          AdError error = new AdError(ERROR_WRONG_CONTROLLER_TYPE, message, ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mediationBannerListener
              .onAdFailedToLoad(FyberMediationAdapter.this, error);
          bannerSpot.destroy();
        }

        InneractiveAdViewUnitController controller =
            (InneractiveAdViewUnitController) bannerSpot.getSelectedUnitController();
        InneractiveAdViewEventsListener listener = createFyberAdViewListener();
        controller.setEventsListener(listener);
        controller.bindView(bannerWrapperView);

        // Validate the ad size returned by Fyber Marketplace with the requested ad size.
        Context context = bannerWrapperView.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        int fyberAdWidth = Math.round(controller.getAdContentWidth() / density);
        int fyberAdHeight = Math.round(controller.getAdContentHeight() / density);

        ArrayList<AdSize> potentials = new ArrayList<>();
        potentials.add(new AdSize(fyberAdWidth, fyberAdHeight));
        AdSize supportedAdSize = MediationUtils
            .findClosestSize(context, requestedAdSize, potentials);
        if (supportedAdSize == null) {
          int requestedAdWidth = Math.round(requestedAdSize.getWidthInPixels(context) / density);
          int requestedAdHeight = Math.round(requestedAdSize.getHeightInPixels(context) / density);
          String message = String.format("The loaded ad size did not match the requested "
                  + "ad size. Requested ad size: %dx%d. Loaded ad size: %dx%d.", requestedAdWidth,
              requestedAdHeight, fyberAdWidth, fyberAdHeight);
          AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH, message, ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
          return;
        }
        mediationBannerListener.onAdLoaded(FyberMediationAdapter.this);
      }

      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
          InneractiveErrorCode inneractiveErrorCode) {
        AdError error = getAdError(inneractiveErrorCode);
        Log.w(TAG, error.getMessage());
        mediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
        if (adSpot != null) {
          adSpot.destroy();
        }
      }
    };
  }

  /**
   * When an ad is fetched successfully, creates a listener for DT Exchange's AdView events.
   *
   * @return the create events listener.
   */
  @NonNull
  private InneractiveAdViewEventsListener createFyberAdViewListener() {
    return new InneractiveAdViewEventsListenerAdapter() {
      @Override
      public void onAdImpression(InneractiveAdSpot adSpot) {
        // Nothing to report back here.
      }

      @Override
      public void onAdClicked(InneractiveAdSpot adSpot) {
        mediationBannerListener.onAdClicked(FyberMediationAdapter.this);
        mediationBannerListener.onAdOpened(FyberMediationAdapter.this);
      }

      @Override
      public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
        mediationBannerListener.onAdClosed(FyberMediationAdapter.this);
      }

      @Override
      public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
        mediationBannerListener.onAdLeftApplication(FyberMediationAdapter.this);
      }
    };
  }

  /**
   * {@link MediationInterstitialAdapter} implementation.
   */
  @Override
  public void requestInterstitialAd(@NonNull final Context context,
      @NonNull final MediationInterstitialListener mediationInterstitialListener,
      @NonNull final Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {

    this.mediationInterstitialListener = mediationInterstitialListener;

    String keyAppId = serverParameters.getString(KEY_APP_ID);
    AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "App ID is null or empty.",
        ERROR_DOMAIN);
    if (TextUtils.isEmpty(keyAppId)) {
      Log.w(TAG, error.getMessage());
      this.mediationInterstitialListener
          .onAdFailedToLoad(FyberMediationAdapter.this, error);
      return;
    }

    InneractiveAdManager.initialize(context, keyAppId, new OnFyberMarketplaceInitializedListener() {
      @Override
      public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
        if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
          AdError error = getAdError(fyberInitStatus);
          Log.w(TAG, error.getMessage());
          FyberMediationAdapter.this.mediationInterstitialListener.onAdFailedToLoad(
              FyberMediationAdapter.this, error);
          return;
        }

        // Check that we got a valid spot id from the server.
        String spotId = serverParameters.getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
          AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
              "Cannot render interstitial ad. Please define a valid spot id on the AdMob UI.",
              ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          FyberMediationAdapter.this.mediationInterstitialListener
              .onAdFailedToLoad(FyberMediationAdapter.this, error);
          return;
        }

        // We need an activity context to show interstitial ads.
        if (!(context instanceof Activity)) {
          AdError error = new AdError(ERROR_CONTEXT_NOT_ACTIVITY_INSTANCE,
              "Cannot request an interstitial ad without an activity context.",
              ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          if (FyberMediationAdapter.this.mediationInterstitialListener != null) {
            FyberMediationAdapter.this.mediationInterstitialListener
                .onAdFailedToLoad(FyberMediationAdapter.this, error);
          }
          return;
        }

        // Cache the context for showInterstitial.
        interstitialActivityRef = new WeakReference<>((Activity) context);

        interstitialSpot = InneractiveAdSpotManager.get().createSpot();
        interstitialSpot.setMediationName(MEDIATOR_NAME);

        InneractiveFullscreenUnitController controller = new InneractiveFullscreenUnitController();
        interstitialSpot.addUnitController(controller);

        InneractiveAdSpot.RequestListener requestListener = createFyberInterstitialAdListener();
        interstitialSpot.setRequestListener(requestListener);

        FyberAdapterUtils.updateFyberExtraParams(mediationExtras);
        InneractiveAdRequest request = new InneractiveAdRequest(spotId);
        interstitialSpot.requestAd(request);
      }
    });
  }

  @Override
  public void showInterstitial() {
    Activity activity = interstitialActivityRef == null ? null : interstitialActivityRef.get();
    if (activity == null) {
      Log.w(TAG, "showInterstitial called, but activity reference was lost.");
      mediationInterstitialListener.onAdOpened(this);
      mediationInterstitialListener.onAdClosed(this);
      return;
    }

    if (!(interstitialSpot
        .getSelectedUnitController() instanceof InneractiveFullscreenUnitController)) {
      Log.w(TAG, "showInterstitial called, but wrong spot has been used (should not happen).");
      mediationInterstitialListener.onAdOpened(this);
      mediationInterstitialListener.onAdClosed(this);
      return;
    }
    InneractiveFullscreenUnitController controller =
        (InneractiveFullscreenUnitController) interstitialSpot.getSelectedUnitController();

    if (!interstitialSpot.isReady()) {
      Log.w(TAG, "showInterstitial called, but Ad has expired.");
      mediationInterstitialListener.onAdOpened(this);
      mediationInterstitialListener.onAdClosed(this);
      return;
    }
    controller.show(activity);
  }

  @NonNull
  private InneractiveAdSpot.RequestListener createFyberInterstitialAdListener() {
    return new InneractiveAdSpot.RequestListener() {
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        if (!(interstitialSpot.getSelectedUnitController() instanceof
            InneractiveFullscreenUnitController)) {
          String message = String.format("Unexpected controller type. Expected: %s. Actual: %s",
              InneractiveUnitController.class.getName(),
              bannerSpot.getSelectedUnitController().getClass().getName());
          AdError error = new AdError(ERROR_WRONG_CONTROLLER_TYPE, message, ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
          interstitialSpot.destroy();
        }

        InneractiveFullscreenUnitController controller =
            (InneractiveFullscreenUnitController) interstitialSpot.getSelectedUnitController();
        InneractiveFullscreenAdEventsListener listener = createFyberInterstitialListener();
        controller.setEventsListener(listener);

        mediationInterstitialListener.onAdLoaded(FyberMediationAdapter.this);
      }

      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
          InneractiveErrorCode inneractiveErrorCode) {
        // Convert DT Exchange error code into custom error code
        AdError error = getAdError(inneractiveErrorCode);
        Log.w(TAG, error.getMessage());
        mediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
      }
    };
  }

  /**
   * Creates a listener for DT Exchange's Interstitial events
   *
   * @return the created event listener.
   */
  @NonNull
  private InneractiveFullscreenAdEventsListener createFyberInterstitialListener() {
    return new InneractiveFullscreenAdEventsListenerAdapter() {
      @Override
      public void onAdImpression(InneractiveAdSpot adSpot) {
        mediationInterstitialListener.onAdOpened(FyberMediationAdapter.this);
      }

      @Override
      public void onAdClicked(InneractiveAdSpot adSpot) {
        mediationInterstitialListener.onAdClicked(FyberMediationAdapter.this);
      }

      @Override
      public void onAdDismissed(InneractiveAdSpot adSpot) {
        mediationInterstitialListener.onAdClosed(FyberMediationAdapter.this);
      }

      @Override
      public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
        mediationInterstitialListener.onAdLeftApplication(FyberMediationAdapter.this);
      }
    };
  }
}
