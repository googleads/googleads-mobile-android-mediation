package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.warren.AdConfig;
import com.vungle.warren.VungleNativeAd;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.vungle.warren.AdConfig.AdSize.VUNGLE_DEFAULT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using
 * Google Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter implements MediationInterstitialAdapter, MediationBannerAdapter {

    private final String TAG = VungleInterstitialAdapter.class.getSimpleName() + "    " + System.identityHashCode(this);
    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private static final String INTERSTITIAL = "interstitial";
    private static int sCounter = 0;
    private String mAdapterId;
    private String mPlacementForPlay;

    //banner
    private static final String BANNER = "banner";
    private Context mContext;
    private volatile RelativeLayout adLayout;
    private VungleNativeAd vungleNativeAd;
    private AtomicBoolean pendingRequestBanner = new AtomicBoolean(false);
    private MediationBannerListener mMediationBannerListener;
    private AdConfig adConfig = new AdConfig();
    private boolean paused;
    private boolean visible;

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
            Log.w(VungleMediationAdapter.TAG, "Failed to load ad from Vungle", e);
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
            Log.w(VungleMediationAdapter.TAG,
                    "Failed to load ad from Vungle: Missing or Invalid Placement ID");
            mMediationInterstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
        mAdapterId = INTERSTITIAL + String.valueOf(sCounter);
        sCounter++;
        VungleInitializer.getInstance().initialize(config.getAppId(),
                context.getApplicationContext(), mAdapterId, new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        loadAd();
                    }

                    @Override
                    public void onInitializeError(String errorMessage) {
                        Log.w(VungleMediationAdapter.TAG, "Failed to load ad from Vungle: " + errorMessage);
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
            mVungleManager.loadAd(mAdapterId, mPlacementForPlay, new VungleListener() {
                @Override
                void onAdAvailable() {
                    mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
                }

                @Override
                void onAdFailedToLoad() {
                    mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
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
            mVungleManager.playAd(mAdapterId, mPlacementForPlay, mAdConfig, new VungleListener() {
                @Override
                void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
                    if (mMediationInterstitialListener != null) {
                        if (wasCallToActionClicked) {
                            // Only the call to action button is clickable for Vungle ads. So the
                            // wasCallToActionClicked can be used for tracking clicks.
                            mMediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
                            mMediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
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
        if (vungleNativeAd != null) {
            vungleNativeAd.finishDisplayingAd();
            mVungleManager.removeActiveBanner(mPlacementForPlay, mAdapterId);
        }
        vungleNativeAd = null;
        adLayout = null;

        if (mVungleManager != null) {
            mVungleManager.removeListeners(mAdapterId);
        }
    }

    //banner
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        paused = false;
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
        mContext = context;
        pendingRequestBanner.set(true);
        mMediationBannerListener = mediationBannerListener;
        AdapterParametersParser.Config config;
        try {
            config = AdapterParametersParser.parse(mediationExtras, serverParameters);
        } catch (IllegalArgumentException e) {
            Log.w(VungleMediationAdapter.TAG, "Failed to load ad from Vungle", e);
            if (mediationBannerListener != null) {
                mediationBannerListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mMediationBannerListener = mediationBannerListener;
        mVungleManager = VungleManager.getInstance();

        mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
        if (mPlacementForPlay != null && !mPlacementForPlay.isEmpty() && hasBannerSizeAd(adSize)) {
            mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
            mAdapterId = BANNER + String.valueOf(sCounter);
            sCounter++;
            //workaround for missing onPause/onResume/onDestroy
            adLayout = new RelativeLayout(mContext) {
                @Override
                protected void onWindowVisibilityChanged(int visibility) {
                    super.onWindowVisibilityChanged(visibility);
                    visible = (visibility == VISIBLE);
                    updateVisibility();
                }
            };
            VungleInitializer.getInstance().initialize(config.getAppId(),
                    context.getApplicationContext(), mAdapterId, new VungleInitializer.VungleInitializationListener() {
                        @Override
                        public void onInitializeSuccess() {
                            loadAdForBanner();
                        }

                        @Override
                        public void onInitializeError(String errorMessage) {
                            Log.w(VungleMediationAdapter.TAG, "Failed to load ad from Vungle: " + errorMessage);
                            if (mMediationBannerListener != null) {
                                mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
                            }
                        }
                    });
        } else {
            Log.w(VungleMediationAdapter.TAG,
                    "Failed to load ad from Vungle: Missing or Invalid Placement ID");
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    private void loadAdForBanner() {
        if (mVungleManager.isAdPlayable(mPlacementForPlay)) {
            if (mMediationBannerListener != null) {
                createBanner();
                mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
            mVungleManager.loadAd(mAdapterId, mPlacementForPlay, new VungleListener() {
                @Override
                void onAdAvailable() {
                    if (mMediationBannerListener != null) {
                        createBanner();
                        mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
                    }
                }

                @Override
                void onAdFailedToLoad() {
                    if (pendingRequestBanner.get() && mMediationBannerListener != null) {
                        mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                }
            });
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            if (mMediationBannerListener != null) {
                mMediationBannerListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    private void createBanner() {
        if (mVungleManager == null || pendingRequestBanner.compareAndSet(false, false))
            return;

        mVungleManager.cleanUpBanner(mPlacementForPlay);
        vungleNativeAd = mVungleManager.getVungleNativeAd(mAdapterId, mPlacementForPlay, adConfig, new VungleListener() {
            @Override
            void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
                if (mMediationBannerListener != null) {
                    if (wasCallToActionClicked) {
                        // Only the call to action button is clickable for Vungle ads. So the
                        // wasCallToActionClicked can be used for tracking clicks.
                        mMediationBannerListener.onAdClicked(VungleInterstitialAdapter.this);
                        mMediationBannerListener.onAdOpened(VungleInterstitialAdapter.this);
                        mMediationBannerListener.onAdLeftApplication(VungleInterstitialAdapter.this);
                        mMediationBannerListener.onAdClosed(VungleInterstitialAdapter.this);
                    }
                }
            }

            @Override
            void onAdStart(String placement) {
                //let's load it again to mimic auto-cache, don't care about errors
                mVungleManager.loadAd(mAdapterId, placement, null);
            }

            @Override
            void onAdFail(String placement) {
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        });
        View adView = vungleNativeAd == null ? null : vungleNativeAd.renderNativeView();
        if (adView != null) {
            updateVisibility();
            adLayout.addView(adView);
        } else {
            //missing resources
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }
    }

    private void updateVisibility() {
        if (vungleNativeAd != null) {
            vungleNativeAd.setAdVisibility(!paused && visible);
        }
    }

    @Override
    public View getBannerView() {
        Log.d(TAG, "getBannerView");
        return adLayout;
    }

    private boolean hasBannerSizeAd(AdSize adSize) {
        if (300 == adSize.getWidth() && 250 == adSize.getHeight()) {
            adConfig.setAdSize(VUNGLE_MREC);
            return true;
        }
        adConfig.setAdSize(VUNGLE_DEFAULT);
        return false;
    }

}
