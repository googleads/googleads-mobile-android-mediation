package com.google.ads.mediation.verizon;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.Configuration;
import com.verizon.ads.VASAds;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;

public class VerizonMediationAdapter extends Adapter
    implements MediationBannerAdapter, MediationInterstitialAdapter, MediationNativeAdapter {

  /**
   * The pixel-to-dpi scale for images downloaded Verizon Ads SDK
   */
  static final double VAS_IMAGE_SCALE = 1.0;

  public static final String TAG = VerizonMediationAdapter.class.getSimpleName();

  /**
   * Weak reference of context.
   */
  private WeakReference<Context> contextWeakRef;

  /**
   * The Verizon Media interstitial renderer.
   */
  private VerizonMediaInterstitialRenderer verizonMediaInterstitialRenderer;

  /**
   * The Verizon Media rewarded ad renderer.
   */
  private VerizonMediaRewardedRenderer verizonMediaRewardedRenderer;

  /**
   * The Verizon Media banner renderer.
   */
  private VerizonMediaBannerRenderer mBannerRenderer;

  /**
   * The Verizon Media native renderer.
   */
  private VerizonMediaNativeRenderer verizonMediaNativeRenderer;

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = com.google.ads.mediation.verizon.BuildConfig.ADAPTER_VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String
        .format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = Configuration.getString("com.verizon.ads", "editionVersion", null);
    if (TextUtils.isEmpty(versionString)) {
      versionString = VASAds.getSDKInfo().version;
    }

    String[] splits = versionString.split("\\.");
    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String
        .format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (!(context instanceof Activity)) {
      initializationCompleteCallback.onInitializationFailed(
          "Verizon Media SDK requires an Activity context to initialize");
      return;
    }

    HashSet<String> siteIDs = new HashSet<>();
    for (MediationConfiguration mediationConfiguration : mediationConfigurations) {
      String siteID = VerizonMediaAdapterUtils.getSiteId(
          mediationConfiguration.getServerParameters(), (Bundle) null);
      if (!TextUtils.isEmpty(siteID)) {
        siteIDs.add(siteID);
      }
    }
    int count = siteIDs.size();
    if (count <= 0) {
      String logMessage = "Initialization failed: Missing or invalid Site ID";
      Log.e(TAG, logMessage);
      initializationCompleteCallback.onInitializationFailed(logMessage);
      return;
    }
    String siteID = siteIDs.iterator().next();
    if (count > 1) {
      String message = String.format("Multiple '%s' entries found: %s. " +
              "Using '%s' to initialize Verizon SDK.", VerizonMediaAdapterUtils.SITE_KEY,
          siteIDs, siteID);
      Log.w(TAG, message);
    }
    if (initializeSDK(context, siteID)) {
      initializationCompleteCallback.onInitializationSucceeded();
    } else {
      initializationCompleteCallback.onInitializationFailed(
          "Verizon SDK initialization failed");
    }
  }

  @Override
  public void requestBannerAd(@NonNull final Context context,
      @NonNull final MediationBannerListener listener, @NonNull final Bundle serverParameters,
      @NonNull com.google.android.gms.ads.AdSize adSize,
      @NonNull final MediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    mBannerRenderer = new VerizonMediaBannerRenderer(this);
    mBannerRenderer.render(context, listener, serverParameters, adSize, mediationAdRequest,
        mediationExtras);
  }

  @NonNull
  @Override
  public View getBannerView() {
    return mBannerRenderer.getBannerView();
  }

  @Override
  public void requestInterstitialAd(@NonNull final Context context,
      @NonNull final MediationInterstitialListener listener, @NonNull final Bundle serverParameters,
      @NonNull final MediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    setContext(context);
    verizonMediaInterstitialRenderer = new VerizonMediaInterstitialRenderer(this);
    verizonMediaInterstitialRenderer.render(context, listener, mediationAdRequest,
        serverParameters, mediationExtras);
  }

  @Override
  public void showInterstitial() {
    Context context = getContext();
    if (context == null) {
      Log.e(TAG, "Failed to show: context is null");
      return;
    }
    verizonMediaInterstitialRenderer.showInterstitial(context);
  }

  @Override
  public void loadRewardedAd(
      @NonNull final MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    verizonMediaRewardedRenderer =
        new VerizonMediaRewardedRenderer(mediationAdLoadCallback,
            mediationRewardedAdConfiguration);
    verizonMediaRewardedRenderer.render();
  }

  @Override
  public void requestNativeAd(@NonNull final Context context,
      @NonNull final MediationNativeListener listener, @NonNull final Bundle serverParameters,
      @NonNull final NativeMediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    verizonMediaNativeRenderer = new VerizonMediaNativeRenderer(this);
    verizonMediaNativeRenderer.render(context, listener, serverParameters, mediationAdRequest,
        mediationExtras);
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "Aborting.");
    if (verizonMediaInterstitialRenderer != null) {
      verizonMediaInterstitialRenderer.destroy();
    }
    if (mBannerRenderer != null) {
      mBannerRenderer.destroy();
    }
    if (verizonMediaNativeRenderer != null) {
      verizonMediaNativeRenderer.destroy();
    }
    if (verizonMediaRewardedRenderer != null) {
      verizonMediaRewardedRenderer.destroy();
    }
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  /**
   * Checks whether Verizon Media SDK is initialized, if not initializes Verizon Media SDK.
   */
  protected static boolean initializeSDK(@NonNull final Context context,
      @NonNull final String siteId) {
    boolean success = true;
    if (!VASAds.isInitialized()) {
      if (!(context instanceof Activity)) {
        Log.e(TAG, "VASAds.initialize must be explicitly called with an Activity context.");
        return false;
      }
      if (TextUtils.isEmpty(siteId)) {
        Log.e(TAG, "Verizon Ads SDK Site ID must be set in mediation extras or server parameters");
        return false;
      }
      try {
        Application application = ((Activity) context).getApplication();
        Log.d(TAG, "Initializing using site ID: " + siteId);
        success = VASAds.initialize(application, siteId);
      } catch (Exception e) {
        Log.w(TAG, "Error occurred initializing Verizon Ads SDK.", e);
        return false;
      }
    }

    VASAds.getActivityStateManager().setState((Activity) context,
        ActivityStateManager.ActivityState.RESUMED);
    VASAds.setDataPrivacy(VerizonPrivacy.getInstance().getDataPrivacy());

    return success;
  }

  private void setContext(@NonNull Context context) {
    contextWeakRef = new WeakReference<>(context);
  }

  @Nullable
  private Context getContext() {
    if (contextWeakRef == null) {
      return null;
    }
    return contextWeakRef.get();
  }
}