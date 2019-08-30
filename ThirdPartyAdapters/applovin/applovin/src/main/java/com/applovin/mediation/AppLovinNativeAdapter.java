package com.applovin.mediation;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;

public class AppLovinNativeAdapter
        implements MediationNativeAdapter {
    static final String TAG = AppLovinNativeAdapter.class.getSimpleName();

    public static final String KEY_EXTRA_AD_ID = "ad_id";
    public static final String KEY_EXTRA_CAPTION_TEXT = "caption_text";

    @Override
    public void requestNativeAd(final Context context,
                                final MediationNativeListener mediationNativeListener,
                                final Bundle serverParameters,
                                final NativeMediationAdRequest nativeMediationAdRequest,
                                final Bundle mediationExtras) {
        if (nativeMediationAdRequest.isContentAdRequested()
                && !nativeMediationAdRequest.isAppInstallAdRequested()) {
            Log.e(TAG, "Failed to request native ad. App install format needs to be requested");
            mediationNativeListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        final AppLovinSdk sdk = AppLovinUtils.retrieveSdk(serverParameters, context);
        AppLovinNativeAdListener listener =
                new AppLovinNativeAdListener(this, mediationNativeListener, sdk, context);
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