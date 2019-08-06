package com.applovin.mediation.rtb;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

/**
 * Created by Thomas So on July 17 2018
 */
public final class AppLovinRtbBannerRenderer
        implements MediationBannerAd, AppLovinAdLoadListener, AppLovinAdDisplayListener,
        AppLovinAdClickListener, AppLovinAdViewEventListener {

    private static final String TAG = AppLovinRtbBannerRenderer.class.getSimpleName();

    /**
     * Data used to render an RTB banner ad.
     */
    private final MediationBannerAdConfiguration adConfiguration;

    /**
     * Callback object to notify the Google Mobile Ads SDK if ad rendering succeeded or failed.
     */
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;

    /**
     * Listener object to notify the Google Mobile Ads SDK of banner presentation events.
     */
    private MediationBannerAdCallback mBannerAdCallback;

    private final AppLovinSdk sdk;
    private final AppLovinAdSize adSize;
    private AppLovinAdView adView;

  public AppLovinRtbBannerRenderer(MediationBannerAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;

    // Convert requested size to AppLovin Ad Size.
    this.adSize = AppLovinUtils.appLovinAdSizeFromAdMobAdSize(
        adConfiguration.getContext(), adConfiguration.getAdSize());
        this.sdk = AppLovinUtils.retrieveSdk(adConfiguration.getServerParameters(),
                adConfiguration.getContext());
    }

    public void loadAd() {
        if (adSize != null) {
            // Create adview object
            adView = new AppLovinAdView(sdk, adSize, adConfiguration.getContext());
            adView.setAdDisplayListener(this);
            adView.setAdClickListener(this);
            adView.setAdViewEventListener(this);

            // Load ad!
            sdk.getAdService().loadNextAdForAdToken(adConfiguration.getBidResponse(), this);
        } else {
            callback.onFailure("Failed to request banner with unsupported size");
        }
    }

    @NonNull
    @Override
    public View getView() {
        return adView;
    }

    //region AppLovin Listeners
    @Override
    public void adReceived(AppLovinAd ad) {
        Log.d(TAG, "Banner did load ad: " + ad.getAdIdNumber());

        mBannerAdCallback = callback.onSuccess(AppLovinRtbBannerRenderer.this);
        adView.renderAd(ad);
    }

    @Override
    public void failedToReceiveAd(int code) {
        Log.e(TAG, "Failed to load banner ad with error: " + code);

        int admobErrorCode = AppLovinUtils.toAdMobErrorCode(code);
        callback.onFailure(Integer.toString(admobErrorCode));
    }

    @Override
    public void adDisplayed(AppLovinAd ad) {
        Log.d(TAG, "Banner displayed");
        mBannerAdCallback.reportAdImpression();
        mBannerAdCallback.onAdOpened();
    }

    @Override
    public void adHidden(AppLovinAd ad) {
        Log.d(TAG, "Banner hidden");
    }

    @Override
    public void adClicked(AppLovinAd ad) {
        Log.d(TAG, "Banner clicked");
        mBannerAdCallback.reportAdClicked();
    }

    @Override
    public void adOpenedFullscreen(AppLovinAd ad, AppLovinAdView adView) {
        Log.d(TAG, "Banner opened fullscreen");
        mBannerAdCallback.onAdOpened();
    }

    @Override
    public void adClosedFullscreen(AppLovinAd ad, AppLovinAdView adView) {
        Log.d(TAG, "Banner closed fullscreen");
        mBannerAdCallback.onAdClosed();
    }

    @Override
    public void adLeftApplication(AppLovinAd ad, AppLovinAdView adView) {
        Log.d(TAG, "Banner left application");
        mBannerAdCallback.onAdLeftApplication();
    }

    @Override
    public void adFailedToDisplay(AppLovinAd ad, AppLovinAdView adView, AppLovinAdViewDisplayErrorCode code) {
        Log.e(TAG, "Banner failed to display: " + code);
    }
    //endregion

}
