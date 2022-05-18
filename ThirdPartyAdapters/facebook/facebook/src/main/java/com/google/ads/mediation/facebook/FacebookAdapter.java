// Copyright 2014 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.facebook;

import static com.google.ads.mediation.facebook.FacebookExtras.NATIVE_BANNER;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.ads.Ad;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdExtendedListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.MediaViewListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeBannerAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.formats.UnifiedNativeAdAssetNames;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mediation adapter for Facebook Audience Network.
 */
@Keep
public final class FacebookAdapter extends FacebookMediationAdapter
    implements MediationBannerAdapter, MediationInterstitialAdapter, MediationNativeAdapter {

  public static final String KEY_ID = "id";
  public static final String KEY_SOCIAL_CONTEXT_ASSET = "social_context";

  private static final int MAX_STAR_RATING = 5;

  private MediationBannerListener mBannerListener;
  private MediationInterstitialListener mInterstitialListener;

  private MediationNativeListener mNativeListener;
  private AdView mAdView;
  private FrameLayout mWrappedAdView;
  private boolean isNativeBanner;

  /**
   * Facebook interstitial ad instance.
   */
  private InterstitialAd mInterstitialAd;

  /**
   * Flag to determine whether the interstitial ad has been presented.
   */
  private AtomicBoolean showInterstitialCalled = new AtomicBoolean();

  /**
   * Flag to determine whether the interstitial ad has been closed.
   */
  private AtomicBoolean didInterstitialAdClose = new AtomicBoolean();

  /**
   * Facebook native ad instance.
   */
  private NativeAd mNativeAd;

  /**
   * Facebook native banner ad instance.
   */
  private NativeBannerAd mNativeBannerAd;

  /**
   * Flag to determine whether or not an impression callback from Facebook SDK has already been sent
   * to the Google Mobile Ads SDK.
   */
  private boolean mIsImpressionRecorded;

  /**
   * A Facebook {@link MediaView} used to show native ad media content.
   */
  private MediaView mMediaView;

  //region MediationAdapter implementation.
  @Override
  public void onDestroy() {
    if (mAdView != null) {
      mAdView.destroy();
    }
    if (mInterstitialAd != null) {
      mInterstitialAd.destroy();
    }
    if (mNativeAd != null) {
      mNativeAd.unregisterView();
      mNativeAd.destroy();
    }
    if (mMediaView != null) {
      mMediaView.destroy();
    }
    if (mNativeBannerAd != null) {
      mNativeBannerAd.unregisterView();
      mNativeBannerAd.destroy();
    }
  }

  @Override
  public void onPause() {
    // Do nothing.
  }

  @Override
  public void onResume() {
    // Do nothing.
  }
  //endregion

  //region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(final Context context,
      MediationBannerListener listener,
      Bundle serverParameters,
      final AdSize adSize,
      final MediationAdRequest adRequest,
      Bundle mediationExtras) {

    Log.w(TAG, "Facebook waterfall mediation is deprecated and will be removed in a future "
        + "adapter version. Please update to serve bidding ads instead. See "
        + "https://fb.me/bNFn7qt6Z0sKtF for more information.");

    mBannerListener = listener;

    final String placementID = getPlacementID(serverParameters);
    if (TextUtils.isEmpty(placementID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mBannerListener.onAdFailedToLoad(this, error);
      return;
    }

    final com.facebook.ads.AdSize facebookAdSize = getAdSize(context, adSize);
    if (facebookAdSize == null) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "There is no matching Facebook ad size for Google ad size.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mBannerListener.onAdFailedToLoad(this, error);
      return;
    }

    FacebookInitializer.getInstance()
        .initialize(
            context,
            placementID,
            new FacebookInitializer.Listener() {
              @Override
              public void onInitializeSuccess() {
                mAdView = new AdView(context, placementID, facebookAdSize);
                buildAdRequest(adRequest);

                FrameLayout.LayoutParams adViewLayoutParams = new FrameLayout.LayoutParams(
                    adSize.getWidthInPixels(context), LayoutParams.WRAP_CONTENT);
                mWrappedAdView = new FrameLayout(context);
                mAdView.setLayoutParams(adViewLayoutParams);
                mWrappedAdView.addView(mAdView);
                mAdView.loadAd(
                    mAdView.buildLoadAdConfig()
                        .withAdListener(new BannerListener())
                        .build()
                );
              }

              @Override
              public void onInitializeError(AdError error) {
                if (mBannerListener != null) {
                  mBannerListener.onAdFailedToLoad(FacebookAdapter.this, error);
                }
              }
            });
  }

  @Override
  public View getBannerView() {
    return mWrappedAdView;
  }
  //endregion

  //region MediationInterstitialAdapter implementation.
  @Override
  public void requestInterstitialAd(final Context context,
      MediationInterstitialListener listener,
      Bundle serverParameters,
      final MediationAdRequest adRequest,
      Bundle mediationExtras) {

    Log.w(TAG, "Facebook waterfall mediation is deprecated and will be removed in a future "
        + "adapter version. Please update to serve bidding ads instead. See "
        + "https://fb.me/bNFn7qt6Z0sKtF for more information.");

    mInterstitialListener = listener;
    final String placementID = getPlacementID(serverParameters);

    if (TextUtils.isEmpty(placementID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty.", ERROR_DOMAIN);
      mInterstitialListener.onAdFailedToLoad(this, error);
      return;
    }

    FacebookInitializer.getInstance()
        .initialize(
            context,
            placementID,
            new FacebookInitializer.Listener() {
              @Override
              public void onInitializeSuccess() {
                createAndLoadInterstitial(context, placementID, adRequest);
              }

              @Override
              public void onInitializeError(AdError error) {
                if (mInterstitialListener != null) {
                  mInterstitialListener.onAdFailedToLoad(FacebookAdapter.this, error);
                }
              }
            });
  }

  @Override
  public void showInterstitial() {
    showInterstitialCalled.set(true);
    if (!mInterstitialAd.show()) {
      AdError error = new AdError(ERROR_FAILED_TO_PRESENT_AD, "Failed to present interstitial ad.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());

      if (mInterstitialListener != null) {
        mInterstitialListener.onAdOpened(FacebookAdapter.this);
        mInterstitialListener.onAdClosed(FacebookAdapter.this);
      }
    }
  }
  //endregion

  @Override
  public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    Log.w(TAG, "Facebook waterfall mediation is deprecated and will be removed in a future "
        + "adapter version. Please update to serve bidding ads instead. See "
        + "https://fb.me/bNFn7qt6Z0sKtF for more information.");

    super.loadRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback);
  }

  //region MediationNativeAdapter implementation.
  @Override
  public void requestNativeAd(final Context context,
      MediationNativeListener listener,
      Bundle serverParameters,
      final NativeMediationAdRequest mediationAdRequest,
      final Bundle mediationExtras) {

    Log.w(TAG, "Facebook waterfall mediation is deprecated and will be removed in a future "
        + "adapter version. Please update to serve bidding ads instead. See "
        + "https://fb.me/bNFn7qt6Z0sKtF for more information.");

    mNativeListener = listener;
    final String placementID = getPlacementID(serverParameters);

    if (TextUtils.isEmpty(placementID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mNativeListener.onAdFailedToLoad(this, error);
      return;
    }

    if (!mediationAdRequest.isUnifiedNativeAdRequested()) {
      AdError error = new AdError(ERROR_REQUIRES_UNIFIED_NATIVE_ADS,
          "Unified Native Ads should be requested.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mNativeListener.onAdFailedToLoad(this, error);
      return;
    }

    FacebookInitializer.getInstance()
        .initialize(
            context,
            placementID,
            new FacebookInitializer.Listener() {
              @Override
              public void onInitializeSuccess() {
                createAndLoadNativeAd(context, placementID, mediationAdRequest, mediationExtras);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.getMessage());
                if (mNativeListener != null) {
                  mNativeListener
                      .onAdFailedToLoad(FacebookAdapter.this, error);
                }
              }
            });
  }
  //endregion

  private void buildAdRequest(MediationAdRequest adRequest) {
    if (adRequest != null) {
      if (adRequest.taggedForChildDirectedTreatment() ==
          MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
        AdSettings.setMixedAudience(true);
      } else if (adRequest.taggedForChildDirectedTreatment() ==
          MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
        AdSettings.setMixedAudience(false);
      }
    }
  }
  //endregion

  //region Banner adapter utility classes.
  private class BannerListener implements AdListener {

    private BannerListener() {
    }

    @Override
    public void onAdClicked(Ad ad) {
      FacebookAdapter.this.mBannerListener.onAdClicked(FacebookAdapter.this);
      FacebookAdapter.this.mBannerListener.onAdOpened(FacebookAdapter.this);
      // The test Facebook ads leave the application when the ad is clicked. Assuming all
      // the ads do the same, sending onAdLeftApplication callback when the ad is clicked.
      FacebookAdapter.this.mBannerListener.onAdLeftApplication(FacebookAdapter.this);
    }

    @Override
    public void onLoggingImpression(Ad ad) {
      // Google Mobile Ads SDK does its own impression tracking for banner ads.
    }

    @Override
    public void onAdLoaded(Ad ad) {
      FacebookAdapter.this.mBannerListener.onAdLoaded(FacebookAdapter.this);
    }

    @Override
    public void onError(Ad ad, com.facebook.ads.AdError adError) {
      AdError error = FacebookMediationAdapter.getAdError(adError);
      Log.w(TAG, error.getMessage());
      FacebookAdapter.this.mBannerListener.onAdFailedToLoad(FacebookAdapter.this, error);
    }
  }
  //endregion

  //region Interstitial adapter utility classes.
  private void createAndLoadInterstitial(Context context,
      String placementID,
      MediationAdRequest adRequest) {
    mInterstitialAd = new InterstitialAd(context, placementID);
    buildAdRequest(adRequest);
    mInterstitialAd.loadAd(
        mInterstitialAd.buildLoadAdConfig()
            .withAdListener(new InterstitialListener())
            .build()
    );
  }

  private class InterstitialListener implements InterstitialAdExtendedListener {

    private InterstitialListener() {
    }

    @Override
    public void onAdClicked(Ad ad) {
      FacebookAdapter.this.mInterstitialListener.onAdClicked(FacebookAdapter.this);
      // The test Facebook ads leave the application when the ad is clicked. Assuming all
      // the ads do the same, sending onAdLeftApplication callback when the ad is clicked.
      FacebookAdapter.this.mInterstitialListener.onAdLeftApplication(FacebookAdapter.this);
    }

    @Override
    public void onLoggingImpression(Ad ad) {
      // Google Mobile Ads SDK does its own impression tracking for interstitial ads.
    }

    @Override
    public void onAdLoaded(Ad ad) {
      FacebookAdapter.this.mInterstitialListener.onAdLoaded(FacebookAdapter.this);
    }

    @Override
    public void onError(Ad ad, com.facebook.ads.AdError adError) {
      AdError error = FacebookMediationAdapter.getAdError(adError);
      Log.w(TAG, error.getMessage());
      if (showInterstitialCalled.get()) {
        FacebookAdapter.this.mInterstitialListener.onAdOpened(FacebookAdapter.this);
        FacebookAdapter.this.mInterstitialListener.onAdClosed(FacebookAdapter.this);
        return;
      }

      FacebookAdapter.this.mInterstitialListener.onAdFailedToLoad(
          FacebookAdapter.this, adError.getErrorCode());
    }

    @Override
    public void onInterstitialDismissed(Ad ad) {
      if (!didInterstitialAdClose.getAndSet(true)) {
        FacebookAdapter.this.mInterstitialListener.onAdClosed(FacebookAdapter.this);
      }
    }

    @Override
    public void onInterstitialDisplayed(Ad ad) {
      FacebookAdapter.this.mInterstitialListener.onAdOpened(FacebookAdapter.this);
    }

    @Override
    public void onInterstitialActivityDestroyed() {
      if (!didInterstitialAdClose.getAndSet(true)) {
        FacebookAdapter.this.mInterstitialListener.onAdClosed(FacebookAdapter.this);
      }
    }

    @Override
    public void onRewardedAdCompleted() {
      //no-op
    }

    @Override
    public void onRewardedAdServerSucceeded() {
      //no-op
    }

    @Override
    public void onRewardedAdServerFailed() {
      //no-op
    }
  }
  //endregion

  //region Native adapter utility methods and classes.
  private void createAndLoadNativeAd(Context context,
      String placementID,
      NativeMediationAdRequest adRequest,
      Bundle mediationExtras) {

    if (mediationExtras != null) {
      isNativeBanner = mediationExtras.getBoolean(NATIVE_BANNER);
    }

    if (isNativeBanner) {
      mNativeBannerAd = new NativeBannerAd(context, placementID);
      buildAdRequest(adRequest);
      mNativeBannerAd.loadAd(
          mNativeBannerAd
              .buildLoadAdConfig()
              .withAdListener(new NativeBannerListener(context, mNativeBannerAd))
              .withMediaCacheFlag(NativeAdBase.MediaCacheFlag.ALL)
              .withPreloadedIconView(
                  NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
                  NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE)
              .build());
    } else {
      mMediaView = new MediaView(context);
      mNativeAd = new NativeAd(context, placementID);
      buildAdRequest(adRequest);
      mNativeAd.loadAd(
          mNativeAd
              .buildLoadAdConfig()
              .withAdListener(new NativeListener(context, mNativeAd))
              .withMediaCacheFlag(NativeAdBase.MediaCacheFlag.ALL)
              .withPreloadedIconView(
                  NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
                  NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE)
              .build());
    }
  }

  private class NativeBannerListener implements AdListener, NativeAdListener {

    /**
     * Context required to create AdOptions View.
     */
    private WeakReference<Context> mContext;

    /**
     * Facebook native banner ad instance.
     */
    private NativeBannerAd mNativeBannerAd;

    private NativeBannerListener(Context context, NativeBannerAd nativeBannerAd) {
      mContext = new WeakReference<>(context);
      mNativeBannerAd = nativeBannerAd;
    }

    @Override
    public void onMediaDownloaded(Ad ad) {
      Log.d(TAG, "onMediaDownloaded");
    }

    @Override
    public void onError(Ad ad, com.facebook.ads.AdError adError) {
      AdError error = FacebookMediationAdapter.getAdError(adError);
      Log.w(TAG, error.getMessage());
      FacebookAdapter.this.mNativeListener.onAdFailedToLoad(FacebookAdapter.this, error);
    }

    @Override
    public void onAdLoaded(Ad ad) {
      if (ad != mNativeBannerAd) {

        AdError error = new AdError(ERROR_WRONG_NATIVE_TYPE, "Ad loaded is not a native banner ad.",
            ERROR_DOMAIN);

        FacebookAdapter.this.mNativeListener.onAdFailedToLoad(FacebookAdapter.this, error);
        return;
      }

      Context context = mContext.get();
      if (context == null) {

        AdError error = new AdError(ERROR_NULL_CONTEXT,
            "Failed to create ad options view. Context is null.", ERROR_DOMAIN);

        mNativeListener.onAdFailedToLoad(FacebookAdapter.this, error);
        return;
      }

      final UnifiedAdMapper mapper = new UnifiedAdMapper(mNativeBannerAd);
      mapper.mapUnifiedNativeAd(context, new NativeAdMapperListener() {
        @Override
        public void onMappingSuccess() {
          mNativeListener.onAdLoaded(FacebookAdapter.this, mapper);
        }

        @Override
        public void onMappingFailed(AdError error) {
          Log.w(TAG, error.getMessage());
          mNativeListener.onAdFailedToLoad(FacebookAdapter.this, error);
        }
      });
    }

    @Override
    public void onAdClicked(Ad ad) {
      FacebookAdapter.this.mNativeListener.onAdClicked(FacebookAdapter.this);
      FacebookAdapter.this.mNativeListener.onAdOpened(FacebookAdapter.this);
      // The test Facebook ads leave the application when the ad is clicked. Assuming all
      // the ads do the same, sending onAdLeftApplication callback when the ad is clicked.
      FacebookAdapter.this.mNativeListener.onAdLeftApplication(FacebookAdapter.this);

    }

    @Override
    public void onLoggingImpression(Ad ad) {
      if (mIsImpressionRecorded) {
        Log.d(TAG, "Received onLoggingImpression callback for a native whose impression"
            + " is already recorded. Ignoring the duplicate callback.");
        return;
      }
      FacebookAdapter.this.mNativeListener.onAdImpression(FacebookAdapter.this);
      mIsImpressionRecorded = true;

    }
  }

  private class NativeListener implements AdListener, NativeAdListener {

    /**
     * Context required to create AdOptions View.
     */
    private WeakReference<Context> mContext;

    /**
     * Facebook native banner ad instance.
     */
    private NativeAd mNativeAd;

    private NativeListener(Context context, NativeAd nativeAd) {
      mContext = new WeakReference<>(context);
      mNativeAd = nativeAd;
    }

    @Override
    public void onAdClicked(Ad ad) {
      FacebookAdapter.this.mNativeListener.onAdClicked(FacebookAdapter.this);
      FacebookAdapter.this.mNativeListener.onAdOpened(FacebookAdapter.this);
      // The test Facebook ads leave the application when the ad is clicked. Assuming all
      // the ads do the same, sending onAdLeftApplication callback when the ad is clicked.
      FacebookAdapter.this.mNativeListener.onAdLeftApplication(FacebookAdapter.this);
    }

    @Override
    public void onLoggingImpression(Ad ad) {
      if (mIsImpressionRecorded) {
        Log.d(TAG, "Received onLoggingImpression callback for a native whose impression"
            + " is already recorded. Ignoring the duplicate callback.");
        return;
      }
      FacebookAdapter.this.mNativeListener.onAdImpression(FacebookAdapter.this);
      mIsImpressionRecorded = true;
    }

    @Override
    public void onAdLoaded(Ad ad) {
      if (ad != mNativeAd) {

        AdError error = new AdError(ERROR_WRONG_NATIVE_TYPE, "Ad loaded is not a native ad.",
            ERROR_DOMAIN);

        Log.w(TAG, error.getMessage());
        FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
            FacebookAdapter.this, error);
        return;
      }

      Context context = mContext.get();
      if (context == null) {

        AdError error = new AdError(ERROR_NULL_CONTEXT,
            "Failed to create ad options view. Context is null", ERROR_DOMAIN);

        Log.w(TAG, error.getMessage());
        mNativeListener.onAdFailedToLoad(FacebookAdapter.this, error);
        return;
      }

      final UnifiedAdMapper mapper = new UnifiedAdMapper(mNativeAd);
      mapper.mapUnifiedNativeAd(context, new NativeAdMapperListener() {
        @Override
        public void onMappingSuccess() {
          mNativeListener.onAdLoaded(FacebookAdapter.this, mapper);
        }

        @Override
        public void onMappingFailed(AdError error) {
          Log.w(TAG, error.getMessage());
          mNativeListener.onAdFailedToLoad(FacebookAdapter.this, error);
        }
      });
    }

    @Override
    public void onError(Ad ad, com.facebook.ads.AdError adError) {
      AdError error = getAdError(adError);
      if (!TextUtils.isEmpty(adError.getErrorMessage())) {
        Log.w(TAG, error.getMessage());
      }
      FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
          FacebookAdapter.this, adError.getErrorCode());
    }

    @Override
    public void onMediaDownloaded(Ad ad) {
      Log.d(TAG, "onMediaDownloaded");
    }
  }

  @Nullable
  private com.facebook.ads.AdSize getAdSize(@NonNull Context context, @NonNull AdSize adSize) {

    // Get the actual width of the ad size since Smart Banners and FULL_WIDTH sizes return a
    // width of -1.
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
    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      return null;
    }
    Log.i(TAG, "Found closest ad size: " + closestSize.toString());

    int adHeight = closestSize.getHeight();
    if (adHeight == com.facebook.ads.AdSize.BANNER_HEIGHT_50.getHeight()) {
      return com.facebook.ads.AdSize.BANNER_HEIGHT_50;
    }

    if (adHeight == com.facebook.ads.AdSize.BANNER_HEIGHT_90.getHeight()) {
      return com.facebook.ads.AdSize.BANNER_HEIGHT_90;
    }

    if (adHeight == com.facebook.ads.AdSize.RECTANGLE_HEIGHT_250.getHeight()) {
      return com.facebook.ads.AdSize.RECTANGLE_HEIGHT_250;
    }
    return null;
  }

  /**
   * The {@link UnifiedAdMapper} class is used to map Facebook native ads to Google unified native
   * ads.
   */
  class UnifiedAdMapper extends UnifiedNativeAdMapper {

    /**
     * The Facebook native ad to be mapped.
     */
    private NativeAd mNativeAd;

    /**
     * The Facebook native banner ad to be mapped.
     */
    private NativeBannerAd mNativeBannerAd;

    /**
     * Default constructor for {@link UnifiedAdMapper}.
     *
     * @param nativeAd The Facebook native ad to be mapped.
     */
    public UnifiedAdMapper(NativeAd nativeAd) {
      UnifiedAdMapper.this.mNativeAd = nativeAd;
    }

    /**
     * Constructor for {@link UnifiedAdMapper}.
     *
     * @param nativeBannerAd The Facebook native banner ad to be mapped.
     */
    public UnifiedAdMapper(NativeBannerAd nativeBannerAd) {
      UnifiedAdMapper.this.mNativeBannerAd = nativeBannerAd;
    }

    /**
     * This method will map the Facebook {@link #mNativeAd} to this mapper and send a success
     * callback if the mapping was successful or a failure callback if the mapping was
     * unsuccessful.
     *
     * @param mapperListener used to send success/failure callbacks when mapping is done.
     */
    public void mapUnifiedNativeAd(Context context, NativeAdMapperListener mapperListener) {

      if (isNativeBanner) {
        if (!containsRequiredFieldsForNativeBannerAd(mNativeBannerAd)) {
          AdError error = new AdError(ERROR_MAPPING_NATIVE_ASSETS,
              "Ad from Facebook doesn't have all assets required for the Native Banner Ad format.",
              ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mapperListener.onMappingFailed(error);
          return;
        }

        setHeadline(mNativeBannerAd.getAdHeadline());
        setBody(mNativeBannerAd.getAdBodyText());
        if (mNativeBannerAd.getPreloadedIconViewDrawable() == null) {
          if (mNativeBannerAd.getAdIcon() == null) {
            setIcon(new FacebookAdapterNativeAdImage());
          } else {
            setIcon(
                new FacebookAdapterNativeAdImage(Uri.parse(mNativeBannerAd.getAdIcon().getUrl())));
          }
        } else {
          Drawable iconDrawable = mNativeBannerAd.getPreloadedIconViewDrawable();
          FacebookAdapterNativeAdImage iconImage = new FacebookAdapterNativeAdImage(iconDrawable);
          setIcon(iconImage);
        }
        setCallToAction(mNativeBannerAd.getAdCallToAction());
        setAdvertiser(mNativeBannerAd.getAdvertiserName());

        Bundle extras = new Bundle();
        extras.putCharSequence(KEY_ID, mNativeBannerAd.getId());
        extras.putCharSequence(KEY_SOCIAL_CONTEXT_ASSET, mNativeBannerAd.getAdSocialContext());
        setExtras(extras);
      } else {
        if (!containsRequiredFieldsForUnifiedNativeAd(mNativeAd)) {
          AdError error = new AdError(ERROR_MAPPING_NATIVE_ASSETS,
              "Ad from Facebook doesn't have all assets required for the Native Banner Ad format.",
              ERROR_DOMAIN);
          Log.w(TAG, error.getMessage());
          mapperListener.onMappingFailed(error);
          return;
        }
        // Map all required assets (headline, one image, body, icon and call to
        // action).
        setHeadline(mNativeAd.getAdHeadline());
        List<com.google.android.gms.ads.formats.NativeAd.Image> images = new ArrayList<>();
        images.add(
            new FacebookAdapterNativeAdImage(Uri.parse(mNativeAd.getAdCoverImage().getUrl())));
        setImages(images);
        setBody(mNativeAd.getAdBodyText());
        if (mNativeAd.getPreloadedIconViewDrawable() == null) {
          if (mNativeAd.getAdIcon() == null) {
            setIcon(new FacebookAdapterNativeAdImage());
          } else {
            setIcon(
                new FacebookAdapterNativeAdImage(Uri.parse(mNativeAd.getAdIcon().getUrl())));
          }
        } else {
          Drawable iconDrawable = mNativeAd.getPreloadedIconViewDrawable();
          FacebookAdapterNativeAdImage iconImage = new FacebookAdapterNativeAdImage(iconDrawable);
          setIcon(iconImage);
        }
        setCallToAction(mNativeAd.getAdCallToAction());
        setAdvertiser(mNativeAd.getAdvertiserName());

        mMediaView.setListener(new MediaViewListener() {
          @Override
          public void onPlay(MediaView mediaView) {
            // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
          }

          @Override
          public void onVolumeChange(MediaView mediaView, float v) {
            // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
          }

          @Override
          public void onPause(MediaView mediaView) {
            // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
          }

          @Override
          public void onComplete(MediaView mediaView) {
            if (FacebookAdapter.this.mNativeListener != null) {
              FacebookAdapter.this.mNativeListener.onVideoEnd(FacebookAdapter.this);
            }
          }

          @Override
          public void onEnterFullscreen(MediaView mediaView) {
            // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
          }

          @Override
          public void onExitFullscreen(MediaView mediaView) {
            // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
          }

          @Override
          public void onFullscreenBackground(MediaView mediaView) {
            // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
          }

          @Override
          public void onFullscreenForeground(MediaView mediaView) {
            // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
          }
        });

        // Because the FAN SDK doesn't offer a way to determine whether a native ad contains
        // a video asset or not, the adapter always returns a MediaView and claims to have
        // video content.
        setMediaView(mMediaView);
        setHasVideoContent(true);

        // Map the optional assets.
        Double starRating = getRating(mNativeAd.getAdStarRating());
        if (starRating != null) {
          setStarRating(starRating);
        }
        Bundle extras = new Bundle();
        extras.putCharSequence(KEY_ID, mNativeAd.getId());
        extras.putCharSequence(KEY_SOCIAL_CONTEXT_ASSET, mNativeAd.getAdSocialContext());
        setExtras(extras);
      }
      NativeAdLayout nativeAdLayout = new NativeAdLayout(context);
      AdOptionsView adOptionsView;
      if (isNativeBanner) {
        adOptionsView = new AdOptionsView(context, mNativeBannerAd, nativeAdLayout);
      } else {
        adOptionsView = new AdOptionsView(context, mNativeAd, nativeAdLayout);
      }
      setAdChoicesContent(adOptionsView);
      mapperListener.onMappingSuccess();
    }

    /**
     * This method will check whether or not the given Facebook native ad contains all the necessary
     * fields for it to be mapped to Google Mobile Ads' Unified install ad.
     *
     * @param nativeAd Facebook native ad.
     * @return {@code true} if the given ad contains all the necessary fields, {@link false}
     * otherwise.
     */
    private boolean containsRequiredFieldsForUnifiedNativeAd(NativeAd nativeAd) {
      return ((nativeAd.getAdHeadline() != null) && (nativeAd.getAdCoverImage() != null)
          && (nativeAd.getAdBodyText() != null) && (nativeAd.getAdIcon() != null)
          && (nativeAd.getAdCallToAction() != null) && (mMediaView != null));
    }

    /**
     * This method will check whether or not the given Facebook native ad contains all the necessary
     * fields for it to be mapped to Google Mobile Ads' Unified install ad.
     *
     * @param nativeBannerAd Facebook native ad.
     * @return {@code true} if the given ad contains all the necessary fields, {@link false}
     * otherwise.
     */
    private boolean containsRequiredFieldsForNativeBannerAd(NativeBannerAd nativeBannerAd) {
      return ((nativeBannerAd.getAdHeadline() != null)
          && (nativeBannerAd.getAdBodyText() != null) && (nativeBannerAd.getAdIcon() != null)
          && (nativeBannerAd.getAdCallToAction() != null));
    }


    @Override
    public void trackViews(View view, Map<String, View> clickableAssetViews,
        Map<String, View> nonClickableAssetViews) {

      // Facebook does its own impression tracking.
      setOverrideImpressionRecording(true);

      // Facebook does its own click handling.
      setOverrideClickHandling(true);
      View iconView = null;

      ArrayList<View> assetViews = new ArrayList<>();
      for (Map.Entry<String, View> clickableAssets : clickableAssetViews.entrySet()) {
        assetViews.add(clickableAssets.getValue());

        if (clickableAssets.getKey().equals(UnifiedNativeAdAssetNames.ASSET_ICON)) {
          iconView = clickableAssets.getValue();
        }
      }

      if (isNativeBanner) {
        // trackViews() gets called after the ad loads, so forwarding onAdFailedToLoad() will be
        // too late.
        if (iconView == null) {
          Log.w(TAG, "Missing or invalid native ad icon asset. Facebook impression "
              + "recording might be impacted for this ad.");
          return;
        }

        if (!(iconView instanceof ImageView)) {
          String errorMessage = String.format("Native ad icon asset is rendered with an "
              + "incompatible class type. Facebook impression recording might be impacted "
              + "for this ad. Expected: ImageView, actual: %s.", iconView.getClass());
          Log.w(TAG, errorMessage);
          return;
        }

        mNativeBannerAd.registerViewForInteraction(view, (ImageView) iconView);
        return;
      }

      if (iconView instanceof ImageView) {
        mNativeAd.registerViewForInteraction(view, mMediaView, (ImageView) iconView, assetViews);
      } else {
        Log.w(TAG, "Native icon asset is not of type ImageView."
            + "Calling registerViewForInteraction() without a reference to the icon view.");
        mNativeAd.registerViewForInteraction(view, mMediaView, assetViews);
      }
    }


    @Override
    public void untrackView(View view) {
      if (isNativeBanner && mNativeBannerAd != null) {
        mNativeBannerAd.unregisterView();
      } else if (mNativeAd != null) {
        mNativeAd.unregisterView();
      }
      super.untrackView(view);
    }

    /**
     * Convert rating to a scale of 1 to 5.
     */
    private Double getRating(NativeAd.Rating rating) {
      if (rating == null) {
        return null;
      }
      return (MAX_STAR_RATING * rating.getValue()) / rating.getScale();
    }
  }

  /**
   * The {@link FacebookAdapterNativeAdImage} class is a subclass of {@link
   * com.google.android.gms.ads.formats.NativeAd.Image} used by the {@link FacebookAdapter} to
   * create images for native ads.
   */
  private class FacebookAdapterNativeAdImage extends
      com.google.android.gms.ads.formats.NativeAd.Image {

    /**
     * A drawable for the Image.
     */
    private Drawable mDrawable;

    /**
     * An Uri from which the image can be obtained.
     */
    private Uri mUri;

    /**
     * Constructor for {@link FacebookAdapterNativeAdImage}.
     */
    public FacebookAdapterNativeAdImage() {
    }

    /**
     * Constructor for {@link FacebookAdapterNativeAdImage}, requires an {@link Uri}.
     *
     * @param uri required to initialize.
     */
    public FacebookAdapterNativeAdImage(Uri uri) {
      this.mUri = uri;
    }

    /**
     * Constructor for {@link FacebookAdapterNativeAdImage}, requires an {@link Drawable}.
     *
     * @param drawable required to initialize.
     */
    public FacebookAdapterNativeAdImage(Drawable drawable) {
      this.mDrawable = drawable;
    }

    /**
     * @param drawable set to {@link #mDrawable}.
     */
    protected void setDrawable(Drawable drawable) {
      this.mDrawable = drawable;
    }

    @Override
    public Drawable getDrawable() {
      return mDrawable;
    }

    @Override
    public Uri getUri() {
      return mUri;
    }

    @Override
    public double getScale() {
      // Default scale is 1.
      return 1;
    }
  }

  /**
   * The {@link NativeAdMapperListener} interface is used to notify the success/failure events after
   * trying to map the native ad.
   */
  private interface NativeAdMapperListener {

    /**
     * This method will be called once the native ad mapping is successfully.
     */
    void onMappingSuccess();

    /**
     * This method will be called if the native ad mapping failed.
     */
    void onMappingFailed(AdError error);
  }
  // endregion
}
