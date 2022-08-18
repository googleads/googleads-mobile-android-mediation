package com.google.ads.mediation.mintegral;

import static com.mintegral.mediation.MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.RequestConfiguration;
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
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.SDKInitStatusListener;
import com.mintegral.mediation.AdapterTools;
import com.mintegral.mediation.MintegralConstants;
import com.mintegral.mediation.rtb.MintegralRtbInterstitialAd;
import com.mintegral.mediation.rtb.MintegralRtbNativeAd;
import com.mintegral.mediation.rtb.MintegralRtbBannerAd;
import com.mintegral.mediation.rtb.MintegralRtbRewardAd;

import java.util.List;
import java.util.Map;

public class MintegralMediationAdapter extends RtbAdapter implements MediationAdapter {
  public static final String TAG = MintegralMediationAdapter.class.getSimpleName();
  private static MBridgeSDK mBridgeSDK;
  private MintegralRtbBannerAd mMintegralRtbBannerAd;
  @Override
  public void collectSignals(@NonNull RtbSignalData rtbSignalData, @NonNull SignalCallbacks signalCallbacks) {
    String buyerUid = BidManager.getBuyerUid(rtbSignalData.getContext());
    signalCallbacks.onSuccess(buyerUid);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
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
    for (MediationConfiguration mediationConfiguration : list) {
      Bundle serverParameters = mediationConfiguration.getServerParameters();
      String appId = serverParameters.getString(MintegralConstants.APP_ID);
      String appKey = serverParameters.getString(MintegralConstants.APP_KEY);
      if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(appKey)) {
        AdError error =
                MintegralConstants.createAdapterError(
                        ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid App ID or App key.");
        Log.w(TAG, error.toString());
        initializationCompleteCallback.onInitializationFailed(error.toString());
        return;
      }
      mBridgeSDK = MBridgeSDKFactory.getMBridgeSDK();
      Map<String, String> configurationMap = mBridgeSDK.getMBConfigurationMap(appId, appKey);
      mBridgeSDK.init(configurationMap, context, new SDKInitStatusListener() {
        @Override
        public void onInitSuccess() {
          AdapterTools.addChannel();
          initializationCompleteCallback.onInitializationSucceeded();
        }

        @Override
        public void onInitFail(String s) {
          initializationCompleteCallback.onInitializationFailed(s);
        }
      });
    }
  }

  @Override
  public void loadRtbBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    mMintegralRtbBannerAd = new MintegralRtbBannerAd(adConfiguration, callback);
    mMintegralRtbBannerAd.load();
  }

  @Override
  public void loadRtbInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    MintegralRtbInterstitialAd rtbInterstitialAd = new MintegralRtbInterstitialAd(adConfiguration, callback);
    rtbInterstitialAd.load();
  }

  @Override
  public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    MintegralRtbNativeAd mintegralRtbNativeAd = new MintegralRtbNativeAd(adConfiguration,callback);
    mintegralRtbNativeAd.load();
  }

  @Override
  public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration, @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    MintegralRtbRewardAd rtbRewardAd = new MintegralRtbRewardAd(adConfiguration, callback);
    rtbRewardAd.load();
  }


  @Override
  public void onDestroy() {
    if(mMintegralRtbBannerAd!= null){
      mMintegralRtbBannerAd.onDestroy();
    }
  }

  @Override
  public void onPause() {

  }

  @Override
  public void onResume() {

  }

  /**
   * Set the COPPA setting in MIntegral SDK.
   *
   * @param coppa Whether the user is allowed to track user information, {@link RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE } does not track, other is to track
   */
  public static void setCoppa(@RequestConfiguration.TagForChildDirectedTreatment int coppa) {
    if(mBridgeSDK != null){
      mBridgeSDK.setDoNotTrackStatus(coppa == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE);
    }
  }

  /**
   * Set the GDPR setting in MIntegral SDK.
   *
   * @param context
   * @param allow Whether the user is allowed to collect gdpr information, true is allowed, false is not allowed
   *
   */
  public static void setGdpr(Context context,boolean allow) {
    if(mBridgeSDK != null){
      mBridgeSDK.setConsentStatus(context,allow? MBridgeConstans.IS_SWITCH_ON: MBridgeConstans.IS_SWITCH_OFF);
    }else {
      Log.w(TAG, "mintegral sdk not init");
    }
  }

  /**
   * Set the CCPA setting in MIntegral SDK.
   *
   * @param ccpa Whether the user is allowed to track user information, true does not track, false is to track
   *
   */
  public static void setCcpa(boolean ccpa) {
    if(mBridgeSDK != null){
      mBridgeSDK.setDoNotTrackStatus(ccpa);
    }else {
      Log.w(TAG, "mintegral sdk not init");
    }
  }
}
