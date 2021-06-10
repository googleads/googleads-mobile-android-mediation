package com.google.ads.mediation.imobile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.IntDef;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.VersionInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import jp.co.imobile.sdkads.android.FailNotificationReason;
import jp.co.imobile.sdkads.android.ImobileSdkAd;
import jp.co.imobile.sdkads.android.ImobileSdkAdListener;
import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData;

/**
 * i-mobile mediation adapter for AdMob native ads.
 */
public final class IMobileMediationAdapter extends Adapter implements MediationNativeAdapter {

  // region - Fields for log.
  /**
   * Tag for log.
   */
  private static final String TAG = IMobileMediationAdapter.class.getSimpleName();
  // end region

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_REQUIRES_ACTIVITY_CONTEXT,
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_EMPTY_NATIVE_ADS_LIST
      })

  public @interface AdapterError {

  }

  /**
   * i-mobile adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.imobile";

  /**
   * i-mobile sdk adapter error domain.
   */
  public static final String IMOBILE_SDK_ERROR_DOMAIN = "jp.co.com.google.ads.mediation.imobile";

  /**
   * Activity context is required.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 101;

  /**
   * Server parameters (e.g. publisher ID) are nil.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 102;

  /**
   * The requested ad size does not match an i-mobile supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 103;

  /**
   * i-mobile's native ad load success callback returned an empty native ads list.
   */
  public static final int ERROR_EMPTY_NATIVE_ADS_LIST = 104;

  // region - Adapter interface
  @Override
  public VersionInfo getSDKVersionInfo() {
    // i-mobile does not have any API to retrieve their SDK version.
    return new VersionInfo(0, 0, 0);
  }

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
  public void initialize(
      Context context,
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> list) {

    // i-mobile does not have any API for initialization.
    initializationCompleteCallback.onInitializationSucceeded();
  }
  // end region

  // region - Fields for native ads.
  /**
   * Listener for native ads.
   */
  private MediationNativeListener mediationNativeListener;
  // endregion

  // region - Methods for native ads.
  @Override
  public void requestNativeAd(
      Context context,
      MediationNativeListener listener,
      Bundle serverParameters,
      NativeMediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {

    // Validate Context.
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Context is not an Activity. ", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(this, error);
      return;
    }
    final Activity activity = (Activity) context;

    // Initialize fields.
    this.mediationNativeListener = listener;

    // Get parameters for i-mobile SDK.
    String publisherId = serverParameters.getString(Constants.KEY_PUBLISHER_ID);
    String mediaId = serverParameters.getString(Constants.KEY_MEDIA_ID);
    String spotId = serverParameters.getString(Constants.KEY_SPOT_ID);

    // Call i-mobile SDK.
    ImobileSdkAd.registerSpotInline(activity, publisherId, mediaId, spotId);
    ImobileSdkAd.start(spotId);
    ImobileSdkAd.getNativeAdData(
        activity,
        spotId,
        new ImobileSdkAdListener() {
          @Override
          public void onNativeAdDataReciveCompleted(List<ImobileSdkAdsNativeAdData> adDataList) {
            if (mediationNativeListener == null) {
              return;
            }

            if (adDataList == null || adDataList.isEmpty()) {
              AdError error = new AdError(ERROR_EMPTY_NATIVE_ADS_LIST,
                  "i-mobile's native ad load success callback returned an empty native ads list.",
                  ERROR_DOMAIN);
              Log.w(TAG, error.getMessage());
              mediationNativeListener.onAdFailedToLoad(IMobileMediationAdapter.this, error);
              return;
            }

            final ImobileSdkAdsNativeAdData adData = adDataList.get(0);
            adData.getAdImage(
                activity,
                new ImobileSdkAdListener() {
                  @Override
                  public void onNativeAdImageReciveCompleted(Bitmap image) {
                    Drawable drawable = new BitmapDrawable(activity.getResources(), image);
                    mediationNativeListener.onAdLoaded(
                        IMobileMediationAdapter.this,
                        new IMobileUnifiedNativeAdMapper(adData, drawable));
                  }
                });
          }

          @Override
          public void onFailed(FailNotificationReason reason) {
            AdError error = AdapterHelper.getAdError(reason);
            Log.w(TAG, error.getMessage());
            if (mediationNativeListener != null) {
              mediationNativeListener.onAdFailedToLoad(IMobileMediationAdapter.this, error);
            }
          }
        });
  }
  // endregion

  // region - Methods of life cycle.
  @Override
  public void onDestroy() {
    // Release objects.
    mediationNativeListener = null;
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }
  // endregion

}