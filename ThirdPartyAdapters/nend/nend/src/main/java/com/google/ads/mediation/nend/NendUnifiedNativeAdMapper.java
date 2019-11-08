package com.google.ads.mediation.nend;

import android.content.Context;
import android.text.TextUtils;

import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;

abstract class NendUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

    NendUnifiedNativeAdMapper(NendNativeMappedImage logoImage) {
        setIcon(logoImage);

        setOverrideImpressionRecording(true);

        // Note: can't set OnClickListener at NendAdNative#activate()
        //setOverrideClickHandling(true);
    }

    static boolean canDownloadImage(Context context, String url) {
        return context != null && !TextUtils.isEmpty(url);
    }

}
