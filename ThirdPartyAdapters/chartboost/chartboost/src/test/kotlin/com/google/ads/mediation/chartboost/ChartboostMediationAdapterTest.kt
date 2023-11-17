package com.google.ads.mediation.chartboost

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.Chartboost.getSDKVersion
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.createChartboostParams
import com.google.ads.mediation.chartboost.ChartboostAdapterUtils.getAdapterVersion
import com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.chartboost.ChartboostInitializer.getInstance
import com.google.ads.mediation.chartboost.ChartboostMediationAdapter.ERROR_MESSAGE_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.chartboost.ChartboostMediationAdapter.ERROR_MESSAGE_MISSING_OR_INVALID_APP_ID
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [ChartboostMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class ChartboostMediationAdapterTest {

  private val mediationConfiguration: MediationConfiguration = mock()
  private val mediationConfiguration1: MediationConfiguration = mock()
  private val initializationCompleteCallback: InitializationCompleteCallback = mock()
  private val chartboostInitializer: ChartboostInitializer = mock()
  private val context = ApplicationProvider.getApplicationContext<Context>()

  private lateinit var adapter: ChartboostMediationAdapter

  @Before
  fun setUp() {
    adapter = ChartboostMediationAdapter()
  }

  @Test
  fun getVersionInfo_valid4Digits_returnsCorrectVersion() {
    mockStatic(ChartboostAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_valid5Digits_returnsCorrectVersion() {
    mockStatic(ChartboostAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4.5"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_invalidVersion_returnsZero() {
    mockStatic(ChartboostAdapterUtils::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getSDKVersionInfo_valid3Digits_returnsCorrectVersion() {
    mockStatic(Chartboost::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2.3"

      adapter.assertGetSdkVersion(expectedValue = "1.2.3")
    }
  }

  @Test
  fun getSDKVersionInfo_valid4Digits_returnsCorrectVersion() {
    mockStatic(Chartboost::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2.3.4"

      adapter.assertGetSdkVersion(expectedValue = "1.2.3")
    }
  }

  @Test
  fun getSDKVersionInfo_invalidSDKVersion_returnsZero() {
    mockStatic(Chartboost::class.java).use {
      whenever(getSDKVersion()) doReturn "1.2"

      adapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  @Test
  fun initialize_invalidAppId_invokesOnInitializationFailedCallback() {
    val invalidServerParameters = bundleOf(ChartboostAdapterUtils.KEY_APP_ID to "")
    whenever(mediationConfiguration.serverParameters).thenReturn(invalidServerParameters)
    // Create an AdError object so that it can be verified that this object's toString() matches the
    // error string that's passed to the initialization callback.
    val adError =
      ChartboostConstants.createAdapterError(
        ERROR_INVALID_SERVER_PARAMETERS,
        ERROR_MESSAGE_MISSING_OR_INVALID_APP_ID
      )

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    val captor = argumentCaptor<String>()
    verify(initializationCompleteCallback).onInitializationFailed(captor.capture())
    assertThat(captor.firstValue).isEqualTo(adError.toString())
  }

  @Test
  fun initialize_nullChartboostParam_invokesOnInitializationFailedCallback() {
    mockStatic(ChartboostAdapterUtils::class.java).use {
      // Theoretically this would never happen because we always new an instance of
      // ChartBoostParams, so we can only mock the method to test the code blocks.
      whenever(createChartboostParams(any())) doReturn null

      val serverParameters = Bundle()
      serverParameters.putString(ChartboostAdapterUtils.KEY_APP_ID, "app_id")
      whenever(mediationConfiguration.serverParameters).thenReturn(serverParameters)

      val adError =
        ChartboostConstants.createAdapterError(
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_MESSAGE_INVALID_SERVER_PARAMETERS
        )

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      val captor = argumentCaptor<String>()
      verify(initializationCompleteCallback).onInitializationFailed(captor.capture())
      assertThat(captor.firstValue).isEqualTo(adError.toString())
    }
  }

  @Test
  fun initialize_emptyAppSignature_invokesOnInitializationFailedCallback() {
    val serverParameters = bundleOf(ChartboostAdapterUtils.KEY_APP_ID to "app_id")
    whenever(mediationConfiguration.serverParameters).thenReturn(serverParameters)

    val adError =
      ChartboostConstants.createAdapterError(
        ERROR_INVALID_SERVER_PARAMETERS,
        ERROR_MESSAGE_INVALID_SERVER_PARAMETERS
      )

    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

    val captor = argumentCaptor<String>()
    verify(initializationCompleteCallback).onInitializationFailed(captor.capture())
    assertThat(captor.firstValue).isEqualTo(adError.toString())
  }

  @Test
  fun initialize_chartboostInitializerSucceeded_invokesOnInitializationSucceedCallback() {
    val serverParameters =
      bundleOf(
        ChartboostAdapterUtils.KEY_APP_ID to "app_id",
        ChartboostAdapterUtils.KEY_APP_SIGNATURE to "app_signature"
      )
    whenever(mediationConfiguration.serverParameters).thenReturn(serverParameters)

    mockStatic(ChartboostInitializer::class.java).use {
      whenever(getInstance()) doReturn chartboostInitializer
      whenever(chartboostInitializer.initialize(any(), any(), any())).doAnswer {
        val listener = it.arguments[2] as ChartboostInitializer.Listener
        listener.onInitializationSucceeded()
      }

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      verify(initializationCompleteCallback).onInitializationSucceeded()
    }
  }

  @Test
  fun initialize_chartboostInitializerSucceeded2AppIds_invokesOnInitializationSucceedCallback() {
    val serverParameters1 =
      bundleOf(
        ChartboostAdapterUtils.KEY_APP_ID to "app_id_foo",
        ChartboostAdapterUtils.KEY_APP_SIGNATURE to "app_signature_foo"
      )
    whenever(mediationConfiguration1.serverParameters).thenReturn(serverParameters1)

    val serverParameters =
      bundleOf(
        ChartboostAdapterUtils.KEY_APP_ID to "app_id_bar",
        ChartboostAdapterUtils.KEY_APP_SIGNATURE to "app_signature_bar"
      )
    whenever(mediationConfiguration.serverParameters).thenReturn(serverParameters)

    mockStatic(ChartboostInitializer::class.java).use {
      whenever(getInstance()) doReturn chartboostInitializer
      whenever(chartboostInitializer.initialize(any(), any(), any())).doAnswer {
        val listener = it.arguments[2] as ChartboostInitializer.Listener
        listener.onInitializationSucceeded()
      }

      adapter.initialize(
        context,
        initializationCompleteCallback,
        listOf(mediationConfiguration1, mediationConfiguration)
      )

      verify(initializationCompleteCallback).onInitializationSucceeded()
    }
  }

  @Test
  fun initialize_chartboostInitializerFailed_invokesOnInitializationFailedCallback() {
    val serverParameters =
      bundleOf(
        ChartboostAdapterUtils.KEY_APP_ID to "app_id",
        ChartboostAdapterUtils.KEY_APP_SIGNATURE to "app_signature"
      )
    val adError =
      ChartboostConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS, "error_message")

    whenever(mediationConfiguration.serverParameters).thenReturn(serverParameters)

    mockStatic(ChartboostInitializer::class.java).use {
      whenever(getInstance()) doReturn chartboostInitializer
      whenever(chartboostInitializer.initialize(any(), any(), any())).doAnswer {
        val listener = it.arguments[2] as ChartboostInitializer.Listener

        listener.onInitializationFailed(adError)
      }

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      verify(initializationCompleteCallback).onInitializationFailed(adError.toString())
    }
  }

  @Test
  fun initialize_setValidAppParam_initializerSucceeded_invokesOnInitializationSucceedCallback() {
    ChartboostMediationAdapter.setAppParams("app_id", "app_signature")

    mockStatic(ChartboostInitializer::class.java).use {
      whenever(getInstance()) doReturn chartboostInitializer
      whenever(chartboostInitializer.initialize(any(), any(), any())).doAnswer {
        val listener = it.arguments[2] as ChartboostInitializer.Listener
        listener.onInitializationSucceeded()
      }

      adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))

      verify(initializationCompleteCallback).onInitializationSucceeded()
    }
  }
}
