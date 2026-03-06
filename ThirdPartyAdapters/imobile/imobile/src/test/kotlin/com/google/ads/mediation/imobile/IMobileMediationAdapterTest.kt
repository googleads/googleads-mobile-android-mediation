package com.google.ads.mediation.imobile

import android.app.Activity
import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.imobile.AdapterHelper.getAdapterVersion
import com.google.ads.mediation.imobile.Constants.KEY_MEDIA_ID
import com.google.ads.mediation.imobile.Constants.KEY_PUBLISHER_ID
import com.google.ads.mediation.imobile.Constants.KEY_SPOT_ID
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_BANNER_SIZE_MISMATCH
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
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
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
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
  private val iMobileSdkWrapper: IMobileSdkWrapper = mock()

  @Before
  fun setUp() {
    adapter = IMobileMediationAdapter(iMobileSdkWrapper)
  }

  @Test
  fun instanceOfIMobileMediationAdapter_returnsAnInstanceOfAdapter() {
    assertThat(adapter is Adapter).isTrue()
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

  private companion object {
    const val PUBLISHER_ID = "a_publisher_id"
    const val MEDIA_ID = "a_media_id"
    const val SPOT_ID = "a_spot_id"
  }
}
