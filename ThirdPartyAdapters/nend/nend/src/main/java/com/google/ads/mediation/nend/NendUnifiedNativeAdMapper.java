package com.google.ads.mediation.nend;

import static com.google.ads.mediation.nend.NendMediationAdapter.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;

abstract class NendUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  NendUnifiedNativeAdMapper(NendNativeMappedImage logoImage) {
    if (logoImage == null) {
      Log.w(TAG,
          "Missing Icon image of nend's native ad, so UnifiedNativeAd#getIcon() will be null.");
    }
    setIcon(logoImage);
    setOverrideImpressionRecording(true);
  }

  static boolean canDownloadImage(Context context, String url) {
    return context != null && !TextUtils.isEmpty(url);
  }
}
