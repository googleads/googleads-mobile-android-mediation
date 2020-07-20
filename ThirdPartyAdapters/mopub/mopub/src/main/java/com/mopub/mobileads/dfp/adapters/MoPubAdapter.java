package com.mopub.mobileads.dfp.adapters;

import static com.google.ads.mediation.mopub.MoPubMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.ERROR_DOWNLOADING_NATIVE_ASSETS;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.ERROR_MINIMUM_BANNER_SIZE;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.ERROR_REQUIRES_UNIFIED_NATIVE_ADS;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.ERROR_WRONG_NATIVE_TYPE;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.createAdapterError;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.createSDKError;
import static com.google.ads.mediation.mopub.MoPubMediationAdapter.getMediationErrorCode;
import static com.google.android.gms.ads.AdRequest.GENDER_FEMALE;
import static com.google.android.gms.ads.AdRequest.GENDER_MALE;
import static com.mopub.mobileads.dfp.adapters.DownloadDrawablesAsync.KEY_IMAGE;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mopub.MoPubSingleton;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
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

/**
 * A {@link com.mopub.mobileads.dfp.adapters.MoPubAdapter} used to mediate banner ads, interstitial
 * ads and native ads from MoPub.
 */
public class MoPubAdapter
    implements MediationNativeAdapter, MediationBannerAdapter, MediationInterstitialAdapter {

  public static final String TAG = MoPubAdapter.class.getSimpleName();

  private Bundle mExtras;
  private MoPubView mMoPubView;
  private Context mContext;
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
  public void onPause() {}

  @Override
  public void onResume() {}

  @Override
  public void requestNativeAd(
      final Context context,
      final MediationNativeListener listener,
      Bundle serverParameters,
      final NativeMediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {

    String adUnit = serverParameters.getString(MOPUB_AD_UNIT_KEY);
    if (TextUtils.isEmpty(adUnit)) {
      String errorMessage =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid MoPub Ad Unit ID.");
      Log.e(TAG, errorMessage);
      listener.onAdFailedToLoad(MoPubAdapter.this, ERROR_INVALID_SERVER_PARAMETERS);
      return;
    }
    if (!mediationAdRequest.isUnifiedNativeAdRequested()
        && !mediationAdRequest.isAppInstallAdRequested()) {
      String errorMessage =
          createAdapterError(
              ERROR_REQUIRES_UNIFIED_NATIVE_ADS,
              "Failed to request native ad: "
                  + "Unified Native Ad or App install Ad should be requested.");
      Log.e(TAG, errorMessage);
      listener.onAdFailedToLoad(this, ERROR_REQUIRES_UNIFIED_NATIVE_ADS);
      return;
    }
    final NativeAdOptions options = mediationAdRequest.getNativeAdOptions();
    if (options != null) {
      privacyIconPlacement = options.getAdChoicesPlacement();
    } else {
      privacyIconPlacement = NativeAdOptions.ADCHOICES_TOP_RIGHT;
    }

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
            if (!(adData instanceof StaticNativeAd)) {
              String errorMessage =
                  createAdapterError(
                      ERROR_WRONG_NATIVE_TYPE,
                      "Loaded native ad is not an instance of StaticNativeAd.");
              Log.w(TAG, errorMessage);
              listener.onAdFailedToLoad(MoPubAdapter.this, ERROR_WRONG_NATIVE_TYPE);
              return;
            }

            final StaticNativeAd staticNativeAd = (StaticNativeAd) adData;

            HashMap<String, URL> map = new HashMap<>();
            try {
              map.put(DownloadDrawablesAsync.KEY_ICON, new URL(staticNativeAd.getIconImageUrl()));
              map.put(KEY_IMAGE, new URL(staticNativeAd.getMainImageUrl()));
            } catch (MalformedURLException e) {
              String errorMessage =
                  createAdapterError(
                      ERROR_DOWNLOADING_NATIVE_ASSETS,
                      "Invalid ad response received from MoPub. Image URLs are malformed.");
              Log.i(TAG, errorMessage);
              listener.onAdFailedToLoad(MoPubAdapter.this, ERROR_DOWNLOADING_NATIVE_ASSETS);
            }

            new DownloadDrawablesAsync(
                    new DrawableDownloadListener() {
                      @Override
                      public void onDownloadSuccess(HashMap<String, Drawable> drawableMap) {
                        Drawable icon = drawableMap.get(DownloadDrawablesAsync.KEY_ICON);
                        Drawable image = drawableMap.get(DownloadDrawablesAsync.KEY_IMAGE);
                        if (mediationAdRequest.isUnifiedNativeAdRequested()) {
                          final MoPubUnifiedNativeAdMapper moPubUnifiedNativeAdMapper =
                              new MoPubUnifiedNativeAdMapper(
                                  context,
                                  staticNativeAd,
                                  icon,
                                  image,
                                  privacyIconPlacement,
                                  mPrivacyIconSize);

                          listener.onAdLoaded(MoPubAdapter.this, moPubUnifiedNativeAdMapper);
                        } else if (mediationAdRequest.isAppInstallAdRequested()) {
                          final MoPubNativeAppInstallAdMapper moPubNativeAppInstallAdMapper =
                              new MoPubNativeAppInstallAdMapper(
                                  context,
                                  staticNativeAd,
                                  icon,
                                  image,
                                  privacyIconPlacement,
                                  mPrivacyIconSize);

                          listener.onAdLoaded(MoPubAdapter.this, moPubNativeAppInstallAdMapper);
                        }
                      }

                      @Override
                      public void onDownloadFailure() {
                        // Failed to download images, send failure callback.
                        String errorMessage =
                            createAdapterError(
                                ERROR_DOWNLOADING_NATIVE_ASSETS, "Failed to download images.");
                        Log.w(TAG, errorMessage);
                        listener.onAdFailedToLoad(
                            MoPubAdapter.this, ERROR_DOWNLOADING_NATIVE_ASSETS);
                      }
                    })
                .execute(map);
          }

          @Override
          public void onNativeFail(NativeErrorCode errorCode) {
            String errorSDKMessage = createSDKError(errorCode);
            Log.w(TAG, errorSDKMessage);
            listener.onAdFailedToLoad(MoPubAdapter.this, getMediationErrorCode(errorCode));
          }
        };

    final MoPubNative moPubNative = new MoPubNative(context, adUnit, moPubNativeNetworkListener);

    ViewBinder viewbinder = new ViewBinder.Builder(0).build();
    MoPubStaticNativeAdRenderer moPubStaticNativeAdRenderer =
        new MoPubStaticNativeAdRenderer(viewbinder);
    moPubNative.registerAdRenderer(moPubStaticNativeAdRenderer);
    EnumSet<RequestParameters.NativeAdAsset> assetsSet =
        EnumSet.of(
            RequestParameters.NativeAdAsset.TITLE,
            RequestParameters.NativeAdAsset.TEXT,
            RequestParameters.NativeAdAsset.CALL_TO_ACTION_TEXT,
            RequestParameters.NativeAdAsset.MAIN_IMAGE,
            RequestParameters.NativeAdAsset.ICON_IMAGE);

    requestParameters =
        new RequestParameters.Builder()
            .keywords(getKeywords(mediationAdRequest, false))
            .userDataKeywords(getKeywords(mediationAdRequest, true))
            .location(mediationAdRequest.getLocation())
            .desiredAssets(assetsSet)
            .build();

    SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnit).build();
    MoPubSingleton.getInstance()
        .initializeMoPubSDK(
            context,
            sdkConfiguration,
            new SdkInitializationListener() {
              @Override
              public void onInitializationFinished() {
                moPubNative.makeRequest(requestParameters);
              }
            });

    // Forwarding MoPub's impression and click events to AdMob.
    mMoPubNativeEventListener =
        new NativeAd.MoPubNativeEventListener() {

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
  public void requestBannerAd(
      final Context context,
      MediationBannerListener mediationBannerListener,
      Bundle bundle,
      final AdSize adSize,
      MediationAdRequest mediationAdRequest,
      final Bundle mediationExtras) {
    mContext = context;
    mAdSize = adSize;
    mExtras = mediationExtras;

    String adUnit = bundle.getString(MOPUB_AD_UNIT_KEY);
    if (TextUtils.isEmpty(adUnit)) {
      String errorMessage =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid MoPub Ad Unit ID.");
      Log.w(TAG, errorMessage);
      mediationBannerListener.onAdFailedToLoad(MoPubAdapter.this, ERROR_INVALID_SERVER_PARAMETERS);
      return;
    }

    mMoPubView = new MoPubView(context);
    mMoPubView.setBannerAdListener(new MBannerListener(mediationBannerListener));
    mMoPubView.setAdUnitId(adUnit);

    // If test mode is enabled
    if (mediationAdRequest.isTesting()) {
      mMoPubView.setTesting(true);
    }

    // If location is available
    if (mediationAdRequest.getLocation() != null) {
      mMoPubView.setLocation(mediationAdRequest.getLocation());
    }

    mMoPubView.setKeywords(getKeywords(mediationAdRequest, false));
    mMoPubView.setUserDataKeywords(getKeywords(mediationAdRequest, true));

    SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnit).build();
    MoPubSingleton.getInstance()
        .initializeMoPubSDK(
            context,
            sdkConfiguration,
            new SdkInitializationListener() {
              @Override
              public void onInitializationFinished() {
                mMoPubView.loadAd();
              }
            });
  }

  @Override
  public View getBannerView() {
    return mMoPubView;
  }

  // Keywords passed from AdMob are separated into 1) personally identifiable, and 2) non-personally
  // identifiable categories before they are forwarded to MoPub due to GDPR.
  public static String getKeywords(MediationAdRequest mediationAdRequest, boolean intendedForPII) {

    Date birthday = mediationAdRequest.getBirthday();
    String ageString = "";

    if (birthday != null) {
      int ageInt = getAge(birthday);
      ageString = "m_age:" + ageInt;
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

    keywordsBuilder =
        keywordsBuilder
            .append(MOPUB_NATIVE_CEVENT_VERSION)
            .append(",")
            .append(ageString)
            .append(",")
            .append(genderString);

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
    return mediationAdRequest.getBirthday() != null
        || mediationAdRequest.getGender() != -1
        || mediationAdRequest.getLocation() != null;
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
    public void onBannerFailed(MoPubView moPubView, MoPubErrorCode moPubErrorCode) {
      String errorSDKMessage = createSDKError(moPubErrorCode);
      Log.w(TAG, errorSDKMessage);
      mMediationBannerListener.onAdFailedToLoad(
          MoPubAdapter.this, getMediationErrorCode(moPubErrorCode));
    }

    @Override
    public void onBannerLoaded(MoPubView moPubView) {
      // If the publisher provides a minimum ad size to be loaded, then that size will be verified
      // against the ad size returned by MoPub.
      if (mExtras != null) {
        int minimumWidth = mExtras.getInt(BundleBuilder.ARG_MINIMUM_BANNER_WIDTH, 0);
        int minimumHeight = mExtras.getInt(BundleBuilder.ARG_MINIMUM_BANNER_HEIGHT, 0);

        if (minimumWidth > 0
            && minimumHeight > 0
            && (moPubView.getAdWidth() < minimumWidth || moPubView.getAdHeight() < minimumHeight)) {
          String errorMessage =
              String.format(
                  "The loaded ad was smaller than the minimum required banner size. "
                      + "Loaded size: %dx%d, minimum size: %dx%d",
                  moPubView.getAdWidth(), moPubView.getAdHeight(), minimumWidth, minimumHeight);
          String logMessage = createAdapterError(ERROR_MINIMUM_BANNER_SIZE, errorMessage);
          Log.e(TAG, logMessage);
          mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this, ERROR_MINIMUM_BANNER_SIZE);
          return;
        }
      }

      ArrayList<AdSize> potentials = new ArrayList<>();
      potentials.add(new AdSize(moPubView.getAdWidth(), moPubView.getAdHeight()));
      AdSize supportedAdSize = MediationUtils.findClosestSize(mContext, mAdSize, potentials);
      if (supportedAdSize == null) {
        // AdSize.SMART_BANNER returns -1 and -2 for getWidth() and getHeight().
        // Use getWidthInPixels() and getHeightInPixels() and divide by density instead.
        float density = mContext.getResources().getDisplayMetrics().density;
        int requestedAdWidth = Math.round(mAdSize.getWidthInPixels(mContext) / density);
        int requestedAdHeight = Math.round(mAdSize.getHeightInPixels(mContext) / density);

        String errorMessage =
            String.format(
                "The loaded ad is not large enough to match the requested banner size. "
                    + "To allow smaller banner sizes to fill this request, "
                    + "call MoPubAdapter.BundleBuilder.setMinimumBannerWidth() and "
                    + "MoPubAdapter.BundleBuilder.setMinimumBannerHeight(), and pass MoPub extras "
                    + "into an ad request by calling AdRequest.Builder().addNetworkExtrasBundle("
                    + "MoPubAdapter.class, MoPubAdapter.BundleBuilder.build()).build(). "
                    + "Loaded ad size: %dx%d, requested size: %dx%d",
                moPubView.getAdWidth(),
                moPubView.getAdHeight(),
                requestedAdWidth,
                requestedAdHeight);
        String logMessage = createAdapterError(ERROR_BANNER_SIZE_MISMATCH, errorMessage);
        Log.w(TAG, logMessage);
        mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this, ERROR_BANNER_SIZE_MISMATCH);
        return;
      }

      mMediationBannerListener.onAdLoaded(MoPubAdapter.this);
    }
  }

  @Override
  public void requestInterstitialAd(
      Context context,
      MediationInterstitialListener mediationInterstitialListener,
      Bundle bundle,
      MediationAdRequest mediationAdRequest,
      Bundle bundle1) {

    if (!(context instanceof Activity)) {
      String errorMessage =
          createAdapterError(
              ERROR_REQUIRES_ACTIVITY_CONTEXT,
              "MoPub SDK requires an Activity context to load interstitial ads.");
      Log.e(TAG, errorMessage);
      mediationInterstitialListener.onAdFailedToLoad(
          MoPubAdapter.this, ERROR_REQUIRES_ACTIVITY_CONTEXT);
      return;
    }

    String adUnit = bundle.getString(MOPUB_AD_UNIT_KEY);
    if (TextUtils.isEmpty(adUnit)) {
      String errorMessage =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid MoPub Ad Unit ID.");
      Log.e(TAG, errorMessage);
      mediationInterstitialListener.onAdFailedToLoad(
          MoPubAdapter.this, ERROR_INVALID_SERVER_PARAMETERS);
      return;
    }

    mMediationInterstitialListener = mediationInterstitialListener;

    mMoPubInterstitial = new MoPubInterstitial((Activity) context, adUnit);
    mMoPubInterstitial.setInterstitialAdListener(
        new mMediationInterstitialListener(mMediationInterstitialListener));

    // If test mode is enabled
    if (mediationAdRequest.isTesting()) {
      mMoPubInterstitial.setTesting(true);
    }

    mMoPubInterstitial.setKeywords(getKeywords(mediationAdRequest, false));
    mMoPubInterstitial.setKeywords(getKeywords(mediationAdRequest, true));

    SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnit).build();
    MoPubSingleton.getInstance()
        .initializeMoPubSDK(
            context,
            sdkConfiguration,
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
      MoPubLog.i("Interstitial was not ready. Unable to load the interstitial.");
      if (mMediationInterstitialListener != null) {
        mMediationInterstitialListener.onAdOpened(MoPubAdapter.this);
        mMediationInterstitialListener.onAdClosed(MoPubAdapter.this);
      }
    }
  }

  private class mMediationInterstitialListener implements MoPubInterstitial.InterstitialAdListener {

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
    public void onInterstitialFailed(
        MoPubInterstitial moPubInterstitial, MoPubErrorCode moPubErrorCode) {
      String errorSDKMessage = createSDKError(moPubErrorCode);
      Log.w(TAG, errorSDKMessage);
      mMediationInterstitialListener.onAdFailedToLoad(
          MoPubAdapter.this, getMediationErrorCode(moPubErrorCode));
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
   * The {@link BundleBuilder} class is used to create a NetworkExtras bundle which can be passed to
   * the adapter to make network-specific customizations.
   */
  public static final class BundleBuilder {

    /** Key to add and obtain {@link #mPrivacyIconSizeDp}. */
    private static final String ARG_PRIVACY_ICON_SIZE_DP = "privacy_icon_size_dp";

    /** Key to add and obtain {@link #mMinimumBannerWidth}. */
    private static final String ARG_MINIMUM_BANNER_WIDTH = "minimum_banner_width";

    /** Key to add and obtain {@link #mMinimumBannerHeight}. */
    private static final String ARG_MINIMUM_BANNER_HEIGHT = "minimum_banner_height";

    /** Key to add and obtain {@link #customRewardData}. */
    public static final String ARG_CUSTOM_REWARD_DATA = "custom_reward_data";

    /** MoPub's privacy icon size in dp. */
    private int mPrivacyIconSizeDp;

    /** Minimum allowable MoPub banner width. */
    private int mMinimumBannerWidth;

    /** Minimum allowable MoPub banner height. */
    private int mMinimumBannerHeight;

    /** Custom reward data for MoPub Rewarded Ads. */
    private String customRewardData;

    /** Sets the privacy icon size in dp. */
    public BundleBuilder setPrivacyIconSize(int iconSizeDp) {
      mPrivacyIconSizeDp = iconSizeDp;
      return BundleBuilder.this;
    }

    /** Sets the minimum allowable MoPub banner width. */
    public BundleBuilder setMinimumBannerWidth(int width) {
      mMinimumBannerWidth = width;
      return BundleBuilder.this;
    }

    /** Sets the minimum allowable MoPub banner height. */
    public BundleBuilder setMinimumBannerHeight(int height) {
      mMinimumBannerHeight = height;
      return BundleBuilder.this;
    }

    /** Sets the custom reward data for MoPub Rewarded Ads. */
    public BundleBuilder setCustomRewardData(@NonNull String customRewardData) {
      this.customRewardData = customRewardData;
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
      bundle.putInt(ARG_MINIMUM_BANNER_WIDTH, mMinimumBannerWidth);
      bundle.putInt(ARG_MINIMUM_BANNER_HEIGHT, mMinimumBannerHeight);
      bundle.putString(ARG_CUSTOM_REWARD_DATA, customRewardData);
      return bundle;
    }
  }
}
