package com.google.ads.mediation.imobile;

import jp.co.imobile.sdkads.android.AdMobMediationSupportAdSize;
import jp.co.imobile.sdkads.android.FailNotificationReason;
import jp.co.imobile.sdkads.android.ImobileSdkAd;
import jp.co.imobile.sdkads.android.ImobileSdkAdListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

/**
 * i-mobile mediation adapter for AdMob banner and interstitial ads.
 */
public final class IMobileAdapter implements MediationBannerAdapter, MediationInterstitialAdapter {

    // region - Fields for log.

    /** Tag for log. */
    private static final String TAG = IMobileAdapter.class.getSimpleName();

    // endregion

    // region - Fields for banner ads.

    /** Listener for banner ads. */
    private MediationBannerListener mediationBannerListener;

    /** View to display banner ads. */
    private ViewGroup bannerView;

    /** Supported ad sizes. */
    private static final AdSize[] supportedSizes;

    static {
        // Initialize static fields.
        AdMobMediationSupportAdSize[] iMobileAdSizes = AdMobMediationSupportAdSize.values();
        supportedSizes = new AdSize[iMobileAdSizes.length];
        for (int i = 0; i < iMobileAdSizes.length; i++) {
            supportedSizes[i] = new AdSize(iMobileAdSizes[i].getWidth(),
                    iMobileAdSizes[i].getHeight());
        }
    }

    // endregion

    // region - Methods for banner ads.

    @Override
    public void requestBannerAd(Context context, MediationBannerListener listener,
            Bundle serverParameters, AdSize adSize, MediationAdRequest mediationAdRequest,
            Bundle mediationExtras) {

        // Validate AdSize.
        boolean isSupportedSize = false;
        for (AdSize iMobileSize : supportedSizes) {
            if (adSize.getWidth() == iMobileSize.getWidth()
                    && adSize.getHeight() == iMobileSize.getHeight()) {
                isSupportedSize = true;
                break;
            }
        }
        if (!isSupportedSize) {
            Log.w(TAG, "Banner : " + adSize.toString() + " is not supported.");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        // Validate Context.
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Banner : Context is not Activity.");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        Activity activity = (Activity) context;

        // Initialize fields.
        mediationBannerListener = listener;

        // Get parameters for i-mobile SDK.
        String publisherId = serverParameters.getString(Constants.KEY_PUBLISHER_ID);
        String mediaId = serverParameters.getString(Constants.KEY_MEDIA_ID);
        String spotId = serverParameters.getString(Constants.KEY_SPOT_ID);

        // Call i-mobile SDK.
        ImobileSdkAd.registerSpotInline(activity, publisherId, mediaId, spotId);
        ImobileSdkAd.start(spotId);
        ImobileSdkAd.setImobileSdkAdListener(spotId, new ImobileSdkAdListener() {
            @Override
            public void onAdReadyCompleted() {
                if (mediationBannerListener != null) {
                    mediationBannerListener.onAdLoaded(IMobileAdapter.this);
                }
            }

            @Override
            public void onAdCliclkCompleted() {
                if (mediationBannerListener != null) {
                    mediationBannerListener.onAdClicked(IMobileAdapter.this);
                    mediationBannerListener.onAdOpened(IMobileAdapter.this);
                    mediationBannerListener.onAdLeftApplication(IMobileAdapter.this);
                }
            }

            @Override
            public void onDismissAdScreen() {
                if (mediationBannerListener != null) {
                    mediationBannerListener.onAdClosed(IMobileAdapter.this);
                }
            }

            @Override
            public void onFailed(FailNotificationReason reason) {
                Log.w(TAG, "Banner : Error. Reason is " + reason);
                if (mediationBannerListener != null) {
                    mediationBannerListener.onAdFailedToLoad(IMobileAdapter.this,
                            AdapterHelper.convertToAdMobErrorCode(reason));
                }
            }
        });

        // Create view to display banner ads.
        bannerView = new FrameLayout(activity);
        bannerView.setLayoutParams(new FrameLayout.LayoutParams(adSize.getWidthInPixels(activity),
                adSize.getHeightInPixels(activity)));

        // Start getting ads.
        ImobileSdkAd.showAdForAdMobMediation(activity, spotId, bannerView);
    }

    @Override
    public View getBannerView() {
        return bannerView;
    }

    // endregion

    // region - Fields for interstitial ads.

    /** Listener for interstitial ads. */
    private MediationInterstitialListener mediationInterstitialListener;

    /** Activity to display interstitial ads. */
    private Activity interstitialActivity;

    /** i-mobile spot ID. */
    private String interstitialSpotId;

    // endregion

    // region - Methods for interstitial ads.

    @Override
    public void requestInterstitialAd(Context context, MediationInterstitialListener listener,
            Bundle serverParameters, MediationAdRequest mediationAdRequest,
            Bundle mediationExtras) {

        // Validate Context.
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Interstitial : Context is not Activity.");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        interstitialActivity = (Activity) context;

        // Initialize fields.
        mediationInterstitialListener = listener;

        // Get parameters for i-mobile SDK.
        String publisherId = serverParameters.getString(Constants.KEY_PUBLISHER_ID);
        String mediaId = serverParameters.getString(Constants.KEY_MEDIA_ID);
        interstitialSpotId = serverParameters.getString(Constants.KEY_SPOT_ID);

        // Call i-mobile SDK.
        ImobileSdkAd.registerSpotFullScreen(interstitialActivity, publisherId, mediaId,
                interstitialSpotId);
        ImobileSdkAd.setImobileSdkAdListener(interstitialSpotId, new ImobileSdkAdListener() {
            @Override
            public void onAdReadyCompleted() {
                if (mediationInterstitialListener != null) {
                    mediationInterstitialListener.onAdLoaded(IMobileAdapter.this);
                }
            }

            @Override
            public void onAdShowCompleted() {
                if (mediationInterstitialListener != null) {
                    mediationInterstitialListener.onAdOpened(IMobileAdapter.this);
                }
            }

            @Override
            public void onAdCliclkCompleted() {
                if (mediationInterstitialListener != null) {
                    mediationInterstitialListener.onAdClicked(IMobileAdapter.this);
                    mediationInterstitialListener.onAdLeftApplication(IMobileAdapter.this);
                }
            }

            @Override
            public void onAdCloseCompleted() {
                if (mediationInterstitialListener != null) {
                    mediationInterstitialListener.onAdClosed(IMobileAdapter.this);
                }
            }

            @Override
            public void onFailed(FailNotificationReason reason) {
                Log.w(TAG, "Interstitial : Error. Reason is " + reason);
                if (mediationInterstitialListener != null) {
                    mediationInterstitialListener.onAdFailedToLoad(IMobileAdapter.this,
                            AdapterHelper.convertToAdMobErrorCode(reason));
                }
            }
        });

        // Start getting ads.
        if (ImobileSdkAd.isShowAd(interstitialSpotId)) {
            mediationInterstitialListener.onAdLoaded(IMobileAdapter.this);
        } else {
            ImobileSdkAd.start(interstitialSpotId);
        }
    }

    @Override
    public void showInterstitial() {
        // Show ad.
        if (interstitialActivity != null && interstitialActivity.hasWindowFocus()
                && interstitialSpotId != null) {
            ImobileSdkAd.showAdforce(interstitialActivity, interstitialSpotId);
        }
    }

    // endregion

    // region - Methods of life cycle.

    @Override
    public void onDestroy() {
        // Release objects.
        mediationBannerListener = null;
        bannerView = null;
        mediationInterstitialListener = null;
        interstitialActivity = null;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    // endregion
}
