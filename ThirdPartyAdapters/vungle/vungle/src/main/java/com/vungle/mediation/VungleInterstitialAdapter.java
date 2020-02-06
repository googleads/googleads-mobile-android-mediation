package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.warren.AdConfig;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleNativeAd;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Keep;

import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using
 * Google Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter implements MediationInterstitialAdapter,
        MediationBannerAdapter {

    private final String TAG = VungleInterstitialAdapter.class.getSimpleName();
    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private String mPlacementForPlay;

    //banner/MREC
    private volatile RelativeLayout adLayout;
    private VungleBanner vungleBannerAd;
    private VungleNativeAd vungleNativeAd;
    private AtomicBoolean pendingRequestBanner = new AtomicBoolean(false);
    private MediationBannerListener mMediationBannerListener;
    private boolean paused;

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {

        AdapterParametersParser.Config config;
        try {
            config = AdapterParametersParser.parse(mediationExtras, serverParameters);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to load ad from Vungle", e);
            if (mediationInterstitialListener != null) {
                mediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mMediationInterstitialListener = mediationInterstitialListener;
        mVungleManager = VungleManager.getInstance();

        mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(mPlacementForPlay)) {
            Log.w(TAG, "Failed to load ad from Vungle: Missing or Invalid Placement ID");
            mMediationInterstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
        VungleInitializer.getInstance().initialize(config.getAppId(),
                context.getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        loadAd();
                    }

                    @Override
                    public void onInitializeError(String errorMessage) {
                        Log.w(TAG, "Failed to load ad from Vungle: " + errorMessage);
                        if (mMediationInterstitialListener != null) {
                            mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }
                }
        );
    }

    private void loadAd() {
        if (mVungleManager.isAdPlayable(mPlacementForPlay)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
            mVungleManager.loadAd(mPlacementForPlay, new VungleListener() {
                @Override
                void onAdAvailable() {
                    mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
                }

                @Override
                void onAdFailedToLoad() {
                    mMediationInterstitialListener.onAdFailedToLoad(
                            VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                }
            });
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }


    @Override
    public void showInterstitial() {
        if (mVungleManager != null)
            mVungleManager.playAd(mPlacementForPlay, mAdConfig, new VungleListener() {
                @Override
                void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
                    if (mMediationInterstitialListener != null) {
                        if (wasCallToActionClicked) {
                            // Only the call to action button is clickable for Vungle ads. So the
                            // wasCallToActionClicked can be used for tracking clicks.
                            mMediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
                        }
                        mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
                    }
                }

                @Override
                void onAdStart(String placement) {
                    if (mMediationInterstitialListener != null) {
                        mMediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
                    }
                }

                @Override
                void onAdFail(String placement) {
                    if (mMediationInterstitialListener != null) {
                        mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
                    }
                }
            });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        paused = true;
        pendingRequestBanner.set(false);
        if (vungleBannerAd != null) {
            vungleBannerAd.destroyAd();
            mVungleManager.cleanUpBanner(mPlacementForPlay, vungleBannerAd);
            vungleBannerAd = null;
        } else if (vungleNativeAd != null) {
            vungleNativeAd.finishDisplayingAd();
            mVungleManager.cleanUpBanner(mPlacementForPlay, vungleNativeAd);
            vungleNativeAd = null;
        }
        adLayout = null;
    }

    //banner
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        paused = true;
        updateVisibility();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        paused = false;
        updateVisibility();
    }

    @Override
    public void requestBannerAd(Context context,
                                final MediationBannerListener mediationBannerListener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        Log.d(TAG, "requestBannerAd");
        pendingRequestBanner.set(true);
        mMediationBannerListener = mediationBannerListener;
        AdapterParametersParser.Config config;
        try {
            config = AdapterParametersParser.parse(mediationExtras, serverParameters);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to load ad from Vungle", e);
            if (mediationBannerListener != null) {
                mediationBannerListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mVungleManager = VungleManager.getInstance();

        mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);

        if (TextUtils.isEmpty(mPlacementForPlay)) {
            String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
            Log.w(TAG, message);
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
        if (VungleExtrasBuilder.isStartMutedNotConfigured(mediationExtras)) {
            mAdConfig.setMuted(true); // start muted by default
        }
        if (!hasBannerSizeAd(context, adSize)) {
            String message = "Failed to load ad from Vungle: Invalid banner size.";
            Log.w(TAG, message);
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        adLayout = new RelativeLayout(context);
        // Make adLayout wrapper match the requested ad size, as Vungle's ad uses MATCH_PARENT for
        // its dimensions.
        RelativeLayout.LayoutParams adViewLayoutParams = new RelativeLayout.LayoutParams(
                adSize.getWidthInPixels(context), adSize.getHeightInPixels(context));
        adLayout.setLayoutParams(adViewLayoutParams);
        VungleInitializer.getInstance().initialize(config.getAppId(),
                context.getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        loadBanner();
                    }

                    @Override
                    public void onInitializeError(String errorMessage) {
                        Log.w(TAG, "Failed to load ad from Vungle: " + errorMessage);
                        if (mMediationBannerListener != null) {
                            mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }
                });
    }

    private void loadBanner() {
        if (mVungleManager.isAdPlayable(mPlacementForPlay, mAdConfig.getAdSize())) {
            createBanner();
        } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
            mVungleManager.loadAd(mPlacementForPlay, mAdConfig.getAdSize(), new VungleListener() {
                @Override
                void onAdAvailable() {
                    createBanner();
                }

                @Override
                void onAdFailedToLoad() {
                    if (pendingRequestBanner.get() && mMediationBannerListener != null) {
                        mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                    }
                }
            });
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            Log.w(TAG, "Invalid Placement: " + mPlacementForPlay);
            if (mMediationBannerListener != null) {
                mMediationBannerListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    private VungleListener mVunglePlayListener = new VungleListener() {
        @Override
        void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
            if (mMediationBannerListener != null) {
                if (wasCallToActionClicked) {
                    // Only the call to action button is clickable for Vungle ads. So the
                    // wasCallToActionClicked can be used for tracking clicks.
                    mMediationBannerListener.onAdClicked(VungleInterstitialAdapter.this);
                    mMediationBannerListener.onAdOpened(VungleInterstitialAdapter.this);
                    mMediationBannerListener.onAdClosed(VungleInterstitialAdapter.this);
                }
            }
        }

        @Override
        void onAdStart(String placement) {
            //let's load it again to mimic auto-cache, don't care about errors
            mVungleManager.loadAd(placement, mAdConfig.getAdSize(), null);
        }

        @Override
        void onAdFail(String placement) {
            Log.w(TAG, "Ad playback error Placement: " + placement);
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }
    };

    private void createBanner() {
        if (mVungleManager == null || !pendingRequestBanner.get())
            return;

        mVungleManager.cleanUpBanner(mPlacementForPlay);

        if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
            vungleBannerAd = mVungleManager.getVungleBanner(mPlacementForPlay, mAdConfig.getAdSize(), mVunglePlayListener);
            if (vungleBannerAd != null) {
                updateVisibility();
                RelativeLayout.LayoutParams adViewParams = (RelativeLayout.LayoutParams) vungleBannerAd.getLayoutParams();
                if (adViewParams == null) {
                    adViewParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                }
                adViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                adViewParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                vungleBannerAd.setLayoutParams(adViewParams);
                adLayout.addView(vungleBannerAd);
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
                }
            } else {
                //missing resources
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        } else {
            vungleNativeAd = mVungleManager.getVungleNativeAd(mPlacementForPlay, mAdConfig, mVunglePlayListener);
            View adView = vungleNativeAd != null ? vungleNativeAd.renderNativeView() : null;
            if (adView != null) {
                updateVisibility();
                adLayout.addView(adView);
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
                }
            } else {
                //missing resources
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        }
    }

    private void updateVisibility() {
        if (vungleBannerAd != null) {
            vungleBannerAd.setAdVisibility(!paused);
        } else if (vungleNativeAd != null) {
            vungleNativeAd.setAdVisibility(!paused);
        }
    }

    @Override
    public View getBannerView() {
        Log.d(TAG, "getBannerView");
        return adLayout;
    }

    private boolean hasBannerSizeAd(Context context, AdSize adSize) {
        AdConfig.AdSize adSizeType = null;

        int width = adSize.getWidth();
        if (width < 0) {
            float density = context.getResources().getDisplayMetrics().density;
            width = Math.round(adSize.getWidthInPixels(context) / density);
        }

        ArrayList<AdSize> potentials = new ArrayList<>(3);
        potentials.add(0, new AdSize(width, 50));
        potentials.add(1, new AdSize(width, 90));
        potentials.add(2, new AdSize(width, 250));
        Log.i(TAG, "Potential ad sizes: " + potentials.toString());
        AdSize closestSize = findClosestSize(context, adSize, potentials);
        if (closestSize == null) {
            Log.i(TAG, "Not found closest ad size: " + adSize);
            return false;
        }
        Log.i(TAG, "Found closest ad size: " + closestSize.toString());

        int adHeight = closestSize.getHeight();

        if (adHeight == VUNGLE_MREC.getHeight()) {
            adSizeType = VUNGLE_MREC;
        } else if (adHeight == BANNER.getHeight()) {
            adSizeType = BANNER;
        } else if (adHeight == BANNER_LEADERBOARD.getHeight()) {
            adSizeType = BANNER_LEADERBOARD;
        }

        mAdConfig.setAdSize(adSizeType);

        return adSizeType != null;
    }

    // Start of helper code to remove when available in SDK
    /**
     * Find the closest supported AdSize from the list of potentials to the provided size. Returns
     * null if none are within given threshold size range.
     */
    public static AdSize findClosestSize(
            Context context, AdSize original, ArrayList<AdSize> potentials) {
        if (potentials == null || original == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int actualWidth = Math.round(original.getWidthInPixels(context) / density);
        int actualHeight = Math.round(original.getHeightInPixels(context) / density);
        original = new AdSize(actualWidth, actualHeight);

        AdSize largestPotential = null;
        for (AdSize potential : potentials) {
            if (isSizeInRange(original, potential)) {
                if (largestPotential == null) {
                    largestPotential = potential;
                } else {
                    largestPotential = getLargerByArea(largestPotential, potential);
                }
            }
        }
        return largestPotential;
    }

    private static boolean isSizeInRange(AdSize original, AdSize potential) {
        if (potential == null) {
            return false;
        }
        double minWidthRatio = 0.5;
        double minHeightRatio = 0.7;

        int originalWidth = original.getWidth();
        int potentialWidth = potential.getWidth();
        int originalHeight = original.getHeight();
        int potentialHeight = potential.getHeight();

        if (originalWidth * minWidthRatio > potentialWidth || originalWidth < potentialWidth) {
            return false;
        }

        if (originalHeight * minHeightRatio > potentialHeight || originalHeight < potentialHeight) {
            return false;
        }
        return true;
    }

    private static AdSize getLargerByArea(AdSize size1, AdSize size2) {
        int area1 = size1.getWidth() * size1.getHeight();
        int area2 = size2.getWidth() * size2.getHeight();
        return area1 > area2 ? size1 : size2;
    }
    // End code to remove when available in SDK

}
