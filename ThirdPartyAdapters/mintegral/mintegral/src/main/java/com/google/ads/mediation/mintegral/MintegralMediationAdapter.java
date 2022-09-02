package com.google.ads.mediation.mintegral;


import static com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdapter;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.foundation.same.net.Aa;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.SDKInitStatusListener;
import com.mintegral.mediation.rtb.MintegralRtbBannerAd;
import com.mintegral.mediation.rtb.MintegralRtbInterstitialAd;
import com.mintegral.mediation.rtb.MintegralRtbNativeAd;
import com.mintegral.mediation.rtb.MintegralRtbRewardedAd;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MintegralMediationAdapter extends RtbAdapter implements MediationAdapter {
  public static final String TAG = MintegralMediationAdapter.class.getSimpleName();
  private static MBridgeSDK mBridgeSDK;
  private MintegralRtbBannerAd mintegralRtbBannerAd;
  private MintegralRtbInterstitialAd mintegralRtbInterstitialAd;
  private MintegralRtbRewardedAd mintegralRtbRewardedAd;
  private MintegralRtbNativeAd mintegralRtbNativeAd;
  private static int CCPAValues = -1;
  private static int GDPRValue = -1;

  @Override
  public void collectSignals(@NonNull RtbSignalData rtbSignalData, @NonNull SignalCallbacks signalCallbacks) {
    String buyerUid = BidManager.getBuyerUid(rtbSignalData.getContext());
    signalCallbacks.onSuccess(buyerUid);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    //SDK_VERSION string: MAL_16.2.11
    String versionString = MBConfiguration.SDK_VERSION;
    String[] versionSplits = versionString.split("_");
    if (versionSplits.length > 1) {
      String[] splits = versionSplits[1].split("\\.");

      if (splits.length >= 3) {
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
      }
    }

    String logMessage =
            String.format(
                    "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.ADAPTER_VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
            String.format(
                    "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
                    versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull Context context,
                         @NonNull InitializationCompleteCallback initializationCompleteCallback,
                         @NonNull List<MediationConfiguration> list) {
    HashSet<String> appIds = new HashSet<>();
    HashSet<String> appKeys = new HashSet<>();
    for (MediationConfiguration mediationConfiguration : list) {
      Bundle serverParameters = mediationConfiguration.getServerParameters();
      String appId = serverParameters.getString(MintegralConstants.APP_ID);
      String appKey = serverParameters.getString(MintegralConstants.APP_KEY);
      if (!TextUtils.isEmpty(appId)) {
        appIds.add(appId);
      }
      if (!TextUtils.isEmpty(appKey)) {
        appKeys.add(appKey);
      }
    }
    int count = appIds.size();
    int keyCount = appKeys.size();
    if (count <= 0) {
      AdError error =
              MintegralConstants.createAdapterError(
                      ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid App ID.");
      Log.w(TAG, error.toString());
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    }
    if (keyCount <= 0) {
      AdError error =
              MintegralConstants.createAdapterError(
                      ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid App key.");
      Log.w(TAG, error.toString());
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    }


    String appId = appIds.iterator().next();
    String appKey = appKeys.iterator().next();
    if (count > 1) {
      String message = String.format(
              "Found multiple app IDs in %s. Using %s to initialize Mintegral SDK.", appIds, appId);
      Log.w(TAG, message);
    }
    if (keyCount > 1) {
      String message = String.format(
              "Found multiple appKeys in %s. Using %s to initialize Mintegral SDK.", appKeys, appKey);
      Log.w(TAG, message);
    }
    mBridgeSDK = MBridgeSDKFactory.getMBridgeSDK();
    Map<String, String> configurationMap = mBridgeSDK.getMBConfigurationMap(appId, appKey);
    if (CCPAValues == 0 || CCPAValues == 1) {
      mBridgeSDK.setDoNotTrackStatus(CCPAValues == 1);
    }
    if (GDPRValue == 0 || GDPRValue == 1) {
      mBridgeSDK.setConsentStatus(context, GDPRValue == 0 ? MBridgeConstans.IS_SWITCH_ON : MBridgeConstans.IS_SWITCH_OFF);
    }
    mBridgeSDK.init(configurationMap, context, new SDKInitStatusListener() {
      @Override
      public void onInitSuccess() {
        addChannel();
        initializationCompleteCallback.onInitializationSucceeded();
      }

      @Override
      public void onInitFail(String s) {
        initializationCompleteCallback.onInitializationFailed(s);
      }
    });

  }

  @Override
  public void loadRtbBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    mintegralRtbBannerAd = new MintegralRtbBannerAd(adConfiguration, callback);
    mintegralRtbBannerAd.load();
  }

  @Override
  public void loadRtbInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    mintegralRtbInterstitialAd = new MintegralRtbInterstitialAd(adConfiguration, callback);
    mintegralRtbInterstitialAd.loadAd();
  }

  @Override
  public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    mintegralRtbNativeAd = new MintegralRtbNativeAd(adConfiguration, callback);
    mintegralRtbNativeAd.loadAd();
  }

  @Override
  public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    mintegralRtbRewardedAd = new MintegralRtbRewardedAd(adConfiguration, callback);
    mintegralRtbRewardedAd.loadAd();
  }


  @Override
  public void onDestroy() {
    if (mintegralRtbBannerAd != null) {
      mintegralRtbBannerAd.onDestroy();
    }
  }

  @Override
  public void onPause() {

  }

  @Override
  public void onResume() {

  }

  /**
   * Call the private static method inside the Mintegral SDK through reflection to set the channel flag
   *
   */
  private void addChannel() {
    try {
      //create Object
      Aa a = new Aa();
      Class c = a.getClass();
      //call private static method "b"
      Method method = c.getDeclaredMethod("b", String.class);
      method.setAccessible(true);
      //set channel flag. "Y+H6DFttYrPQYcIBicKwJQKQYrN=" is used to mark the AdMob channel
      method.invoke(a, "Y+H6DFttYrPQYcIBicKwJQKQYrN=");
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Set the GDPR setting in Mintegral SDK.
   *
   * @param gdpr an {@code Integer} value that indicates whether the user consents the use of
   *             personal data to serve ads under GDPR. {@code 0} means the user consents. {@code 1}
   *             means the user does not consent. {@code -1} means the user hasn't specified. Any
   *             value outside of -1, 0, or 1 will result in this method being a no-op.
   *             See <a
   *             href="https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-android&lang=en">Mintegral's
   *             documentation</a> for more information about what values may be provided.
   */
  public static void setGdpr(int gdpr) {
    if (gdpr != 0 && gdpr != 1 && gdpr != -1) {
      // no-op
      Log.w(TAG, "Invalid GDPR value. Mintegral SDK only accepts -1, 0 or 1.");
      return;
    }
    GDPRValue = gdpr;
  }

  /**
   * Set the CCPA setting in Mintegral SDK.
   *
   * @param ccpa an {@code Integer} value that indicates whether the user opts in of the "sale" of
   *             the "personal information" under CCPA. {@code 0} means the user opts in. {@code 1}
   *             means the user opts out. {@code -1} means the user hasn't specified. Any value
   *             outside of -1, 0, or 1 will result in this method being a no-op.See <a
   *             href="https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-android&lang=en">Mintegral's
   *             documentation</a> for more information about what values may be provided.
   */
  public static void setCcpa(int ccpa) {
    if (ccpa != 0 && ccpa != 1 && ccpa != -1) {
      // no-op
      Log.w(TAG, "Invalid CCPA value. Mintegral SDK only accepts -1, 0 or 1.");
      return;
    }
    CCPAValues = ccpa;
  }
}
