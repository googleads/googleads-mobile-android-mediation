package com.google.ads.mediation.imobile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;

import java.util.List;

import jp.co.imobile.sdkads.android.FailNotificationReason;
import jp.co.imobile.sdkads.android.ImobileSdkAd;
import jp.co.imobile.sdkads.android.ImobileSdkAdListener;
import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData;

/**
 * i-mobile mediation adapter for AdMob native ads.
 */
public final class IMobileMediationAdapter implements MediationNativeAdapter {

    // region - Fields for log.

    /** Tag for log. */
    private static final String TAG = IMobileMediationAdapter.class.getSimpleName();

    // endregion

    // region - Fields for native ads.

    /** Listener for native ads. */
    private MediationNativeListener mediationNativeListener;

    // endregion

    // region - Methods for native ads.

    @Override
    public void requestNativeAd(Context context, MediationNativeListener listener,
            Bundle serverParameters, NativeMediationAdRequest mediationAdRequest,
            Bundle mediationExtras) {

        // Validate Context.
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Native : Context is not Activity.");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        final Activity activity = (Activity) context;

        // Check request type.
        if (!mediationAdRequest.isUnifiedNativeAdRequested()) {
            Log.w(TAG, "Native : i-mobile SDK only support UnifiedNativeAd.");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        // Initialize fields.
        this.mediationNativeListener = listener;

        // Get parameters for i-mobile SDK.
        String publisherId = serverParameters.getString(Constants.KEY_PUBLISHER_ID);
        String mediaId = serverParameters.getString(Constants.KEY_MEDIA_ID);
        String spotId = serverParameters.getString(Constants.KEY_SPOT_ID);

        // Call i-mobile SDK.
        ImobileSdkAd.registerSpotInline(activity, publisherId, mediaId, spotId);
        ImobileSdkAd.start(spotId);
        ImobileSdkAd.getNativeAdData(activity, spotId, new ImobileSdkAdListener() {
            @Override
            public void onNativeAdDataReciveCompleted(List<ImobileSdkAdsNativeAdData> adDataList) {
                if (mediationNativeListener == null) {
                    return;
                }

                if (adDataList == null || adDataList.isEmpty()) {
                    Log.w(TAG, "Native : No ads.");
                    mediationNativeListener.onAdFailedToLoad(IMobileMediationAdapter.this,
                            AdRequest.ERROR_CODE_NO_FILL);
                    return;
                }

                final ImobileSdkAdsNativeAdData adData = adDataList.get(0);
                adData.getAdImage(activity, new ImobileSdkAdListener() {
                    @Override
                    public void onNativeAdImageReciveCompleted(Bitmap image) {
                        Drawable drawable = new BitmapDrawable(activity.getResources(), image);
                        mediationNativeListener.onAdLoaded(IMobileMediationAdapter.this,
                                new IMobileUnifiedNativeAdMapper(adData, drawable));
                    }
                });
            }

            @Override
            public void onFailed(FailNotificationReason reason) {
                Log.w(TAG, "Native : Error. Reason is " + reason);
                if (mediationNativeListener != null) {
                    mediationNativeListener.onAdFailedToLoad(IMobileMediationAdapter.this,
                            AdapterHelper.convertToAdMobErrorCode(reason));
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
