// Copyright 2025 Google LLC
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

package com.google.ads.mediation.pubmatic

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.adaptertestkit.createMediationRewardedAdConfiguration
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_INVALID_AD_FORMAT
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_INVALID_BANNER_AD_SIZE
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_INVALID_BANNER_AD_SIZE_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_AD_UNIT_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_AD_UNIT_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_OR_INVALID_PROFILE_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_MISSING_PUBLISHER_ID_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_NULL_REWARDED_AD
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_NULL_REWARDED_AD_MSG
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_AD_UNIT
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_PROFILE_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.KEY_PUBLISHER_ID
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
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
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.common.truth.Truth.assertThat
import com.pubmatic.sdk.common.OpenWrapSDK
import com.pubmatic.sdk.common.OpenWrapSDK.initialize
import com.pubmatic.sdk.common.OpenWrapSDK.setCoppa
import com.pubmatic.sdk.common.OpenWrapSDKConfig
import com.pubmatic.sdk.common.OpenWrapSDKInitializer
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.nativead.POBNativeAdLoader
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import com.pubmatic.sdk.openwrap.core.POBConstants.KEY_POB_ADMOB_WATERMARK
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import com.pubmatic.sdk.openwrap.core.signal.POBSignalConfig
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial
import com.pubmatic.sdk.rewardedad.POBRewardedAd
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
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class PubMaticMediationAdapterTest {
  // Subject of testing
  private lateinit var adapter: PubMaticMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val openWrapSdkConfigCaptor = argumentCaptor<OpenWrapSDKConfig>()

  private val initializationCompleteCallback = mock<InitializationCompleteCallback>()

  private val adErrorStringCaptor = argumentCaptor<String>()

  private val adErrorCaptor = argumentCaptor<AdError>()

  private val openWrapSdkInitListenerCaptor = argumentCaptor<OpenWrapSDKInitializer.Listener>()

  private val signalCallbacks = mock<SignalCallbacks>()

  private val pobSignalConfigCaptor = argumentCaptor<POBSignalConfig>()

  private val pubMaticSignalGenerator = mock<PubMaticSignalGenerator>()

  private val pobBiddingHostCaptor = argumentCaptor<POBBiddingHost>()

  private val pobInterstitial = mock<POBInterstitial>()

  private val pobRewardedAd = mock<POBRewardedAd>()

  private val pobBannerView = mock<POBBannerView>()

  private val pobNativeAdLoader = mock<POBNativeAdLoader>()

  private val pubMaticAdFactory =
    mock<PubMaticAdFactory> {
      on { createPOBInterstitial(any()) } doReturn pobInterstitial
      on {
        createPOBInterstitial(context, TEST_PUBLISHER_ID, TEST_PROFILE_ID_1.toInt(), TEST_AD_UNIT)
      } doReturn pobInterstitial
      on { createPOBRewardedAd(any()) } doReturn pobRewardedAd
      on {
        createPOBRewardedAd(context, TEST_PUBLISHER_ID, TEST_PROFILE_ID_1.toInt(), TEST_AD_UNIT)
      } doReturn pobRewardedAd
      on { createPOBBannerView(any()) } doReturn pobBannerView
      on {
        createPOBBannerView(
          any(),
          eq(TEST_PUBLISHER_ID),
          eq(TEST_PROFILE_ID_1.toInt()),
          eq(TEST_AD_UNIT),
          any(),
        )
      } doReturn pobBannerView
      on { createPOBNativeAdLoader(any()) } doReturn pobNativeAdLoader
      on {
        createPOBNativeAdLoader(context, TEST_PUBLISHER_ID, TEST_PROFILE_ID_1.toInt(), TEST_AD_UNIT)
      } doReturn pobNativeAdLoader
    }

  @Before
  fun setUp() {
    adapter = PubMaticMediationAdapter(pubMaticSignalGenerator, pubMaticAdFactory)
  }

  // region Version tests
  @Test
  fun getSDKVersionInfo_returnsValidVersionInfo() {
    PubMaticMediationAdapter.pubMaticSdkVersionDelegate = "1.2.3"

    adapter.assertGetSdkVersion(expectedValue = "1.2.3")

    // Resetting value
    PubMaticMediationAdapter.pubMaticSdkVersionDelegate = null
  }

  @Test
  fun getVersionInfo_returnsValidVersionInfo() {
    PubMaticMediationAdapter.adapterVersionDelegate = "1.2.3.4"

    adapter.assertGetVersionInfo(expectedValue = "1.2.304")

    // Resetting value
    PubMaticMediationAdapter.adapterVersionDelegate = null
  }

  // endregion

  // region initialize() tests

  @Test
  fun initialize_whenTFCDIsTrue_setsOpenWrapCoppaTrue() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build()
    )

    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      adapter.initialize(
        context = context,
        initializationCompleteCallback = mock(),
        mediationConfigurations = emptyList(),
      )

      openWrapSdk.verify { setCoppa(eq(true)) }
    }
  }

  @Test
  fun initialize_whenTFCDIsFalse_setsOpenWrapCoppaFalse() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        .build()
    )

    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      adapter.initialize(
        context = context,
        initializationCompleteCallback = mock(),
        mediationConfigurations = emptyList(),
      )

      openWrapSdk.verify { setCoppa(eq(false)) }
    }
  }

  @Test
  fun initialize_whenTFCDIsUnset_doesNotSetOpenWrapCoppa() {
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .build()
    )

    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      adapter.initialize(
        context = context,
        initializationCompleteCallback = mock(),
        mediationConfigurations = emptyList(),
      )

      openWrapSdk.verify({ setCoppa(any()) }, times(0))
    }
  }

  @Test
  fun initialize_initializesOpenWrapSdk() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, serverParameters)

      adapter.initialize(context, mock(), listOf(mediationConfiguration))

      openWrapSdk.verify { initialize(eq(context), openWrapSdkConfigCaptor.capture(), any()) }
      val openWrapSdkConfig = openWrapSdkConfigCaptor.firstValue
      assertThat(openWrapSdkConfig.publisherId).isEqualTo(TEST_PUBLISHER_ID)
      assertThat(openWrapSdkConfig.profileIds.size).isEqualTo(1)
      assertThat(openWrapSdkConfig.profileIds[0]).isEqualTo(TEST_PROFILE_ID_1.toInt())
    }
  }

  @Test
  fun initialize_ifPublisherIdIsMissing_fails() {
    val serverParameters = bundleOf(PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1)
    val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, serverParameters)

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    val expectedError =
      AdError(ERROR_MISSING_PUBLISHER_ID, "Publisher ID is missing.", ADAPTER_ERROR_DOMAIN)
    verify(initializationCompleteCallback).onInitializationFailed(adErrorStringCaptor.capture())
    assertThat(adErrorStringCaptor.firstValue).isEqualTo(expectedError.toString())
  }

  @Test
  fun initialize_withMultipleProfileIds_initializesOpenWrapSdkWithMultipleProfileIds() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters1 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration1 = MediationConfiguration(AdFormat.BANNER, serverParameters1)
      val serverParameters2 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_2,
        )
      val mediationConfiguration2 = MediationConfiguration(AdFormat.BANNER, serverParameters2)

      adapter.initialize(context, mock(), listOf(mediationConfiguration1, mediationConfiguration2))

      openWrapSdk.verify { initialize(eq(context), openWrapSdkConfigCaptor.capture(), any()) }
      val openWrapSdkConfig = openWrapSdkConfigCaptor.firstValue
      assertThat(openWrapSdkConfig.publisherId).isEqualTo(TEST_PUBLISHER_ID)
      assertThat(openWrapSdkConfig.profileIds.size).isEqualTo(2)
      assertThat(openWrapSdkConfig.profileIds).contains(TEST_PROFILE_ID_1.toInt())
      assertThat(openWrapSdkConfig.profileIds).contains(TEST_PROFILE_ID_2.toInt())
    }
  }

  @Test
  fun initialize_ifAProfileIdIsNotInt_stillInitializesSuccessfullyWithOtherProfileIds() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters1 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration1 = MediationConfiguration(AdFormat.BANNER, serverParameters1)
      val serverParameters2 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_2,
        )
      val mediationConfiguration2 = MediationConfiguration(AdFormat.BANNER, serverParameters2)
      val serverParameters3 =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to INVALID_PROFILE_ID,
        )
      val mediationConfiguration3 = MediationConfiguration(AdFormat.BANNER, serverParameters3)

      adapter.initialize(
        context,
        mock(),
        listOf(mediationConfiguration1, mediationConfiguration2, mediationConfiguration3),
      )

      openWrapSdk.verify { initialize(eq(context), openWrapSdkConfigCaptor.capture(), any()) }
      val openWrapSdkConfig = openWrapSdkConfigCaptor.firstValue
      assertThat(openWrapSdkConfig.publisherId).isEqualTo(TEST_PUBLISHER_ID)
      assertThat(openWrapSdkConfig.profileIds.size).isEqualTo(2)
      assertThat(openWrapSdkConfig.profileIds).contains(TEST_PROFILE_ID_1.toInt())
      assertThat(openWrapSdkConfig.profileIds).contains(TEST_PROFILE_ID_2.toInt())
    }
  }

  @Test
  fun initialize_ifOpenWrapInitializationSucceeds_invokesInitializationSucceededCallback() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, serverParameters)

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
      openWrapSdk.verify {
        initialize(
          eq(context),
          openWrapSdkConfigCaptor.capture(),
          openWrapSdkInitListenerCaptor.capture(),
        )
      }
      val openWrapSdkInitListener = openWrapSdkInitListenerCaptor.firstValue
      // Let OpenWrap SDK init succeed.
      openWrapSdkInitListener.onSuccess()

      verify(initializationCompleteCallback).onInitializationSucceeded()
    }
  }

  @Test
  fun initialize_ifOpenWrapInitializationFails_invokesInitializationFailedCallback() {
    mockStatic(OpenWrapSDK::class.java).use { openWrapSdk ->
      val serverParameters =
        bundleOf(
          PubMaticMediationAdapter.KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
          PubMaticMediationAdapter.KEY_PROFILE_ID to TEST_PROFILE_ID_1,
        )
      val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, serverParameters)

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
      openWrapSdk.verify {
        initialize(
          eq(context),
          openWrapSdkConfigCaptor.capture(),
          openWrapSdkInitListenerCaptor.capture(),
        )
      }
      val openWrapSdkInitListener = openWrapSdkInitListenerCaptor.firstValue
      // Let OpenWrap SDK init failure.
      val openWrapSdkInitError = POBError(OPENWRAP_INIT_ERROR_CODE, OPENWRAP_INIT_ERROR_MSG)
      openWrapSdkInitListener.onFailure(openWrapSdkInitError)

      val expectedError =
        AdError(
          OPENWRAP_INIT_ERROR_CODE,
          "INVALID_REQUEST: $OPENWRAP_INIT_ERROR_MSG",
          SDK_ERROR_DOMAIN,
        )
      verify(initializationCompleteCallback).onInitializationFailed(adErrorStringCaptor.capture())
      assertThat(adErrorStringCaptor.firstValue).isEqualTo(expectedError.toString())
    }
  }

  // endregion

  // region signal collection tests

  @Test
  fun collectSignals_setsAdmobAsBiddingHostAndCollectsPubMaticSignals() {
    whenever(pubMaticSignalGenerator.generateSignal(any(), any(), any()))
      .thenReturn(PUBMATIC_SIGNALS)
    val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, bundleOf())
    val rtbSignalData = RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), null)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(pubMaticSignalGenerator).generateSignal(any(), pobBiddingHostCaptor.capture(), any())
    assertThat(pobBiddingHostCaptor.firstValue).isEqualTo(POBBiddingHost.ADMOB)
    verify(signalCallbacks).onSuccess(eq(PUBMATIC_SIGNALS))
  }

  @Test
  fun collectSignals_forBannerFormat_generatesPubMaticSignalsForBanner() {
    val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, bundleOf())
    val rtbSignalData =
      RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), AdSize.BANNER)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(pubMaticSignalGenerator).generateSignal(any(), any(), pobSignalConfigCaptor.capture())
    val pobSignalConfig = pobSignalConfigCaptor.firstValue
    assertThat(pobSignalConfig.adFormat).isEqualTo(POBAdFormat.BANNER)
  }

  @Test
  fun collectSignals_forMRECFormat_generatesPubMaticSignalsForMREC() {
    val mediationConfiguration = MediationConfiguration(AdFormat.BANNER, bundleOf())
    val rtbSignalData =
      RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), AdSize.MEDIUM_RECTANGLE)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(pubMaticSignalGenerator).generateSignal(any(), any(), pobSignalConfigCaptor.capture())
    val pobSignalConfig = pobSignalConfigCaptor.firstValue
    assertThat(pobSignalConfig.adFormat).isEqualTo(POBAdFormat.MREC)
  }

  @Test
  fun collectSignals_forInterstitialFormat_generatesPubMaticSignalsForInterstitial() {
    val mediationConfiguration = MediationConfiguration(AdFormat.INTERSTITIAL, bundleOf())
    val rtbSignalData = RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), null)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(pubMaticSignalGenerator).generateSignal(any(), any(), pobSignalConfigCaptor.capture())
    val pobSignalConfig = pobSignalConfigCaptor.firstValue
    assertThat(pobSignalConfig.adFormat).isEqualTo(POBAdFormat.INTERSTITIAL)
  }

  @Test
  fun collectSignals_forRewardedFormat_generatesPubMaticSignalsForRewarded() {
    val mediationConfiguration = MediationConfiguration(AdFormat.REWARDED, bundleOf())
    val rtbSignalData = RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), null)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(pubMaticSignalGenerator).generateSignal(any(), any(), pobSignalConfigCaptor.capture())
    val pobSignalConfig = pobSignalConfigCaptor.firstValue
    assertThat(pobSignalConfig.adFormat).isEqualTo(POBAdFormat.REWARDEDAD)
  }

  @Test
  fun collectSignals_forNativeFormat_generatesPubMaticSignalsForNativeFormat() {
    val mediationConfiguration = MediationConfiguration(AdFormat.NATIVE, bundleOf())
    val rtbSignalData = RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), null)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(pubMaticSignalGenerator).generateSignal(any(), any(), pobSignalConfigCaptor.capture())
    val pobSignalConfig = pobSignalConfigCaptor.firstValue
    assertThat(pobSignalConfig.adFormat).isEqualTo(POBAdFormat.NATIVE)
  }

  @Test
  fun collectSignals_forFormatNotSupportedByPubMatic_returnsInvalidFormatError() {
    val mediationConfiguration = MediationConfiguration(AdFormat.APP_OPEN_AD, bundleOf())
    val rtbSignalData = RtbSignalData(context, listOf(mediationConfiguration), bundleOf(), null)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(signalCallbacks).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_INVALID_AD_FORMAT)
    assertThat(adError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun collectSignals_ifFormatIsMissingInRtbSignalData_returnsInvalidFormatError() {
    val rtbSignalData = RtbSignalData(context, listOf(), bundleOf(), null)

    adapter.collectSignals(rtbSignalData, signalCallbacks)

    verify(signalCallbacks).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_INVALID_AD_FORMAT)
    assertThat(adError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  // endregion

  // region Waterfall ad load tests.

  @Test
  fun loadInterstitialAd_loadsPubMaticInterstitial() {
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to TEST_PROFILE_ID_1,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )

    adapter.loadInterstitialAd(mediationInterstitialAdConfiguration, mock())

    verify(pobInterstitial).setListener(any())
    verify(pobInterstitial, never()).loadAd(any(), any())
    verify(pobInterstitial).loadAd()
  }

  @Test
  fun loadInterstitialAd_withMissingPublisherId_invokesOnFailure() {
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context,
        serverParameters =
          bundleOf(KEY_PROFILE_ID to TEST_PROFILE_ID_1, KEY_AD_UNIT to TEST_AD_UNIT),
      )
    val mockCallback =
      mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
    val expectedAdError =
      AdError(ERROR_MISSING_PUBLISHER_ID, ERROR_MISSING_PUBLISHER_ID_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadInterstitialAd(mediationInterstitialAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadInterstitialAd_withInvalidProfileId_invokesOnFailure() {
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to INVALID_PROFILE_ID,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )
    val mockCallback =
      mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
    val extectedAdError =
      AdError(
        ERROR_MISSING_OR_INVALID_PROFILE_ID,
        ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG,
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadInterstitialAd(mediationInterstitialAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(extectedAdError)))
  }

  @Test
  fun loadInterstitialAd_withMissingAdUnit_invokesOnFailure() {
    val mediationInterstitialAdConfiguration =
      createMediationInterstitialAdConfiguration(
        context,
        serverParameters =
          bundleOf(KEY_PUBLISHER_ID to TEST_PUBLISHER_ID, KEY_PROFILE_ID to TEST_PROFILE_ID_1),
      )
    val mockCallback =
      mock<MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>>()
    val expectedAdError =
      AdError(ERROR_MISSING_AD_UNIT_ID, ERROR_MISSING_AD_UNIT_ID_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadInterstitialAd(mediationInterstitialAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_loadsPubMaticRewardedAd() {
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        context,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to TEST_PROFILE_ID_1,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )

    adapter.loadRewardedAd(mediationRewardedAdConfiguration, mock())

    verify(pobRewardedAd).setListener(any())
    verify(pobRewardedAd, never()).loadAd(any(), any())
    verify(pobRewardedAd).loadAd()
  }

  @Test
  fun loadRewardedAd_withMissingPublisherId_invokesOnFailure() {
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        context,
        serverParameters =
          bundleOf(KEY_PROFILE_ID to TEST_PROFILE_ID_1, KEY_AD_UNIT to TEST_AD_UNIT),
      )
    val mockCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    val expectedAdError =
      AdError(ERROR_MISSING_PUBLISHER_ID, ERROR_MISSING_PUBLISHER_ID_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadRewardedAd(mediationRewardedAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_withInvalidProfileId_invokesOnFailure() {
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        context,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to INVALID_PROFILE_ID,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )
    val mockCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    val extectedAdError =
      AdError(
        ERROR_MISSING_OR_INVALID_PROFILE_ID,
        ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG,
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadRewardedAd(mediationRewardedAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(extectedAdError)))
  }

  @Test
  fun loadRewardedAd_withMissingAdUnit_invokesOnFailure() {
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        context,
        serverParameters =
          bundleOf(KEY_PUBLISHER_ID to TEST_PUBLISHER_ID, KEY_PROFILE_ID to TEST_PROFILE_ID_1),
      )
    val mockCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    val expectedAdError =
      AdError(ERROR_MISSING_AD_UNIT_ID, ERROR_MISSING_AD_UNIT_ID_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadRewardedAd(mediationRewardedAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_whenReturnedRewardedAdIsNull_invokesOnFailure() {
    whenever(pubMaticAdFactory.createPOBRewardedAd(any(), any(), any(), any())) doReturn null
    val mediationRewardedAdConfiguration =
      createMediationRewardedAdConfiguration(
        context,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to TEST_PROFILE_ID_1,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )
    val mockCallback =
      mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>>()
    val expectedAdError =
      AdError(ERROR_NULL_REWARDED_AD, ERROR_NULL_REWARDED_AD_MSG, SDK_ERROR_DOMAIN)

    adapter.loadRewardedAd(mediationRewardedAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadBannerAd_disablesAutoRefreshAndSetsWatermarkAndLoadsPubMaticBannerAd() {
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        context = context,
        isTesting = true,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to TEST_PROFILE_ID_1,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
        adSize = AdSize.BANNER,
      )

    adapter.loadBannerAd(mediationBannerAdConfiguration, mock())

    verify(pobBannerView).setListener(any())
    verify(pobBannerView).pauseAutoRefresh()
    verify(pobBannerView, never()).addExtraInfo(any(), any())
    verify(pobBannerView).loadAd()
  }

  @Test
  fun loadBannerAd_withMissingPublisherId_invokesOnFailure() {
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        context,
        serverParameters =
          bundleOf(KEY_PROFILE_ID to TEST_PROFILE_ID_1, KEY_AD_UNIT to TEST_AD_UNIT),
        adSize = AdSize.BANNER,
      )
    val mockCallback = mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
    val expectedAdError =
      AdError(ERROR_MISSING_PUBLISHER_ID, ERROR_MISSING_PUBLISHER_ID_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadBannerAd(mediationBannerAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadBannerAd_withInvalidProfileId_invokesOnFailure() {
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        context,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to INVALID_PROFILE_ID,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )
    val mockCallback = mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
    val extectedAdError =
      AdError(
        ERROR_MISSING_OR_INVALID_PROFILE_ID,
        ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG,
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadBannerAd(mediationBannerAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(extectedAdError)))
  }

  @Test
  fun loadBannerAd_withMissingAdUnit_invokesOnFailure() {
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        context,
        serverParameters =
          bundleOf(KEY_PUBLISHER_ID to TEST_PUBLISHER_ID, KEY_PROFILE_ID to TEST_PROFILE_ID_1),
      )
    val mockCallback = mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
    val expectedAdError =
      AdError(ERROR_MISSING_AD_UNIT_ID, ERROR_MISSING_AD_UNIT_ID_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadBannerAd(mediationBannerAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadBannerAd_whenInvalidAdSize_invokesOnFailure() {
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(
        context,
        adSize = AdSize(900, 900), // Invalid Ad Size for PubMatic
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to TEST_PROFILE_ID_1,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )
    val mockCallback = mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>>()
    val expectedAdError =
      AdError(ERROR_INVALID_BANNER_AD_SIZE, ERROR_INVALID_BANNER_AD_SIZE_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadBannerAd(mediationBannerAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadNativeAdMapper_disablesAutoRefreshAndSetsWatermarkAndLoadsPubMaticBannerAd() {
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        isTesting = true,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to TEST_PROFILE_ID_1,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )

    adapter.loadNativeAdMapper(mediationNativeAdConfiguration, mock())

    verify(pobNativeAdLoader).setAdLoaderListener(any())
    verify(pobNativeAdLoader, never()).addExtraInfo(any(), any())
    verify(pobNativeAdLoader).loadAd()
  }

  @Test
  fun loadNativeAdMapper_withMissingPublisherId_invokesOnFailure() {
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        isTesting = true,
        serverParameters =
          bundleOf(KEY_PROFILE_ID to TEST_PROFILE_ID_1, KEY_AD_UNIT to TEST_AD_UNIT),
      )
    val mockCallback = mock<MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>>()
    val expectedAdError =
      AdError(ERROR_MISSING_PUBLISHER_ID, ERROR_MISSING_PUBLISHER_ID_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadNativeAdMapper(mediationNativeAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadNativeAdMapper_withInvalidProfileId_invokesOnFailure() {
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        isTesting = true,
        serverParameters =
          bundleOf(
            KEY_PUBLISHER_ID to TEST_PUBLISHER_ID,
            KEY_PROFILE_ID to INVALID_PROFILE_ID,
            KEY_AD_UNIT to TEST_AD_UNIT,
          ),
      )
    val mockCallback = mock<MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>>()
    val expectedAdError =
      AdError(
        ERROR_MISSING_OR_INVALID_PROFILE_ID,
        ERROR_MISSING_OR_INVALID_PROFILE_ID_MSG,
        ADAPTER_ERROR_DOMAIN,
      )

    adapter.loadNativeAdMapper(mediationNativeAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadNativeAdMapper_withMissingAdUnit_invokesOnFailure() {
    val mediationNativeAdConfiguration =
      createMediationNativeAdConfiguration(
        context = context,
        isTesting = true,
        serverParameters =
          bundleOf(KEY_PUBLISHER_ID to TEST_PUBLISHER_ID, KEY_PROFILE_ID to TEST_PROFILE_ID_1),
      )
    val mockCallback = mock<MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>>()
    val expectedAdError =
      AdError(ERROR_MISSING_AD_UNIT_ID, ERROR_MISSING_AD_UNIT_ID_MSG, ADAPTER_ERROR_DOMAIN)

    adapter.loadNativeAdMapper(mediationNativeAdConfiguration, mockCallback)

    verify(mockCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  // endregion

  // region RTB ad load tests.

  @Test
  fun loadRtbInterstitialAd_setsWatermarkAndLoadsPubMaticInterstitial() {
    val mediationInterstitialAdConfiguration =
      MediationInterstitialAdConfiguration(
        context,
        BID_RESPONSE,
        /*serverParameters=*/ bundleOf(),
        /*mediationExtras=*/ bundleOf(),
        /*isTesting=*/ true,
        /*location=*/ null,
        TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        WATERMARK,
      )

    adapter.loadRtbInterstitialAd(mediationInterstitialAdConfiguration, mock())

    verify(pobInterstitial).setListener(any())
    verify(pobInterstitial).addExtraInfo(KEY_POB_ADMOB_WATERMARK, WATERMARK)
    verify(pobInterstitial).loadAd(BID_RESPONSE, POBBiddingHost.ADMOB)
  }

  @Test
  fun loadRtbRewardedAd_setsWatermarkAndLoadsPubMaticRewardedAd() {
    val mediationRewardedAdConfiguration =
      MediationRewardedAdConfiguration(
        context,
        BID_RESPONSE,
        /*serverParameters=*/ bundleOf(),
        /*mediationExtras=*/ bundleOf(),
        /*isTesting=*/ true,
        /*location=*/ null,
        TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        WATERMARK,
      )

    adapter.loadRtbRewardedAd(mediationRewardedAdConfiguration, mock())

    verify(pobRewardedAd).setListener(any())
    verify(pobRewardedAd).addExtraInfo(KEY_POB_ADMOB_WATERMARK, WATERMARK)
    verify(pobRewardedAd).loadAd(BID_RESPONSE, POBBiddingHost.ADMOB)
  }

  @Test
  fun loadRtbBannerAd_disablesAutoRefreshAndSetsWatermarkAndLoadsPubMaticBannerAd() {
    val mediationBannerAdConfiguration =
      MediationBannerAdConfiguration(
        context,
        BID_RESPONSE,
        /*serverParameters=*/ bundleOf(),
        /*mediationExtras=*/ bundleOf(),
        /*isTesting=*/ true,
        /*location=*/ null,
        TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        AdSize.BANNER,
        WATERMARK,
      )

    adapter.loadRtbBannerAd(mediationBannerAdConfiguration, mock())

    verify(pobBannerView).setListener(any())
    verify(pobBannerView).pauseAutoRefresh()
    verify(pobBannerView).addExtraInfo(KEY_POB_ADMOB_WATERMARK, WATERMARK)
    verify(pobBannerView).loadAd(BID_RESPONSE, POBBiddingHost.ADMOB)
  }

  @Test
  fun loadRtbNativeAd_setsWatermarkAndLoadsPubMaticNativeAd() {
    val mediationNativeAdConfiguration =
      MediationNativeAdConfiguration(
        context,
        BID_RESPONSE,
        /*serverParameters=*/ bundleOf(),
        /*mediationExtras=*/ bundleOf(),
        /*isTesting=*/ true,
        /*location=*/ null,
        TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
        TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
        /*maxAdContentRating=*/ "",
        WATERMARK,
        /*nativeAdOptions=*/ null,
      )

    adapter.loadRtbNativeAdMapper(mediationNativeAdConfiguration, mock())

    verify(pobNativeAdLoader).setAdLoaderListener(any())
    verify(pobNativeAdLoader).addExtraInfo(KEY_POB_ADMOB_WATERMARK, WATERMARK)
    verify(pobNativeAdLoader).loadAd(BID_RESPONSE, POBBiddingHost.ADMOB)
  }

  // endregion

  private companion object {
    const val TEST_PUBLISHER_ID = "a_pubmatic_publisher_id"
    const val TEST_PROFILE_ID_1 = "1234"
    const val TEST_PROFILE_ID_2 = "5678"
    // Profile ID should be parsable as an integer.
    const val INVALID_PROFILE_ID = "a123"
    const val TEST_AD_UNIT = "test_ad_unit"
    const val OPENWRAP_INIT_ERROR_CODE = 1001
    const val OPENWRAP_INIT_ERROR_MSG = "Init failed"
    const val PUBMATIC_SIGNALS = "PubMatic-SDK-collected signals"
    const val BID_RESPONSE = "bid response"
    const val WATERMARK = "test watermark"
  }
}
