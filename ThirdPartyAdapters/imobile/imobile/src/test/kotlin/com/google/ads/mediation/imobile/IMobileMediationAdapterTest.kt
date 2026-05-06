package com.google.ads.mediation.imobile

import android.app.Activity
import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.imobile.AdapterHelper.getAdapterVersion
import com.google.ads.mediation.imobile.Constants.KEY_MEDIA_ID
import com.google.ads.mediation.imobile.Constants.KEY_PUBLISHER_ID
import com.google.ads.mediation.imobile.Constants.KEY_SPOT_ID
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_USER_IS_AGE_RESTRICTED
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_USER_IS_AGE_RESTRICTED_MSG
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.Adapter
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [IMobileMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class IMobileMediationAdapterTest {

  private lateinit var adapter: IMobileMediationAdapter

  private val initializationCompleteCallback: InitializationCompleteCallback = mock()
  private val mediationConfiguration: MediationConfiguration = mock()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val mockBannerAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()
  private val mockBannerAdConfiguration: MediationBannerAdConfiguration = mock()
  private val mockInterstitialAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock()
  private val mockInterstitialAdConfiguration: MediationInterstitialAdConfiguration = mock()
  private val mediationNativeAdConfig: MediationNativeAdConfiguration = mock()
  private val nativeAdLoadCallback:
    MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> =
    mock()
  private val iMobileSdkWrapper: IMobileSdkWrapper = mock()
  private val mediationUtils: MediationUtilsWrapper = mock()

  @Before
  fun setUp() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    adapter = IMobileMediationAdapter(iMobileSdkWrapper, mediationUtils)
  }

  @Test
  fun instanceOfIMobileMediationAdapter_returnsAnInstanceOfAdapter() {
    assertIs<Adapter>(adapter)
  }

  @Test
  fun getVersionInfo_invalid3Digits_returnsZeros() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_invalidString_returnsZeros() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "foobar"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_valid4Digits_returnsValid() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_valid5Digits_returnsValid() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4.5"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getSDKVersionInfo_returnsDefault() {
    adapter.assertGetSdkVersion(expectedValue = "0.0.0")
  }

  @Test
  fun initialize_success() {
    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
    verify(initializationCompleteCallback).onInitializationSucceeded()
  }

  @Test
  fun initialize_withTFCDAndTFUAFalse_invokesOnInitializationSucceeded() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.mediationAdapterInitializeVerifySuccess(
      context,
      initializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
    )
  }

  @Test
  fun initialize_withTFCDTrue_invokesOnInitializationFailed() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.mediationAdapterInitializeVerifyFailure(
      context,
      initializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
      ERROR_USER_IS_AGE_RESTRICTED_MSG,
    )
  }

  @Test
  fun initialize_withTFUATrue_invokesOnInitializationFailed() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.mediationAdapterInitializeVerifyFailure(
      context,
      initializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
      ERROR_USER_IS_AGE_RESTRICTED_MSG,
    )
  }

  @Test
  fun loadBannerAd_withTFCDTrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, ERROR_USER_IS_AGE_RESTRICTED_MSG, ERROR_DOMAIN)

    adapter.loadBannerAd(mockBannerAdConfiguration, mockBannerAdLoadCallback)

    verify(mockBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadBannerAd_withTFUATrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, ERROR_USER_IS_AGE_RESTRICTED_MSG, ERROR_DOMAIN)

    adapter.loadBannerAd(mockBannerAdConfiguration, mockBannerAdLoadCallback)

    verify(mockBannerAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadBannerAd_ifContextIsNotActivity_fails() {
    whenever(mockBannerAdConfiguration.context) doReturn context
    val adErrorCaptor = argumentCaptor<AdError>()

    adapter.loadBannerAd(mockBannerAdConfiguration, mockBannerAdLoadCallback)

    verify(mockBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_REQUIRES_ACTIVITY_CONTEXT)
    assertThat(adError.domain).isEqualTo(IMobileMediationAdapter.ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_ifRequestedBannerSizeIsNotSupported_fails() {
    whenever(mockBannerAdConfiguration.context) doReturn activity
    whenever(mockBannerAdConfiguration.adSize) doReturn AdSize(1, 1) // Unsupported size
    val adErrorCaptor = argumentCaptor<AdError>()

    adapter.loadBannerAd(mockBannerAdConfiguration, mockBannerAdLoadCallback)

    verify(mockBannerAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_BANNER_SIZE_MISMATCH)
    assertThat(adError.domain).isEqualTo(IMobileMediationAdapter.ERROR_DOMAIN)
  }

  @Test
  fun loadBannerAd_loadsIMobileBannerAd() {
    whenever(mockBannerAdConfiguration.context) doReturn activity
    whenever(mockBannerAdConfiguration.adSize) doReturn AdSize.BANNER
    whenever(mediationUtils.findClosestSize(eq(activity), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER
    val serverParams =
      bundleOf(KEY_PUBLISHER_ID to PUBLISHER_ID, KEY_MEDIA_ID to MEDIA_ID, KEY_SPOT_ID to SPOT_ID)
    whenever(mockBannerAdConfiguration.serverParameters) doReturn serverParams

    adapter.loadBannerAd(mockBannerAdConfiguration, mockBannerAdLoadCallback)

    verify(iMobileSdkWrapper).registerSpotInline(activity, PUBLISHER_ID, MEDIA_ID, SPOT_ID)
    verify(iMobileSdkWrapper).start(SPOT_ID)
    verify(iMobileSdkWrapper).setImobileSdkAdListener(eq(SPOT_ID), any())
    verify(iMobileSdkWrapper).showAdForAdMobMediation(eq(activity), eq(SPOT_ID), any(), any())
  }

  @Test
  fun loadInterstitialAd_withTFCDTrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, ERROR_USER_IS_AGE_RESTRICTED_MSG, ERROR_DOMAIN)

    adapter.loadInterstitialAd(mockInterstitialAdConfiguration, mockInterstitialAdLoadCallback)

    verify(mockInterstitialAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadInterstitialAd_withTFUATrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, ERROR_USER_IS_AGE_RESTRICTED_MSG, ERROR_DOMAIN)

    adapter.loadInterstitialAd(mockInterstitialAdConfiguration, mockInterstitialAdLoadCallback)

    verify(mockInterstitialAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadInterstitialAd_ifContextIsNotActivity_fails() {
    whenever(mockInterstitialAdConfiguration.context) doReturn context
    val adErrorCaptor = argumentCaptor<AdError>()

    adapter.loadInterstitialAd(mockInterstitialAdConfiguration, mockInterstitialAdLoadCallback)

    verify(mockInterstitialAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_REQUIRES_ACTIVITY_CONTEXT)
    assertThat(adError.domain).isEqualTo(IMobileMediationAdapter.ERROR_DOMAIN)
  }

  @Test
  fun loadInterstitialAd_loadsIMobileInterstitialAd() {
    whenever(mockInterstitialAdConfiguration.context) doReturn activity
    val serverParams =
      bundleOf(KEY_PUBLISHER_ID to PUBLISHER_ID, KEY_MEDIA_ID to MEDIA_ID, KEY_SPOT_ID to SPOT_ID)
    whenever(mockInterstitialAdConfiguration.serverParameters) doReturn serverParams

    adapter.loadInterstitialAd(mockInterstitialAdConfiguration, mockInterstitialAdLoadCallback)

    verify(iMobileSdkWrapper).registerSpotFullScreen(activity, PUBLISHER_ID, MEDIA_ID, SPOT_ID)
    verify(iMobileSdkWrapper).setImobileSdkAdListener(eq(SPOT_ID), any())
    verify(iMobileSdkWrapper).start(SPOT_ID)
  }

  @Test
  fun loadInterstitialAd_ifAdIsAlreadyReadyForShow_callsLoadSuccessCallback() {
    whenever(mockInterstitialAdConfiguration.context) doReturn activity
    val serverParams =
      bundleOf(KEY_PUBLISHER_ID to PUBLISHER_ID, KEY_MEDIA_ID to MEDIA_ID, KEY_SPOT_ID to SPOT_ID)
    whenever(mockInterstitialAdConfiguration.serverParameters) doReturn serverParams
    whenever(iMobileSdkWrapper.isShowAd(SPOT_ID)) doReturn true

    adapter.loadInterstitialAd(mockInterstitialAdConfiguration, mockInterstitialAdLoadCallback)

    verify(iMobileSdkWrapper).registerSpotFullScreen(activity, PUBLISHER_ID, MEDIA_ID, SPOT_ID)
    verify(iMobileSdkWrapper).setImobileSdkAdListener(eq(SPOT_ID), any())
    verify(iMobileSdkWrapper, never()).start(any())
    verify(mockInterstitialAdLoadCallback).onSuccess(any())
  }

  @Test
  fun loadNativeAdMapper_withTFCDTrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    whenever(mediationNativeAdConfig.context) doReturn context
    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, ERROR_USER_IS_AGE_RESTRICTED_MSG, ERROR_DOMAIN)

    adapter.loadNativeAdMapper(mediationNativeAdConfig, nativeAdLoadCallback)

    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadNativeAdMapper_withTFUATrue_invokesOnFailure() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    whenever(mediationNativeAdConfig.context) doReturn context
    val expectedAdError =
      AdError(ERROR_USER_IS_AGE_RESTRICTED, ERROR_USER_IS_AGE_RESTRICTED_MSG, ERROR_DOMAIN)

    adapter.loadNativeAdMapper(mediationNativeAdConfig, nativeAdLoadCallback)

    verify(nativeAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadNativeAdMapper_ifContextIsNotActivity_fails() {
    whenever(mediationNativeAdConfig.context) doReturn context

    adapter.loadNativeAdMapper(mediationNativeAdConfig, nativeAdLoadCallback)

    val adErrorCaptor = argumentCaptor<AdError>()
    verify(nativeAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_REQUIRES_ACTIVITY_CONTEXT)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun loadNativeAdMapper_loadsIMobileNativeAdData() {
    whenever(mediationNativeAdConfig.context) doReturn activity
    val serverParams =
      bundleOf(KEY_PUBLISHER_ID to PUBLISHER_ID, KEY_MEDIA_ID to MEDIA_ID, KEY_SPOT_ID to SPOT_ID)
    whenever(mediationNativeAdConfig.serverParameters) doReturn serverParams

    adapter.loadNativeAdMapper(mediationNativeAdConfig, nativeAdLoadCallback)

    verify(iMobileSdkWrapper).registerSpotInline(activity, PUBLISHER_ID, MEDIA_ID, SPOT_ID)
    verify(iMobileSdkWrapper).start(SPOT_ID)
    verify(iMobileSdkWrapper).getNativeAdData(eq(activity), eq(SPOT_ID), any())
  }

  private companion object {
    const val PUBLISHER_ID = "a_publisher_id"
    const val MEDIA_ID = "a_media_id"
    const val SPOT_ID = "a_spot_id"
  }
}
