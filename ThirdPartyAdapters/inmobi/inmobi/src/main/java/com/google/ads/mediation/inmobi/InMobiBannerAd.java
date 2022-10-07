package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_INMOBI_NOT_INITIALIZED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.INMOBI_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.inmobi.InMobiAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.exceptions.SdkNotInitializedException;
import com.inmobi.ads.listeners.BannerAdEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InMobiBannerAd implements MediationBannerAd {

    private final MediationBannerAdConfiguration mMediationBannerAdConfiguration;
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mMediationAdLoadCallback;
    private FrameLayout mWrappedAdView;
    private MediationBannerAdCallback mMediationBannerAdCallback;

    private static Boolean sDisableHardwareFlag = false;

    public InMobiBannerAd (MediationBannerAdConfiguration mMediationBannerAdConfiguration,
                           MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mMediationAdLoadCallback){
        this.mMediationBannerAdConfiguration = mMediationBannerAdConfiguration;
        this.mMediationAdLoadCallback = mMediationAdLoadCallback;
    }

    /**
     * Ad Configuration getAdSize null check case todo
     */
    public void load() {
        final Context context = mMediationBannerAdConfiguration.getContext();
        Bundle serverParameters = mMediationBannerAdConfiguration.getServerParameters();

        final AdSize inMobiMediationAdSize = getSupportedAdSize(context, mMediationBannerAdConfiguration.getAdSize());
        if (inMobiMediationAdSize == null) {
            String errorMessage = String
                    .format("InMobi SDK supported banner sizes are not valid for the requested size: %s",
                            mMediationBannerAdConfiguration.getAdSize().toString());
            AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH, errorMessage, ERROR_DOMAIN);
            Log.w(TAG, errorMessage);
            mMediationAdLoadCallback.onFailure(error);
            return;
        }

        String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
        if (TextUtils.isEmpty(accountID)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Account ID.",
                    ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationAdLoadCallback.onFailure(error);
            return;
        }

        final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
        InMobiInitializer.getInstance().init(context, accountID, new InMobiInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                createAndLoadBannerAd(context, placementId);
            }

            @Override
            public void onInitializeError(@NonNull AdError error) {
                Log.w(TAG, error.getMessage());
                if (mMediationAdLoadCallback != null) {
                    mMediationAdLoadCallback.onFailure(error);
                }
            }
        });
    }

    private void createAndLoadBannerAd(Context context, long placementId){

        if (placementId <= 0L) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or Invalid Placement ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationAdLoadCallback.onFailure(error);
            return;
        }

        FrameLayout.LayoutParams wrappedLayoutParams = new FrameLayout.LayoutParams(
                mMediationBannerAdConfiguration.getAdSize().getWidthInPixels(context),
                mMediationBannerAdConfiguration.getAdSize().getHeightInPixels(context));
        InMobiBanner adView;
        try {
            adView = new InMobiBanner(context, placementId);
        } catch (SdkNotInitializedException exception) {
            AdError error = new AdError(ERROR_INMOBI_NOT_INITIALIZED, exception.getLocalizedMessage(),
                    ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationAdLoadCallback.onFailure(error);
            return;
        }

        // Turn off automatic refresh.
        adView.setEnableAutoRefresh(false);
        // Turn off the animation.
        adView.setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF);

        if (mMediationBannerAdConfiguration.getMediationExtras().keySet() != null) {
            adView.setKeywords(TextUtils.join(", ", mMediationBannerAdConfiguration.getMediationExtras().keySet()));
        }

        //Update Age Restricted User
        InMobiAdapterUtils.updateAgeRestrictedUser(mMediationBannerAdConfiguration);

        // Create request parameters.
        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(mMediationBannerAdConfiguration);
        adView.setExtras(paramMap);

        Bundle mediationExtras = mMediationBannerAdConfiguration.getMediationExtras();

        adView.setListener(new BannerAdEventListener() {
            @Override
            public void onUserLeftApplication(@NonNull InMobiBanner inMobiBanner) {
                Log.d(TAG, "InMobi banner left application.");
                mMediationBannerAdCallback.onAdLeftApplication();
            }

            @Override
            public void onRewardsUnlocked(@NonNull InMobiBanner inMobiBanner,
                                          Map<Object, Object> rewards) {
                // No-op.
            }

            @Override
            public void onAdLoadSucceeded(@NonNull InMobiBanner inMobiBanner,
                                          @NonNull AdMetaInfo adMetaInfo) {
                Log.d(TAG, "InMobi banner has been loaded.");
                if (mMediationAdLoadCallback != null) {
                    mMediationBannerAdCallback =
                            mMediationAdLoadCallback.onSuccess(InMobiBannerAd.this);
                }
            }

            @Override
            public void onAdLoadFailed(@NonNull InMobiBanner inMobiBanner,
                                       @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
                AdError error = new AdError(
                        InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
                        inMobiAdRequestStatus.getMessage(), INMOBI_SDK_ERROR_DOMAIN);
                Log.w(TAG, error.getMessage());
                if (mMediationAdLoadCallback != null) {
                    mMediationAdLoadCallback.onFailure(error);
                }
            }

            @Override
            public void onAdDisplayed(@NonNull InMobiBanner inMobiBanner) {
                Log.d(TAG, "InMobi banner opened a full screen view.");
                if (mMediationBannerAdCallback != null) {
                    mMediationBannerAdCallback.onAdOpened();
                }
            }

            @Override
            public void onAdDismissed(@NonNull InMobiBanner inMobiBanner) {
                Log.d(TAG, "InMobi banner has been dismissed.");
                if (mMediationBannerAdCallback != null) {
                    mMediationBannerAdCallback.onAdClosed();
                }
            }

            @Override
            public void onAdClicked(@NonNull InMobiBanner inMobiBanner,
                                    Map<Object, Object> map) {
                Log.d(TAG, "InMobi banner has been clicked.");
                if (mMediationBannerAdCallback != null) {
                    mMediationBannerAdCallback.reportAdClicked();
                }
            }

            @Override
            public void onAdImpression(@NonNull InMobiBanner inMobiBanner) {
                Log.d(TAG, "InMobi banner has logged an impression.");
                if (mMediationBannerAdCallback != null) {
                    mMediationBannerAdCallback.reportAdImpression();
                }
            }
        });

        if (sDisableHardwareFlag) {
            adView.disableHardwareAcceleration();
        }

        /*
         * We wrap the ad View in a FrameLayout to ensure that it's the right
         * size. Without this the ad takes up the maximum width possible,
         * causing artifacts on high density screens (like the Galaxy Nexus) or
         * in landscape view. If the underlying library sets the appropriate
         * size instead of match_parent, this wrapper can be removed.
         */
        mWrappedAdView = new FrameLayout(context);
        mWrappedAdView.setLayoutParams(wrappedLayoutParams);
        adView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        mMediationBannerAdConfiguration.getAdSize().getWidthInPixels(context),
                        mMediationBannerAdConfiguration.getAdSize().getHeightInPixels(context)));
        mWrappedAdView.addView(adView);
        InMobiAdapterUtils.configureGlobalTargeting(mediationExtras);
        adView.load();
    }

    @NonNull
    @Override
    public View getView() {
        return mWrappedAdView;
    }

    @Nullable
    private AdSize getSupportedAdSize(@NonNull Context context, @NonNull AdSize adSize) {
    /*
        Supported Sizes (ref: https://www.inmobi.com/ui/pdfs/ad-specs.pdf)
        320x50,
        300x250,
        728x90.
     */

        ArrayList<AdSize> potentials = new ArrayList<>();
        potentials.add(new AdSize(320, 50));
        potentials.add(new AdSize(300, 250));
        potentials.add(new AdSize(728, 90));
        return MediationUtils.findClosestSize(context, adSize, potentials);
    }
}
