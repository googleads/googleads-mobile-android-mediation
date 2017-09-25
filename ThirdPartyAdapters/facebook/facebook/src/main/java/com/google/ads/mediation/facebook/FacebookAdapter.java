package com.google.ads.mediation.facebook;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdChoicesView;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.MediaViewListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdViewAttributes;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Mediation adapter for Facebook Audience Network.
 */
@Keep
public final class FacebookAdapter
        implements MediationBannerAdapter, MediationInterstitialAdapter,
        MediationRewardedVideoAdAdapter, MediationNativeAdapter {

    public static final String KEY_AD_VIEW_ATTRIBUTES = "ad_view_attributes";
    public static final String KEY_AUTOPLAY = "autoplay";
    public static final String KEY_BACKGROUND_COLOR = "background_color";
    public static final String KEY_BUTTON_BORDER_COLOR = "button_border_color";
    public static final String KEY_BUTTON_COLOR = "button_color";
    public static final String KEY_BUTTON_TEXT_COLOR = "button_text_color";
    public static final String KEY_DESCRIPTION_TEXT_COLOR = "description_text_color";
    public static final String KEY_DESCRIPTION_TEXT_SIZE = "description_text_size";
    public static final String KEY_ID = "id";
    public static final String KEY_IS_BOLD = "is_bold";
    public static final String KEY_IS_ITALIC = "is_italic";
    public static final String KEY_SOCIAL_CONTEXT_ASSET = "social_context";
    public static final String KEY_STYLE = "style";
    public static final String KEY_SUBTITLE_ASSET = "subtitle";
    public static final String KEY_TITLE_TEXT_COLOR = "title_text_color";
    public static final String KEY_TITLE_TEXT_SIZE = "title_text_size";
    public static final String KEY_TYPEFACE = "typeface";

    private static final int DRAWABLE_FUTURE_TIMEOUT_SECONDS = 10;

    private static final String PLACEMENT_PARAMETER = "pubid";

    private static final int MAX_STAR_RATING = 5;
    private static final String TAG = "FacebookAdapter";

    private MediationBannerListener mBannerListener;
    private MediationInterstitialListener mInterstitialListener;

    /**
     * Mediation rewarded video ad listener used to forward reward-based video ad events from
     * Facebook SDK to Google Mobile Ads SDK.
     */
    private MediationRewardedVideoAdListener mRewardedListener;
    private MediationNativeListener mNativeListener;
    private AdView mAdView;
    private RelativeLayout mWrappedAdView;
    private InterstitialAd mInterstitialAd;

    /**
     * Facebook rewarded video ad instance.
     */
    private RewardedVideoAd mRewardedVideoAd;
    private NativeAd mNativeAd;

    /**
     * Flag to determine whether or not the rewarded video adapter has been initialized.
     */
    private boolean mIsInitialized;

    /**
     * Flag to determine whether or not an impression callback from Facebook SDK has already been
     * sent to the Google Mobile Ads SDK.
     */
    private boolean mIsImpressionRecorded;

    /**
     * Flag to determine whether or not to make AdChoices icon for native ads expandable.
     * {@code true} by default.
     */
    private boolean mIsAdChoicesIconExpandable = true;

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
        if (mRewardedVideoAd != null) {
            mRewardedVideoAd.destroy();
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
    public void requestBannerAd(Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest adRequest,
                                Bundle mediationExtras) {
        mBannerListener = listener;
        if (!isValidRequestParameters(context, serverParameters)) {
            mBannerListener.onAdFailedToLoad(
                    FacebookAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        if (adSize == null) {
            Log.w(TAG, "Fail to request banner ad, adSize is null");
            mBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        String placementId = serverParameters.getString(PLACEMENT_PARAMETER);

        com.facebook.ads.AdSize facebookAdSize = getAdSize(context, adSize);
        if (facebookAdSize == null) {
            Log.w(TAG,
                    "The input ad size " + adSize.toString() + " is not supported at this moment.");
            mBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_NO_FILL);
            return;
        }
        mAdView = new AdView(context, placementId, facebookAdSize);
        mAdView.setAdListener(new BannerListener());
        buildAdRequest(adRequest);
        RelativeLayout.LayoutParams adViewLayoutParams = new RelativeLayout.LayoutParams(
                adSize.getWidthInPixels(context), adSize.getHeightInPixels(context));
        mWrappedAdView = new RelativeLayout(context);
        mAdView.setLayoutParams(adViewLayoutParams);
        mWrappedAdView.addView(mAdView);
        mAdView.loadAd();
    }

    @Override
    public View getBannerView() {
        return mWrappedAdView;
    }
    //endregion

    //region MediationInterstitialAdapter implementation.
    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest adRequest,
                                      Bundle mediationExtras) {
        mInterstitialListener = listener;
        if (!isValidRequestParameters(context, serverParameters)) {
            mInterstitialListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        String placementId = serverParameters.getString(PLACEMENT_PARAMETER);

        mInterstitialAd = new InterstitialAd(context, placementId);
        mInterstitialAd.setAdListener(new InterstitialListener());
        buildAdRequest(adRequest);
        mInterstitialAd.loadAd();
    }

    @Override
    public void showInterstitial() {
        if (mInterstitialAd.isAdLoaded()) {
            mInterstitialAd.show();
        }
    }
    //endregion

    //region MediationRewardedVideoAdAdapter implementation.
    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String unused,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        mRewardedListener = mediationRewardedVideoAdListener;
        if (!isValidRequestParameters(context, serverParameters)) {
            mRewardedListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        String placementId = serverParameters.getString(PLACEMENT_PARAMETER);
        mRewardedVideoAd = new RewardedVideoAd(context, placementId);
        mRewardedVideoAd.setAdListener(new RewardedVideoListener());
        mIsInitialized = true;
        mRewardedListener.onInitializationSucceeded(this);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle networkExtras) {
        if (mRewardedVideoAd == null) {
            Log.w(TAG, "Failed to request rewarded video ad, adapter has not been initialized.");
            mIsInitialized = false;
            if (mRewardedListener != null) {
                mRewardedListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        } else {
            if (mRewardedVideoAd.isAdLoaded()) {
                mRewardedListener.onAdLoaded(this);
            } else {
                buildAdRequest(mediationAdRequest);
                mRewardedVideoAd.loadAd(true);
            }
        }
    }

    @Override
    public void showVideo() {
        if (mRewardedVideoAd != null && mRewardedVideoAd.isAdLoaded()) {
            mRewardedVideoAd.show();
            // Facebook's rewarded video listener does not have an equivalent callback for
            // onAdOpened() but an ad is shown immediately after calling show(), so sending an
            // onAdOpened callback.
            mRewardedListener.onAdOpened(FacebookAdapter.this);
            mRewardedListener.onVideoStarted(FacebookAdapter.this);
        } else {
            // No ads to show, but already sent onAdLoaded. Log a warning and send ad opened and
            // ad closed callbacks.
            Log.w(TAG, "No ads to show.");
            if (mRewardedListener != null) {
                mRewardedListener.onAdOpened(this);
                mRewardedListener.onAdClosed(this);
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return mIsInitialized;
    }
    //endregion

    //region MediationNativeAdapter implementation.
    @Override
    public void requestNativeAd(Context context,
                                MediationNativeListener listener,
                                Bundle serverParameters,
                                NativeMediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        mNativeListener = listener;
        if (!isValidRequestParameters(context, serverParameters)) {
            mNativeListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        // Verify that the request is for both app install and content ads.
        if (!(mediationAdRequest.isAppInstallAdRequested()
                && mediationAdRequest.isContentAdRequested())) {
            Log.w(TAG, "Failed to request native ad. Both app install and content ad should be "
                    + "requested");
            mNativeListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        String placementId = serverParameters.getString(PLACEMENT_PARAMETER);

        // Get the optional extras if set by the publisher.
        if (mediationExtras != null) {
            mIsAdChoicesIconExpandable = mediationExtras.getBoolean(
                    FacebookExtrasBundleBuilder.KEY_EXPANDABLE_ICON, true);
        }

        mMediaView = new MediaView(context);

        mNativeAd = new NativeAd(context, placementId);
        mNativeAd.setAdListener(new NativeListener(mNativeAd, mediationAdRequest));
        buildAdRequest(mediationAdRequest);
        mNativeAd.loadAd();
    }
    //endregion

    //region Common methods.

    /**
     * Checks whether or not the request parameters needed to load Facebook ads are null.
     *
     * @param context          an Android {@link Context}.
     * @param serverParameters a {@link Bundle} containing server parameters needed to request ads
     *                         from Facebook.
     * @return {@code false} if any of the request parameters are null.
     */
    private static boolean isValidRequestParameters(Context context, Bundle serverParameters) {
        if (context == null) {
            Log.w(TAG, "Failed to request ad, Context is null.");
            return false;
        }

        if (serverParameters == null) {
            Log.w(TAG, "Failed to request ad, serverParameters is null.");
            return false;
        }

        if (TextUtils.isEmpty(serverParameters.getString(PLACEMENT_PARAMETER))) {
            Log.w(TAG, "Failed to request ad, placementId is null or empty.");
            return false;
        }
        return true;
    }

    /**
     * Converts an {@link AdError} code to Google Mobile Ads SDK readable error code.
     *
     * @param adError the {@link AdError} to be converted.
     * @return an {@link AdRequest} error code.
     */
    private int convertErrorCode(AdError adError) {
        if (adError == null) {
            return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
        int errorCode = adError.getErrorCode();
        switch (errorCode) {
            case AdError.NETWORK_ERROR_CODE:
            case AdError.SERVER_ERROR_CODE:
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case AdError.NO_FILL_ERROR_CODE:
                return AdRequest.ERROR_CODE_NO_FILL;
            case AdError.LOAD_TOO_FREQUENTLY_ERROR_CODE:
                return AdRequest.ERROR_CODE_INVALID_REQUEST;
            case AdError.INTERNAL_ERROR_CODE:
            default:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }

    private void buildAdRequest(MediationAdRequest adRequest) {
        if (adRequest != null) {
            AdSettings.setIsChildDirected((adRequest.taggedForChildDirectedTreatment()
                    == MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE));
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
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, errorMessage);
            }
            FacebookAdapter.this.mBannerListener.onAdFailedToLoad(
                    FacebookAdapter.this, convertErrorCode(adError));
        }
    }
    //endregion

    //region Interstitial adapter utility classes.
    private class InterstitialListener implements InterstitialAdListener {
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
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, errorMessage);
            }
            FacebookAdapter.this.mInterstitialListener.onAdFailedToLoad(
                    FacebookAdapter.this, convertErrorCode(adError));
        }

        @Override
        public void onInterstitialDismissed(Ad ad) {
            FacebookAdapter.this.mInterstitialListener.onAdClosed(FacebookAdapter.this);
        }

        @Override
        public void onInterstitialDisplayed(Ad ad) {
            FacebookAdapter.this.mInterstitialListener.onAdOpened(FacebookAdapter.this);
        }
    }
    //endregion

    //region Rewarded video adapter utility classes.

    /**
     * A {@link RewardedVideoAdListener} used to listen to rewarded video ad events from Facebook
     * SDK and forward to Google Mobile Ads SDK using {@link #mRewardedListener}
     */
    private class RewardedVideoListener implements RewardedVideoAdListener {
        private RewardedVideoListener() {
        }

        @Override
        public void onRewardedVideoCompleted() {
            // Facebook SDK doesn't provide a reward value. The publisher is expected to
            // override the reward in AdMob UI.
            mRewardedListener.onRewarded(FacebookAdapter.this, new FacebookReward());
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, errorMessage);
            }

            mRewardedListener.onAdFailedToLoad(FacebookAdapter.this, convertErrorCode(adError));
        }

        @Override
        public void onAdLoaded(Ad ad) {
            mRewardedListener.onAdLoaded(FacebookAdapter.this);
        }

        @Override
        public void onAdClicked(Ad ad) {
            mRewardedListener.onAdClicked(FacebookAdapter.this);
            mRewardedListener.onAdLeftApplication(FacebookAdapter.this);
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            // Google Mobile Ads SDK does its own impression tracking for rewarded video ads.
        }

        @Override
        public void onRewardedVideoClosed() {
            mRewardedListener.onAdClosed(FacebookAdapter.this);
        }
    }

    /**
     * An implementation of {@link RewardItem} that will be given to the app when a Facebook reward
     * is granted. Because the FAN SDK doesn't provide reward amounts and types, defaults are used
     * here.
     */
    private class FacebookReward implements RewardItem {

        @Override
        public String getType() {
            // Facebook SDK does not provide a reward type.
            return "";
        }

        @Override
        public int getAmount() {
            // Facebook SDK does not provide reward amount, default to 1.
            return 1;
        }
    }
    //endregion

    //region Native adapter utility methods and classes.
    private class NativeListener implements AdListener {
        private NativeAd mNativeAd;
        private NativeMediationAdRequest mMediationAdRequest;

        private NativeListener(NativeAd nativeAd, NativeMediationAdRequest mediationAdRequest) {
            mNativeAd = nativeAd;
            mMediationAdRequest = mediationAdRequest;
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
                Log.w(TAG, "Ad loaded is not a native ad.");
                FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
                        FacebookAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                return;
            }

            NativeAdOptions options = mMediationAdRequest.getNativeAdOptions();
            // We always convert the ad into an app install ad.
            final AppInstallMapper mapper = new AppInstallMapper(mNativeAd, options);
            mapper.mapNativeAd(new NativeAdMapperListener() {
                @Override
                public void onMappingSuccess() {
                    mNativeListener.onAdLoaded(FacebookAdapter.this, mapper);
                }

                @Override
                public void onMappingFailed() {
                    mNativeListener.onAdFailedToLoad(FacebookAdapter.this,
                            AdRequest.ERROR_CODE_NO_FILL);
                }
            });
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, errorMessage);
            }
            FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
                    FacebookAdapter.this, convertErrorCode(adError));
        }
    }

    private com.facebook.ads.AdSize getAdSize(Context context, AdSize adSize) {
        if (adSize.getWidth() == com.facebook.ads.AdSize.BANNER_320_50.getWidth()
                && adSize.getHeight() == com.facebook.ads.AdSize.BANNER_320_50.getHeight()) {
            return com.facebook.ads.AdSize.BANNER_320_50;
        }

        // adSize.getHeight will return -2 for smart banner. So we need to use
        // adSize.getHeightInPixels here.
        int heightInDip = pixelToDip(adSize.getHeightInPixels(context));
        if (heightInDip == com.facebook.ads.AdSize.BANNER_HEIGHT_50.getHeight()) {
            return com.facebook.ads.AdSize.BANNER_HEIGHT_50;
        }

        if (heightInDip == com.facebook.ads.AdSize.BANNER_HEIGHT_90.getHeight()) {
            return com.facebook.ads.AdSize.BANNER_HEIGHT_90;
        }

        if (heightInDip == com.facebook.ads.AdSize.RECTANGLE_HEIGHT_250.getHeight()) {
            return com.facebook.ads.AdSize.RECTANGLE_HEIGHT_250;
        }
        return null;
    }

    private int pixelToDip(int pixel) {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return Math.round(pixel / displayMetrics.density);
    }

    /**
     * The {@link AppInstallMapper} class is used to map Facebook native ads to Google Mobile Ads'
     * native app install ads.
     */
    class AppInstallMapper extends NativeAppInstallAdMapper {

        /**
         * The Facebook native ad to be mapped.
         */
        private NativeAd mNativeAd;

        /**
         * Google Mobile Ads native ad options.
         */
        private NativeAdOptions mNativeAdOptions;

        /**
         * Default constructor for {@link AppInstallMapper}.
         *
         * @param nativeAd  The Facebook native ad to be mapped.
         * @param adOptions {@link NativeAdOptions} containing the preferences to be used when
         *                  mapping the native ad.
         */
        public AppInstallMapper(NativeAd nativeAd, NativeAdOptions adOptions) {
            AppInstallMapper.this.mNativeAd = nativeAd;
            AppInstallMapper.this.mNativeAdOptions = adOptions;
        }

        /**
         * This method will map the Facebook {@link #mNativeAd} to this mapper and send a success
         * callback if the mapping was successful or a failure callback if the mapping was
         * unsuccessful.
         *
         * @param mapperListener used to send success/failure callbacks when mapping is done.
         */
        public void mapNativeAd(NativeAdMapperListener mapperListener) {
            if (!containsRequiredFieldsForNativeAppInstallAd(mNativeAd)) {
                Log.w(TAG, "Ad from Facebook doesn't have all assets required for the app install"
                        + " format.");
                mapperListener.onMappingFailed();
                return;
            }

            // Map all required assets (headline, one image, body, icon and call to
            // action).
            setHeadline(mNativeAd.getAdTitle());
            List<com.google.android.gms.ads.formats.NativeAd.Image> images = new ArrayList<>();
            images.add(new FacebookAdapterNativeAdImage(
                    Uri.parse(mNativeAd.getAdCoverImage().getUrl())));
            setImages(images);
            setBody(mNativeAd.getAdBody());
            setIcon(new FacebookAdapterNativeAdImage(Uri.parse(mNativeAd.getAdIcon().getUrl())));
            setCallToAction(mNativeAd.getAdCallToAction());

            if (mMediaView != null) {
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
                mMediaView.setNativeAd(mNativeAd);

                // Because the FAN SDK doesn't offer a way to determine whether a native ad contains
                // a video asset or not, the adapter always returns a MediaView and claims to have
                // video content.
                setMediaView(mMediaView);
                setHasVideoContent(true);
            } else {
                Log.w(TAG, "Couldn't set MediaView.");
                setHasVideoContent(false);
            }

            // Map the optional assets.
            Double starRating = getRating(mNativeAd.getAdStarRating());
            if (starRating != null) {
                setStarRating(starRating);
            }

            // Pass all the assets not supported by Google as extras.
            Bundle extras = new Bundle();
            extras.putCharSequence(KEY_ID, mNativeAd.getId());
            extras.putCharSequence(KEY_SOCIAL_CONTEXT_ASSET, mNativeAd.getAdSocialContext());
            extras.putCharSequence(KEY_SUBTITLE_ASSET, mNativeAd.getAdSubtitle());

            NativeAdViewAttributes attributes = mNativeAd.getAdViewAttributes();
            if (attributes != null) {
                Bundle attributesBundle = new Bundle();
                attributesBundle.putBoolean(KEY_AUTOPLAY, attributes.getAutoplay());
                attributesBundle.putInt(KEY_BACKGROUND_COLOR, attributes.getBackgroundColor());
                attributesBundle.putInt(KEY_BUTTON_BORDER_COLOR, attributes.getButtonBorderColor());
                attributesBundle.putInt(KEY_BUTTON_COLOR, attributes.getButtonColor());
                attributesBundle.putInt(KEY_BUTTON_TEXT_COLOR, attributes.getButtonTextColor());
                attributesBundle.putInt(KEY_DESCRIPTION_TEXT_COLOR,
                        attributes.getDescriptionTextColor());
                attributesBundle.putInt(KEY_DESCRIPTION_TEXT_SIZE,
                        attributes.getDescriptionTextSize());
                attributesBundle.putInt(KEY_TITLE_TEXT_COLOR, attributes.getTitleTextColor());
                attributesBundle.putInt(KEY_TITLE_TEXT_SIZE, attributes.getTitleTextSize());

                Typeface typeface = attributes.getTypeface();
                if (typeface != null) {
                    Bundle typefaceBundle = new Bundle();
                    typefaceBundle.putBoolean(KEY_IS_BOLD, typeface.isBold());
                    typefaceBundle.putBoolean(KEY_IS_ITALIC, typeface.isItalic());
                    typefaceBundle.putInt(KEY_STYLE, typeface.getStyle());
                    attributesBundle.putBundle(KEY_TYPEFACE, typefaceBundle);
                }
                extras.putBundle(KEY_AD_VIEW_ATTRIBUTES, attributesBundle);
            }
            setExtras(extras);

            // Respect the publisher's setting whether to return a drawable or not.
            boolean urlsOnly = false;
            if (mNativeAdOptions != null) {
                urlsOnly = mNativeAdOptions.shouldReturnUrlsForImageAssets();
            }

            if (urlsOnly) {
                mapperListener.onMappingSuccess();
            } else {
                new DownloadDrawablesAsync(mapperListener).execute(AppInstallMapper.this);
            }
        }

        /**
         * This method will check whether or not the given Facebook native ad contains all the
         * necessary fields for it to be mapped to Google Mobile Ads' native app install ad.
         *
         * @param nativeAd Facebook native ad.
         * @return {@code true} if the given ad contains all the necessary fields, {@link false}
         * otherwise.
         */
        private boolean containsRequiredFieldsForNativeAppInstallAd(NativeAd nativeAd) {
            return ((nativeAd.getAdTitle() != null) && (nativeAd.getAdCoverImage() != null)
                    && (nativeAd.getAdBody() != null) && (nativeAd.getAdIcon() != null)
                    && (nativeAd.getAdCallToAction() != null));
        }

        @Override
        public void trackView(View view) {
            ViewGroup adView = (ViewGroup) view;
            // Find the overlay view in the given ad view. The overlay view will always be the
            // top most view in the hierarchy.
            View overlayView = adView.getChildAt(adView.getChildCount() - 1);
            if (overlayView instanceof FrameLayout) {
                // Create and add Facebook's AdChoicesView to the overlay view.
                AdChoicesView adChoicesView =
                        new AdChoicesView(view.getContext(), mNativeAd, mIsAdChoicesIconExpandable);
                ((ViewGroup) overlayView).addView(adChoicesView);
                // We know that the overlay view is a FrameLayout, so we get the FrameLayout's
                // LayoutParams from the AdChoicesView.
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) adChoicesView.getLayoutParams();
                if (mNativeAdOptions != null) {
                    switch (mNativeAdOptions.getAdChoicesPlacement()) {
                        case NativeAdOptions.ADCHOICES_TOP_LEFT:
                            params.gravity = Gravity.TOP | Gravity.LEFT;
                            break;
                        case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
                            params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                            break;
                        case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
                            params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                            break;
                        case NativeAdOptions.ADCHOICES_TOP_RIGHT:
                        default:
                            params.gravity = Gravity.TOP | Gravity.RIGHT;
                    }
                } else {
                    // Default to top right if native ad options are not provided.
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
                }
                adView.requestLayout();
            } else {
                Log.w(TAG, "Failed to show AdChoices icon.");
            }

            // Facebook does its own impression tracking.
            setOverrideImpressionRecording(true);

            // Facebook does its own click handling.
            setOverrideClickHandling(true);

            if (view instanceof NativeAppInstallAdView) {
                NativeAppInstallAdView appInstallAdView = (NativeAppInstallAdView) view;
                ArrayList<View> assetViews = new ArrayList<>();

                if (appInstallAdView.getHeadlineView() != null) {
                    assetViews.add(appInstallAdView.getHeadlineView());
                }

                if (appInstallAdView.getBodyView() != null) {
                    assetViews.add(appInstallAdView.getBodyView());
                }

                if (appInstallAdView.getCallToActionView() != null) {
                    assetViews.add(appInstallAdView.getCallToActionView());
                }

                if (appInstallAdView.getIconView() != null) {
                    assetViews.add(appInstallAdView.getIconView());
                }

                if (appInstallAdView.getImageView() != null) {
                    assetViews.add(appInstallAdView.getImageView());
                }

                if (appInstallAdView.getPriceView() != null) {
                    assetViews.add(appInstallAdView.getPriceView());
                }

                if (appInstallAdView.getStarRatingView() != null) {
                    assetViews.add(appInstallAdView.getStarRatingView());
                }

                if (appInstallAdView.getStoreView() != null) {
                    assetViews.add(appInstallAdView.getStoreView());
                }

                if (appInstallAdView.getMediaView() != null) {
                    assetViews.add(appInstallAdView.getMediaView());
                }

                mNativeAd.registerViewForInteraction(view, assetViews);
            } else {
                Log.w(TAG, "Failed to register view for interaction.");
            }
        }

        @Override
        public void untrackView(View view) {
            super.untrackView(view);
            // Called when the native ad view no longer needs tracking. Remove any previously
            // added trackers.

            ViewGroup adView = (ViewGroup) view;
            // Find the overlay view in the given ad view. The overlay view will always be the
            // top most view in the hierarchy.
            View overlayView = adView.getChildAt(adView.getChildCount() - 1);
            if (overlayView instanceof FrameLayout) {
                ((FrameLayout) overlayView).removeAllViews();
            }

            mNativeAd.unregisterView();
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
     * The {@link DownloadDrawablesAsync} class is used to download the drawables and send the
     * necessary callbacks.
     */
    private static class DownloadDrawablesAsync extends AsyncTask<Object, Void, Boolean> {

        /**
         * The image drawable listener used to send the success/fail callbacks once the task
         * completed the download process.
         */
        private NativeAdMapperListener mDrawableListener;

        public DownloadDrawablesAsync(NativeAdMapperListener listener) {
            this.mDrawableListener = listener;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            AppInstallMapper mapper = (AppInstallMapper) params[0];
            ExecutorService executorService = Executors.newCachedThreadPool();

            // Create all the Futures before calling get on them, so that the images can be
            // downloaded in parallel. This HashMap is used to set each drawable to its image
            // once the Future finishes downloading.
            HashMap<FacebookAdapterNativeAdImage, Future<Drawable>> futuresMap = new HashMap<>();

            List<com.google.android.gms.ads.formats.NativeAd.Image> images = mapper.getImages();
            for (int i = 0; i < images.size(); i++) {
                FacebookAdapterNativeAdImage image =
                        (FacebookAdapterNativeAdImage) images.get(i);
                Future<Drawable> drawableFuture =
                        getDrawableFuture(image.getUri(), executorService);
                futuresMap.put(image, drawableFuture);
            }

            FacebookAdapterNativeAdImage iconImage =
                    (FacebookAdapterNativeAdImage) mapper.getIcon();
            Future<Drawable> drawableFuture =
                    getDrawableFuture(iconImage.getUri(), executorService);
            futuresMap.put(iconImage, drawableFuture);

            for (Map.Entry<FacebookAdapterNativeAdImage, Future<Drawable>> pair
                    : futuresMap.entrySet()) {
                Drawable drawable;
                try {
                    drawable =
                            pair.getValue().get(DRAWABLE_FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                    Log.w(TAG, "Exception occurred while waiting for future to return. "
                            + "Returning null as drawable : " + exception);
                    return false;
                }
                pair.getKey().setDrawable(drawable);
            }

            return true;
        }

        /**
         * This method will use the given {@link ExecutorService} to create a
         * {@link Future<Drawable>} which will download the image drawable. The Future will start
         * downloading as soon as the {@link ExecutorService} has a thread available.
         *
         * @param uri             the {@link Uri} from which to get the Image.
         * @param executorService needed to create the {@link Future<Drawable>}.
         * @return a {@link Future<Drawable>} which will start downloading the image from the
         * given Uri.
         */
        private Future<Drawable> getDrawableFuture(final Uri uri, ExecutorService executorService) {
            // The call() will be executed as the threads in executorService's thread pool become
            // available.
            return executorService.submit(new Callable<Drawable>() {
                @Override
                public Drawable call() throws Exception {
                    InputStream in = new URL(uri.toString()).openStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(in);

                    // Defaulting to a scale of 1.
                    bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                    return new BitmapDrawable(Resources.getSystem(), bitmap);
                }
            });
        }

        @Override
        protected void onPostExecute(Boolean isDownloadSuccessful) {
            super.onPostExecute(isDownloadSuccessful);
            if (isDownloadSuccessful) {
                // Image download successful, send on success callback.
                mDrawableListener.onMappingSuccess();
            } else {
                mDrawableListener.onMappingFailed();
            }
        }
    }

    /**
     * The {@link FacebookExtrasBundleBuilder} class is used to create a network extras bundle that
     * can be passed to the adapter to make network specific customizations.
     */
    public static class FacebookExtrasBundleBuilder {

        /**
         * Key to add and obtain {@link #mIsExpandableIcon}.
         */
        private static final String KEY_EXPANDABLE_ICON = "expandable_icon";

        /**
         * Whether or not ad choices icon for native ads is expandable.
         */
        private boolean mIsExpandableIcon;

        public FacebookExtrasBundleBuilder setNativeAdChoicesIconExpandable(
                boolean isExpandableIcon) {
            this.mIsExpandableIcon = isExpandableIcon;
            return FacebookExtrasBundleBuilder.this;
        }

        public Bundle build() {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_EXPANDABLE_ICON, mIsExpandableIcon);
            return bundle;
        }
    }

    /**
     * The {@link FacebookAdapterNativeAdImage} class is a subclass of
     * {@link com.google.android.gms.ads.formats.NativeAd.Image} used by the {@link FacebookAdapter}
     * to create images for native ads.
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
         * Default constructor for {@link FacebookAdapterNativeAdImage}, requires an {@link Uri}.
         *
         * @param uri required to initialize.
         */
        public FacebookAdapterNativeAdImage(Uri uri) {
            this.mUri = uri;
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
     * The {@link NativeAdMapperListener} interface is used to notify the success/failure
     * events after trying to map the native ad.
     */
    private interface NativeAdMapperListener {

        /**
         * This method will be called once the native ad mapping is successfully.
         */
        void onMappingSuccess();

        /**
         * This method will be called if the native ad mapping failed.
         */
        void onMappingFailed();
    }
    //endregion
}
