// Copyright 2023 Google LLC
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

package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.core.os.bundleOf
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.adaptertestkit.loadAppOpenAdWithFailure
import com.google.ads.mediation.adaptertestkit.loadRtbAppOpenAdWithFailure
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.MintegralUtils.getAdapterVersion
import com.google.ads.mediation.mintegral.MintegralUtils.getSdkVersion
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MintegralMediationAdapterTest {
  // Subject of tests
  private var mintegralMediationAdapter = MintegralMediationAdapter()

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockAppOpenAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock()

  @Before
  fun setUp() {
    mintegralMediationAdapter = MintegralMediationAdapter()
  }

  // region version tests
  @Test
  fun getSdkVersion_returnsCorrectVersion() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getSdkVersion()) doReturn "TEST_3.2.1"

      mintegralMediationAdapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getSdkVersion_withoutUnderscoreDivider_returnsZeroes() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getSdkVersion()) doReturn "3.2.1"

      mintegralMediationAdapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSdkVersion_withInvalidValues_returnsZeroes() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getSdkVersion()) doReturn "TEST_3.2"

      mintegralMediationAdapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_returnsTheSameVersion() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "3.2.1.0"

      mintegralMediationAdapter.assertGetVersionInfo(expectedValue = "3.2.100")
    }
  }

  @Test
  fun getVersionInfo_withInvalidValue_returnsZeroes() {
    mockStatic(MintegralUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "3.2.1"

      mintegralMediationAdapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  // endregion

  // region AppOpen Ad Tests
  @Test
  fun loadAppOpenAd_withoutAdUnitId_invokesOnFailure() {
    val mediationAppOpenAdConfiguration = createMediationAppOpenAdConfiguration(context = context)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadAppOpenAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadAppOpenAd_withoutPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadAppOpenAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadAppOpenAd_preLoadsSplashAd() {
    mockStatic(MintegralFactory::class.java).use {
      val mockSplashAd = mock<MintegralSplashAdWrapper>()
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn mockSplashAd
      val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
      val mediationAppOpenAdConfiguration =
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters = serverParameters
        )

      mintegralMediationAdapter.loadAppOpenAd(
        mediationAppOpenAdConfiguration,
        mockAppOpenAdLoadCallback
      )

      verify(mockSplashAd).createAd(TEST_PLACEMENT_ID, TEST_AD_UNIT)
      verify(mockSplashAd).setSplashLoadListener(any())
      verify(mockSplashAd).setSplashShowListener(any())
      verify(mockSplashAd).preLoad()
    }
  }

  @Test
  fun loadRtbAppOpenAd_withoutAdUnitId_invokesOnFailure() {
    val mediationAppOpenAdConfiguration = createMediationAppOpenAdConfiguration(context = context)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadRtbAppOpenAd_withEmptyAdUnitId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid ad Unit ID configured for this ad source instance in the AdMob or Ad" +
          " Manager UI."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadRtbAppOpenAd_withoutPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT)
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadRtbAppOpenAd_withEmptyPlacementId_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to "")
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
        ("Missing or invalid Placement ID configured for this ad source instance in the" +
          " AdMob or Ad Manager UI."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadRtbAppOpenAd_withEmptyBidResponse_invokesOnFailure() {
    val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
    val mediationAppOpenAdConfiguration =
      createMediationAppOpenAdConfiguration(context = context, serverParameters = serverParameters)
    val expectedError =
      AdError(
        MintegralConstants.ERROR_INVALID_BID_RESPONSE,
        ("Missing or invalid Mintegral bidding signal in this ad request."),
        MintegralConstants.ERROR_DOMAIN
      )

    mintegralMediationAdapter.loadRtbAppOpenAdWithFailure(
      mediationAppOpenAdConfiguration,
      mockAppOpenAdLoadCallback,
      expectedError
    )
  }

  @Test
  fun loadRtbAppOpenAd_invokesPreLoadByToken() {
    mockStatic(MintegralFactory::class.java).use {
      val mockSplashAd = mock<MintegralSplashAdWrapper>()
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn mockSplashAd
      val serverParameters = bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
      val mediationAppOpenAdConfiguration =
        createMediationAppOpenAdConfiguration(
          context = context,
          serverParameters = serverParameters,
          bidResponse = TEST_BID_RESPONSE,
          watermark = TEST_WATERMARK
        )

      mintegralMediationAdapter.loadRtbAppOpenAd(
        mediationAppOpenAdConfiguration,
        mockAppOpenAdLoadCallback
      )

      verify(mockSplashAd).setExtraInfo(any())
      verify(mockSplashAd).createAd(TEST_PLACEMENT_ID, TEST_AD_UNIT)
      verify(mockSplashAd).setSplashLoadListener(any())
      verify(mockSplashAd).setSplashShowListener(any())
      verify(mockSplashAd).preLoadByToken(TEST_BID_RESPONSE)
    }
  }

  // endregion
}
