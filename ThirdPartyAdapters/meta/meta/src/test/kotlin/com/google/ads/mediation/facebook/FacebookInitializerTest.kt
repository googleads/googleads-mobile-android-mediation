package com.google.ads.mediation.facebook

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.AudienceNetworkAds.InitResult
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FACEBOOK_INITIALIZATION
import com.google.android.gms.ads.AdError
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [FacebookInitializer]. */
@RunWith(AndroidJUnit4::class)
class FacebookInitializerTest {

  /** Unit under test. */
  private val facebookInitializer = FacebookInitializer.getInstance()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val metaInitSettingsBuilder =
    mock<AudienceNetworkAds.InitSettingsBuilder> {
      on { withMediationService(any()) } doReturn it
      on { withPlacementIds(any()) } doReturn it
      on { withInitListener(any()) } doReturn it
    }

  @Test
  fun initialize_initializesMetaSdkWithCorrectConfig() {
    Mockito.mockStatic(AudienceNetworkAds::class.java).use {
      whenever(AudienceNetworkAds.buildInitSettings(any())) doReturn metaInitSettingsBuilder

      facebookInitializer.initialize(context, TEST_PLACEMENT_ID, mock())
    }

    verify(metaInitSettingsBuilder).withMediationService("GOOGLE:" + BuildConfig.ADAPTER_VERSION)
    verify(metaInitSettingsBuilder).withPlacementIds(listOf(TEST_PLACEMENT_ID))
    verify(metaInitSettingsBuilder).withInitListener(facebookInitializer)
    verify(metaInitSettingsBuilder).initialize()
  }

  @Test
  fun initialize_withMultiplePlacementIds_initializesMetaSdkWithMultiplePlacementIds() {
    Mockito.mockStatic(AudienceNetworkAds::class.java).use {
      whenever(AudienceNetworkAds.buildInitSettings(any())) doReturn metaInitSettingsBuilder

      facebookInitializer.initialize(
        context,
        arrayListOf(TEST_PLACEMENT_ID, ANOTHER_PLACEMENT_ID),
        mock()
      )
    }

    verify(metaInitSettingsBuilder).withMediationService("GOOGLE:" + BuildConfig.ADAPTER_VERSION)
    verify(metaInitSettingsBuilder)
      .withPlacementIds(listOf(TEST_PLACEMENT_ID, ANOTHER_PLACEMENT_ID))
    verify(metaInitSettingsBuilder).withInitListener(facebookInitializer)
    verify(metaInitSettingsBuilder).initialize()
  }

  @Test
  fun initialize_calledTwice_initializesMetaSdkOnlyOnce() {
    Mockito.mockStatic(AudienceNetworkAds::class.java).use {
      whenever(AudienceNetworkAds.buildInitSettings(any())) doReturn metaInitSettingsBuilder

      facebookInitializer.initialize(context, TEST_PLACEMENT_ID, mock())
      facebookInitializer.initialize(context, ANOTHER_PLACEMENT_ID, mock())
    }

    verify(metaInitSettingsBuilder, times(1)).initialize()
  }

  @Test
  fun initialize_ifMetaSdkIsAlreadyInitialized_invokesOnInitializeSuccessWithoutCallingInitializeOnMetaSdk() {
    facebookInitializer.onInitialized(mock { on { isSuccess } doReturn true })
    val initializationListener = mock<FacebookInitializer.Listener>()
    Mockito.mockStatic(AudienceNetworkAds::class.java).use {
      whenever(AudienceNetworkAds.buildInitSettings(any())) doReturn metaInitSettingsBuilder

      facebookInitializer.initialize(context, TEST_PLACEMENT_ID, initializationListener)
    }

    verify(initializationListener).onInitializeSuccess()
    verify(metaInitSettingsBuilder, times(0)).initialize()
  }

  @Test
  fun onInitialized_forInitializationSuccess_invokesOnInitializeSuccessOnAllListeners() {
    val initializationListener1 = mock<FacebookInitializer.Listener>()
    val initializationListener2 = mock<FacebookInitializer.Listener>()
    facebookInitializer.initialize(context, TEST_PLACEMENT_ID, initializationListener1)
    facebookInitializer.initialize(context, ANOTHER_PLACEMENT_ID, initializationListener2)

    facebookInitializer.onInitialized(mock { on { isSuccess } doReturn true })

    verify(initializationListener1).onInitializeSuccess()
    verify(initializationListener2).onInitializeSuccess()
  }

  @Test
  fun onInitialized_forInitializationFailure_invokesOnInitializeErrorOnAllListeners() {
    val initializationListener1 = mock<FacebookInitializer.Listener>()
    val initializationListener2 = mock<FacebookInitializer.Listener>()
    facebookInitializer.initialize(context, TEST_PLACEMENT_ID, initializationListener1)
    facebookInitializer.initialize(context, ANOTHER_PLACEMENT_ID, initializationListener2)
    val metaInitResult =
      mock<InitResult> {
        on { isSuccess } doReturn false
        on { message } doReturn "Meta SDK initialization failed."
      }

    facebookInitializer.onInitialized(metaInitResult)

    val expectedAdError =
      AdError(ERROR_FACEBOOK_INITIALIZATION, "Meta SDK initialization failed.", ERROR_DOMAIN)
    verify(initializationListener1).onInitializeError(argThat(AdErrorMatcher(expectedAdError)))
    verify(initializationListener2).onInitializeError(argThat(AdErrorMatcher(expectedAdError)))
  }

  @After
  fun tearDown() {
    // Call facebookInitializer.onInitialized with failed InitResult in tearDown() to clear all the
    // initializer listeners and reset isInitializing and isInitialized to false after each test.
    facebookInitializer.onInitialized(mock { on { isSuccess } doReturn false })
  }

  companion object {
    private const val ANOTHER_PLACEMENT_ID = "anotherPlacementId"
  }
}
