package com.google.ads.mediation.ironsource

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.createMediationConfiguration
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.ironsource.IronSourceAdapterUtils.getAdapterVersion
import com.google.ads.mediation.ironsource.IronSourceConstants.KEY_APP_KEY
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSource.initISDemandOnly
import com.ironsource.mediationsdk.utils.IronSourceUtils
import com.ironsource.mediationsdk.utils.IronSourceUtils.getSDKVersion
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [IronSourceMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class IronSourceMediationAdapterTest {

  private lateinit var adapter: IronSourceMediationAdapter

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()

  @Before
  fun setUp() {
    adapter = IronSourceMediationAdapter()
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor3Digits_returnsTheSameVersion() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "7.3.2"

      adapter.assertGetSdkVersion(expectedValue = "7.3.2")
    }
  }

  @Test
  fun getSDKVersionInfo_validSDKVersionFor4Digits_returnsTheSameVersion() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "7.3.2.1"

      adapter.assertGetSdkVersion(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZeros() {
    mockStatic(IronSourceUtils::class.java).use {
      whenever(getSDKVersion()) doReturn "3.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_validVersionWith4Digits_returnsTheSameVersion() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2.1"

      adapter.assertGetVersionInfo(expectedValue = "7.3.201")
    }
  }

  @Test
  fun getVersionInfo_validVersionWith5Digits_returnsTheSameVersion() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2.1.8"

      adapter.assertGetVersionInfo(expectedValue = "7.3.20108")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZeros() {
    mockStatic(IronSourceAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "7.3.2"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun initialize_withNoAppKeyInServerParameters_invokesOnInitializationFailed() {
    val mediationConfiguration = createMediationConfiguration(AdFormat.BANNER)

    adapter.initialize(context, mockInitializationCompleteCallback, listOf(mediationConfiguration))

    verify(mockInitializationCompleteCallback)
      .onInitializationFailed(MISSING_OR_INVALID_APP_KEY_MESSAGE)
  }

  @Test
  fun initialize_withEmptyAppKey_invokesOnInitializationFailed() {
    adapter.mediationAdapterInitializeVerifyFailure(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(KEY_APP_KEY to ""),
      /* expectedError= */ MISSING_OR_INVALID_APP_KEY_MESSAGE
    )
  }

  @Test
  fun initialize_withMediationConfigurations_invokesOnInitializationSucceeded() {
    adapter.mediationAdapterInitializeVerifySuccess(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(KEY_APP_KEY to TEST_APP_ID_1)
    )
  }

  @Test
  fun initialize_withMultipleMediationConfigurations_invokesOnInitializationSucceededOnlyOnce() {
    mockStatic(IronSource::class.java).use {
      val mediationConfiguration1 =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_1)
        )
      val mediationConfiguration2 =
        createMediationConfiguration(
          AdFormat.BANNER,
          serverParameters = bundleOf(KEY_APP_KEY to TEST_APP_ID_2)
        )

      adapter.initialize(
        context,
        mockInitializationCompleteCallback,
        listOf(mediationConfiguration1, mediationConfiguration2)
      )

      verify(mockInitializationCompleteCallback).onInitializationSucceeded()
      it.verify {
        initISDemandOnly(
          any(),
          argThat { appKey -> listOf(TEST_APP_ID_1, TEST_APP_ID_2).contains(appKey) },
          eq(IronSource.AD_UNIT.INTERSTITIAL),
          eq(IronSource.AD_UNIT.REWARDED_VIDEO),
          eq(IronSource.AD_UNIT.BANNER)
        )
      }
    }
  }

  @Test
  fun initialize_alreadyInitialized_invokesOnInitializationSucceededOnlyOnce() {
    adapter.setIsInitialized(true)

    adapter.initialize(
      context,
      mockInitializationCompleteCallback,
      /* mediationConfigurations= */ listOf()
    )

    verify(mockInitializationCompleteCallback).onInitializationSucceeded()
  }

  @After
  fun tearDown() {
    adapter.setIsInitialized(false)
  }

  private fun createMediationConfiguration(
    adFormat: AdFormat,
    serverParameters: Bundle = bundleOf(),
  ) = MediationConfiguration(adFormat, serverParameters)

  private companion object {
    const val TEST_APP_ID_1 = "testAppId1"
    const val TEST_APP_ID_2 = "testAppId2"
    const val MISSING_OR_INVALID_APP_KEY_MESSAGE = "Missing or invalid app key."
  }
}
