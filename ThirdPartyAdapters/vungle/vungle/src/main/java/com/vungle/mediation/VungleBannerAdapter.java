package com.vungle.mediation;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdRequest;
import com.vungle.warren.AdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleNativeAd;
import com.vungle.warren.error.VungleException;

import java.util.concurrent.atomic.AtomicBoolean;

class VungleBannerAdapter {
    private static final String TAG = VungleBannerAdapter.class.getSimpleName();

    @NonNull
    private String mPlacementId;
    @Nullable
    private String mUniquePubRequestId;
    @Nullable
    private VungleListener mVungleListener;
    @NonNull
    private RelativeLayout mAdLayout;
    @NonNull
    private AdConfig mAdConfig;

    private AtomicBoolean mIsLoading = new AtomicBoolean(true);
    private boolean mPendingRequestBanner = false;
    private boolean mVisibility = true;
    @Nullable
    private VungleBanner mVungleBannerAd;
    @Nullable
    private VungleNativeAd mVungleNativeAd;

    @NonNull
    private VungleManager mVungleManager;

    VungleBannerAdapter(@NonNull String placementId, @Nullable String uniquePubRequestId) {
        mVungleManager = VungleManager.getInstance();
        this.mPlacementId = placementId;
        this.mUniquePubRequestId = uniquePubRequestId;
    }

    @NonNull
    String getPlacementId() {
        return mPlacementId;
    }

    @Nullable
    String getUniquePubRequestId() {
        return mUniquePubRequestId;
    }

    boolean isLoading() {
        return mIsLoading.get();
    }

    void setVungleListener(@Nullable VungleListener vungleListener) {
        this.mVungleListener = vungleListener;
    }

    void setAdLayout(@NonNull RelativeLayout adLayout) {
        this.mAdLayout = adLayout;
    }

    void setAdConfig(@NonNull AdConfig adConfig) {
        this.mAdConfig = adConfig;
    }

    void requestBannerAd(@NonNull Context context, @NonNull String appId) {
        Log.d(TAG, "requestBannerAd: " + this);
        mIsLoading.set(true);
        mPendingRequestBanner = true;
        VungleInitializer.getInstance().initialize(appId, context.getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        loadBanner();
                    }

                    @Override
                    public void onInitializeError(String errorMessage) {
                        Log.d(TAG, "SDK init failed:" + VungleBannerAdapter.this);
                        mIsLoading.set(false);
                        mVungleManager.removeActiveBannerAd(mPlacementId);
                        if (mVungleListener != null) {
                            mVungleListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }
                });
    }

    void destroy(@Nullable View adView) {
        Log.d(TAG, "Vungle banner adapter try to destroy:" + this);
        if (adView == mAdLayout) {
            Log.d(TAG, "Vungle banner adapter destroy:" + this);
            mIsLoading.set(false);
            mVisibility = false;
            mPendingRequestBanner = false;
            mVungleManager.removeActiveBannerAd(mPlacementId);
            cleanUp();
        }
    }

    void cleanUp() {
        Log.d(TAG, "Vungle banner adapter try to cleanUp:" + this);
        if (mVungleBannerAd != null) {
            Log.d(TAG, "Vungle banner adapter cleanUp: destroyAd # " + mVungleBannerAd.hashCode());
            mVungleBannerAd.destroyAd();
            mVungleBannerAd = null;
        }
        if (mVungleNativeAd != null) {
            Log.d(TAG, "Vungle banner adapter cleanUp: finishDisplayingAd # " + mVungleNativeAd.hashCode());
            mVungleNativeAd.finishDisplayingAd();
            mVungleNativeAd = null;
        }
    }

    void preCache() {
        if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
            Banners.loadBanner(mPlacementId, mAdConfig.getAdSize(), null);
        } else {
            Vungle.loadAd(mPlacementId, null);
        }
    }

    void updateVisibility(boolean visible) {
        this.mVisibility = visible;
        if (mVungleBannerAd != null) {
            mVungleBannerAd.setAdVisibility(visible);
        } else if (mVungleNativeAd != null) {
            mVungleNativeAd.setAdVisibility(visible);
        }
    }

    private LoadAdCallback mAdLoadCallback = new LoadAdCallback() {
        @Override
        public void onAdLoad(String id) {
            createBanner();
        }

        @Override
        public void onError(String id, VungleException exception) {
            Log.d(TAG, "Ad load failed:" + VungleBannerAdapter.this);
            mIsLoading.set(false);
            mVungleManager.removeActiveBannerAd(mPlacementId);
            if (mPendingRequestBanner && mVungleListener != null) {
                mVungleListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
            }
        }
    };

    private PlayAdCallback mAdPlayCallback = new PlayAdCallback() {
        @Override
        public void onAdStart(String placementId) {
            if (mPendingRequestBanner && mVungleListener != null) {
                mVungleListener.onAdStart(placementId);
            }
        }

        @Override
        public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) {
            if (mPendingRequestBanner && mVungleListener != null) {
                mVungleListener.onAdEnd(placementId, completed, isCTAClicked);
            }
        }

        @Override
        public void onError(String placementId, VungleException exception) {
            Log.d(TAG, "Ad play failed:" + VungleBannerAdapter.this);
            mIsLoading.set(false);
            mVungleManager.removeActiveBannerAd(mPlacementId);
            if (mPendingRequestBanner && mVungleListener != null) {
                mVungleListener.onAdFail(placementId);
            }
        }
    };

    private void loadBanner() {
        Log.d(TAG, "loadBanner:" + this);
        if (mVungleManager.isAdPlayable(mPlacementId, mAdConfig.getAdSize())) {
            createBanner();
        } else if (mVungleManager.isValidPlacement(mPlacementId)) {
            if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
                Banners.loadBanner(mPlacementId, mAdConfig.getAdSize(), mAdLoadCallback);
            } else {
                Vungle.loadAd(mPlacementId, mAdLoadCallback);
            }
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            mIsLoading.set(false);
            mVungleManager.removeActiveBannerAd(mPlacementId);
            if (mVungleListener != null) {
                mVungleListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    private void createBanner() {
        Log.d(TAG, "create banner:" + this);
        if (!mPendingRequestBanner) {
            return;
        }
        cleanUp();
        mIsLoading.set(false);
        RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
            mVungleBannerAd = Banners.getBanner(mPlacementId, mAdConfig.getAdSize(), mAdPlayCallback);
            if (mVungleBannerAd != null) {
                Log.d(TAG, "display banner:" + mVungleBannerAd.hashCode() + this);
                mVungleManager.restoreActiveBannerAd(mPlacementId, this);
                updateVisibility(mVisibility);
                mVungleBannerAd.setLayoutParams(adParams);
                mAdLayout.addView(mVungleBannerAd);
                if (mVungleListener != null) {
                    mVungleListener.onAdAvailable();
                }
            } else {
                Log.d(TAG, "Can't display banner:" + this);
                //missing resources
                if (mVungleListener != null) {
                    mVungleListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        } else {
            View adView = null;
            mVungleNativeAd = Vungle.getNativeAd(mPlacementId, mAdConfig, mAdPlayCallback);
            if (mVungleNativeAd != null) {
                adView = mVungleNativeAd.renderNativeView();
                mVungleManager.restoreActiveBannerAd(mPlacementId, this);
            }
            if (adView != null) {
                Log.d(TAG, "display MREC:" + mVungleNativeAd.hashCode() + this);
                updateVisibility(mVisibility);
                adView.setLayoutParams(adParams);
                mAdLayout.addView(adView);
                if (mVungleListener != null) {
                    mVungleListener.onAdAvailable();
                }
            } else {
                Log.d(TAG, "Can't display MREC:" + this);
                //missing resources
                if (mVungleListener != null) {
                    mVungleListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return " [placementId=" + mPlacementId + " # uniqueRequestId=" + mUniquePubRequestId + "] hashcode: " + hashCode();
    }
}
