package com.google.ads.mediation.fyber;

import static com.google.ads.mediation.fyber.FyberAdapterUtils.getAdError;

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
import com.fyber.inneractive.sdk.external.InneractiveUserConfig;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
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
import com.google.android.gms.ads.mediation.VersionInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Fyber's official AdMob 3rd party adapter class. Implements Banners and Interstitials by
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
   * Fyber requires to know the host mediation platform.
   */
  private final static InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

  /**
   * Key to obtain App id, required for initializing Fyber's SDK.
   */
  private static final String KEY_APP_ID = "applicationId";

  /**
   * Key to obtain a placement name or spot id. Required for creating a Fyber ad request.
   */
  static final String KEY_SPOT_ID = "spotId";

  /**
   * Requested banner ad size.
   */
  private AdSize requestedAdSize;

  /**
   * Fyber's Spot object for the banner.
   */
  private InneractiveAdSpot mBannerSpot;

  /**
   * Holds the banner view which is created by Fyber, in order to return when AdMob calls getView.
   */
  private ViewGroup mBannerWrapperView;

  /**
   * AdMob's external Banner listener.
   */
  private MediationBannerListener mMediationBannerListener;

  /**
   * AdMob's external Interstitial listener.
   */
  private MediationInterstitialListener mMediationInterstitialListener;

  /**
   * The context which was passed by AdMob to {@link #requestInterstitialAd}.
   */
  private WeakReference<Context> mInterstitialContext;

  /**
   * Fyber's spot object for interstitial.
   */
  private InneractiveAdSpot mInterstitialSpot;

  /**
   * Fyber rewarded ad video renderer.
   */
  private FyberRewardedVideoRenderer mRewardedRenderer;

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
   * Fyber adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.fyber";

  /**
   * Server parameters, such as app ID or spot ID, are invalid.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Fyber supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 103;

  /**
   * Fyber SDK loaded an ad but returned an unexpected controller.
   */
  public static final int ERROR_WRONG_CONTROLLER_TYPE = 105;

  /**
   * Ad not ready.
   */
  public static final int ERROR_AD_NOT_READY = 106;

  /**
   * Only rewarded ads are implemented using the new Adapter interface.
   */
  public void loadRewardedAd(final MediationRewardedAdConfiguration configuration,
      @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
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
            mRewardedRenderer = new FyberRewardedVideoRenderer(configuration, callback);
            mRewardedRenderer.render();
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

    List<String> configuredAppIds = new ArrayList<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      if (serverParameters == null) {
        continue;
      }

      String appId = serverParameters.getString(KEY_APP_ID);
      if (!TextUtils.isEmpty(appId)) {
        configuredAppIds.add(appId);
      }
    }

    if (configuredAppIds.isEmpty()) {
      if (completionCallback != null) {
        AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
            "Failed to initialize. Fyber SDK requires an appId to be configured on the AdMob UI.",
            ERROR_DOMAIN);
        Log.w(TAG, error.getMessage());
        completionCallback.onInitializationFailed(error.getMessage());
      }
      return;
    }

    // We can only use one app id.
    String appIdForInitialization = configuredAppIds.get(0);
    if (configuredAppIds.size() > 1) {
      if (completionCallback != null) {
        String message = String.format("Multiple '%s' entries found: %s. "
                + "Using '%s' to initialize the Fyber Marketplace SDK.", KEY_APP_ID,
            configuredAppIds.toString(), appIdForInitialization);
        AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, message, ERROR_DOMAIN);
        Log.w(TAG, error.getMessage());
        completionCallback.onInitializationFailed(error.getMessage());
      }
      return;
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
      final Bundle serverParameters, @NonNull final AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable final Bundle mediationExtras) {
    mMediationBannerListener = mediationBannerListener;
    String keyAppId = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(keyAppId)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "App ID is null or empty.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
      return;
    }

    InneractiveAdManager.initialize(context, keyAppId, new OnFyberMarketplaceInitializedListener() {
      @Override
      public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
        if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
          AdError error = getAdError(fyberInitStatus);
          Log.w(TAG, error.getMessage());
          mMediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
          return;
        }

        // Check that we got a valid Spot ID from the server.
        String spotId = serverParameters.getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
          AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
              "Cannot render banner ad. Please define a valid spot id on the AdMob UI.",
              ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mMediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
          return;
        }

        mBannerSpot = InneractiveAdSpotManager.get().createSpot();
        mBannerSpot.setMediationName(MEDIATOR_NAME);

        InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
        mBannerSpot.addUnitController(controller);

        // Prepare wrapper view before making request.
        mBannerWrapperView = new RelativeLayout(context);

        InneractiveAdSpot.RequestListener requestListener = createFyberBannerAdListener();
        mBannerSpot.setRequestListener(requestListener);

        requestedAdSize = adSize;
        InneractiveAdRequest request = new InneractiveAdRequest(spotId);
        final InneractiveUserConfig inneractiveUserConfig = FyberAdapterUtils
            .generateUserConfig(mediationExtras);
        request.setUserParams(inneractiveUserConfig);
        mBannerSpot.requestAd(request);
      }
    });
  }

  @NonNull
  @Override
  public View getBannerView() {
    return mBannerWrapperView;
  }

  @Override
  public void onDestroy() {
    if (mBannerSpot != null) {
      mBannerSpot.destroy();
      mBannerSpot = null;
    }

    if (mInterstitialSpot != null) {
      mInterstitialSpot.destroy();
      mInterstitialSpot = null;
    }

    if (mInterstitialContext != null) {
      mInterstitialContext.clear();
      mInterstitialContext = null;
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
   * Creates Fyber's banner ad request listener.
   *
   * @return the created request listener.
   */
  @NonNull
  private InneractiveAdSpot.RequestListener createFyberBannerAdListener() {
    return new InneractiveAdSpot.RequestListener() {
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        // Just a double check that we have the right type of selected controller.
        if (!(mBannerSpot.getSelectedUnitController() instanceof
            InneractiveAdViewUnitController)) {
          String message = String.format("Unexpected controller type. Expected: %s. Actual: %s",
              InneractiveUnitController.class.getName(),
              mBannerSpot.getSelectedUnitController().getClass().getName());
          AdError error = new AdError(ERROR_WRONG_CONTROLLER_TYPE, message, ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mMediationBannerListener
              .onAdFailedToLoad(FyberMediationAdapter.this, error);
          mBannerSpot.destroy();
        }

        InneractiveAdViewUnitController controller =
            (InneractiveAdViewUnitController) mBannerSpot.getSelectedUnitController();
        InneractiveAdViewEventsListener listener = createFyberAdViewListener();
        controller.setEventsListener(listener);
        controller.bindView(mBannerWrapperView);

        // Validate the ad size returned by Fyber Marketplace with the requested ad size.
        Context context = mBannerWrapperView.getContext();
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
          mMediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
          return;
        }
        mMediationBannerListener.onAdLoaded(FyberMediationAdapter.this);
      }

      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
          InneractiveErrorCode inneractiveErrorCode) {
        AdError error = getAdError(inneractiveErrorCode);
        Log.w(TAG, error.getMessage());
        mMediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
      }
    };
  }

  /**
   * When an ad is fetched successfully, creates a listener for Fyber's AdView events.
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
        mMediationBannerListener.onAdClicked(FyberMediationAdapter.this);
        mMediationBannerListener.onAdOpened(FyberMediationAdapter.this);
      }

      @Override
      public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
        mMediationBannerListener.onAdClosed(FyberMediationAdapter.this);
      }

      @Override
      public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
        mMediationBannerListener.onAdLeftApplication(FyberMediationAdapter.this);
      }
    };
  }

  /**
   * {@link MediationInterstitialAdapter} implementation.
   */
  @Override
  public void requestInterstitialAd(@NonNull final Context context,
      @NonNull final MediationInterstitialListener mediationInterstitialListener,
      final Bundle serverParameters,
      @NonNull MediationAdRequest mediationAdRequest,
      @NonNull final Bundle mediationExtras) {

    mMediationInterstitialListener = mediationInterstitialListener;

    String keyAppId = serverParameters.getString(KEY_APP_ID);
    AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "App ID is null or empty.",
        ERROR_DOMAIN);
    if (TextUtils.isEmpty(keyAppId)) {
      Log.w(TAG, error.getMessage());
      mMediationInterstitialListener
          .onAdFailedToLoad(FyberMediationAdapter.this, error);
      return;
    }

    InneractiveAdManager.initialize(context, keyAppId, new OnFyberMarketplaceInitializedListener() {
      @Override
      public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
        if (fyberInitStatus != FyberInitStatus.SUCCESSFULLY) {
          AdError error = getAdError(fyberInitStatus);
          Log.w(TAG, error.getMessage());
          mMediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
          return;
        }

        // Check that we got a valid spot id from the server.
        String spotId = serverParameters.getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
          AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
              "Cannot render interstitial ad. Please define a valid spot id on the AdMob UI.",
              ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mMediationInterstitialListener
              .onAdFailedToLoad(FyberMediationAdapter.this, error);
          return;
        }

        // Cache the context for showInterstitial.
        mInterstitialContext = new WeakReference<>(context);

        mInterstitialSpot = InneractiveAdSpotManager.get().createSpot();
        mInterstitialSpot.setMediationName(MEDIATOR_NAME);

        InneractiveFullscreenUnitController controller = new InneractiveFullscreenUnitController();
        mInterstitialSpot.addUnitController(controller);

        InneractiveAdSpot.RequestListener requestListener = createFyberInterstitialAdListener();
        mInterstitialSpot.setRequestListener(requestListener);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);
        final InneractiveUserConfig inneractiveUserConfig = FyberAdapterUtils
            .generateUserConfig(mediationExtras);
        request.setUserParams(inneractiveUserConfig);
        mInterstitialSpot.requestAd(request);
      }
    });
  }

  @Override
  public void showInterstitial() {
    Context context = (mInterstitialContext != null ? mInterstitialContext.get() : null);
    if (context == null) {
      Log.w(TAG, "showInterstitial called, but context reference was lost.");
      mMediationInterstitialListener.onAdOpened(this);
      mMediationInterstitialListener.onAdClosed(this);
      return;
    }

    if (!(mInterstitialSpot
        .getSelectedUnitController() instanceof InneractiveFullscreenUnitController)) {
      Log.w(TAG, "showInterstitial called, but wrong spot has been used (should not happen).");
      mMediationInterstitialListener.onAdOpened(this);
      mMediationInterstitialListener.onAdClosed(this);
      return;
    }
    InneractiveFullscreenUnitController controller =
        (InneractiveFullscreenUnitController) mInterstitialSpot.getSelectedUnitController();

    if (!mInterstitialSpot.isReady()) {
      Log.w(TAG, "showInterstitial called, but Ad has expired.");
      mMediationInterstitialListener.onAdOpened(this);
      mMediationInterstitialListener.onAdClosed(this);
      return;
    }
    controller.show(context);
  }

  @NonNull
  private InneractiveAdSpot.RequestListener createFyberInterstitialAdListener() {
    return new InneractiveAdSpot.RequestListener() {
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        if (!(mInterstitialSpot.getSelectedUnitController() instanceof
            InneractiveFullscreenUnitController)) {
          String message = String.format("Unexpected controller type. Expected: %s. Actual: %s",
              InneractiveUnitController.class.getName(),
              mBannerSpot.getSelectedUnitController().getClass().getName());
          AdError error = new AdError(ERROR_WRONG_CONTROLLER_TYPE, message, ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mMediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
          mInterstitialSpot.destroy();
        }

        InneractiveFullscreenUnitController controller =
            (InneractiveFullscreenUnitController) mInterstitialSpot.getSelectedUnitController();
        InneractiveFullscreenAdEventsListener listener = createFyberInterstitialListener();
        controller.setEventsListener(listener);

        mMediationInterstitialListener.onAdLoaded(FyberMediationAdapter.this);
      }

      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
          InneractiveErrorCode inneractiveErrorCode) {
        // Convert Fyber's Marketplace error code into custom error code
        AdError error = getAdError(inneractiveErrorCode);
        Log.w(TAG, error.getMessage());
        mMediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, error);
      }
    };
  }

  /**
   * Creates a listener for Fyber's Interstitial events
   *
   * @return the created event listener.
   */
  @NonNull
  private InneractiveFullscreenAdEventsListener createFyberInterstitialListener() {
    return new InneractiveFullscreenAdEventsListenerAdapter() {
      @Override
      public void onAdImpression(InneractiveAdSpot adSpot) {
        mMediationInterstitialListener.onAdOpened(FyberMediationAdapter.this);
      }

      @Override
      public void onAdClicked(InneractiveAdSpot adSpot) {
        mMediationInterstitialListener.onAdClicked(FyberMediationAdapter.this);
      }

      @Override
      public void onAdDismissed(InneractiveAdSpot adSpot) {
        mMediationInterstitialListener.onAdClosed(FyberMediationAdapter.this);
      }

      @Override
      public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
        mMediationInterstitialListener.onAdLeftApplication(FyberMediationAdapter.this);
      }
    };
  }
}
