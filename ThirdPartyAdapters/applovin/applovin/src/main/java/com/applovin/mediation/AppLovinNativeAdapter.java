package com.applovin.mediation;

import static
    com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_REQUIRES_UNIFIED_NATIVE_ADS;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.createAdapterError;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;

public class AppLovinNativeAdapter implements MediationNativeAdapter {

  static final String TAG = AppLovinNativeAdapter.class.getSimpleName();

  public static final String KEY_EXTRA_AD_ID = "ad_id";
  public static final String KEY_EXTRA_CAPTION_TEXT = "caption_text";

  @Override
  public void requestNativeAd(final Context context,
      final MediationNativeListener mediationNativeListener,
      final Bundle serverParameters,
      final NativeMediationAdRequest nativeMediationAdRequest,
      final Bundle mediationExtras) {
    if (!nativeMediationAdRequest.isUnifiedNativeAdRequested()
        && !nativeMediationAdRequest.isAppInstallAdRequested()) {
      String errorMessage = createAdapterError(ERROR_REQUIRES_UNIFIED_NATIVE_ADS,
          "Failed to request native ad. Unified Native Ad or App install Ad should " +
              "be requested");
      Log.e(TAG, errorMessage);
      mediationNativeListener.onAdFailedToLoad(this,
          ERROR_REQUIRES_UNIFIED_NATIVE_ADS);
      return;
    }

    final AppLovinSdk sdk = AppLovinUtils.retrieveSdk(serverParameters, context);
    AppLovinNativeAdListener listener = new AppLovinNativeAdListener(this,
        mediationNativeListener, sdk, context, nativeMediationAdRequest);
    sdk.getNativeAdService().loadNativeAds(1, listener);
  }

  @Override
  public void onDestroy() {
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }
}