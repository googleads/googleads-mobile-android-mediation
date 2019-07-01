package com.mopub.mobileads.dfp.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.ads.mediation.mopub.MoPubSingleton;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;
import com.mopub.nativeads.BaseNativeAd;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.StaticNativeAd;
import com.mopub.nativeads.ViewBinder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;

import static com.google.android.gms.ads.AdRequest.GENDER_FEMALE;
import static com.google.android.gms.ads.AdRequest.GENDER_MALE;
import static com.mopub.mobileads.dfp.adapters.DownloadDrawablesAsync.KEY_IMAGE;

/**
 * A {@link com.mopub.mobileads.dfp.adapters.MoPubAdapter} used to mediate banner ads,
 * interstitial ads and native ads from MoPub.
 */
public class MoPubAdapter implements MediationNativeAdapter, MediationBannerAdapter,
        MediationInterstitialAdapter {
    public static final String TAG = MoPubAdapter.class.getSimpleName();

    private MoPubView mMoPubView;
    private AdSize mAdSize;

    private MoPubInterstitial mMoPubInterstitial;
    private MediationInterstitialListener mMediationInterstitialListener;
    public static final String MOPUB_NATIVE_CEVENT_VERSION = "gmext";
    public static final double DEFAULT_MOPUB_IMAGE_SCALE = 1;
    private static final String MOPUB_AD_UNIT_KEY = "adUnitId";
    private int privacyIconPlacement;
    private int mPrivacyIconSize;

    private static final int MINIMUM_MOPUB_PRIVACY_ICON_SIZE_DP = 10;
    private static final int DEFAULT_MOPUB_PRIVACY_ICON_SIZE_DP = 20;
    private static final int MAXIMUM_MOPUB_PRIVACY_ICON_SIZE_DP = 30;

    private NativeAd.MoPubNativeEventListener mMoPubNativeEventListener;
    private RequestParameters requestParameters;

    @Override
    public void onDestroy() {
        if (mMoPubInterstitial != null) {
            mMoPubInterstitial.destroy();
            mMoPubInterstitial = null;
        }
        if (mMoPubView != null) {
            mMoPubView.destroy();
            mMoPubView = null;
        }
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void requestNativeAd(final Context context,
                                final MediationNativeListener listener,
                                Bundle serverParameters,
                                NativeMediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {

        String adUnit = serverParameters.getString(MOPUB_AD_UNIT_KEY);
        if (TextUtils.isEmpty(adUnit)) {
            Log.d(TAG, "Missing or Invalid MoPub Ad Unit ID.");
            listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        final NativeAdOptions options = mediationAdRequest.getNativeAdOptions();

        if (options != null)
            privacyIconPlacement = options.getAdChoicesPlacement();
        else
            privacyIconPlacement = NativeAdOptions.ADCHOICES_TOP_RIGHT;

        if (mediationExtras != null) {
            int iconSizeExtra = mediationExtras.getInt(BundleBuilder.ARG_PRIVACY_ICON_SIZE_DP);
            if (iconSizeExtra < MINIMUM_MOPUB_PRIVACY_ICON_SIZE_DP) {
                mPrivacyIconSize = MINIMUM_MOPUB_PRIVACY_ICON_SIZE_DP;
            } else if (iconSizeExtra > MAXIMUM_MOPUB_PRIVACY_ICON_SIZE_DP) {
                mPrivacyIconSize = MAXIMUM_MOPUB_PRIVACY_ICON_SIZE_DP;
            } else {
                mPrivacyIconSize = iconSizeExtra;
            }
        } else {
            mPrivacyIconSize = DEFAULT_MOPUB_PRIVACY_ICON_SIZE_DP;
        }

        MoPubNative.MoPubNativeNetworkListener moPubNativeNetworkListener =
                new MoPubNative.MoPubNativeNetworkListener() {

                    @Override
                    public void onNativeLoad(NativeAd nativeAd) {
                        // Setting a native event listener for MoPub's impression & click events.
                        nativeAd.setMoPubNativeEventListener(mMoPubNativeEventListener);

                        BaseNativeAd adData = nativeAd.getBaseNativeAd();
                        if (adData instanceof StaticNativeAd) {
                            final StaticNativeAd staticNativeAd = (StaticNativeAd) adData;

                            try {
                                HashMap<String, URL> map = new HashMap<>();
                                try {
                                    map.put(DownloadDrawablesAsync.KEY_ICON,
                                            new URL(staticNativeAd.getIconImageUrl()));
                                    map.put(KEY_IMAGE,
                                            new URL(staticNativeAd.getMainImageUrl()));
                                } catch (MalformedURLException e) {
                                    Log.d(TAG, "Invalid ad response received from MoPub. Image URLs"
                                            + " are invalid");
                                    listener.onAdFailedToLoad(MoPubAdapter.this,
                                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                }

                                new DownloadDrawablesAsync(new DrawableDownloadListener() {
                                    @Override
                                    public void onDownloadSuccess(
                                            HashMap<String, Drawable> drawableMap) {

                                        try {
                                            final MoPubNativeAppInstallAdMapper
                                                    moPubNativeAppInstallAdMapper =
                                                    new MoPubNativeAppInstallAdMapper(
                                                            staticNativeAd,
                                                            drawableMap,
                                                            privacyIconPlacement,
                                                            mPrivacyIconSize);

                                            // Returning the ImageView containing the main image via
                                            // AdMob's MediaView.
                                            ImageView imageView = new ImageView(context);
                                            imageView.setImageDrawable(drawableMap.get(KEY_IMAGE));

                                            moPubNativeAppInstallAdMapper.setMediaView(imageView);

                                            listener.onAdLoaded(MoPubAdapter.this,
                                                    moPubNativeAppInstallAdMapper);

                                        } catch (Exception e) {
                                            Log.d(TAG, "Exception trying to download native ad "
                                                    + "drawables");
                                            listener.onAdFailedToLoad(MoPubAdapter.this,
                                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                        }
                                    }

                                    @Override
                                    public void onDownloadFailure() {
                                        // Failed to download images, send failure callback.
                                        listener.onAdFailedToLoad(MoPubAdapter.this,
                                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                    }
                                }).execute(map);
                            } catch (Exception e) {
                                Log.d(TAG, "Exception constructing the native ad");
                                listener.onAdFailedToLoad(
                                        MoPubAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                            }
                        }
                    }

                    @Override
                    public void onNativeFail(NativeErrorCode errorCode) {
                        switch (errorCode) {
                            case EMPTY_AD_RESPONSE:
                                listener.onAdFailedToLoad(MoPubAdapter.this,
                                        AdRequest.ERROR_CODE_NO_FILL);
                                break;
                            case INVALID_REQUEST_URL:
                                listener.onAdFailedToLoad(MoPubAdapter.this,
                                        AdRequest.ERROR_CODE_INVALID_REQUEST);
                                break;
                            case CONNECTION_ERROR:
                                listener.onAdFailedToLoad(MoPubAdapter.this,
                                        AdRequest.ERROR_CODE_INVALID_REQUEST);
                                break;
                            case UNSPECIFIED:
                                listener.onAdFailedToLoad(MoPubAdapter.this,
                                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                break;
                            default:
                                listener.onAdFailedToLoad(MoPubAdapter.this,
                                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
                                break;
                        }
                    }
                };

        final MoPubNative moPubNative = new MoPubNative(context, adUnit, moPubNativeNetworkListener);

        ViewBinder viewbinder = new ViewBinder.Builder(0).build();
        MoPubStaticNativeAdRenderer moPubStaticNativeAdRenderer =
                new MoPubStaticNativeAdRenderer(viewbinder);
        moPubNative.registerAdRenderer(moPubStaticNativeAdRenderer);
        EnumSet<RequestParameters.NativeAdAsset> assetsSet =
                EnumSet.of(RequestParameters.NativeAdAsset.TITLE,
                        RequestParameters.NativeAdAsset.TEXT,
                        RequestParameters.NativeAdAsset.CALL_TO_ACTION_TEXT,
                        RequestParameters.NativeAdAsset.MAIN_IMAGE,
                        RequestParameters.NativeAdAsset.ICON_IMAGE);

        requestParameters = new RequestParameters.Builder()
                .keywords(getKeywords(mediationAdRequest, false))
                .userDataKeywords(getKeywords(mediationAdRequest, true))
                .location(mediationAdRequest.getLocation())
                .desiredAssets(assetsSet)
                .build();

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnit).build();
        MoPubSingleton.getInstance().initializeMoPubSDK(context, sdkConfiguration,
                new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
                moPubNative.makeRequest(requestParameters);
            }
        });

        // Forwarding MoPub's impression and click events to AdMob.
        mMoPubNativeEventListener = new NativeAd.MoPubNativeEventListener() {

            @Override
            public void onImpression(View view) {
                listener.onAdImpression(MoPubAdapter.this);
                Log.d(TAG, "onImpression");
            }

            @Override
            public void onClick(View view) {
                listener.onAdClicked(MoPubAdapter.this);
                listener.onAdOpened(MoPubAdapter.this);
                listener.onAdLeftApplication(MoPubAdapter.this);
                Log.d(TAG, "onClick");
            }
        };
    }

    @Override
    public void requestBannerAd(Context context,
                                MediationBannerListener mediationBannerListener,
                                Bundle bundle,
                                AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle bundle1) {

        String adUnit = bundle.getString(MOPUB_AD_UNIT_KEY);
        if (TextUtils.isEmpty(adUnit)) {
            Log.d(TAG, "Missing or Invalid MoPub Ad Unit ID.");
            mediationBannerListener.onAdFailedToLoad(MoPubAdapter.this,
                    AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdSize = getSupportedAdSize(context, adSize);
        if (mAdSize == null) {
            Log.w(TAG, "Failed to request ad, AdSize is null.");
            mediationBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mMoPubView = new MoPubView(context);
        mMoPubView.setBannerAdListener(new MBannerListener(mediationBannerListener));
        mMoPubView.setAdUnitId(adUnit);

        //If test mode is enabled
        if (mediationAdRequest.isTesting()) {
            mMoPubView.setTesting(true);
        }

        //If location is available
        if (mediationAdRequest.getLocation() != null) {
            mMoPubView.setLocation(mediationAdRequest.getLocation());
        }

        mMoPubView.setKeywords(getKeywords(mediationAdRequest, false));
        mMoPubView.setUserDataKeywords(getKeywords(mediationAdRequest, true));

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnit).build();
        MoPubSingleton.getInstance().initializeMoPubSDK(context, sdkConfiguration,
                new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
               mMoPubView.loadAd();
            }
        });
    }

    private AdSize getSupportedAdSize(Context context, AdSize adSize) {
        AdSize original = new AdSize(adSize.getWidth(),
                adSize.getHeight());

        ArrayList<AdSize> potentials = new ArrayList<>(2);
        potentials.add(AdSize.BANNER);
        potentials.add(AdSize.MEDIUM_RECTANGLE);
        potentials.add(AdSize.LEADERBOARD);
        potentials.add(AdSize.WIDE_SKYSCRAPER);
        Log.i(TAG, potentials.toString());
        return findClosestSize(context, original, potentials);
    }

    // Start of helper code to remove when available in SDK
    /**
     * Find the closest supported AdSize from the list of potentials to the provided size.
     * Returns null if none are within given threshold size range.
     */
    public static AdSize findClosestSize(
            Context context, AdSize original, ArrayList<AdSize> potentials) {
        if (potentials == null || original == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int actualWidth = Math.round(original.getWidthInPixels(context)/density);
        int actualHeight = Math.round(original.getHeightInPixels(context)/density);
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

        if (originalWidth * minWidthRatio > potentialWidth ||
                originalWidth < potentialWidth) {
            return false;
        }

        if (originalHeight * minHeightRatio > potentialHeight ||
                originalHeight < potentialHeight) {
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

    @Override
    public View getBannerView() {
        return mMoPubView;
    }

    /* Keywords passed from AdMob are separated into 1) personally identifiable, and 2) non-personally
    identifiable categories before they are forwarded to MoPub due to GDPR.
     */
    public static String getKeywords(MediationAdRequest mediationAdRequest, boolean intendedForPII) {

        Date birthday = mediationAdRequest.getBirthday();
        String ageString = "";

        if (birthday != null) {
            int ageInt = getAge(birthday);
            ageString = "m_age:" + Integer.toString(ageInt);
        }

        int gender = mediationAdRequest.getGender();
        String genderString = "";

        if (gender != -1) {
            if (gender == GENDER_FEMALE) {
                genderString = "m_gender:f";
            } else if (gender == GENDER_MALE) {
                genderString = "m_gender:m";
            }
        }

        StringBuilder keywordsBuilder = new StringBuilder();

        keywordsBuilder = keywordsBuilder.append(MOPUB_NATIVE_CEVENT_VERSION)
                .append(",").append(ageString)
                .append(",").append(genderString);

        if (intendedForPII) {
            if (MoPub.canCollectPersonalInformation()) {
                return keywordsContainPII(mediationAdRequest) ? keywordsBuilder.toString() : "";
            } else {
                return "";
            }
        } else {
            return keywordsContainPII(mediationAdRequest) ? "" : keywordsBuilder.toString();
        }
    }

    // Check whether passed keywords contain personally-identifiable information
    private static boolean keywordsContainPII(MediationAdRequest mediationAdRequest) {
        return mediationAdRequest.getBirthday() != null || mediationAdRequest.getGender() !=
                -1 || mediationAdRequest.getLocation() != null;
    }

    private static int getAge(Date birthday) {
        int givenYear = Integer.parseInt((String) DateFormat.format("yyyy", birthday));
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        return currentYear - givenYear;
    }

    private class MBannerListener implements MoPubView.BannerAdListener {
        private MediationBannerListener mMediationBannerListener;

        public MBannerListener(MediationBannerListener bannerListener) {
            mMediationBannerListener = bannerListener;
        }

        @Override
        public void onBannerClicked(MoPubView moPubView) {
            mMediationBannerListener.onAdClicked(MoPubAdapter.this);
            mMediationBannerListener.onAdLeftApplication(MoPubAdapter.this);
        }

        @Override
        public void onBannerCollapsed(MoPubView moPubView) {
            mMediationBannerListener.onAdClosed(MoPubAdapter.this);
        }

        @Override
        public void onBannerExpanded(MoPubView moPubView) {
            mMediationBannerListener.onAdOpened(MoPubAdapter.this);
        }

        @Override
        public void onBannerFailed(MoPubView moPubView,
                                   MoPubErrorCode moPubErrorCode) {
            try {
                switch (moPubErrorCode) {
                    case NO_FILL:
                        mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                        break;
                    case NETWORK_TIMEOUT:
                        mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_NETWORK_ERROR);
                        break;
                    case SERVER_ERROR:
                        mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    default:
                        mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                }
            } catch (NoClassDefFoundError e) {
            }
        }

        @Override
        public void onBannerLoaded(MoPubView moPubView) {
            if (!(mAdSize.getWidth() == moPubView.getAdWidth()
                    && mAdSize.getHeight() == moPubView.getAdHeight())) {
                Log.e(TAG, "The banner ad size loaded does not match the request size. Update the"
                        + " ad size on your MoPub UI to match the request size.");
                mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                return;
            }
            mMediationBannerListener.onAdLoaded(MoPubAdapter.this);

        }
    }

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle bundle,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle bundle1) {

        if (!(context instanceof Activity)) {
            Log.w(TAG, "MoPub SDK requires an Activity context to load interstitial ads.");
            mediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this,
                    AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        String adUnit = bundle.getString(MOPUB_AD_UNIT_KEY);
        if (TextUtils.isEmpty(adUnit)) {
            Log.d(TAG, "Missing or Invalid MoPub Ad Unit ID.");
            mediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this,
                    AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mMediationInterstitialListener = mediationInterstitialListener;

        mMoPubInterstitial = new MoPubInterstitial((Activity) context, adUnit);
        mMoPubInterstitial.setInterstitialAdListener(
                new mMediationInterstitialListener(mMediationInterstitialListener));

        //If test mode is enabled
        if (mediationAdRequest.isTesting()) {
            mMoPubInterstitial.setTesting(true);
        }

        mMoPubInterstitial.setKeywords(getKeywords(mediationAdRequest, false));
        mMoPubInterstitial.setKeywords(getKeywords(mediationAdRequest, true));

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnit).build();
        MoPubSingleton.getInstance().initializeMoPubSDK(context, sdkConfiguration,
                new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
                mMoPubInterstitial.load();
            }
        });
    }

    @Override
    public void showInterstitial() {
        if (mMoPubInterstitial.isReady()) {
            mMoPubInterstitial.show();
        } else {
            MoPubLog.i("Interstitial was not ready. Unable to load the interstitial");
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdOpened(MoPubAdapter.this);
                mMediationInterstitialListener.onAdClosed(MoPubAdapter.this);
            }
        }
    }

    private class mMediationInterstitialListener implements
            MoPubInterstitial.InterstitialAdListener {
        private MediationInterstitialListener mMediationInterstitialListener;

        public mMediationInterstitialListener(MediationInterstitialListener interstitialListener) {
            mMediationInterstitialListener = interstitialListener;
        }

        @Override
        public void onInterstitialClicked(MoPubInterstitial moPubInterstitial) {
            mMediationInterstitialListener.onAdClicked(MoPubAdapter.this);
            mMediationInterstitialListener.onAdLeftApplication(MoPubAdapter.this);
        }

        @Override
        public void onInterstitialDismissed(MoPubInterstitial moPubInterstitial) {
            mMediationInterstitialListener.onAdClosed(MoPubAdapter.this);
        }

        @Override
        public void onInterstitialFailed(MoPubInterstitial moPubInterstitial,
                                         MoPubErrorCode moPubErrorCode) {
            try {
                switch (moPubErrorCode) {
                    case NO_FILL:
                        mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                        break;
                    case NETWORK_TIMEOUT:
                        mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_NETWORK_ERROR);
                        break;
                    case SERVER_ERROR:
                        mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    case EXPIRED:
                        // MoPub Rewarded video ads expire after 4 hours.
                        Log.i(TAG, "The MoPub Ad has expired. Please make a new Ad Request.");
                        mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                        break;
                    default:
                        mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                }
            } catch (NoClassDefFoundError e) {
            }
        }

        @Override
        public void onInterstitialLoaded(MoPubInterstitial moPubInterstitial) {
            mMediationInterstitialListener.onAdLoaded(MoPubAdapter.this);
        }

        @Override
        public void onInterstitialShown(MoPubInterstitial moPubInterstitial) {
            mMediationInterstitialListener.onAdOpened(MoPubAdapter.this);
        }
    }

    /**
     * The {@link BundleBuilder} class is used to create a NetworkExtras bundle which can be passed
     * to the adapter to make network-specific customizations.
     */
    public static final class BundleBuilder {

        /**
         * Key to add and obtain {@link #mPrivacyIconSizeDp}.
         */
        private static final String ARG_PRIVACY_ICON_SIZE_DP = "privacy_icon_size_dp";

        /**
         * MoPub's privacy icon size in dp.
         */
        private int mPrivacyIconSizeDp;

        /**
         * Sets the privacy icon size in dp.
         */
        public BundleBuilder setPrivacyIconSize(int iconSizeDp) {
            mPrivacyIconSizeDp = iconSizeDp;
            return BundleBuilder.this;
        }

        /**
         * Constructs a Bundle with the specified extras.
         *
         * @return a {@link Bundle} containing the specified extras.
         */
        public Bundle build() {
            Bundle bundle = new Bundle();
            bundle.putInt(ARG_PRIVACY_ICON_SIZE_DP, mPrivacyIconSizeDp);
            return bundle;
        }
    }
}
